package com.claudianapolitano.leyla.feature.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudianapolitano.leyla.BuildConfig
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.core.Coordinate
import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.LocationRepository
import com.claudianapolitano.leyla.core.PartnerLocation
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.core.SharedConfig
import com.claudianapolitano.leyla.designsystem.Avatar
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaCard
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// The same demo pair Home uses, so the two screens agree before real sharing is
// switched on: Claudia in Naples, Alex in Tashkent.
private val SAMPLE_MINE = Coordinate(40.8518, 14.2681)
private val SAMPLE_PARTNER = Coordinate(41.2995, 69.2401)

/** The full-screen map behind Home's distance card. Port of `PartnerMapView`. */
data class PartnerMapUiState(
    val mine: Coordinate? = null,
    val partner: Coordinate? = null,
    val myName: String = "",
    val partnerName: String = "Partner",
    val myAvatarPath: String? = null,
    val partnerAvatarPath: String? = null,
    val partnerSharing: Boolean = false,
    val isSharing: Boolean = false,
    val permissionDenied: Boolean = false,
    val km: Double? = null,
)

class PartnerMapViewModel : ViewModel() {

    private val _state = MutableStateFlow(PartnerMapUiState())
    val state: StateFlow<PartnerMapUiState> = _state.asStateFlow()

    private var partnerLocation: PartnerLocation? = null

    init {
        viewModelScope.launch {
            combine(
                Session.snapshot,
                LocationRepository.currentLocation,
                LocationRepository.isSharing,
                LocationRepository.permissionDenied,
            ) { session, myLocation, sharing, denied ->
                Quad(session, myLocation.takeIf { sharing }, sharing, denied)
            }.collect { (session, realMine, sharing, denied) ->
                val mine = realMine ?: SAMPLE_MINE.takeIf { SharedConfig.DEMO_MODE }
                val partner = realPartner() ?: SAMPLE_PARTNER.takeIf { SharedConfig.DEMO_MODE }
                _state.update {
                    it.copy(
                        mine = mine,
                        partner = partner,
                        myName = session.user?.displayName
                            ?: if (SharedConfig.DEMO_MODE) "Claudia" else "You",
                        partnerName = partnerName(partnerLocation?.partnerName ?: session.partner?.displayName),
                        myAvatarPath = session.user?.avatarPath,
                        partnerAvatarPath = session.partner?.avatarPath,
                        partnerSharing = partnerLocation?.sharing == true,
                        isSharing = sharing,
                        permissionDenied = denied,
                        km = LocationRepository.kmBetween(mine, partner),
                    )
                }
                Session.publishDistance(_state.value.km)
            }
        }
        startPolling()
    }

    /**
     * Keep the map live while it is open: the partner's pin (and the distance)
     * follow them without pull-to-refresh.
     */
    private fun startPolling() {
        viewModelScope.launch {
            loadPartner()
            while (true) {
                delay(30_000)
                loadPartner()
            }
        }
    }

    private suspend fun loadPartner() {
        partnerLocation = runCatching { LeylaApi.partnerLocation() }.getOrNull()
        val partner = realPartner() ?: SAMPLE_PARTNER.takeIf { SharedConfig.DEMO_MODE }
        _state.update {
            it.copy(
                partner = partner,
                partnerSharing = partnerLocation?.sharing == true,
                partnerName = partnerName(partnerLocation?.partnerName ?: it.partnerName),
                km = LocationRepository.kmBetween(it.mine, partner),
            )
        }
        Session.publishDistance(_state.value.km)
    }

    private fun realPartner(): Coordinate? {
        val p = partnerLocation ?: return null
        if (!p.sharing) return null
        val lat = p.lat ?: return null
        val lng = p.lng ?: return null
        return Coordinate(lat, lng)
    }

