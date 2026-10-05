package com.claudianapolitano.leyla.feature.together

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme

/**
 * Maps a catalog `colorKey` to the soft two-tone card gradient. Port of the iOS
 * `QuizPalette` enum.
 *
 * These cards carry `LeylaColors.ink` text, which is near-black on light and
 * off-white on dark — so the cards themselves have to flip with it. Each hue
 * keeps its identity in both schemes: a pale wash on light, a deep saturated
 * version of the same hue on dark.
 */
object QuizPalette {

    @Composable
    @ReadOnlyComposable
    fun colors(key: String): List<Color> {
        val dark = LeylaTheme.colors.isDark
        return when (key) {
            "purple" -> listOf(
                d(dark, 0.83f, 0.78f, 0.98f, 0.24f, 0.19f, 0.38f),
                d(dark, 0.90f, 0.86f, 1.0f, 0.17f, 0.14f, 0.28f),
            )
            "pink" -> listOf(
                d(dark, 1.0f, 0.80f, 0.88f, 0.38f, 0.18f, 0.28f),
                d(dark, 1.0f, 0.89f, 0.93f, 0.28f, 0.13f, 0.21f),
            )
            "red" -> listOf(
                d(dark, 1.0f, 0.78f, 0.78f, 0.38f, 0.17f, 0.17f),
                d(dark, 1.0f, 0.87f, 0.86f, 0.28f, 0.12f, 0.12f),
            )
            "amber" -> listOf(
                d(dark, 1.0f, 0.88f, 0.70f, 0.36f, 0.25f, 0.10f),
                d(dark, 1.0f, 0.93f, 0.80f, 0.27f, 0.19f, 0.08f),
            )
            "green" -> listOf(
                d(dark, 0.80f, 0.93f, 0.83f, 0.15f, 0.32f, 0.21f),
                d(dark, 0.89f, 0.96f, 0.90f, 0.11f, 0.24f, 0.16f),
            )
            "blue" -> listOf(
                d(dark, 0.80f, 0.89f, 1.0f, 0.15f, 0.26f, 0.42f),
                d(dark, 0.89f, 0.94f, 1.0f, 0.11f, 0.20f, 0.32f),
            )
            else -> listOf(
                d(dark, 1.0f, 0.85f, 0.88f, 0.34f, 0.20f, 0.24f),
                d(dark, 1.0f, 0.91f, 0.85f, 0.26f, 0.16f, 0.19f),
            )
        }
    }

    /**
     * SwiftUI's `.topLeading → .bottomTrailing` gradient. Compose's
     * `linearGradient` defaults to top-left → bottom-right over the drawn box
     * when both endpoints are left at their defaults, which is the same run.
     */
    @Composable
    @ReadOnlyComposable
    fun gradient(key: String, alpha: Float = 1f): Brush =
        Brush.linearGradient(colors(key).map { it.copy(alpha = it.alpha * alpha) })

    /**
     * A stronger tint of the same hue for accents (progress bar, badge text).
     * Lifted in dark mode so it still separates from the deep card behind it.
     */
    @Composable
    @ReadOnlyComposable
    fun accent(key: String): Color {
        val dark = LeylaTheme.colors.isDark
        return when (key) {
            "purple" -> d(dark, 0.55f, 0.40f, 0.90f, 0.72f, 0.62f, 1.0f)
            "pink" -> d(dark, 0.93f, 0.35f, 0.60f, 1.0f, 0.55f, 0.76f)
            "red" -> d(dark, 0.90f, 0.30f, 0.30f, 1.0f, 0.52f, 0.52f)
            "amber" -> d(dark, 0.90f, 0.60f, 0.15f, 1.0f, 0.76f, 0.35f)
            "green" -> d(dark, 0.20f, 0.65f, 0.40f, 0.42f, 0.85f, 0.60f)
            "blue" -> d(dark, 0.25f, 0.50f, 0.90f, 0.50f, 0.72f, 1.0f)
            else -> Theme.coral
        }
    }

    /** A colour with a light-mode and a dark-mode value. */
    private fun d(
        dark: Boolean,
        lr: Float, lg: Float, lb: Float,
        dr: Float, dg: Float, db: Float,
    ): Color = if (dark) Color(dr, dg, db) else Color(lr, lg, lb)
}

/** The error red, lifted in dark mode the way iOS's dynamic `.systemRed` is. */
@Composable
@ReadOnlyComposable
fun errorRed(): Color =
    if (LeylaTheme.colors.isDark) Color(1.0f, 0.55f, 0.55f) else Color(0.85f, 0.18f, 0.16f)
