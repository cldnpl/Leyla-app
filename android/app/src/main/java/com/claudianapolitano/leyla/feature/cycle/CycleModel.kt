package com.claudianapolitano.leyla.feature.cycle

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.claudianapolitano.leyla.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A menstrual-cycle phase. Four phases, each with its own colour drawn from the
 * app's warm palette. The raw values are the wire format (kept in sync with the
 * backend's accepted phases). Port of `Us/Features/Cycle/CycleModel.swift`.
 */
enum class CyclePhase(
    val wire: String,
    @param:StringRes val titleRes: Int,
    /** Short line shown to the person whose cycle it is. */
    @param:StringRes val detailRes: Int,
    /** Gentle one-liner used on the partner's Home card. */
    @param:StringRes val partnerHintRes: Int,
    /** What the phase is and why it happens — for the supporting partner. */
    @param:StringRes val aboutRes: Int,
    /** Main symptoms and difficulties of the phase. */
    @param:StringRes val symptomsRes: Int,
    /** Concrete, kind things a supporting partner can do this phase. */
    val partnerTipsRes: List<Int>,
    val color: Color,
    val icon: ImageVector,
) {
    MENSTRUAL(
        wire = "menstrual",
        titleRes = R.string.phase_menstrual,
        detailRes = R.string.phase_detail_menstrual,
        partnerHintRes = R.string.phase_hint_menstrual,
        aboutRes = R.string.phase_about_menstrual,
        symptomsRes = R.string.phase_symptoms_menstrual,
        partnerTipsRes = listOf(
            R.string.phase_tip_menstrual,
            R.string.phase_tip_menstrual_2,
            R.string.phase_tip_menstrual_3,
            R.string.phase_tip_menstrual_4,
        ),
        color = Color(1.00f, 0.42f, 0.42f), // coral red
        icon = Icons.Filled.WaterDrop,
    ),
    FOLLICULAR(
        wire = "follicular",
        titleRes = R.string.phase_follicular,
        detailRes = R.string.phase_detail_follicular,
        partnerHintRes = R.string.phase_hint_follicular,
        aboutRes = R.string.phase_about_follicular,
        symptomsRes = R.string.phase_symptoms_follicular,
        partnerTipsRes = listOf(
            R.string.phase_tip_follicular,
            R.string.phase_tip_follicular_2,
            R.string.phase_tip_follicular_3,
        ),
        color = Color(0.98f, 0.68f, 0.44f), // warm peach
        icon = Icons.Filled.Spa,
    ),
    OVULATION(
        wire = "ovulation",
        titleRes = R.string.phase_ovulation,
        detailRes = R.string.phase_detail_ovulation,
        partnerHintRes = R.string.phase_hint_ovulation,
        aboutRes = R.string.phase_about_ovulation,
        symptomsRes = R.string.phase_symptoms_ovulation,
        partnerTipsRes = listOf(
            R.string.phase_tip_ovulation,
            R.string.phase_tip_ovulation_2,
            R.string.phase_tip_ovulation_3,
        ),
        color = Color(1.00f, 0.48f, 0.66f), // rose pink
        icon = Icons.Filled.AutoAwesome,
    ),
    LUTEAL(
        wire = "luteal",
        titleRes = R.string.phase_luteal,
        detailRes = R.string.phase_detail_luteal,
        partnerHintRes = R.string.phase_hint_luteal,
        aboutRes = R.string.phase_about_luteal,
        symptomsRes = R.string.phase_symptoms_luteal,
        partnerTipsRes = listOf(
            R.string.phase_tip_luteal,
            R.string.phase_tip_luteal_2,
            R.string.phase_tip_luteal_3,
            R.string.phase_tip_luteal_4,
            R.string.phase_tip_luteal_5,
        ),
        color = Color(0.76f, 0.55f, 0.80f), // soft plum
        icon = Icons.Filled.Bedtime,
    );

    companion object {
        fun from(wire: String?): CyclePhase? = entries.firstOrNull { it.wire == wire }
    }
}

/** Everything the app derives locally about the current cycle. */
data class CycleInsights(
    val phase: CyclePhase,
    val cycleDay: Int,
    val cycleLength: Int,
    val daysUntilNextPeriod: Int,
    val predictedNextPeriod: LocalDate,
    val currentCycleStart: LocalDate,
    val nextPhase: CyclePhase,
    val daysToNextPhase: Int,
    /** True when we had too little history and fell back to a 28-day estimate. */
    val isEstimated: Boolean,
)

/** How much of your cycle you share with your partner. Off by default. */
enum class CycleShareLevel(val wire: String, @param:StringRes val titleRes: Int) {
    OFF("off", R.string.cycle_share_off),
    CYCLE("cycle", R.string.cycle_share_cycle),
    CYCLE_AND_THOUGHTS("cycleAndThoughts", R.string.cycle_share_cycle_thoughts);

