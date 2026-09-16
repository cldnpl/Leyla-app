package com.claudianapolitano.leyla.feature.cycle

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaCard
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.together.PrimaryButton
import com.claudianapolitano.leyla.feature.together.TextAction
import com.claudianapolitano.leyla.feature.together.plainClickable

/**
 * Health Connect identification, shown wherever cycle data appears. Port of
 * `Us/Features/Cycle/AppleHealthViews.swift`.
 *
 * iOS is explicit about the HealthKit integration because App Review asks it to
 * be (guideline 2.5.1). Google has no equivalent rule, but Play's Health Apps
 * policy does expect the same things said plainly — what is read, that nothing
 * is written, and where the user takes it back — and it is the right thing to
 * tell someone regardless, so the copy is kept in step with iOS.
 */
object HealthConnect {
    /** Opens Health Connect's own settings, wherever it lives on this release. */
    fun settingsIntent(): Intent = Intent(ACTION_HEALTH_CONNECT_SETTINGS)

    private const val ACTION_HEALTH_CONNECT_SETTINGS =
        "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
}

/**
 * A small, unmistakable "Health Connect" pill, placed at the top of every
 * cycle-related card so the source is obvious at a glance. Port of
 * `AppleHealthBadge`.
 */
@Composable
fun HealthConnectBadge(modifier: Modifier = Modifier) {
    val red = HealthRed
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(red.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.MonitorHeart,
            contentDescription = leylaString(R.string.health_source_accessibility),
            tint = red,
            modifier = Modifier.size(14.dp),
        )
        Text(
            leylaString(R.string.health_source),
            style = IOSText.caption.weight(FontWeight.SemiBold),
            color = red,
        )
    }
}

/**
 * Full card identifying the Health Connect integration, with the connect action
 * when access hasn't been granted yet. Port of `AppleHealthCard`.
 */
@Composable
fun HealthConnectCard(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    statusDetail: String? = null,
    isBusy: Boolean = false,
    onConnect: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
) {
    val colors = LeylaTheme.colors
    val context = LocalContext.current

    LeylaCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.MonitorHeart,
                    contentDescription = null,
                    tint = Theme.rose,
                    modifier = Modifier.width(34.dp).size(26.dp),
                )
                Column(
                    Modifier.weight(1f).padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        leylaString(R.string.health_source),
                        style = IOSText.headline,
                        color = colors.ink,
                    )
                    Text(
                        statusDetail ?: leylaString(
                            if (isConnected) R.string.health_connected_detail
                            else R.string.health_not_connected,
                        ),
                        style = IOSText.subheadline,
                        color = colors.secondary,
                    )
                }
                if (isConnected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = leylaString(R.string.health_connected),
                        tint = Color(0.20f, 0.66f, 0.33f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Text(leylaString(R.string.health_reads_line), style = IOSText.subheadline, color = colors.secondary)
            Text(leylaString(R.string.health_other_apps_line), style = IOSText.footnote, color = colors.secondary)
            Text(leylaString(R.string.health_never_writes_line), style = IOSText.footnote, color = colors.secondary)

            if (onConnect != null && !isConnected) {
                PrimaryButton(
                    text = leylaString(R.string.health_connect_action),
                    onClick = onConnect,
                    icon = Icons.Filled.MonitorHeart,
                    loading = isBusy,
                    enabled = !isBusy,
                )
            }

            if (onRefresh != null) {
                TextAction(
                    text = leylaString(R.string.health_check_again),
                    onClick = onRefresh,
                    icon = Icons.Filled.Refresh,
                    color = Theme.rose,
                    style = IOSText.subheadline.weight(FontWeight.SemiBold),
                    enabled = !isBusy,
                )
            }

            TextAction(
                text = leylaString(R.string.health_open_settings),
                onClick = {
                    runCatching { context.startActivity(HealthConnect.settingsIntent()) }
                },
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                color = Theme.rose,
                style = IOSText.subheadline.weight(FontWeight.SemiBold),
            )

            Text(leylaString(R.string.health_manage_line), style = IOSText.caption, color = colors.secondary)
        }
    }
}

/**
 * Compact one-line identification: icon, name, connection status, chevron.
 * Tapping opens the full explanation. Port of `AppleHealthRow`.
 *
 * This is what the cycle screen shows once tracking is running — the whole
 * explanation is a tap away rather than half a screen of copy on every visit.
 */
@Composable
fun HealthConnectRow(
    isConnected: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    statusDetail: String? = null,
) {
    val colors = LeylaTheme.colors
    LeylaCard(modifier.plainClickable(onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.MonitorHeart,
                contentDescription = null,
                tint = Theme.rose,
                modifier = Modifier.width(28.dp).size(22.dp),
            )
            Text(
                leylaString(R.string.health_source),
                style = IOSText.subheadline.weight(FontWeight.SemiBold),
                color = colors.ink,
            )
            Box(Modifier.weight(1f))
            Text(
                statusDetail ?: leylaString(
                    if (isConnected) R.string.health_connected else R.string.health_not_connected,
                ),
                style = IOSText.subheadline,
                color = colors.secondary,
                maxLines = 1,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.secondary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** One-line "where this comes from" note, for screens showing cycle numbers. */
@Composable
fun HealthConnectSourceNote(modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.MonitorHeart,
            contentDescription = null,
            tint = colors.secondary,
            modifier = Modifier.size(14.dp).padding(end = 0.dp),
        )
        Text(
            "  " + leylaString(R.string.health_source_note),
            style = IOSText.caption,
            color = colors.secondary,
        )
    }
}

/**
 * The full Health Connect explanation on its own screen, so the cycle screen
 * can stay about the cycle. Port of `AppleHealthDetailView`.
 */
@Composable
fun HealthConnectDetailScreen(
    isConnected: Boolean,
    onRefresh: (() -> Unit)?,
    isBusy: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        HealthConnectCard(isConnected = isConnected, isBusy = isBusy, onRefresh = onRefresh)
    }
}

/** Progress spinner sized for inline use inside a card. */
@Composable
fun InlineSpinner(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        color = Theme.rose,
        strokeWidth = 2.dp,
        modifier = modifier.size(20.dp),
    )
}
