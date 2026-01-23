package no.oslomet.travelbehavior.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.location.Granularity

// Type alias for the callback function that receives location updates
typealias OnPoint = (lat: Double, lon: Double, acc: Float) -> Unit

/**
 * Client responsible for requesting and receiving GPS location updates.
 */
class LocationClient(private val context: Context) {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private var callback: LocationCallback? = null

    /**
     * Starts location tracking with high accuracy and a 4-second interval.
     */
    @SuppressLint("MissingPermission") // Permission is checked in MainActivity
    fun start(onPoint: OnPoint) {
        // Configure location request settings
        val req = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 4000L  // Desired interval: 4 second
        )
            .setMinUpdateIntervalMillis(4000L)      // Minimum update interval: 4 second
            .setMinUpdateDistanceMeters(0f)         // Log points even without movement
            .setGranularity(Granularity.GRANULARITY_FINE) // Require fine location accuracy
            .setWaitForAccurateLocation(false)      // Do not wait for "perfect" fix
            .build()

        // Define the callback that handles location results
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    Log.d("LocationClient", "New point: lat=${loc.latitude}, lon=${loc.longitude}, acc=${loc.accuracy}")
                    onPoint(loc.latitude, loc.longitude, loc.accuracy)
                }
            }
        }
        
        // Request updates from the fused location provider
        fused.requestLocationUpdates(req, callback!!, Looper.getMainLooper())
    }

    /**
     * Stops location tracking and removes the callback.
     */
    fun stop() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
    }
}