    @get:StringRes
    val explanationRes: Int
        get() = when (this) {
            OFF -> R.string.cycle_share_off_explain
            CYCLE -> R.string.cycle_share_cycle_explain
            CYCLE_AND_THOUGHTS -> R.string.cycle_share_cycle_thoughts_explain
        }

    companion object {
        /** Legacy values ("mood"/"full") map to sharing the cycle, never thoughts. */
        fun from(wire: String?): CycleShareLevel = when (wire) {
            CYCLE.wire, "mood", "full" -> CYCLE
            CYCLE_AND_THOUGHTS.wire -> CYCLE_AND_THOUGHTS
            else -> OFF
        }
    }
}

/** Everything derived locally from a due date. */
data class PregnancyInsights(
    /** Completed weeks (0…42). */
    val week: Int,
    val daysToDue: Int,
    val dueDate: LocalDate,
    /** 1, 2 or 3. */
    val trimester: Int,
    @param:StringRes val babySizeRes: Int,
)

/** Pure pregnancy math from a due date (40 weeks / 280 days to term). */
object PregnancyEngine {

    const val TERM_DAYS = 280L

    fun insights(dueDate: LocalDate, today: LocalDate = LocalDate.now()): PregnancyInsights {
        val daysToDue = ChronoUnit.DAYS.between(today, dueDate).toInt()
        val gestationDays = maxOf(0, TERM_DAYS.toInt() - daysToDue)
        val week = minOf(42, gestationDays / 7)
        val trimester = if (week <= 13) 1 else if (week <= 27) 2 else 3
        return PregnancyInsights(
            week = week,
            daysToDue = daysToDue,
            dueDate = dueDate,
            trimester = trimester,
            babySizeRes = babySize(week),
        )
    }

    fun parseDueDate(raw: String?): LocalDate? = raw?.let {
        runCatching { LocalDate.parse(it.take(10)) }.getOrNull()
    }

    /** A friendly size comparison for the given week (nearest defined week). */
    @StringRes
    fun babySize(week: Int): Int {
        val sizes = listOf(
            4 to R.string.baby_size_poppy_seed, 5 to R.string.baby_size_sesame_seed,
            6 to R.string.baby_size_lentil, 7 to R.string.baby_size_blueberry,
            8 to R.string.baby_size_raspberry, 9 to R.string.baby_size_cherry,
            10 to R.string.baby_size_strawberry, 11 to R.string.baby_size_lime,
            12 to R.string.baby_size_plum, 13 to R.string.baby_size_peach,
            14 to R.string.baby_size_lemon, 15 to R.string.baby_size_apple,
            16 to R.string.baby_size_avocado, 17 to R.string.baby_size_pear,
            18 to R.string.baby_size_bell_pepper, 19 to R.string.baby_size_mango,
            20 to R.string.baby_size_banana, 22 to R.string.baby_size_papaya,
            24 to R.string.baby_size_corn, 26 to R.string.baby_size_zucchini,
            28 to R.string.baby_size_eggplant, 30 to R.string.baby_size_cabbage,
            32 to R.string.baby_size_squash, 34 to R.string.baby_size_cantaloupe,
            36 to R.string.baby_size_honeydew, 38 to R.string.baby_size_pumpkin,
            40 to R.string.baby_size_watermelon,
        )
        return sizes.lastOrNull { it.first <= week }?.second ?: sizes.first().second
    }

    @StringRes
    fun trimesterTitle(trimester: Int): Int = when (trimester) {
        1 -> R.string.trimester_first
        2 -> R.string.trimester_second
        else -> R.string.trimester_third
    }

    /** What's happening this trimester (shown to both). */
    @StringRes
    fun trimesterAbout(trimester: Int): Int = when (trimester) {
        1 -> R.string.trimester_about_first
        2 -> R.string.trimester_about_second
        else -> R.string.trimester_about_third
    }

    /** How the partner can support her this trimester. */
    fun trimesterSupport(trimester: Int): List<Int> = when (trimester) {
        1 -> listOf(
            R.string.trimester_support_first_1, R.string.trimester_support_first_2,
            R.string.trimester_support_first_3, R.string.trimester_support_first_4,
        )
        2 -> listOf(
            R.string.trimester_support_second_1, R.string.trimester_support_second_2,
            R.string.trimester_support_second_3, R.string.trimester_support_second_4,
        )
        else -> listOf(
            R.string.trimester_support_third_1, R.string.trimester_support_third_2,
            R.string.trimester_support_third_3, R.string.trimester_support_third_4,
        )
    }
}
