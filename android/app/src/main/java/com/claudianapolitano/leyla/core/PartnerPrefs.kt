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
        get() = PartnerPronoun.from(stored)
        set(value) {
            if (::prefs.isInitialized) prefs.edit().putString(KEY_PRONOUN, value.wire).apply()
        }

    /**
     * Whether the person actually picked one, as opposed to reading the "they"
     * default. iOS keeps this distinction by storing the pronoun as optional;
     * here [pronoun] has to answer with something, so the raw value is what
     * says whether the question was ever asked — which is what decides whether
     * the setup flow runs.
     */
    val hasChosenPronoun: Boolean get() = stored != null

    private val stored: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_PRONOUN, null) else null
}
