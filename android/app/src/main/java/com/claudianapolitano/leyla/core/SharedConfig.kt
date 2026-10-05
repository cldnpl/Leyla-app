package com.claudianapolitano.leyla.core

/**
 * Constants shared between the app and (later) the home-screen widget. Mirrors
 * the iOS `SharedConfig` enum so both platforms talk to the same backend and
 * agree on what demo mode means.
 */
object SharedConfig {
    /** Base URL of the Leyla backend. Kept in sync with iOS's `SharedConfig`. */
    const val API_BASE_URL = "https://us-app-production-9aa4.up.railway.app"

    /**
     * TEST ONLY: enables the "0000" pairing bypass + demo data (Claudia/Alex,
     * Naples/Tashkent) so the app can be tested without a real partner — even in
     * a release build. Set to `false` before any public launch.
     */
    const val DEMO_MODE = true
}
