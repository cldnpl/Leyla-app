package com.claudianapolitano.leyla.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.Coordinate
import com.claudianapolitano.leyla.designsystem.Avatar
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.softShadow
import com.claudianapolitano.leyla.designsystem.weight
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A card showing both partners on a map plus the distance connector. Appears on
 * Home only when both people are sharing their location (or in demo mode).
 * Port of `Us/Features/Home/DistanceViews.swift`.
 */
@Composable
fun DistanceMapCard(
    mine: Coordinate,
    partner: Coordinate,
    myName: String,
    partnerName: String,
    km: Double,
    myAvatarPath: String?,
    partnerAvatarPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().softShadow(26.dp),
        shape = RoundedCornerShape(26.dp),
        color = colors.card,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PartnerMiniMap(
                mine = mine,
                partner = partner,
                myName = myName,
                partnerName = partnerName,
                myAvatarPath = myAvatarPath,
                partnerAvatarPath = partnerAvatarPath,
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )
            DistanceConnector(myName = myName, partnerName = partnerName, km = km)
        }
    }
}

/**
 * "You ──♥── Alex" with the km distance. The connecting line grows longer the
 * farther apart the two people are.
 */
@Composable
fun DistanceConnector(myName: String, partnerName: String, km: Double, modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    val distanceText = if (km < 1) {
        stringResource(R.string.distance_less_than_km)
    } else {
        stringResource(R.string.distance_km_apart, km.roundToInt())
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(distanceText, style = IOSText.title3.weight(FontWeight.Bold), color = Theme.rose)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ConnectorName(myName, Modifier.weight(1f, fill = false))
            LineWithHeart(km)
            ConnectorName(partnerName, Modifier.weight(1f, fill = false))
        }
    }
}

@Composable
private fun ConnectorName(name: String, modifier: Modifier = Modifier) {
    Text(
        name,
        style = IOSText.subheadline.weight(FontWeight.SemiBold),
        color = LeylaTheme.colors.ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

/** Line length scales with distance (with a cap so it always fits the card). */
@Composable
private fun LineWithHeart(km: Double) {
    val colors = LeylaTheme.colors
    val lineWidth = min(170.0, 38.0 + sqrt(km) * 9).dp
    Box(contentAlignment = Alignment.Center, modifier = Modifier.clearAndSetSemantics {}) {
        val dashColor = Theme.rose.copy(alpha = 0.65f)
        Canvas(Modifier.width(lineWidth).height(24.dp)) {
            drawLine(
                color = dashColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx())),
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.solidBackground)
                .padding(4.dp),
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = Theme.rose,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}
