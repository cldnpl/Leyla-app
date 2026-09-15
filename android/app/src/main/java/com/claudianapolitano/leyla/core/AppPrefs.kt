package com.claudianapolitano.leyla.core

import android.content.Context
import android.content.SharedPreferences

/**
 * The few flags that decide where the app opens. Port of the `UserDefaults`
 * keys `Us/Core/Session.swift` reads: whether the "about you" step is done, and
 * whether the demo pairing bypass was used.
 *
 * Deliberately not in [TokenStore]: that file is wiped on sign-out, and where
 * someone got to in onboarding is not a credential.
 */
object AppPrefs {
    private const val FILE = "leyla_app_prefs"
    private const val KEY_ONBOARDING_DONE = "didCompletePersonalOnboarding"
    private const val KEY_TEST_PAIRED = "testPaired"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }

    /**
     * Whether the person has completed the "about you" onboarding (name, cycle,
     * location), which runs once after sign-in and before pairing.
     */
    var personalOnboardingDone: Boolean
        get() = ::prefs.isInitialized && prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) {
            if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()
        }

    /** Whether the "0000" demo bypass was used, so a relaunch goes to Home. */
    var testPaired: Boolean
        get() = ::prefs.isInitialized && prefs.getBoolean(KEY_TEST_PAIRED, false)
        set(value) {
            if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_TEST_PAIRED, value).apply()
        }

    /** Clears what belongs to the account. Onboarding progress is device-level. */
    fun clearAccountState() {
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_TEST_PAIRED, false).apply()
    }
}
