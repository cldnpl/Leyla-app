package com.claudianapolitano.leyla.core

import android.content.Context
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class TranslationsResponse(val lang: String, val strings: Map<String, String> = emptyMap())

/**
 * The UI string table for the current language, fetched from the backend and
 * cached on disk. Port of `Us/Core/TranslationStore.swift`.
 *
 * The table is keyed by the **English string itself**, which is how iOS asks
 * for it and therefore how the backend is shaped. Android normally resolves
 * strings through `values-xx/strings.xml`, but those would have to ship in the
 * APK; the whole point here is that a translation added on the server reaches
 * both apps without a release. So Android keeps one English `strings.xml` and
 * translates its *values* through this table — see [tr].
 */
object Translations {

    private val _strings = MutableStateFlow<Map<String, String>>(emptyMap())
    val strings: StateFlow<Map<String, String>> = _strings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var cacheDir: File? = null

    fun init(context: Context) {
        cacheDir = File(context.cacheDir, "translations").apply { mkdirs() }
    }

    /**
     * Loads the disk cache synchronously if present, so a relaunch in a
     * non-English language shows translated text immediately rather than
     * flashing English while the fetch is in flight. Does not fetch.
     */
    fun loadCachedIfAvailable(lang: String) {
        readCache(lang)?.let { _strings.value = it }
    }

    /**
     * Fetches the table, publishes it and writes the cache. A network failure
     * falls back to whatever is on disk; if there is nothing, English stays —
     * the keys *are* English, so an empty table is a working app, not a broken
     * one.
     */
    suspend fun load(lang: String) {
        _isLoading.value = true
        try {
            val response: TranslationsResponse = ApiClient.send(
                "/v1/translations/$lang",
                HttpMethod.Get,
                null,
                authorized = false,
            )
            _strings.value = response.strings
            writeCache(lang, response.strings)
        } catch (_: Exception) {
            val cached = readCache(lang)
            // Do not leave the previous language's table active after a failed
            // switch: falling back to English is honest, showing the language
            // you just left is not.
            _strings.value = cached ?: emptyMap()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Translates one English string. Unknown keys come back unchanged, which is
     * the correct answer: the key is already the English text.
     */
    fun tr(english: String): String = _strings.value[english] ?: english

    // MARK: - Disk cache

    private fun cacheFile(lang: String): File? = cacheDir?.let { File(it, "$lang.json") }

    private fun readCache(lang: String): Map<String, String>? {
        val file = cacheFile(lang)?.takeIf { it.exists() } ?: return null
        return runCatching {
            ApiClient.json.decodeFromString<Map<String, String>>(file.readText())
        }.getOrNull()
    }

    private suspend fun writeCache(lang: String, strings: Map<String, String>) {
        val file = cacheFile(lang) ?: return
        withContext(Dispatchers.IO) {
            runCatching { file.writeText(ApiClient.json.encodeToString(strings)) }
        }
    }
}

/**
 * Bridges Android's placeholder syntax to the one the shared table uses.
 *
 * The table came from iOS, so its keys spell arguments the Objective-C way:
 * `%@` for the first, `%1$@`/`%2$@` when there is more than one. Android's
 * `strings.xml` spells the same thing `%1$s` or `%d`. These two functions are
 * the only place that difference exists — everywhere else a string is just a
 * string.
 */
internal object Placeholders {

    private val androidArg = Regex("""%(\d+\$)?[sdf]""")
    private val iosArg = Regex("""%(\d+\$)?@""")

    /** "Day %1${'$'}d · next period %2${'$'}s" → "Day %1${'$'}@ · next period %2${'$'}@" */
    fun toIos(template: String): String {
        var index = 0
        return androidArg.replace(template) { match ->
            index++
            val explicit = match.groupValues[1]
            if (explicit.isNotEmpty()) "%$explicit@" else "%$index\$@"
        }.let { if (index == 1) it.replace("%1\$@", "%@") else it }
    }

    /** "%1${'$'}@ giorni insieme" → "%1${'$'}s giorni insieme", ready for String.format. */
    fun toAndroid(template: String): String {
        var index = 0
        return iosArg.replace(template) { match ->
            index++
            val explicit = match.groupValues[1]
            if (explicit.isNotEmpty()) "%${explicit}s" else "%$index\$s"
        }
    }
}
