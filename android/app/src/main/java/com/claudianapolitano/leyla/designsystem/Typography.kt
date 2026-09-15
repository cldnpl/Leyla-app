package com.claudianapolitano.leyla.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * SwiftUI's semantic text styles, restated in Compose so the port can be read
 * side by side with the Swift it came from.
 *
 * The sizes are iOS's Dynamic Type defaults at the "Large" setting, which is
 * what the App Store screenshots were taken at. iOS's `design: .rounded` has no
 * stock Android counterpart, so those call sites fall back to the platform
 * sans — the weight and size still carry the emphasis.
 */
object IOSText {
    val largeTitle = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Normal)

    /**
     * The onboarding headline: iOS's `.largeTitle` in the rounded design. Its
     * line height is explicit because these titles wrap to two lines, and a
     * size set without one falls back to the theme's body leading — which draws
     * the second line straight through the first.
     */
    val display = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold)
    val title = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal)
    val title2 = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal)
    val title3 = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal)
    val headline = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val subheadline = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    val footnote = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
    val caption2 = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Normal)
}

/** `.font(.headline)` → `IOSText.headline`; `.font(.subheadline.weight(.semibold))` → this. */
fun TextStyle.weight(weight: FontWeight): TextStyle = copy(fontWeight = weight)

internal val LeylaTypography = Typography(
    displayLarge = IOSText.largeTitle,
    headlineLarge = IOSText.title,
    headlineMedium = IOSText.title2,
    headlineSmall = IOSText.title3,
    titleMedium = IOSText.headline,
    bodyLarge = IOSText.body,
    bodyMedium = IOSText.subheadline,
    bodySmall = IOSText.footnote,
    labelMedium = IOSText.caption,
    labelSmall = IOSText.caption2,
)
