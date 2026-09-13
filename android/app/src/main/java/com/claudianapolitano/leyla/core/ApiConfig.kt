package com.claudianapolitano.leyla.core

import coil.request.ImageRequest
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

object ApiConfig {
    /**
     * Base URL of the Leyla backend, shared with the widget via [SharedConfig].
     *
     * For local backend development, temporarily point [SharedConfig.API_BASE_URL]
     * at your Mac's LAN IP, e.g. `http://192.168.3.8:8080` (and allow cleartext
     * for it in network_security_config.xml).
     */
    val baseUrl: String get() = SharedConfig.API_BASE_URL

    /**
     * Avatar and media endpoints are authenticated, so Coil has to carry the
     * bearer token the same way [ApiClient] does.
     */
    @Composable
    fun imageRequest(path: String): ImageRequest {
        val context = LocalContext.current
        val builder = ImageRequest.Builder(context)
            .data(baseUrl + path)
            .crossfade(true)
        TokenStore.accessToken?.let { builder.addHeader("Authorization", "Bearer $it") }
        return builder.build()
    }
}
