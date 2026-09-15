package com.claudianapolitano.leyla.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A plain lat/lng pair, so the UI layer doesn't have to import Play Services. */
data class Coordinate(val latitude: Double, val longitude: Double)

/**
 * Where "am I sharing, and from where?" lives. Port of the iOS
 * `LocationManager`, reduced to what Home reads: a sharing flag and the last
 * fix. Continuous background sharing arrives with the Map and Settings screens.
 */
object LocationRepository {
    private const val FILE = "leyla_location"
    private const val KEY_SHARING = "isSharing"

    private val _currentLocation = MutableStateFlow<Coordinate?>(null)
    val currentLocation: StateFlow<Coordinate?> = _currentLocation.asStateFlow()

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    /** Set when the user turned sharing on but the OS refused the permission. */
    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    /**
     * Sharing outlives any one screen — the Map can be closed while a fix is
     * still on its way to the backend — so the pushes run on a repository-owned
     * scope rather than a ViewModel's.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        _isSharing.value = prefs.getBoolean(KEY_SHARING, false)
    }

    fun setSharing(context: Context, sharing: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHARING, sharing).apply()
        _isSharing.value = sharing
        if (sharing) {
            // Deliberately not flagged as denied here: the caller may have just
            // put the system prompt on screen, and answering it is what decides.
            // [onPermissionResult] is where a real refusal is recorded.
            if (hasPermission(context)) {
                _permissionDenied.value = false
                refresh(context)
            }
        } else {
            _currentLocation.value = null
            _permissionDenied.value = false
            // Tell the backend too: a partner who stops sharing should vanish
            // from the other phone's map, not freeze at their last position.
            scope.launch { runCatching { LeylaApi.stopSharingLocation() } }
            Session.publishDistance(null)
        }
    }

    /** Called once the OS has answered the runtime permission prompt. */
    fun onPermissionResult(context: Context, granted: Boolean) {
        _permissionDenied.value = !granted
        if (granted) refresh(context) else setSharing(context, false)
    }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Reads one fix, if we're sharing and allowed to. Home calls this on
     * appear; it is a no-op otherwise, which is what leaves the demo
     * coordinates in place.
     */
    @SuppressLint("MissingPermission")
    fun refresh(context: Context) {
        if (!_isSharing.value || !hasPermission(context)) return
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc == null) return@addOnSuccessListener
                _currentLocation.value = Coordinate(loc.latitude, loc.longitude)
                // Publish it, or the partner's phone has nothing to draw.
                scope.launch {
                    runCatching {
                        LeylaApi.updateLocation(
                            lat = loc.latitude,
                            lng = loc.longitude,
                            accuracy = loc.accuracy.toDouble(),
                            mode = "live",
                        )
                    }
                }
            }
    }

    /** Great-circle distance in kilometres, or null when either point is missing. */
    fun kmBetween(a: Coordinate?, b: Coordinate?): Double? {
        if (a == null || b == null) return null
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            a.latitude, a.longitude, b.latitude, b.longitude, results,
        )
        return results[0] / 1000.0
    }
}
