package no.oslomet.travelbehavior

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
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
    // KART: Klient for å hente posisjon direkte fra Google Play Services.
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // KART: Callbacks for å kommunisere fra Activity-logikk til Compose UI.
    private var setTrackingForCompose: ((Boolean) -> Unit)? = null
    private var setHasPermissionForCompose: ((Boolean) -> Unit)? = null
    private var addPointToPathForCompose: ((LatLng) -> Unit)? = null
    private var setTrackingRequestPendingForCompose: ((Boolean) -> Unit)? = null

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.any { it }
        if (granted) {
            // KART: Informer Compose om at tillatelse er gitt.
            setHasPermissionForCompose?.invoke(true)
        } else {
            // KART: Bruker avslo tillatelse. Nullstill flagget og vis en melding.
            setTrackingRequestPendingForCompose?.invoke(false)
            Toast.makeText(this, "Posisjonstillatelse er nødvendig for sporing.", Toast.LENGTH_LONG).show()
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
            // KART: Legg til nytt punkt i den tegnede ruten (Polyline).
            addPointToPathForCompose?.invoke(newPoint)

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
        // KART: Initialiser posisjonsklienten.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // KART: Be om posisjonstillatelse ved oppstart hvis den ikke allerede er gitt.
        if (!hasPermission()) {
            requestPerms.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        setContent {
            BachelorAppH2025Theme {
                var tracking by remember { mutableStateOf(false) }
                var hasLocationPermission by remember { mutableStateOf(hasPermission()) }
                // KART: Liste over punkter for å tegne ruten (Polyline).
                var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
                // KART: Flag som venter på posisjonstillatelse etter at "Start" er trykket.
                var trackingRequestPendingPermission by remember { mutableStateOf(false) }

                // KART: Koble callbacks til Compose state-variabler.
                setTrackingForCompose = { isTracking -> tracking = isTracking }
                setHasPermissionForCompose = { hasPermission -> hasLocationPermission = hasPermission }
                addPointToPathForCompose = { newPoint -> pathPoints = pathPoints + newPoint }
                setTrackingRequestPendingForCompose = { isPending -> trackingRequestPendingPermission = isPending }


                // KART: Startposisjon for kameraet (Oslo) før brukerposisjon er kjent.
                val oslo = LatLng(59.9139, 10.7522)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(oslo, 10f)
                }

                // KART: Effekt som starter sporing automatisk ETTER at tillatelse er gitt via "Start"-knappen.
                LaunchedEffect(hasLocationPermission) {
                    if (hasLocationPermission && trackingRequestPendingPermission) {
                        pathPoints = emptyList()
                        startLocationTrackingLogic()
                        trackingRequestPendingPermission = false // Nullstill flagget
                    }
                }

                // KART: Effekt som kjører når appen får posisjonstillatelse.
                // Setter opp en kontinuerlig lytter for å følge brukerens posisjon.
                DisposableEffect(hasLocationPermission) {
                    if (hasLocationPermission) {
                        // Endret intervallet til 1 sekund for jevnere kamerafølging.
                        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()

                        val locationCallback = object : LocationCallback() {
                            override fun onLocationResult(result: LocationResult) {
                                result.lastLocation?.let {
                                    val userLatLng = LatLng(it.latitude, it.longitude)
                                    // KART: Flytt kameraet for å følge brukeren.
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
                                }
                            }
                        }

                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

                        // KART: Rydd opp og fjern lytteren når skjermen lukkes for å spare batteri.
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
                        // KART: Tøm forrige rute før en ny startes.
                        pathPoints = emptyList()
                        startLocationTrackingLogic()
                    } else {
                        // KART: Sett flag og be om tillatelse. Effekten over vil starte sporingen.
                        trackingRequestPendingPermission = true
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
                    // KART: Hovedkomponenten for Google Maps.
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            // KART: Viser den blå "min posisjon"-prikken.
                            isMyLocationEnabled = hasLocationPermission
                        ),
                        uiSettings = MapUiSettings(
                            // KART: Skjuler sikte-knappen siden kameraet følger automatisk.
                            myLocationButtonEnabled = false
                        )
                    ) {
                        // KART: Tegner ruten som en rød linje på kartet.
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