package no.oslomet.travelbehavior.ui.screens.tracking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import no.oslomet.travelbehavior.ui.navigation.Screen
import no.oslomet.travelbehavior.ui.theme.TextLight

@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: TrackingViewModel
) {
    // FIKS: Henter den kombinerte tilstanden fra ViewModel
    val combinedState by viewModel.uiState.collectAsState()
    // FIKS: Deler opp tilstanden i ViewModel-state og sanntidsdata fra tjenesten
    val (vmState, trackingState) = combinedState
    val (isTracking, pathPoints) = trackingState

    val context = LocalContext.current

    // Tillatelse for lokasjon
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // FIKS: Ny tillatelse for notifikasjoner (Android 13+)
    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true) // Ikke nødvendig for eldre versjoner
        }
    }

    val startAction = { viewModel.startTracking() }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                hasNotificationPermission = true
                startAction() // Begge tillatelser er nå gitt, start sporing
            }
        }
    )

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                hasLocationPermission = true
                // Nå som vi har lokasjon, sjekk om vi trenger notifikasjonstillatelse
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    startAction() // Start direkte hvis ikke nødvendig
                }
            }
        }
    )

    // FIKS: Hovedbetingelsen bruker nå `isTracking` fra tjenesten
    if (isTracking) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(59.9139, 10.7522), 12f)
        }

        // FIKS: Bruker `pathPoints` fra tjenesten
        LaunchedEffect(pathPoints) {
            pathPoints.lastOrNull()?.let {
                cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(it, 15f), durationMs = 1000)
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = true)
            ) {
                if (pathPoints.size > 1) {
                    Polyline(points = pathPoints, color = Color(0xFFE53935), width = 12f)
                }
            }

            Button(
                onClick = {
                    viewModel.stopTracking()?.let { tripId ->
                        navController.navigate(Screen.SaveTrip.createRoute(tripId))
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                colors = ButtonDefaults.buttonColors(contentColor = TextLight)
            ) {
                Text("Stop Tracking")
            }
        }
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    // FIKS: Oppdatert logikk for å håndtere begge tillatelsene
                    if (hasLocationPermission) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            startAction()
                        }
                    } else {
                        locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                colors = ButtonDefaults.buttonColors(contentColor = TextLight)
            ) {
                Text("Start Tracking Route")
            }
        }
    }
}
