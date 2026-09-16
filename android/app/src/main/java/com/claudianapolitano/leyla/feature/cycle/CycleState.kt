package com.claudianapolitano.leyla.feature.cycle

import android.content.Context
import com.claudianapolitano.leyla.core.HealthConnectManager
import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.PartnerCycle
import com.claudianapolitano.leyla.core.PartnerPregnancy
import com.claudianapolitano.leyla.core.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * Owns the cycle state for the UI: your own insights (derived from Health
 * Connect), your partner's shared summary (read from the backend), your sharing
 * choice, and the free-text note for the day. Port of
 * `Us/Features/Cycle/CycleManager.swift`.
 *
 * The dates Health Connect gave us are cached on this phone (see [CyclePrefs])
 * so the cycle survives a relaunch and a sign-out — the health permission
 * belongs to the device, not to the account.
 */
object CycleState {

    data class Snapshot(
        /** null = the "do you have a cycle?" question was never answered. */
        val userHasCycle: Boolean? = null,
        val insights: CycleInsights? = null,
        val isPregnant: Boolean = false,
        val pregnancyInsights: PregnancyInsights? = null,
        val partner: PartnerCycle? = null,
        val partnerPregnancy: PartnerPregnancy? = null,
        val shareLevel: CycleShareLevel = CycleShareLevel.OFF,
        val todayNote: String = "",
        /** Whether Health Connect has granted us the menstrual reads. */
        val healthConnected: Boolean = false,
        val healthAvailability: HealthConnectManager.Availability =
            HealthConnectManager.Availability.UNSUPPORTED,
    )

    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()

    /**
     * Restores what this phone already knows, before any network or health read
     * answers. Health Connect may take a moment, and the account being signed
     * in has nothing to do with the data already on the device.
     */
    fun restoreLocal() {
        val cached = CyclePrefs.cachedPeriodStarts
        _snapshot.update {
            it.copy(
                userHasCycle = CyclePrefs.userHasCycle,
                insights = CycleEngine.insights(cached),
                isPregnant = CyclePrefs.isPregnant,
                pregnancyInsights = CyclePrefs.dueDate
                    ?.takeIf { _ -> CyclePrefs.isPregnant }
                    ?.let(PregnancyEngine::insights),
                shareLevel = CyclePrefs.shareLevel,
                todayNote = CyclePrefs.note(),
            )
        }
    }

    /**
     * Called when a screen appears, mirroring iOS's `cycle.refreshOnAppear()`:
     * refresh the partner card, reload today's note, and — for someone who has
     * a cycle — re-read Health Connect and keep her shared summary current.
     */
    suspend fun refreshOnAppear(context: Context) {
        restoreLocal()

        val partner = runCatching { LeylaApi.partnerCycle() }.getOrNull()
        val pregnancy = runCatching { LeylaApi.partnerPregnancy() }.getOrNull()
        val availability = HealthConnectManager.availability(context)
        val connected = HealthConnectManager.hasPermission(context)

        _snapshot.update {
            it.copy(
                partner = partner,
                partnerPregnancy = pregnancy,
                healthAvailability = availability,
                healthConnected = connected,
                // The answer lives on the account, so a reinstall or a second
                // device picks it up rather than asking again.
                userHasCycle = Session.current.user?.hasCycle ?: it.userHasCycle,
            )
        }
        adoptServerShareLevel()

        // People without a cycle have nothing to read from Health Connect.
        if (_snapshot.value.userHasCycle == false) return
        if (connected) {
            refreshInsights(context)
            if (_snapshot.value.shareLevel != CycleShareLevel.OFF) pushShare()
        }
    }

    /**
     * The server wins when it has an answer. When it doesn't but this device
     * does — someone upgrading from a build that only stored the level locally
     * — the local choice is pushed up so it isn't silently reset to off.
     */
    private suspend fun adoptServerShareLevel() {
        val raw = Session.current.user?.cycleShareLevel ?: return
        val server = CycleShareLevel.from(raw)
        val local = _snapshot.value.shareLevel
        if (server == CycleShareLevel.OFF && local != CycleShareLevel.OFF) {
            runCatching { LeylaApi.updateCycleSettings(shareLevel = local.wire) }
        } else if (server != local) {
            CyclePrefs.shareLevel = server
            _snapshot.update { it.copy(shareLevel = server) }
        }
    }

