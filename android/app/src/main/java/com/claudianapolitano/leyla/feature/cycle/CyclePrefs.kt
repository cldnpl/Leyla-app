package com.claudianapolitano.leyla.feature.cycle

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Local persistence for the cycle feature's personal flags. Port of `CyclePrefs`
 * in `Us/Features/Cycle/CycleManager.swift`.
 *
 * Everything in here belongs to *this phone*, not to the account signed in on
 * it: the Health Connect permission is granted to the app by the person holding
 * the device, and Android does not revoke it on sign-out. So none of these keys
 * are cleared when the user signs out — otherwise the cycle would vanish and
 * have to be set up again on the way back in.
 */
object CyclePrefs {

    private const val FILE = "leyla.cycle"
    private const val KEY_HAS_CYCLE = "userHasCycle"
    private const val KEY_PREGNANT = "isPregnant"
    private const val KEY_DUE_DATE = "pregnancyDueDate"
    private const val KEY_SHARE_LEVEL = "cycleShareLevel"
    private const val KEY_PERIOD_STARTS = "cycleCachedPeriodStarts"
    private const val KEY_NOTE_PREFIX = "cycleNote."

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }

    /** ISO dates are the storage format, so the user's locale cannot shift them. */
    private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** null until the user answers (in onboarding or from the cycle screen). */
    var userHasCycle: Boolean?
        get() = if (prefs.contains(KEY_HAS_CYCLE)) prefs.getBoolean(KEY_HAS_CYCLE, false) else null
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_HAS_CYCLE) else putBoolean(KEY_HAS_CYCLE, value)
        }.apply()

    var isPregnant: Boolean
        get() = prefs.getBoolean(KEY_PREGNANT, false)
        set(value) = prefs.edit().putBoolean(KEY_PREGNANT, value).apply()

    var dueDate: LocalDate?
        get() = prefs.getString(KEY_DUE_DATE, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_DUE_DATE) else putString(KEY_DUE_DATE, value.format(isoDate))
        }.apply()

    var shareLevel: CycleShareLevel
        get() = CycleShareLevel.from(prefs.getString(KEY_SHARE_LEVEL, null))
        set(value) = prefs.edit().putString(KEY_SHARE_LEVEL, value.wire).apply()

    /**
     * The period-start dates last read from Health Connect, kept on this phone
     * so the cycle still renders when a live read isn't possible — at launch
     * before the provider answers, offline, and right after signing back in.
     *
     * The cache never leaves the device; the only thing uploaded is the summary
     * the user explicitly chooses to share with her partner.
     */
    var cachedPeriodStarts: List<LocalDate>
        get() = prefs.getStringSet(KEY_PERIOD_STARTS, emptySet())
            .orEmpty()
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .sorted()
        set(value) = prefs.edit()
            .putStringSet(KEY_PERIOD_STARTS, value.map { it.format(isoDate) }.toSet())
            .apply()

    /**
     * Today's note. The key is a fixed ISO date rather than a localized one: a
     * Thai or Persian locale would otherwise render a different calendar's year
     * and silently point at a different note every day.
     */
    private fun noteKey(day: LocalDate) = KEY_NOTE_PREFIX + day.format(isoDate)

    fun note(day: LocalDate = LocalDate.now()): String = prefs.getString(noteKey(day), "").orEmpty()

    fun saveNote(text: String, day: LocalDate = LocalDate.now()) {
        val editor = prefs.edit()
        if (text.isBlank()) editor.remove(noteKey(day)) else editor.putString(noteKey(day), text)
        editor.apply()
    }
}
