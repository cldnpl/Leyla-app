package com.claudianapolitano.leyla.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.TextButton
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.core.WidgetStore
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaCard
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.softShadow
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.together.PrimaryButton
import com.claudianapolitano.leyla.widget.LeylaWidget
import kotlin.math.roundToInt

/**
 * Guides the person through adding the Leyla widget. Port of
 * `Us/Features/Home/AddWidgetGuideView.swift`.
 *
 * iOS can only teach the gesture: WidgetKit gives an app no way to place its
 * own widget, so that screen leads with a screen recording of the real thing.
 * Android does have a way — [LeylaWidget.requestPin] asks the launcher to
 * place it, and the launcher confirms — so the button leads here instead of a
 * video, and the written steps stay underneath for the launchers that refuse
 * the request and for anyone who would rather do it themselves.
 *
 * The preview above the steps is the widget itself, drawn in Compose from the
 * same snapshot the real one reads, so what you are promised is what lands.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWidgetGuideSheet(onDismiss: () -> Unit) {
    val colors = LeylaTheme.colors
    val context = LocalContext.current
    val session by Session.snapshot.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val snapshot = remember { WidgetStore.load(context) }
    val canPin = remember { LeylaWidget.canRequestPin(context) }
    var requested by remember { mutableStateOf(false) }
    /** Bumps on every request, so "Try again" restarts the wait from zero. */
    var attempt by remember { mutableStateOf(0) }
    var added by remember { mutableStateOf(false) }
    /** The request went out but no widget landed — the launcher ignored it. */
    var stalled by remember { mutableStateOf(false) }

    // `requestPinAppWidget` returning true only means the launcher took the
    // request. Some (Xiaomi's, notably) then drop it without a word unless the
    // app may add home-screen shortcuts — so watch for the widget to actually
    // arrive, and own up when it doesn't instead of saying "check your home
    // screen" forever.
    LaunchedEffect(attempt) {
        if (attempt == 0) return@LaunchedEffect
        val before = LeylaWidget.placedCount(context)
        var waitedMs = 0
        while (true) {
            delay(500)
            waitedMs += 500
            if (LeylaWidget.placedCount(context) > before) {
                added = true
                stalled = false
                requested = false
                return@LaunchedEffect
            }
            if (waitedMs >= 6_000) stalled = true
        }
    }

    val partnerName = session.partner?.displayName?.takeIf { it.isNotBlank() }
        ?: leylaString(R.string.your_partner)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundTop,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                leylaString(R.string.widget_title),
                style = IOSText.title3.weight(FontWeight.Bold),
                color = colors.ink,
            )

            WidgetPreview(
                days = snapshot.daysTogether,
                distanceKm = snapshot.distanceKm,
                partnerName = snapshot.partnerName,
            )

            if (canPin) {
                PrimaryButton(
                    text = leylaString(
                        when {
                            added -> R.string.widget_added
                            stalled -> R.string.widget_try_again
                            requested -> R.string.widget_check_home
                            else -> R.string.widget_add_for_me
                        },
                    ),
                    onClick = {
                        stalled = false
                        if (LeylaWidget.requestPin(context)) {
                            requested = true
                            attempt++
                        } else {
                            stalled = true
                        }
                    },
                    icon = Icons.AutoMirrored.Filled.AddToHomeScreen,
                    enabled = !added && (!requested || stalled),
                )

                if (stalled) PinStalledHint()
            }

            LeylaCard(cornerRadius = 20.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (canPin) {
                        Text(
                            leylaString(R.string.widget_or_by_hand),
                            style = IOSText.footnote.weight(FontWeight.SemiBold),
                            color = colors.secondary,
                        )
                    }
                    Step(1, leylaString(R.string.widget_step_1))
                    Step(2, leylaString(R.string.widget_step_2))
                    Step(3, leylaString(R.string.widget_step_3))
                    Step(4, leylaString(R.string.widget_step_4))
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Theme.rose,
                    modifier = Modifier.padding(top = 2.dp).size(14.dp),
                )
                Text(
                    leylaString(R.string.widget_heart_note, partnerName),
                    style = IOSText.footnote,
                    color = colors.secondary,
                )
            }
        }
    }
}

/**
 * The widget as it will look, drawn from the same snapshot the real one reads.
 * iOS shows a recording of the home screen; a live preview is the closer thing
 * here, because the launcher's own gallery already shows the widget and what
 * someone actually wants to know is what it will say.
 */
@Composable
private fun WidgetPreview(days: Int?, distanceKm: Double?, partnerName: String?) {
    val colors = LeylaTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                .size(150.dp)
                .softShadow(28.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Theme.roseGradient)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                days?.toString() ?: leylaString(R.string.widget_dash),
                style = IOSText.largeTitle.weight(FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                leylaString(R.string.widget_days_together),
                style = IOSText.caption2,
                color = Color.White.copy(alpha = 0.9f),
            )
            Text(
                distanceKm?.let { leylaString(R.string.widget_km_apart, it.roundToInt()) }
                    ?: leylaString(R.string.widget_no_distance),
                style = IOSText.caption2,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            partnerName ?: leylaString(R.string.app_name),
            style = IOSText.caption2.weight(FontWeight.Bold),
            color = colors.secondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * Shown when the launcher accepted the pin request but nothing landed. On
 * Xiaomi that is the "Home screen shortcuts" permission, off by default for
 * apps not installed from their store; elsewhere the manual steps still work.
 */
@Composable
private fun PinStalledHint() {
    val colors = LeylaTheme.colors
    val context = LocalContext.current
    val isXiaomi = remember { LeylaWidget.isXiaomi() }
    LeylaCard(cornerRadius = 20.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                leylaString(R.string.widget_nothing_happened),
                style = IOSText.subheadline.weight(FontWeight.SemiBold),
                color = colors.ink,
            )
            Text(
                leylaString(if (isXiaomi) R.string.widget_xiaomi_hint else R.string.widget_generic_hint),
                style = IOSText.footnote,
                color = colors.secondary,
            )
            if (isXiaomi) {
                TextButton(onClick = { LeylaWidget.openShortcutPermission(context) }) {
                    Text(
                        leylaString(R.string.widget_open_permissions),
                        style = IOSText.subheadline.weight(FontWeight.SemiBold),
                        color = Theme.rose,
                    )
                }
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    val colors = LeylaTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(Theme.rose),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number.toString(),
                style = IOSText.headline.weight(FontWeight.Bold),
                color = Color.White,
            )
        }
        Text(
            text,
            style = IOSText.subheadline,
            color = colors.ink,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
