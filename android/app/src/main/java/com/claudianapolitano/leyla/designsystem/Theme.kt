package com.claudianapolitano.leyla.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * Shared visual language for Leyla — warm blush→coral→peach gradients, rounded
 * cards. Port of the iOS `Theme` enum.
 *
 * The brand hues (blush/coral/peach/rose) are fixed: they are the identity, and
 * they read fine against both a light and a dark backdrop. Anything used as
 * *text* or as a *surface* adapts to the colour scheme instead, because those
 * are the pairings that decide legibility — [LeylaColors] carries those.
 */
object Theme {
    val blush = Color(1.0f, 0.71f, 0.76f)
    val coral = Color(1.0f, 0.42f, 0.42f)
    val peach = Color(1.0f, 0.85f, 0.73f)

    /**
     * Brand rose used across the app (chrome, hero, accents). The soft warm pink
     * sampled straight from the app icon's gradient (its mid-tone), so the whole
     * app matches the icon instead of the old darker magenta.
     */
    val rose = Color(1.0f, 0.55f, 0.57f)

    /** Rose gradient for the hero "miss you" button (matches the chrome). */
    val roseGradient: Brush
        get() = diagonalGradient(listOf(rose, rose.copy(alpha = 0.82f)))

    val warmGradient: Brush
        get() = diagonalGradient(listOf(blush, coral.copy(alpha = 0.85f), peach))

    /**
     * SwiftUI's `.topLeading → .bottomTrailing` gradient always spans the drawn
     * box; Compose's `linearGradient` wants absolute pixels, so resolve the end
     * point from the actual draw size instead of guessing at a constant.
     */
    private fun diagonalGradient(colors: List<Color>): Brush = object : ShaderBrush() {
        override fun createShader(size: Size): Shader = LinearGradientShader(
            from = Offset.Zero,
            to = Offset(size.width, size.height),
            colors = colors,
        )
    }
}

/**
 * The colour-scheme-dependent half of the palette. Matches the iOS
 * `Color(dynamic:dark:)` pairs one for one.
 */
data class LeylaColors(
    /**
     * Primary text colour. Near-black on light, warm off-white on dark — it is
     * drawn on translucent cards and on [background], both of which invert with
     * the colour scheme, so a fixed near-black would disappear.
     */
    val ink: Color,
    /** Secondary text (SwiftUI `.secondary`). */
    val secondary: Color,
    /** Tertiary text — the chevrons on cards (SwiftUI `.tertiary`). */
    val tertiary: Color,
    /** Translucent plate for pills and tiles that sit on [background]. */
    val surface: Color,
    /** Hairline rim used on cards/tiles. White-ish on light, barely-there on dark. */
    val hairline: Color,
    /**
     * SwiftUI `.regularMaterial` stand-in: the card fill.
     *
     * Opaque on purpose. A translucent fill lets the card's own drop shadow
     * show through from underneath, which paints a visible darker ring ~20dp
     * inside every edge — so this is the flattened result of the material over
     * [backgroundTop] instead, which is what the eye reads anyway over a
     * gradient this slow.
     */
    val card: Color,
    /** SwiftUI `.background`: the opaque plate behind the heart on the connector. */
    val solidBackground: Color,
    /** The two stops of the full-screen app backdrop. */
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val isDark: Boolean,
) {
    /**
     * Full-screen app backdrop. In light mode it's the warm peach/blush wash;
     * in dark mode a deep warm plum, so it still reads as *ours* without
     * washing out to muddy maroon over black.
     */
    val background: Brush
        get() = Brush.verticalGradient(listOf(backgroundTop, backgroundBottom))
}

private val LightColors = LeylaColors(
    ink = Color(0.18f, 0.16f, 0.20f),
    secondary = Color(0.18f, 0.16f, 0.20f).copy(alpha = 0.60f),
    tertiary = Color(0.18f, 0.16f, 0.20f).copy(alpha = 0.30f),
    surface = Color.White.copy(alpha = 0.55f),
    hairline = Color.White.copy(alpha = 0.35f),
    card = Color(0xFFFEFBFA),
    solidBackground = Color.White,
    // peach @ 35% and blush @ 25%, flattened over white the way iOS composites
    // them onto the window.
    backgroundTop = Color(0xFFFAF0E8),
    backgroundBottom = Color(0xFFFFF3F4),
    isDark = false,
)

private val DarkColors = LeylaColors(
    ink = Color(0.96f, 0.94f, 0.95f),
    secondary = Color(0.96f, 0.94f, 0.95f).copy(alpha = 0.62f),
    tertiary = Color(0.96f, 0.94f, 0.95f).copy(alpha = 0.32f),
    surface = Color.White.copy(alpha = 0.10f),
    hairline = Color.White.copy(alpha = 0.12f),
    card = Color(0xFF2E2630),
    solidBackground = Color(0xFF1B141B),
    backgroundTop = Color(0.13f, 0.09f, 0.13f),
    backgroundBottom = Color(0.09f, 0.07f, 0.10f),
    isDark = true,
)

val LocalLeylaColors = staticCompositionLocalOf { LightColors }

/** Shorthand for the scheme-aware palette: `LeylaTheme.colors.ink`. */
object LeylaTheme {
    val colors: LeylaColors
        @Composable @ReadOnlyComposable get() = LocalLeylaColors.current
}

@Composable
fun LeylaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val material = if (darkTheme) {
        darkColorScheme(
            primary = Theme.rose,
            onPrimary = Color.White,
            background = colors.backgroundBottom,
            onBackground = colors.ink,
            surface = colors.backgroundBottom,
            onSurface = colors.ink,
            onSurfaceVariant = colors.secondary,
            error = Theme.coral,
        )
    } else {
        lightColorScheme(
            primary = Theme.rose,
            onPrimary = Color.White,
            background = colors.backgroundTop,
            onBackground = colors.ink,
            surface = colors.backgroundTop,
            onSurface = colors.ink,
            onSurfaceVariant = colors.secondary,
            error = Theme.coral,
        )
    }
    CompositionLocalProvider(LocalLeylaColors provides colors) {
        MaterialTheme(colorScheme = material, typography = LeylaTypography, content = content)
    }
}
