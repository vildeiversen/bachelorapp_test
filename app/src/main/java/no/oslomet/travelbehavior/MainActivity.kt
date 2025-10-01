package no.oslomet.travelbehavior

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import no.oslomet.travelbehavior.location.LocationClient
import no.oslomet.travelbehavior.ui.theme.BachelorAppH2025Theme
import no.oslomet.travelbehavior.data.AppDatabase
import no.oslomet.travelbehavior.data.TrackPoint
import no.oslomet.travelbehavior.data.TrackPointDao

class MainActivity : ComponentActivity() {

    private lateinit var locClient: LocationClient
    private lateinit var dao: TrackPointDao
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var setTrackingForCompose: ((Boolean) -> Unit)? = null
    private var setHasPermissionForCompose: ((Boolean) -> Unit)? = null
    private var addPointToPathForCompose: ((LatLng) -> Unit)? = null

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.any { it }
        if (granted) {
            setHasPermissionForCompose?.invoke(true)
        }
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationTrackingLogic() {
        locClient.start { lat, lon, acc ->
            val newPoint = LatLng(lat, lon)
            addPointToPathForCompose?.invoke(newPoint) // Add point to the polyline

            lifecycleScope.launch {
                dao.insert(
                    TrackPoint(
                        timestamp = System.currentTimeMillis(),
                        lat = lat, lon = lon, acc = acc
                    )
                )
                val count = dao.count()
                android.util.Log.d("GPS", "Lagret punkt #$count: $lat, $lon")
            }
        }
        setTrackingForCompose?.invoke(true)
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locClient = LocationClient(this)
        dao = AppDatabase.getInstance(this).trackPointDao()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            BachelorAppH2025Theme {
                var tracking by remember { mutableStateOf(false) }
                var hasLocationPermission by remember { mutableStateOf(hasPermission()) }
                var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

                setTrackingForCompose = { isTracking -> tracking = isTracking }
                setHasPermissionForCompose = { hasPermission -> hasLocationPermission = hasPermission }
                addPointToPathForCompose = { newPoint -> pathPoints = pathPoints + newPoint }

                val oslo = LatLng(59.9139, 10.7522)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(oslo, 10f)
                }

                DisposableEffect(hasLocationPermission) {
                    if (hasLocationPermission) {
                        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()

                        val locationCallback = object : LocationCallback() {
                            override fun onLocationResult(result: LocationResult) {
                                result.lastLocation?.let {
                                    val userLatLng = LatLng(it.latitude, it.longitude)
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
                                }
                            }
                        }

                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

                        onDispose {
                            fusedLocationClient.removeLocationUpdates(locationCallback)
                        }
                    } else {
                        onDispose { }
                    }
                }

                fun startTracking() {
                    if (hasPermission()) {
                        hasLocationPermission = true
                        pathPoints = emptyList() // Clear the previous path
                        startLocationTrackingLogic()
                    } else {
                        requestPerms.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                }

                fun stopTracking() {
                    locClient.stop()
                    tracking = false
                }

                Box(Modifier.fillMaxSize()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = hasLocationPermission
                        ),
                        uiSettings = MapUiSettings(
                            myLocationButtonEnabled = false
                        )
                    ) {
                        // Draw the polyline on the map if it has points
                        if (pathPoints.size > 1) {
                            Polyline(
                                points = pathPoints,
                                color = Color.Red,
                                width = 8f
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!tracking) {
                            Button(onClick = { startTracking() }) { Text("Start") }
                        } else {
                            Button(onClick = { stopTracking() }) { Text("Stopp") }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locClient.stop()
    }
}