package com.claudianapolitano.leyla.feature.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.PartnerCycle
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaCard
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import androidx.compose.ui.text.font.FontWeight

/**
 * The Home cycle cards, ported from `Us/Features/Cycle/CycleViews.swift`.
 *
 * iOS labels the data source "Apple Health". The Android build reads the same
 * kind of data from Health Connect, so the badge says that instead — the only
 * copy that intentionally differs from iOS.
 */
@Composable
private fun HealthSourceBadge(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.health_source)
    val accessibility = stringResource(R.string.health_source_accessibility)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(HealthRed.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics { contentDescription = accessibility },
    ) {
        Icon(Icons.Filled.MonitorHeart, contentDescription = null, tint = HealthRed, modifier = Modifier.size(14.dp))
        Text(label, style = IOSText.caption.weight(FontWeight.SemiBold), color = HealthRed)
    }
}

private val HealthRed = Color(0xFFE5484D)

/**
 * The shared body of every cycle card: a leading glyph, a title, a subtitle,
 * and the chevron that says this opens the detail screen.
 */
@Composable
private fun CycleCardRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    subtitleMaxLines: Int = Int.MAX_VALUE,
) {
    val colors = LeylaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(title, style = IOSText.headline, color = colors.ink)
            Text(
                subtitle,
                style = IOSText.subheadline,
                color = colors.secondary,
                maxLines = subtitleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.tertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Compact card showing *your own* cycle (people who have one). */
@Composable
fun SelfCycleCard(insights: CycleInsights, modifier: Modifier = Modifier) {
    val nextPeriod = when (insights.daysUntilNextPeriod) {
        0 -> stringResource(R.string.cycle_next_today)
        1 -> stringResource(R.string.cycle_next_tomorrow)
        else -> stringResource(R.string.cycle_next_in_days, insights.daysUntilNextPeriod)
    }
    LeylaCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HealthSourceBadge()
            CycleCardRow(
                icon = insights.phase.icon,
                iconTint = insights.phase.color,
                title = stringResource(insights.phase.titleRes),
                subtitle = stringResource(R.string.cycle_self_subtitle, insights.cycleDay, nextPeriod),
            )
        }
    }
}

/**
 * "His" card — for a partner who doesn't have a cycle. Shows her current phase
 * and the first supportive tip, or a prompt when she isn't sharing yet.
 */
@Composable
fun PartnerPeriodCard(partner: PartnerCycle?, partnerName: String, modifier: Modifier = Modifier) {
    val phase = partner?.takeIf { it.sharing }?.let { CyclePhase.from(it.phase) }
    LeylaCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HealthSourceBadge()
            if (phase != null) {
                CycleCardRow(
                    icon = phase.icon,
                    iconTint = phase.color,
                    title = stringResource(R.string.cycle_partner_title, partnerName, stringResource(phase.titleRes)),
                    subtitle = stringResource(phase.firstPartnerTipRes),
                    subtitleMaxLines = 2,
                )
            } else {
                CycleCardRow(
                    icon = Icons.Filled.MonitorHeart,
                    iconTint = Theme.rose,
                    title = stringResource(R.string.cycle_partner_prompt_title, partnerName),
                    subtitle = stringResource(R.string.cycle_partner_prompt_body),
                    subtitleMaxLines = 2,
                )
            }
        }
    }
}

/**
 * Neutral entry shown on Home when tracking isn't set up yet (or the "do you
 * have a cycle?" question hasn't been answered). Opens the detail/setup screen.
 */
@Composable
fun CycleSetupCard(modifier: Modifier = Modifier) {
    LeylaCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HealthSourceBadge()
            CycleCardRow(
                icon = Icons.Filled.MonitorHeart,
                iconTint = Theme.rose,
                title = stringResource(R.string.cycle_setup_title),
                subtitle = stringResource(R.string.cycle_setup_body),
            )
        }
    }
}

/** Home card shown while a pregnancy is active/shared — week + due countdown. */
@Composable
fun PregnancyHomeCard(insights: PregnancyInsights, title: String, modifier: Modifier = Modifier) {
    val subtitle = if (insights.daysToDue <= 0) {
        stringResource(R.string.pregnancy_week_due_any_day, insights.week)
    } else {
        stringResource(R.string.pregnancy_week_days_to_go, insights.week, insights.daysToDue)
    }
    LeylaCard(modifier) {
        CycleCardRow(
            icon = Icons.Filled.ChildCare,
            iconTint = Theme.rose,
            title = title,
            subtitle = subtitle,
        )
    }
}
