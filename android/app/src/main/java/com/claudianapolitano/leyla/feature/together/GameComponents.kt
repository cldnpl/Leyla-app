package com.claudianapolitano.leyla.feature.together

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.softShadow
import com.claudianapolitano.leyla.designsystem.weight

/**
 * The small shared pieces every Games screen is built from. Ports of the views
 * that live alongside the iOS quiz models: `QuizIconTile`, `StepDots`,
 * `YourTurnHint`, `ProgressBar`, `TurnBadge`, `PillButtonStyle`,
 * `PrimaryButtonStyle` and `PremiumLockBadge`.
 */

/** A rounded, tinted tile holding a category/quiz icon. */
@Composable
fun QuizIconTile(
    icon: String?,
    colorKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
) {
    QuizIconTile(sfSymbolIcon(icon), colorKey, modifier, size)
}

@Composable
fun QuizIconTile(
    icon: ImageVector,
    colorKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
) {
    val colors = LeylaTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = QuizPalette.accent(colorKey),
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/**
 * The row of capsule step indicators shared by every multi-step play flow
 * (quiz, know-me, debate): the current step is a wide capsule, already-answered
 * steps are tinted, the rest are grey.
 */
@Composable
fun StepDots(
    total: Int,
    index: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    isDone: (Int) -> Boolean = { false },
) {
    val colors = LeylaTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxOf(total, 1)) { i ->
            val fill = when {
                i == index -> accent
                isDone(i) -> accent.copy(alpha = 0.4f)
                else -> colors.secondary.copy(alpha = 0.2f)
            }
            Box(
                Modifier
                    .width(if (i == index) 22.dp else 8.dp)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(fill),
            )
        }
    }
}

/**
 * "Sam finished — your turn": one consistent nudge for every place where one of
 * you has answered something the other hasn't, so nobody has to open a quiz to
 * find out whose move it is.
 */
@Composable
fun YourTurnHint(
    partnerName: String,
    accent: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = if (compact) "Your turn" else "$partnerName finished — your turn",
            style = IOSText.caption.weight(FontWeight.Bold),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Prominent "WAITING" / "YOUR TURN" pill used on hero game cards so a glance
 * tells you whose move it is without opening the game.
 */
@Composable
fun TurnBadge(text: String, accent: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = IOSText.caption2.weight(FontWeight.Bold).copy(letterSpacing = 1.sp),
        color = Color.White,
        modifier = modifier
            .clip(CircleShape)
            .background(accent)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun ProgressBar(value: Double, accent: Color, modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(colors.surface),
    ) {
        Box(
            Modifier
                .fillMaxWidth(value.coerceIn(0.0, 1.0).toFloat())
                .height(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
    }
}

/** The little padlock pill stamped on locked quiz packs and game cards. */
@Composable
fun PremiumLockBadge(modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(
        modifier = modifier
            .softShadow(20.dp)
            .clip(CircleShape)
            .background(Theme.warmGradient)
            .padding(horizontal = if (compact) 7.dp else 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(if (compact) 9.dp else 10.dp),
        )
        if (!compact) {
            Text(
                "PREMIUM",
                style = IOSText.caption2.weight(FontWeight.Bold).copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
                color = Color.White,
            )
        }
    }
}

/**
 * Pill-shaped primary action used in every play flow's nav bar. Port of
 * `PillButtonStyle`.
 */
@Composable
fun PillButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (enabled) color else color.copy(alpha = 0.4f))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Text(text, style = IOSText.headline, color = Color.White)
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Full-width coral action button. Port of `PrimaryButtonStyle`. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Theme.coral else Theme.coral.copy(alpha = 0.4f))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text(text, style = IOSText.headline, color = Color.White)
            }
        }
    }
}

/** A flat, text-only action — iOS's bare `Button` with a tinted label. */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = LeylaTheme.colors.secondary,
    style: androidx.compose.ui.text.TextStyle = IOSText.footnote.weight(FontWeight.Bold),
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        }
        Text(text, style = style, color = color)
    }
}

/**
 * The card body shared by the "Quiz" entry card and the game cards: an opaque
 * plate with a hairline rim and a soft lift, optionally tappable.
 */
@Composable
fun MaterialCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val colors = LeylaTheme.colors
    val base = modifier
        .fillMaxWidth()
        .softShadow(cornerRadius)
    Surface(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        shape = RoundedCornerShape(cornerRadius),
        color = colors.card,
        border = BorderStroke(1.dp, colors.hairline),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/**
 * A card painted with a catalog hue's gradient rather than the neutral plate —
 * the treatment every list row on this tab uses.
 */
@Composable
fun GradientCard(
    colorKey: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    contentPadding: Dp = 16.dp,
    alpha: Float = 1f,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .softShadow(cornerRadius)
        .clip(RoundedCornerShape(cornerRadius))
        .background(QuizPalette.gradient(colorKey, alpha))
    Column(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base)
            .padding(contentPadding),
        content = content,
    )
}

/** Centred spinner / error text shared by every list screen while it loads. */
@Composable
fun LoadingOrError(
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val colors = LeylaTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Theme.rose)
        } else if (errorMessage != null) {
            Text(
                errorMessage,
                style = IOSText.footnote,
                color = colors.secondary,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                TextAction("Retry", onRetry, color = Theme.rose)
            }
        }
    }
}

/** A selectable outline used by the option rows, drawn only when picked. */
fun Modifier.selectionBorder(selected: Boolean, accent: Color, cornerRadius: Dp, width: Dp = 2.dp): Modifier =
    if (selected) border(width, accent, RoundedCornerShape(cornerRadius)) else this

/** Ripple-free tap, matching SwiftUI's `.buttonStyle(.plain)` cards. */
@Composable
fun Modifier.plainClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = clickable(
    enabled = enabled,
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick,
)
