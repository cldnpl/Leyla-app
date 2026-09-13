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

/**
 * A menstrual-cycle phase. Four phases, each with its own colour drawn from the
 * app's warm palette. The raw values are the wire format (kept in sync with the
 * backend's accepted phases). Port of `Us/Features/Cycle/CycleModel.swift`.
 */
enum class CyclePhase(
    val wire: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val partnerHintRes: Int,
    @param:StringRes val firstPartnerTipRes: Int,
    val color: Color,
    val icon: ImageVector,
) {
    MENSTRUAL(
        wire = "menstrual",
        titleRes = R.string.phase_menstrual,
        partnerHintRes = R.string.phase_hint_menstrual,
        firstPartnerTipRes = R.string.phase_tip_menstrual,
        color = Color(1.00f, 0.42f, 0.42f), // coral red
        icon = Icons.Filled.WaterDrop,
    ),
    FOLLICULAR(
        wire = "follicular",
        titleRes = R.string.phase_follicular,
        partnerHintRes = R.string.phase_hint_follicular,
        firstPartnerTipRes = R.string.phase_tip_follicular,
        color = Color(0.98f, 0.68f, 0.44f), // warm peach
        icon = Icons.Filled.Spa,
    ),
    OVULATION(
        wire = "ovulation",
        titleRes = R.string.phase_ovulation,
        partnerHintRes = R.string.phase_hint_ovulation,
        firstPartnerTipRes = R.string.phase_tip_ovulation,
        color = Color(1.00f, 0.48f, 0.66f), // rose pink
        icon = Icons.Filled.AutoAwesome,
    ),
    LUTEAL(
        wire = "luteal",
        titleRes = R.string.phase_luteal,
        partnerHintRes = R.string.phase_hint_luteal,
        firstPartnerTipRes = R.string.phase_tip_luteal,
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
    val nextPhase: CyclePhase,
    val daysToNextPhase: Int,
    /** True when we had too little history and fell back to a 28-day estimate. */
    val isEstimated: Boolean,
)

/** Week/countdown derived from a shared due date. */
data class PregnancyInsights(val week: Int, val daysToDue: Int)
