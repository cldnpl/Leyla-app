package com.claudianapolitano.leyla.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Where the access/refresh pair lives. The iOS build keeps these in the
 * Keychain; the Android equivalent is an encrypted preference file backed by a
 * key in the hardware keystore.
 *
 * Unlike the Keychain, this file is wiped when the app is uninstalled, so the
 * "stale tokens after reinstall" dance iOS's `Session.bootstrap()` has to do
 * has no counterpart here.
 */
object TokenStore {
    private const val FILE = "leyla_tokens"
    private const val KEY_ACCESS = "accessToken"
    private const val KEY_REFRESH = "refreshToken"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var accessToken: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_ACCESS, null) else null
        set(value) = write(KEY_ACCESS, value)

    var refreshToken: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_REFRESH, null) else null
        set(value) = write(KEY_REFRESH, value)

    fun save(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }

    fun clear() {
        if (::prefs.isInitialized) prefs.edit().clear().apply()
    }

    private fun write(key: String, value: String?) {
        if (!::prefs.isInitialized) return
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}
