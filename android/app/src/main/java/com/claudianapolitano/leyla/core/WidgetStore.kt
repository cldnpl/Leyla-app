package com.claudianapolitano.leyla.core

import android.content.Context
import android.content.SharedPreferences
import com.claudianapolitano.leyla.widget.LeylaWidget
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * What the app leaves behind for the widget to read. Port of
 * `ios/Shared/WidgetSnapshot.swift`.
 *
 * iOS shares an App Group container between the app and its widget extension.
 * On Android a widget runs in the same process and package, so plain
 * SharedPreferences is the equivalent — and the simpler one.
 *
 * The widget must render without a network call: it wakes on the system's
 * schedule, often with no connectivity and no signed-in session in memory. So
 * everything it needs is written here whenever the app learns it.
 */
data class WidgetSnapshot(
    val partnerName: String? = null,
    val myName: String? = null,
    val daysTogether: Int? = null,
    val distanceKm: Double? = null,
    /** What the heart tap is doing right now; null once it has expired. */
    val missYouStatus: MissYouStatus? = null,
)

/** The feedback a heart tap shows on the widget itself, for a few seconds. */
enum class MissYouStatus { SENDING, SENT, FAILED }

object WidgetStore {

    private const val FILE = "leyla.widget"
    private const val KEY_PARTNER_NAME = "partnerName"
    private const val KEY_MY_NAME = "myName"
    private const val KEY_START_DATE = "startDate"
    private const val KEY_DISTANCE_KM = "distanceKm"
    private const val KEY_STATUS = "missYouStatus"
    private const val KEY_STATUS_UNTIL = "missYouStatusUntil"

    private lateinit var prefs: SharedPreferences

    /**
     * The application context, kept so [Session] can publish without carrying
     * one around: the things the widget shows are learned in places that have
     * no Activity to hand.
     */
    private lateinit var appContext: Context

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    private val ready: Boolean get() = ::prefs.isInitialized

    /**
     * Stores the names and the day the couple began. The day count is derived
     * on read rather than stored: a number written today is wrong tomorrow, and
     * the widget can easily go a week between updates.
     */
    fun saveCouple(myName: String?, partnerName: String?, startDate: String?) {
        if (!ready) return
        prefs.edit()
            .putString(KEY_MY_NAME, myName)
            .putString(KEY_PARTNER_NAME, partnerName)
            .putString(KEY_START_DATE, startDate?.take(10))
            .apply()
        LeylaWidget.refresh(appContext)
    }

    /** Distance is published by Home as the partner's position moves. */
    fun saveDistance(km: Double?) {
        if (!ready) return
        prefs.edit().apply {
            if (km == null) remove(KEY_DISTANCE_KM) else putFloat(KEY_DISTANCE_KM, km.toFloat())
        }.apply()
        LeylaWidget.refresh(appContext)
    }

    /**
     * Stores the heart tap's status with an expiry rather than relying on
     * someone to clear it: if the process dies mid-send, the next redraw
     * simply finds it expired instead of saying "Sending…" forever.
     */
    fun saveMissYouStatus(status: MissYouStatus?, forMillis: Long = 0) {
        if (!ready) return
        prefs.edit().apply {
            if (status == null) {
                remove(KEY_STATUS)
                remove(KEY_STATUS_UNTIL)
            } else {
                putString(KEY_STATUS, status.name)
                putLong(KEY_STATUS_UNTIL, System.currentTimeMillis() + forMillis)
            }
        }.apply()
    }

    fun load(context: Context): WidgetSnapshot {
        init(context)
        val start = prefs.getString(KEY_START_DATE, null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return WidgetSnapshot(
            partnerName = prefs.getString(KEY_PARTNER_NAME, null),
            myName = prefs.getString(KEY_MY_NAME, null),
            daysTogether = start?.let {
                ChronoUnit.DAYS.between(it, LocalDate.now()).coerceAtLeast(0).toInt()
            },
            distanceKm = if (prefs.contains(KEY_DISTANCE_KM)) {
                prefs.getFloat(KEY_DISTANCE_KM, 0f).toDouble()
            } else {
                null
            },
            missYouStatus = prefs.getString(KEY_STATUS, null)
                ?.takeIf { System.currentTimeMillis() < prefs.getLong(KEY_STATUS_UNTIL, 0) }
                ?.let { runCatching { MissYouStatus.valueOf(it) }.getOrNull() },
        )
    }

    /** Sign-out: the next account's widget must not show this one's couple. */
    fun clear() {
        if (!ready) return
        prefs.edit().clear().apply()
        LeylaWidget.refresh(appContext)
    }
}
