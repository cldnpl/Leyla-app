package com.claudianapolitano.leyla.feature.cycle

import com.claudianapolitano.leyla.core.HealthConnectManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The cycle math decides what the ring says, so it is worth pinning down. These
 * mirror the behaviour of iOS's `CycleEngine`, which is the reference.
 */
class CycleEngineTest {

    private val today = LocalDate.of(2026, 9, 15)

    @Test
    fun `no logged period means no insights at all`() {
        assertNull(CycleEngine.insights(emptyList(), today))
    }

    @Test
    fun `day one of a period is menstrual, day one`() {
        val i = CycleEngine.insights(listOf(today), today)!!
        assertEquals(CyclePhase.MENSTRUAL, i.phase)
        assertEquals(1, i.cycleDay)
        assertEquals(28, i.cycleLength)
        assertEquals(28, i.daysUntilNextPeriod)
    }

    @Test
    fun `a single logged period is flagged as an estimate`() {
        assertTrue(CycleEngine.insights(listOf(today), today)!!.isEstimated)
        val twoCycles = listOf(today.minusDays(56), today.minusDays(28))
        assertTrue(!CycleEngine.insights(twoCycles, today)!!.isEstimated)
    }

    @Test
    fun `the four phases land on the right days of a 28-day cycle`() {
        // Ovulation sits ~14 days before the next period, so day 14 is it.
        val expected = mapOf(
            1 to CyclePhase.MENSTRUAL,
            5 to CyclePhase.MENSTRUAL,
            6 to CyclePhase.FOLLICULAR,
            12 to CyclePhase.FOLLICULAR,
            13 to CyclePhase.OVULATION,
            14 to CyclePhase.OVULATION,
            15 to CyclePhase.OVULATION,
            16 to CyclePhase.LUTEAL,
            28 to CyclePhase.LUTEAL,
        )
        expected.forEach { (day, phase) ->
            assertEquals("day $day", phase, CycleEngine.phase(day, 28))
        }
    }

    @Test
    fun `every day of every sane cycle length falls inside a phase window`() {
        for (length in 21..35) {
            for (day in 1..length) {
                val windows = CycleEngine.windows(length)
                val phase = CycleEngine.phase(day, length)
                assertTrue(
                    "length $length day $day landed outside its window",
                    windows.any { it.phase == phase && day >= it.start && day <= it.end },
                )
            }
        }
    }

    @Test
    fun `luteal rolls round to the next period rather than off the end`() {
        val progress = CycleEngine.phaseProgress(cycleDay = 28, cycleLength = 28)
        assertEquals(CyclePhase.MENSTRUAL, progress.nextPhase)
        assertEquals(1, progress.daysToNextPhase)
    }

    @Test
    fun `cycle length is the mean of recent gaps, clamped to 21-35 days`() {
        // 30-day gaps.
        val starts = (0..3).map { today.minusDays(90L - it * 30) }
        assertEquals(30, CycleEngine.averageCycleLength(starts))

        // A wild outlier is excluded rather than dragging the average.
        val withGarbage = listOf(today.minusDays(400), today.minusDays(56), today.minusDays(28))
        assertEquals(28, CycleEngine.averageCycleLength(withGarbage))

        // Nothing to average from.
        assertEquals(28, CycleEngine.averageCycleLength(listOf(today)))
    }

    /**
     * Someone who logs a period and then stops logging must not see the ring
     * frozen on day 40-something. iOS rolls forward by whole cycles; so do we.
     */
    @Test
    fun `a stale last entry rolls forward into the current cycle`() {
        // Two 28-day cycles, last logged 70 days ago → 2 cycles elapsed, day 15.
        val starts = listOf(today.minusDays(98), today.minusDays(70))
        val i = CycleEngine.insights(starts, today)!!
        assertEquals(28, i.cycleLength)
        assertEquals(today.minusDays(14), i.currentCycleStart)
        assertEquals(15, i.cycleDay)
        assertTrue("cycle day must stay inside the cycle", i.cycleDay in 1..i.cycleLength)
    }

