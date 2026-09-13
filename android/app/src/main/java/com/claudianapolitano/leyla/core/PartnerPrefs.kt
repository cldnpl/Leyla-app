package com.claudianapolitano.leyla.core

import android.content.Context
import android.content.SharedPreferences

/** How this user refers to their partner. Drives the hero button's copy. */
enum class PartnerPronoun(val wire: String) {
    SHE("she"), HE("he"), THEY("they");

    companion object {
        fun from(wire: String?): PartnerPronoun =
            entries.firstOrNull { it.wire == wire } ?: THEY
    }
}

/**
 * Small preferences the app and (later) the widget both read. Mirrors iOS's
 * `PartnerPrefs`, which lives in the shared App Group container.
 */
object PartnerPrefs {
    private const val FILE = "leyla_partner_prefs"
    private const val KEY_PRONOUN = "partnerPronoun"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }

    var pronoun: PartnerPronoun
        get() = PartnerPronoun.from(
            if (::prefs.isInitialized) prefs.getString(KEY_PRONOUN, null) else null
        )
        set(value) {
            if (::prefs.isInitialized) prefs.edit().putString(KEY_PRONOUN, value.wire).apply()
        }
}
