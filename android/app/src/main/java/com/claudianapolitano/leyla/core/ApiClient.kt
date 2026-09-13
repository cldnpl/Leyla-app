package com.claudianapolitano.leyla.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * Talks to the Leyla backend. Port of `Us/Core/APIClient.swift`: one entry
 * point, a bearer token pulled from [TokenStore], and a single transparent
 * refresh-and-retry on a 401.
 */
object ApiClient {

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val http = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
    }

    /** Serialises refreshes so a burst of 401s rotates the token only once. */
    private val refreshLock = Mutex()

    /**
     * The language the server should localise its payloads into. iOS reads this
     * from its in-app language picker; until that screen is ported, follow the
     * device.
     */
    var languageCode: String = Locale.getDefault().language

    suspend inline fun <reified T> get(path: String): T = send(path, HttpMethod.Get, null)

    suspend inline fun <reified T> post(path: String, body: Any? = null): T =
        send(path, HttpMethod.Post, body)

    suspend inline fun <reified T> send(path: String, method: HttpMethod, body: Any?): T =
        json.decodeFromString(raw(path, method, body))

    /** Fire-and-forget variant for endpoints that answer with an empty body. */
    suspend fun sendVoid(path: String, method: HttpMethod, body: Any? = null) {
        raw(path, method, body)
    }

    /**
     * Performs the request and returns the response body as text, retrying once
     * after a successful token rotation. Everything else surfaces as an
     * [ApiException] — or [UnauthorizedException] when the refresh token itself
     * is gone or rejected, the one case that means "sign out".
     */
    suspend fun raw(
        path: String,
        method: HttpMethod,
        body: Any? = null,
        authorized: Boolean = true,
        retryOn401: Boolean = true,
    ): String {
        val response = execute(path, method, body, authorized)
        if (response.status.value == 401 && authorized && retryOn401) {
            if (refresh()) return raw(path, method, body, authorized, retryOn401 = false)
            throw UnauthorizedException()
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(response.status.value, decodeError(text))
        }
        return text
    }

    private suspend fun execute(
        path: String,
        method: HttpMethod,
        body: Any?,
        authorized: Boolean,
    ): HttpResponse = http.request(ApiConfig.baseUrl + path) {
        this.method = method
        // Every endpoint takes `lang`, so it is attached here rather than at the
        // call sites — that way no handler can accidentally omit it.
        parameter("lang", languageCode)
        if (authorized) {
            TokenStore.accessToken?.let { header("Authorization", "Bearer $it") }
        }
        if (body != null) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    /** Attempts to rotate the refresh token. Returns true on success. */
    private suspend fun refresh(): Boolean = refreshLock.withLock {
        val rt = TokenStore.refreshToken ?: return false
        val response = execute(
            "/v1/auth/refresh",
            HttpMethod.Post,
            mapOf("refreshToken" to rt),
            authorized = false,
        )
        if (!response.status.isSuccess()) {
            // Only a definitive rejection of the refresh token is a real
            // sign-out; a 500 or a timeout leaves the session alone.
            if (response.status.value == 401) TokenStore.clear()
            return false
        }
        val auth: AuthResponse = json.decodeFromString(response.bodyAsText())
        TokenStore.save(auth.accessToken, auth.refreshToken)
        return true
    }

    private fun decodeError(text: String): ApiErrorResponse? = runCatching {
        json.decodeFromString<ApiErrorResponse>(text)
    }.getOrNull()
}
