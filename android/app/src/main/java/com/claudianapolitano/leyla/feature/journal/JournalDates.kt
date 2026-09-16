package com.claudianapolitano.leyla.feature.journal

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Date handling for the diary. Port of `JournalDates` and the day helpers in
 * `Us/Features/Journal/JournalView.swift`.
 *
 * The API carries calendar dates — a milestone's date, an entry's day — as
 * midnight UTC. They are therefore read in **UTC**, so someone west of Greenwich
 * doesn't see every entry shift back a day.
 *
 * Writing goes the other way: a date the person picked is read in the *local*
 * calendar, because that is the day they actually tapped. Reading UTC there
 * caused iOS an off-by-one east of Greenwich, where local midnight is still the
 * previous day in UTC.
 */
object JournalDates {

    /** Parses a server timestamp into the calendar day it stands for. */
    fun day(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        // Full timestamps ("2026-09-15T00:00:00Z") and bare days both appear.
        runCatching { return Instant.parse(raw).atZone(ZoneOffset.UTC).toLocalDate() }
        return runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
    }

    /** `yyyy-MM-dd` for the API, from a day the person picked locally. */
    fun isoDay(date: LocalDate): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

    /** The grouping key for a day card. */
    fun dayKey(date: LocalDate): String = isoDay(date)

    /** The grouping key for a month section. */
    fun monthKey(date: LocalDate): String = "%04d-%02d".format(date.year, date.monthValue)

    /** "Feb 12, 2026" — the medium form, in the reader's locale. */
    fun medium(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    /** "12" — the number on the date badge. */
    fun dayNumber(date: LocalDate): String = date.dayOfMonth.toString()

    /** "THU" — the weekday under the badge. */
    fun weekdayShort(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofPattern("EEE", locale)).uppercase(locale)

    /** "February 2026" — the heading that separates months in the feed. */
    fun monthYear(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