    /**
     * Re-read Health Connect and update the cycle.
     *
     * A read that comes back empty — the tracking app hasn't synced yet, access
     * revoked in settings — must never wipe what we already know. The cached
     * dates stay, and the ring keeps advancing from them until there is
     * something newer to say.
     */
    suspend fun refreshInsights(context: Context) {
        val starts = runCatching { HealthConnectManager.periodStartDates(context) }.getOrDefault(emptyList())
        if (starts.isEmpty()) {
            if (_snapshot.value.insights == null) {
                _snapshot.update { it.copy(insights = CycleEngine.insights(CyclePrefs.cachedPeriodStarts)) }
            }
            return
        }
        CyclePrefs.cachedPeriodStarts = starts
        _snapshot.update { it.copy(insights = CycleEngine.insights(starts), healthConnected = true) }
    }

    /** Called after the permission sheet closes, whatever the answer was. */
    suspend fun onPermissionResult(context: Context) {
        val connected = HealthConnectManager.hasPermission(context)
        _snapshot.update { it.copy(healthConnected = connected) }
        if (connected) {
            refreshInsights(context)
            if (_snapshot.value.shareLevel != CycleShareLevel.OFF) pushShare()
        }
    }

    /**
     * Answers the "do you have a cycle?" question. Held locally straight away so
     * the Home card switches immediately, then persisted to the account.
     */
    suspend fun setUserHasCycle(hasCycle: Boolean) {
        CyclePrefs.userHasCycle = hasCycle
        _snapshot.update { it.copy(userHasCycle = hasCycle) }
        runCatching { LeylaApi.updateCycleSettings(hasCycle = hasCycle) }
            .getOrNull()
            ?.let(Session::updateUser)
    }

    /** Change the sharing level and immediately push (or clear) the summary. */
    suspend fun setShareLevel(level: CycleShareLevel) {
        CyclePrefs.shareLevel = level
        _snapshot.update { it.copy(shareLevel = level) }
        runCatching { LeylaApi.updateCycleSettings(shareLevel = level.wire) }
        pushShare()
    }

    // MARK: - Today's note

    /** Persist the day's note locally (no network). */
    fun saveNote(text: String) {
        CyclePrefs.saveNote(text)
        _snapshot.update { it.copy(todayNote = text) }
    }

    /** Push the latest note to the partner if sharing includes thoughts. */
    suspend fun syncNoteIfSharing() {
        if (_snapshot.value.shareLevel == CycleShareLevel.CYCLE_AND_THOUGHTS) pushShare()
    }

    /** Upload the summary the current level allows, or clear it when off. */
    private suspend fun pushShare() {
        val state = _snapshot.value
        val insights = state.insights
        if (state.shareLevel == CycleShareLevel.OFF || insights == null) {
            runCatching { LeylaApi.stopSharingCycle() }
            return
        }
        val trimmed = state.todayNote.trim()
        val note = trimmed.takeIf {
            state.shareLevel == CycleShareLevel.CYCLE_AND_THOUGHTS && it.isNotEmpty()
        }
        runCatching {
            LeylaApi.putCycle(
                phase = insights.phase.wire,
                cycleDay = insights.cycleDay,
                periodInDays = insights.daysUntilNextPeriod,
                note = note,
            )
        }
    }

    // MARK: - Pregnancy

    /** Enter pregnancy mode with a due date and share it with the partner. */
    suspend fun startPregnancy(dueDate: LocalDate) {
        CyclePrefs.isPregnant = true
        CyclePrefs.dueDate = dueDate
        _snapshot.update {
            it.copy(isPregnant = true, pregnancyInsights = PregnancyEngine.insights(dueDate))
        }
        runCatching { LeylaApi.putPregnancy(dueDate.toString()) }
    }

    /** Leave pregnancy mode and stop sharing the due date. */
    suspend fun endPregnancy() {
        CyclePrefs.isPregnant = false
        CyclePrefs.dueDate = null
        _snapshot.update { it.copy(isPregnant = false, pregnancyInsights = null) }
        runCatching { LeylaApi.stopSharingPregnancy() }
    }

    /**
     * Sign-out cleanup. Only the partner's shared data belongs to the account
     * that just signed out; the health connection, the cached cycle and the
     * "do you have a cycle?" answer belong to this phone and are deliberately
     * kept, so signing back in never means setting the cycle up again.
     */
    fun forgetPartnerData() {
        _snapshot.update { it.copy(partner = null, partnerPregnancy = null) }
    }
}
