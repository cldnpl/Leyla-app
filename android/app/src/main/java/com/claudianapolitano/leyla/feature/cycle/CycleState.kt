package com.claudianapolitano.leyla.feature.cycle

import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.PartnerCycle
import com.claudianapolitano.leyla.core.PartnerPregnancy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * What Home needs to know about cycles. Port of the read side of
 * `Us/Features/Cycle/CycleManager.swift`.
 *
 * iOS derives [CycleState.insights] from HealthKit. The Android counterpart is
 * Health Connect, which lands with the Cycle screen — until then [userHasCycle]
 * stays null for a fresh install, which is exactly the state that makes Home
 * show the setup card, same as iOS.
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
    )

    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()

    /** Called when Home appears, mirroring iOS's `cycle.refreshOnAppear()`. */
    suspend fun refreshOnAppear() {
        val partner = runCatching { LeylaApi.partnerCycle() }.getOrNull()
        val pregnancy = runCatching { LeylaApi.partnerPregnancy() }.getOrNull()
        _snapshot.update { it.copy(partner = partner, partnerPregnancy = pregnancy) }
    }
}

/** Weeks elapsed and days remaining for a due date, counted from a 40-week term. */
object PregnancyEngine {
    private const val TERM_DAYS = 280L

    fun insights(dueDate: LocalDate, today: LocalDate = LocalDate.now()): PregnancyInsights {
        val daysToDue = ChronoUnit.DAYS.between(today, dueDate).toInt()
        val elapsed = TERM_DAYS - daysToDue
        val week = (elapsed / 7).toInt().coerceIn(0, 42)
        return PregnancyInsights(week = week, daysToDue = daysToDue)
    }

    fun parseDueDate(raw: String?): LocalDate? = raw?.let {
        runCatching { LocalDate.parse(it.take(10)) }.getOrNull()
    }
}
