package no.oslomet.travelbehavior.ui.screens.tracking

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                hasLocationPermission = true
                viewModel.startTracking() // Start tracking via ViewModel after permission
            }
        }
    )

    if (uiState.isTracking && hasLocationPermission) {
        // ##### KART-VISNING #####
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(59.9139, 10.7522), 12f)
        }

        // Animer kamera til brukerens posisjon
        LaunchedEffect(uiState.pathPoints) {
            uiState.pathPoints.lastOrNull()?.let { lastPoint ->
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(lastPoint, 15f),
                    durationMs = 1000
                )
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = true)
            ) {
                if (uiState.pathPoints.size > 1) {
                    Polyline(
                        points = uiState.pathPoints,
                        color = Color(0xFFE53935), // En rød-farge
                        width = 12f
                    )
                }
            }

            Button(
                onClick = { viewModel.stopTracking() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            ) {
                Text("Stop Tracking & Sync")
            }
        }
    } else {
        // ##### START-KNAPP-VISNING #####
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = {
                if (hasLocationPermission) {
                    viewModel.startTracking()
                } else {
                    locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }) {
                Text("Start Tracking Route")
            }
        }
    }
}
