package com.claudianapolitano.leyla.feature.cycle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight

private val RING_SIZE = 236.dp
private val RING_STROKE = 18.dp

/**
 * The cycle wheel: a coloured progress ring (colour per phase) with the phase
 * name, current cycle day, and a countdown to the next phase centred inside.
 * Port of `PhaseRing` in `Us/Features/Cycle/CycleViews.swift`.
 */
@Composable
fun PhaseRing(
    phase: CyclePhase,
    cycleDay: Int,
    cycleLength: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    val target = if (cycleLength > 0) (cycleDay.toFloat() / cycleLength).coerceIn(0.02f, 1f) else 0.02f
    val progress by animateFloatAsState(target, tween(500), label = "phase-ring")

    val next = CycleEngine.phaseProgress(cycleDay, cycleLength)
    val nextTitle = leylaString(next.nextPhase.titleRes).lowercase()
    val nextPhaseText = when {
        next.daysToNextPhase <= 0 -> leylaString(R.string.cycle_ring_starting, nextTitle)
        else -> pluralDays(next.daysToNextPhase, nextTitle)
    }

    Box(modifier.size(RING_SIZE), contentAlignment = Alignment.Center) {
        Ring(progress = progress, color = phase.color)
        Column(
            Modifier.padding(38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                leylaString(phase.titleRes),
                style = IOSText.title2.weight(FontWeight.Bold),
                color = phase.color,
                textAlign = TextAlign.Center,
            )
            Text(
                leylaString(R.string.cycle_ring_day, cycleDay),
                style = IOSText.subheadline.weight(FontWeight.SemiBold),
                color = colors.secondary,
            )
            Text(
                nextPhaseText,
                style = IOSText.caption,
                color = colors.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The pregnancy wheel: progress toward 40 weeks, with the current week and a
 * countdown to the due date centred inside. Port of `PregnancyRing`.
 */
@Composable
fun PregnancyRing(week: Int, daysToDue: Int, modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    val progress by animateFloatAsState(
        (week / 40f).coerceIn(0.02f, 1f), tween(500), label = "pregnancy-ring",
    )

    Box(modifier.size(RING_SIZE), contentAlignment = Alignment.Center) {
        Ring(progress = progress, color = Theme.rose)
        Column(
            Modifier.padding(38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                leylaString(R.string.pregnancy_ring_week, week),
                style = IOSText.largeTitle.weight(FontWeight.Bold),
                color = Theme.rose,
            )
            Text(
                leylaString(R.string.pregnancy_ring_of_40),
                style = IOSText.subheadline.weight(FontWeight.SemiBold),
                color = colors.secondary,
            )
            Text(
                if (daysToDue <= 0) leylaString(R.string.pregnancy_ring_due_any_day)
                else leylaString(R.string.pregnancy_ring_days_to_go, daysToDue),
                style = IOSText.caption,
                color = colors.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The track and the filled arc, drawn from 12 o'clock like SwiftUI's. */
@Composable
private fun Ring(progress: Float, color: Color) {
    Canvas(Modifier.size(RING_SIZE)) {
        val stroke = RING_STROKE.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = color.copy(alpha = 0.16f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = color,
            // -90° puts day 1 at the top; SwiftUI gets there by rotating the view.
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun pluralDays(days: Int, nextTitle: String): String =
    if (days == 1) leylaString(R.string.cycle_ring_one_day_to, nextTitle)
    else leylaString(R.string.cycle_ring_days_to, days, nextTitle)
