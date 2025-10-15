package no.oslomet.travelbehavior.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.location.Granularity

typealias OnPoint = (lat: Double, lon: Double, acc: Float) -> Unit

class LocationClient(private val context: Context) {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission") // vi ber om runtime-permission i MainActivity
    fun start(onPoint: OnPoint) {
        val req = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L  // ønsket intervall: 1s
        )
            .setMinUpdateIntervalMillis(1000L)      // ikke sjeldnere enn 1s
            .setMinUpdateDistanceMeters(0f)         // logg selv uten bevegelse
            .setGranularity(Granularity.GRANULARITY_FINE) // krev fin nøyaktighet
            .setWaitForAccurateLocation(false)      // ikke vent på "perfekt" fix
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    // FIKS: Logger hvert punkt til Logcat
                    Log.d("LocationClient", "New point: lat=${loc.latitude}, lon=${loc.longitude}, acc=${loc.accuracy}")
                    onPoint(loc.latitude, loc.longitude, loc.accuracy)
                }
            }
        }
        fused.requestLocationUpdates(req, callback!!, Looper.getMainLooper())
    }

    fun stop() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
    }
}
