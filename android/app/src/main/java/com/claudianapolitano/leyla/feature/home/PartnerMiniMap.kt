package com.claudianapolitano.leyla.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.claudianapolitano.leyla.BuildConfig
import com.claudianapolitano.leyla.core.Coordinate
import com.claudianapolitano.leyla.designsystem.Avatar
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlin.math.abs

/**
 * The two of you on a map. iOS uses MapKit with a region derived from the two
 * coordinates; here the same framing is expressed as `LatLngBounds` with the
 * identical 1.8× padding and 0.04° floor, so the two builds frame a given pair
 * of points the same way.
 *
 * The map is non-interactive: on both platforms it is a *preview* that opens
 * the full map when tapped, so every gesture belongs to the card, not the map.
 */
@Composable
fun PartnerMiniMap(
    mine: Coordinate,
    partner: Coordinate,
    myName: String,
    partnerName: String,
    myAvatarPath: String?,
    partnerAvatarPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (BuildConfig.MAPS_API_KEY.isBlank()) {
        MissingMapsKeyPanel(modifier)
        return
    }

    val bounds = remember(mine, partner) { framing(mine, partner) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng((mine.latitude + partner.latitude) / 2, (mine.longitude + partner.longitude) / 2),
            2f,
        )
    }

    Box(modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.NORMAL),
            uiSettings = MapUiSettings(
                compassEnabled = false,
                mapToolbarEnabled = false,
                rotationGesturesEnabled = false,
                scrollGesturesEnabled = false,
                tiltGesturesEnabled = false,
                zoomControlsEnabled = false,
                zoomGesturesEnabled = false,
            ),
            onMapLoaded = {
                runCatching {
                    cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 48))
                }
            },
        ) {
            MapPin(mine, myName, myAvatarPath)
            MapPin(partner, partnerName, partnerAvatarPath)
        }
        // The card owns the tap. A GoogleMap swallows touches even with every
        // gesture disabled, so the tap target is this transparent overlay rather
        // than the card behind it — SwiftUI gets there with
        // `.allowsHitTesting(false)` on the map.
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
        )
    }
}

@Composable
private fun MapPin(at: Coordinate, name: String, avatarPath: String?) {
    val colors = LeylaTheme.colors
    val markerState = rememberUpdatedMarkerState(position = LatLng(at.latitude, at.longitude))
    MarkerComposable(keys = arrayOf<Any>(name, avatarPath ?: "", colors.isDark), state = markerState) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.solidBackground)
                    .padding(2.dp),
            ) {
                Avatar(path = avatarPath, name = name, size = 30.dp)
            }
            Text(
                name,
                style = IOSText.caption2.weight(FontWeight.Bold),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                // Material, not white: the map itself goes dark in dark mode and
                // the label's text follows the scheme — on a fixed white capsule
                // that would be white-on-white.
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.solidBackground)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

/**
 * Shown in place of the map when no Google Maps key is configured. Keeps the
 * card's shape and the rest of Home intact instead of handing the user a grey
 * "for development purposes only" tile grid.
 */
@Composable
private fun MissingMapsKeyPanel(modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    // Follows the scheme: the warm wash would turn muddy brown sitting on a
    // dark card, so dark mode gets a plum tint of the same weight instead.
    val wash = if (colors.isDark) {
        listOf(Theme.rose.copy(alpha = 0.14f), Theme.coral.copy(alpha = 0.10f))
    } else {
        listOf(Theme.blush.copy(alpha = 0.45f), Theme.peach.copy(alpha = 0.55f))
    }
    Box(
        modifier = modifier.background(colors.card).background(Brush.linearGradient(wash)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Map,
            contentDescription = null,
            tint = Theme.rose,
            modifier = Modifier.size(34.dp),
        )
    }
}

/**
 * iOS's region maths, kept literally: centre on the midpoint, span 1.8× the
 * separation plus a 0.04° floor so two neighbours don't get a street-level
 * zoom.
 */
private fun framing(mine: Coordinate, partner: Coordinate): LatLngBounds {
    val midLat = (mine.latitude + partner.latitude) / 2
    val midLng = (mine.longitude + partner.longitude) / 2
    val latDelta = abs(mine.latitude - partner.latitude) * 1.8 + 0.04
    val lngDelta = abs(mine.longitude - partner.longitude) * 1.8 + 0.04
    return LatLngBounds(
        LatLng((midLat - latDelta / 2).coerceAtLeast(-85.0), midLng - lngDelta / 2),
        LatLng((midLat + latDelta / 2).coerceAtMost(85.0), midLng + lngDelta / 2),
    )
}

