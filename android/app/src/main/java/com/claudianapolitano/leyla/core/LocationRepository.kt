package com.claudianapolitano.leyla.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        _isSharing.value = prefs.getBoolean(KEY_SHARING, false)
    }

    fun setSharing(context: Context, sharing: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHARING, sharing).apply()
        _isSharing.value = sharing
        if (!sharing) _currentLocation.value = null
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
                if (loc != null) _currentLocation.value = Coordinate(loc.latitude, loc.longitude)
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
