package com.claudianapolitano.leyla.core

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Thin read-only bridge to Health Connect for menstrual data. Port of
 * `Us/Core/HealthKitManager.swift`.
 *
 * We never write and never store anything ourselves — the source of truth is
 * whatever app the user logs her period in (Flo, Clue, Samsung Health…), synced
 * into Health Connect.
 *
 * One real difference from iOS: HealthKit refuses, for privacy, to say whether
 * read access was granted, so iOS has to treat "denied" and "no data" as the
 * same thing and remember a "connected" flag of its own. Health Connect *does*
 * answer ([hasPermission]), so the Android screen can tell those two apart and
 * can re-ask, which iOS cannot.
 */
object HealthConnectManager {

    /** Read-only, and only the two record types the cycle is derived from. */
    private val readPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
        HealthPermission.getReadPermission(MenstruationFlowRecord::class),
    )

    /**
     * Android 15 hands back only the last 30 days unless the app also holds the
     * history permission — rarely even two cycles, so the average cycle length
     * would have nothing to work with. It is asked for alongside the reads, and
     * only where it exists; a refusal is survivable, just less accurate.
     */
    private const val READ_HISTORY = "android.permission.health.READ_HEALTH_DATA_HISTORY"

    /** What to hand the permission launcher. */
    val permissions: Set<String>
        get() = if (Build.VERSION.SDK_INT >= 35) readPermissions + READ_HISTORY else readPermissions

    /** Why a device might not be able to do this at all. */
    enum class Availability { AVAILABLE, NEEDS_PROVIDER_UPDATE, UNSUPPORTED }

    fun availability(context: Context): Availability =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NEEDS_PROVIDER_UPDATE
            else -> Availability.UNSUPPORTED
        }

    fun isAvailable(context: Context): Boolean = availability(context) == Availability.AVAILABLE

    private fun clientOrNull(context: Context): HealthConnectClient? =
        if (isAvailable(context)) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() else null

    /**
     * Whether the user granted the reads this feature needs. Deliberately does
     * not require [READ_HISTORY]: without it the cycle still works, on a
     * shorter window, and treating that as "not connected" would push someone
     * back to a Connect button for no reason.
     */
    suspend fun hasPermission(context: Context): Boolean {
        val client = clientOrNull(context) ?: return false
        val granted = runCatching { client.permissionController.getGrantedPermissions() }.getOrNull() ?: return false
        return granted.containsAll(readPermissions)
    }

    /**
     * The contract to launch from a Composable to show the Health Connect
     * permission sheet. Unlike iOS's one-shot sheet this can be shown again
     * after a refusal, so the screen keeps offering the button.
     */
    fun requestPermissionsContract() = PermissionController.createRequestPermissionResultContract()

    /**
     * Period-start dates from the last [monthsBack] months, oldest first. Empty
     * when nothing is logged or permission is missing.
     *
     * Two record types matter, and they are not redundant. A
     * [MenstruationPeriodRecord] *is* a period with a start date — the best
     * possible answer, and something HealthKit has no equivalent of. Apps that
     * only write daily [MenstruationFlowRecord] entries give us bleeding days
     * instead, which [periodStarts] clusters the same way iOS does.
     */
    suspend fun periodStartDates(context: Context, monthsBack: Long = 12): List<LocalDate> {
        val client = clientOrNull(context) ?: return emptyList()
        if (!hasPermission(context)) return emptyList()

        val zone = ZoneId.systemDefault()
        val end = LocalDateTime.now()
        val range = TimeRangeFilter.between(end.minusMonths(monthsBack), end)

        val periodStarts = runCatching {
            client.readRecords(ReadRecordsRequest(MenstruationPeriodRecord::class, range))
                .records.map { LocalDate.ofInstant(it.startTime, it.startZoneOffset ?: zone.rules.getOffset(it.startTime)) }
        }.getOrDefault(emptyList())

        val flowDays = runCatching {
            client.readRecords(ReadRecordsRequest(MenstruationFlowRecord::class, range))
                .records.map { LocalDate.ofInstant(it.time, it.zoneOffset ?: zone.rules.getOffset(it.time)) }
        }.getOrDefault(emptyList())

        return periodStarts(explicitStarts = periodStarts, flowDays = flowDays)
    }

    /**
     * Reduces what Health Connect gave us to period-start days: every explicitly
     * recorded period start, plus the first bleeding day after a gap of more
     * than two days.
     *
     * The gap rule is iOS's, and it is what keeps a stray logged day from
     * reading as a whole new cycle.
     */
    fun periodStarts(explicitStarts: List<LocalDate>, flowDays: List<LocalDate>): List<LocalDate> {
        val starts = explicitStarts.toMutableSet()

        var previous: LocalDate? = null
        for (day in flowDays.distinct().sorted()) {
            val gap = previous?.let { ChronoUnit.DAYS.between(it, day) }
            // First bleeding day we've seen, or the first after a real break.
            if (gap == null || gap > 2) starts.add(day)
            previous = day
        }
        return starts.sorted()
    }
}
