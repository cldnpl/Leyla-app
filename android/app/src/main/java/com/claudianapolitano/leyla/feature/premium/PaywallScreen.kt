package com.claudianapolitano.leyla.feature.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.feature.together.MaterialCard
import com.claudianapolitano.leyla.feature.together.PrimaryButton
import com.claudianapolitano.leyla.feature.together.QuizIconTile
import com.claudianapolitano.leyla.feature.together.plainClickable

/**
 * Where the paywall was opened from — only changes the headline. Port of
 * `PaywallTrigger`.
 */
sealed interface PaywallTrigger {
    data object General : PaywallTrigger
    data class QuizCategory(val title: String) : PaywallTrigger
    data class Game(val title: String) : PaywallTrigger

    val headline: String
        get() = when (this) {
            is General -> "Unlock everything"
            is QuizCategory -> "Unlock $title and every other pack"
            is Game -> "Unlock $title and every other game"
        }
}

/**
 * The Leyla Premium paywall, shown whenever someone taps a locked quiz pack or
 * game. Port of `PaywallView.swift`.
 *
 * An animated phone plays through what's behind the lock, then the perks, then
 * one price. The purchase button is wired to [PremiumStore], which is waiting on
 * Play Billing — until that lands it reports the store as unavailable rather
 * than pretending to charge anyone.
 */
@Composable
fun PaywallScreen(
    trigger: PaywallTrigger,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    val isPremium by PremiumStore.isPremium.collectAsStateWithLifecycle()
    val storeError by PremiumStore.errorMessage.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "LEYLA PREMIUM",
                    style = IOSText.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp),
                    color = Theme.rose,
                )
                Text(
                    trigger.headline,
                    style = IOSText.title.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "One subscription for both of you — every quiz pack, every game, no limits.",
                    style = IOSText.subheadline,
                    color = colors.secondary,
                    textAlign = TextAlign.Center,
                )
            }

            PaywallPhone()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Perk(
                    "square.grid.2x2.fill", "purple", "All 12 quiz packs",
                    "Sex & Love, Money, Travel, Family, Values and more — beyond the two free ones.",
                )
                Perk(
                    "gamecontroller.fill", "pink", "Every game unlocked",
                    "Couples Debate, Draw Together and Snap Hunt, plus everything we add next.",
                )
                Perk(
                    "infinity", "blue", "No limits",
                    "Unlimited photos in your gallery and journal, and the full storage quota.",
                )
                Perk(
                    "heart.fill", "green", "Covers you both",
                    "One subscription unlocks Premium for you and your partner.",
                )
            }

            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (storeError != null) {
                    Text(
                        storeError!!,
                        style = IOSText.footnote,
                        color = Theme.coral,
                        textAlign = TextAlign.Center,
                    )
                }
                PrimaryButton(
                    text = "Unlock everything · ${PremiumStore.priceLine}",
                    icon = Icons.Filled.AutoAwesome,
                    onClick = {
                        // Play Billing is not connected yet; unlocking here
                        // would hand out Premium for free, so the button says
                        // what is actually true instead.
                        PremiumStore.setEntitled(isPremium)
                    },
                    enabled = false,
                )
                Text(
                    "Subscriptions are coming to Android shortly. Auto-renews monthly, cancel anytime in Google Play.",
                    style = IOSText.caption2,
                    color = colors.secondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Close button, floating over the scroll the way the iOS overlay does.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp, top = 12.dp)
                .clip(CircleShape)
                .background(colors.card)
                .plainClickable(onClick = onDismiss)
                .padding(10.dp),
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close",
                tint = colors.secondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun Perk(icon: String, colorKey: String, title: String, body: String) {
    val colors = LeylaTheme.colors
    MaterialCard(cornerRadius = 20.dp, contentPadding = 14.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            QuizIconTile(icon, colorKey, size = 42.dp)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = IOSText.subheadline.copy(fontWeight = FontWeight.Bold), color = colors.ink)
                Text(body, style = IOSText.footnote, color = colors.secondary)
            }
        }
    }
}
