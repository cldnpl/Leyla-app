package com.claudianapolitano.leyla.feature.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Pure, testable cycle math. Takes the period-start dates read from Health
 * Connect and derives the current day, phase, next phase, and next-period
 * prediction. Port of `CycleEngine` in `Us/Features/Cycle/CycleModel.swift`.
 *
 * Everything here is an estimate — the UI says so — never a medical claim.
 */
object CycleEngine {

    const val DEFAULT_CYCLE_LENGTH = 28
    const val PERIOD_LENGTH = 5

    data class Window(val phase: CyclePhase, val start: Int, val end: Int)

    /** The four phase windows (1-based cycle days) for a given cycle length. */
    fun windows(cycleLength: Int): List<Window> {
        val p = PERIOD_LENGTH
        val ovulation = maxOf(p + 3, cycleLength - 14) // ~14 days before next period
        return listOf(
            Window(CyclePhase.MENSTRUAL, 1, p),
            Window(CyclePhase.FOLLICULAR, p + 1, ovulation - 2),
            Window(CyclePhase.OVULATION, ovulation - 1, ovulation + 1),
            Window(CyclePhase.LUTEAL, ovulation + 2, cycleLength),
        )
    }

    fun phase(cycleDay: Int, cycleLength: Int): CyclePhase {
        val ws = windows(cycleLength)
        return (ws.firstOrNull { cycleDay >= it.start && cycleDay <= it.end } ?: ws.last()).phase
    }

    data class PhaseProgress(val nextPhase: CyclePhase, val daysToNextPhase: Int)

    /** Which phase comes next and how many days until it begins. */
    fun phaseProgress(cycleDay: Int, cycleLength: Int): PhaseProgress {
        val ws = windows(cycleLength)
        val current = phase(cycleDay, cycleLength)
        val idx = ws.indexOfFirst { it.phase == current }
        if (idx in 0 until ws.size - 1) {
            val next = ws[idx + 1]
            return PhaseProgress(next.phase, maxOf(0, next.start - cycleDay))
        }
        // Luteal → next period (a new cycle) at day cycleLength + 1.
        return PhaseProgress(CyclePhase.MENSTRUAL, maxOf(0, (cycleLength + 1) - cycleDay))
    }

    fun insights(periodStarts: List<LocalDate>, today: LocalDate = LocalDate.now()): CycleInsights? {
        val days = periodStarts.distinct().sorted()
        val last = days.lastOrNull() ?: return null

        val cycleLength = averageCycleLength(days)

        // Most recent cycle start on/before today: roll forward from the last
        // logged start, so a few weeks of not logging still lands on the right
        // day instead of freezing the ring at the last entry.
        val gap = ChronoUnit.DAYS.between(last, today)
        val cyclesElapsed = if (gap > 0) gap / cycleLength else 0
        val currentStart = last.plusDays(cyclesElapsed * cycleLength)

        val cycleDay = (ChronoUnit.DAYS.between(currentStart, today) + 1).toInt().coerceIn(1, cycleLength)
        val predictedNext = currentStart.plusDays(cycleLength.toLong())
        val daysUntil = maxOf(0, ChronoUnit.DAYS.between(today, predictedNext).toInt())
        val progress = phaseProgress(cycleDay, cycleLength)

        return CycleInsights(
            phase = phase(cycleDay, cycleLength),
            cycleDay = cycleDay,
            cycleLength = cycleLength,
            daysUntilNextPeriod = daysUntil,
            predictedNextPeriod = predictedNext,
            currentCycleStart = currentStart,
            nextPhase = progress.nextPhase,
            daysToNextPhase = progress.daysToNextPhase,
            isEstimated = days.size < 2,
        )
    }

    /** Mean gap of the last few cycles, clamped to a sane 21–35 days. */
    fun averageCycleLength(starts: List<LocalDate>): Int {
        if (starts.size < 2) return DEFAULT_CYCLE_LENGTH
        val gaps = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
            .filter { it in 15..60 }
        if (gaps.isEmpty()) return DEFAULT_CYCLE_LENGTH
        val recent = gaps.takeLast(6)
        val avg = (recent.sum().toDouble() / recent.size).roundToInt()
        return avg.coerceIn(21, 35)
    }
}