    @Test
    fun `the predicted next period is one cycle after the current start`() {
        val i = CycleEngine.insights(listOf(today.minusDays(10)), today)!!
        assertEquals(i.currentCycleStart.plusDays(28), i.predictedNextPeriod)
        assertEquals(18, i.daysUntilNextPeriod)
    }
}

/**
 * Reducing Health Connect's records to period starts. The clustering rule is
 * what stops a single stray logged day from reading as a whole new cycle.
 */
class PeriodStartDetectionTest {

    private val day = LocalDate.of(2026, 9, 1)

    @Test
    fun `consecutive bleeding days are one period, not five`() {
        val flow = (0..4).map { day.plusDays(it.toLong()) }
        val starts = HealthConnectManager.periodStarts(explicitStarts = emptyList(), flowDays = flow)
        assertEquals(listOf(day), starts)
    }

    @Test
    fun `a gap of more than two days starts a new period`() {
        val flow = listOf(day, day.plusDays(1), day.plusDays(5), day.plusDays(6))
        val starts = HealthConnectManager.periodStarts(explicitStarts = emptyList(), flowDays = flow)
        assertEquals(listOf(day, day.plusDays(5)), starts)
    }

    @Test
    fun `a one-day skip mid-period does not split it`() {
        // Missed logging day 2; still the same period.
        val flow = listOf(day, day.plusDays(2), day.plusDays(3))
        val starts = HealthConnectManager.periodStarts(explicitStarts = emptyList(), flowDays = flow)
        assertEquals(listOf(day), starts)
    }

    @Test
    fun `an explicitly recorded period start is always kept`() {
        val explicit = listOf(day.plusDays(30))
        val flow = listOf(day, day.plusDays(1))
        val starts = HealthConnectManager.periodStarts(explicit, flow)
        assertEquals(listOf(day, day.plusDays(30)), starts)
    }

    @Test
    fun `duplicate days from two apps collapse to one start`() {
        val flow = listOf(day, day, day.plusDays(1))
        val starts = HealthConnectManager.periodStarts(explicitStarts = listOf(day), flowDays = flow)
        assertEquals(listOf(day), starts)
    }

    @Test
    fun `nothing logged means no starts`() {
        assertEquals(emptyList<LocalDate>(), HealthConnectManager.periodStarts(emptyList(), emptyList()))
    }
}

/** Pregnancy is pure date math from the due date. */
class PregnancyEngineTest {

    private val today = LocalDate.of(2026, 9, 15)

    @Test
    fun `a due date 280 days out is week zero`() {
        val i = PregnancyEngine.insights(today.plusDays(280), today)
        assertEquals(0, i.week)
        assertEquals(280, i.daysToDue)
        assertEquals(1, i.trimester)
    }

    @Test
    fun `the due date itself is week 40 with nothing to go`() {
        val i = PregnancyEngine.insights(today, today)
        assertEquals(40, i.week)
        assertEquals(0, i.daysToDue)
        assertEquals(3, i.trimester)
    }

    @Test
    fun `trimesters break at weeks 13 and 27`() {
        fun trimesterAtWeek(week: Int) =
            PregnancyEngine.insights(today.plusDays(280L - week * 7), today).trimester
        assertEquals(1, trimesterAtWeek(13))
        assertEquals(2, trimesterAtWeek(14))
        assertEquals(2, trimesterAtWeek(27))
        assertEquals(3, trimesterAtWeek(28))
    }

    @Test
    fun `an overdue baby stays at week 42 rather than running away`() {
        val i = PregnancyEngine.insights(today.minusDays(30), today)
        assertEquals(42, i.week)
        assertTrue("overdue shows as negative days to due", i.daysToDue < 0)
    }

    @Test
    fun `a malformed due date from the server is ignored, not crashed on`() {
        assertNull(PregnancyEngine.parseDueDate(null))
        assertNull(PregnancyEngine.parseDueDate("not a date"))
        assertEquals(LocalDate.of(2026, 12, 1), PregnancyEngine.parseDueDate("2026-12-01T00:00:00Z"))
    }
}
