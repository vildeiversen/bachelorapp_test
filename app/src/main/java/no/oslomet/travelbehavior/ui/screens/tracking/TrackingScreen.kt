package no.oslomet.travelbehavior.ui.screens.tracking

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import no.oslomet.travelbehavior.ui.navigation.Screen

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: TrackingViewModel
) {
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val permissionState = rememberMultiplePermissionsState(
        permissions = requiredPermissions,
        onPermissionsResult = { permissions ->
            if (permissions.all { it.value }) {
                viewModel.startTracking()
            }
        }
    )

    TrackingScreenContent(
        modifier = modifier,
        viewModel = viewModel,
        navController = navController,
        permissionState = permissionState
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TrackingScreenContent(
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel,
    navController: NavController,
    permissionState: MultiplePermissionsState,
    mapProperties: MapProperties = MapProperties(isMyLocationEnabled = true) // New parameter
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isTracking) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(59.9139, 10.7522), 12f)
        }

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
                //properties = MapProperties(isMyLocationEnabled = true), //Old code. Remove commenting on this line and the lines with "//New paramater" to reverse.
                properties = mapProperties, // New parameter
                uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
            ) {
                if (uiState.pathPoints.size > 1) {
                    Polyline(
                        points = uiState.pathPoints,
                        color = Color(0xFFE53935),
                        width = 12f
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.stopTracking()?.let { tripId ->
                        navController.navigate(Screen.SaveTrip.createRoute(tripId))
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                // HVA: Endret fra TextLight til MaterialTheme.colorScheme.onPrimary
                // HVORFOR: UU-kontrast! Sikrer at teksten blir mørk i Dark Mode.
                colors = ButtonDefaults.buttonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
            )
            {
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
                    if (permissionState.allPermissionsGranted) {
                        viewModel.startTracking()
                    } else {
                        permissionState.launchMultiplePermissionRequest()
                    }
                },
                // HVA: Endret fra TextLight til MaterialTheme.colorScheme.onPrimary
                colors = ButtonDefaults.buttonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Start Tracking Route")
            }
        }
    }
}
