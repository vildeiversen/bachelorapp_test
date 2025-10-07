package no.oslomet.travelbehavior

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
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
                val context = LocalContext.current

                // KART: Variabel for å holde på brukerens nåværende posisjon.
                var userLocation by remember { mutableStateOf<LatLng?>(null) }
                val coroutineScope = rememberCoroutineScope()

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
                // Setter opp en kontinuerlig lytter for å hente brukerens posisjon.
                DisposableEffect(hasLocationPermission) {
                    if (hasLocationPermission) {
                        try {
                            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()

                            val locationCallback = object : LocationCallback() {
                                override fun onLocationResult(result: LocationResult) {
                                    result.lastLocation?.let {
                                        // KART: Lagre brukerposisjon, men IKKE flytt kameraet.
                                        userLocation = LatLng(it.latitude, it.longitude)
                                    }
                                }
                            }

                            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

                            onDispose {
                                fusedLocationClient.removeLocationUpdates(locationCallback)
                            }
                        } catch (e: SecurityException) {
                            Log.e("MainActivity", "Klarte ikke å starte posisjonsoppdatering på grunn av SecurityException.", e)
                            Toast.makeText(context, "Posisjonstjenesten feilet. Prøv å 'Wipe Data' på emulatoren.", Toast.LENGTH_LONG).show()
                            onDispose { } // Må ha en onDispose selv om den er tom
                        }
                    } else {
                        onDispose { }
                    }
                }

                fun startTracking() {
                    if (hasPermission()) {
                        hasLocationPermission = true
                        pathPoints = emptyList()
                        startLocationTrackingLogic()
                    } else {
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
                            isMyLocationEnabled = hasLocationPermission
                        ),
                        uiSettings = MapUiSettings(
                            myLocationButtonEnabled = false // Vi bruker vår egen knapp.
                        )
                    ) {
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

                    // KART: Knapp for å manuelt sentrere kameraet på brukerens posisjon.
                    if (hasLocationPermission) {
                        FloatingActionButton(
                            onClick = {
                                userLocation?.let { loc ->
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(loc, 15f)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Senterer kartet på din posisjon"
                            )
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