    private fun partnerName(displayName: String?): String {
        if (SharedConfig.DEMO_MODE && (displayName.isNullOrEmpty() || displayName == "Partner")) {
            return "Alex"
        }
        return displayName?.takeIf { it.isNotEmpty() } ?: "Partner"
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun PartnerMapScreen(
    modifier: Modifier = Modifier,
    viewModel: PartnerMapViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants.values.any { it }
        LocationRepository.onPermissionResult(context, granted)
    }

    // This is the screen where sharing gets turned on, so it is also where a
    // stale position is most obvious. Ask for a fresh fix on every visit —
    // otherwise the only fix ever taken is the one at the moment the toggle
    // flipped, and a phone that had no fix yet then never publishes one.
    LaunchedEffect(Unit) { LocationRepository.refresh(context) }

    Box(modifier.fillMaxSize().background(colors.background)) {
        if (BuildConfig.MAPS_API_KEY.isBlank()) {
            // Without a key the Maps SDK renders a grey tile grid; say why
            // instead, exactly as the Home mini map does.
            Text(
                leylaString(R.string.maps_key_missing),
                style = IOSText.footnote,
                color = colors.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        } else {
            MapCanvas(state)
        }

        ControlCard(
            state = state,
            onToggleSharing = { wantsSharing ->
                when {
                    !wantsSharing -> LocationRepository.setSharing(context, false)
                    LocationRepository.hasPermission(context) ->
                        LocationRepository.setSharing(context, true)
                    // Turn sharing on optimistically so the switch follows the
                    // tap, then let the prompt's answer confirm or undo it.
                    else -> {
                        LocationRepository.setSharing(context, true)
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}

@Composable
private fun MapCanvas(state: PartnerMapUiState) {
    val mine = state.mine
    val partner = state.partner
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(30.0, 20.0), 1f)
    }

    // Reframe whenever either pin moves, the way `fitRegion()` does on iOS.
    LaunchedEffect(mine, partner) {
        val points = listOfNotNull(mine, partner).map { LatLng(it.latitude, it.longitude) }
        when (points.size) {
            0 -> Unit
            1 -> cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(points[0], 13f))
            else -> {
                val bounds = LatLngBounds.builder().apply { points.forEach(::include) }.build()
                runCatching {
                    cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 160))
                }
            }
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.NORMAL),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
    ) {
        mine?.let {
            MapAvatarMarker(it, state.myName, state.myAvatarPath)
        }
        partner?.let {
            MapAvatarMarker(it, state.partnerName, state.partnerAvatarPath)
        }
    }
}

@Composable
private fun MapAvatarMarker(coordinate: Coordinate, name: String, avatarPath: String?) {
    val colors = LeylaTheme.colors
    MarkerComposable(
        keys = arrayOf(name, avatarPath ?: "", coordinate),
        state = rememberUpdatedMarkerState(LatLng(coordinate.latitude, coordinate.longitude)),
        title = name,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(colors.solidBackground)
                    .padding(3.dp),
            ) {
                Avatar(path = avatarPath, name = name, size = 36.dp)
            }
            Text(
                name,
                style = IOSText.caption2.weight(FontWeight.Bold),
                color = colors.ink,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.solidBackground)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun ControlCard(
    state: PartnerMapUiState,
    onToggleSharing: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    LeylaCard(modifier = modifier, cornerRadius = 22.dp, contentPadding = 16.dp) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.km?.let { km ->
                Text(
                    leylaString(R.string.distance_km_apart, Math.round(km).toInt()),
                    style = IOSText.headline,
                    color = Theme.rose,
                )
            }

            Text(
                if (state.partnerSharing) {
                    leylaString(R.string.map_partner_sharing, state.partnerName)
                } else {
                    leylaString(R.string.map_partner_not_sharing, state.partnerName)
                },
                style = IOSText.subheadline,
                color = if (state.partnerSharing) colors.ink else colors.secondary,
                textAlign = TextAlign.Center,
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    leylaString(
                        if (state.isSharing) R.string.map_sharing_on else R.string.map_sharing_off,
                    ),
                    style = IOSText.body,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.isSharing,
                    onCheckedChange = onToggleSharing,
                    colors = SwitchDefaults.colors(checkedTrackColor = Theme.rose),
                )
            }

            if (state.permissionDenied) {
                Text(
                    leylaString(R.string.map_permission_denied),
                    style = IOSText.caption,
                    color = Theme.coral,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                leylaString(R.string.map_off_unless_on),
                style = IOSText.caption2,
                color = colors.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
