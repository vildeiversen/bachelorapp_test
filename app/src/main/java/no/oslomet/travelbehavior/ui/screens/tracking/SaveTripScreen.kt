package no.oslomet.travelbehavior.ui.screens.tracking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

/**
 * Screen where users can review their finished trip, provide feedback and rating,
 * and choose to save or delete the recorded data.
 */
@Composable
fun SaveTripScreen(
    navController: NavController,
    tripId: String?,
    viewModel: TrackingViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Navigate back to the previous screen once the trip is successfully saved or deleted
    LaunchedEffect(uiState.isSaving, uiState.activeTripId) {
        if (!uiState.isSaving && uiState.activeTripId == null) {
            navController.popBackStack()
        }
    }

    if (tripId == null) {
        Text("Error: No trip ID found.")
        return
    }

    // Confirmation dialog for permanent trip deletion
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Trip?") },
            text = { Text("Are you sure you want to delete this trip? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTrip(tripId)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isSaving) {
            // Loading state shown during database and network synchronization
            Text("Saving trip...")
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        } else {
            Text("Your Trip:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Displays a map preview of the recorded path
            TripSummaryPreview(tripId = tripId, viewModel = viewModel, navController = navController)

            Spacer(modifier = Modifier.height(24.dp))

            // Form for user ratings and delay comments
            TripFeedbackSheet {
                tripRating, delayRating, delayMinutes, delayComment ->
                viewModel.saveTripAndRatings(tripId, tripRating, delayRating, delayMinutes, delayComment)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delete button highlighted with error color
            Button(
                onClick = { showDeleteDialog = true },
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete Trip")
            }
        }
    }
}

/**
 * Renders a static map preview showing the polyline of the recorded trip.
 */
@Composable
private fun TripSummaryPreview(
    tripId: String,
    viewModel: TrackingViewModel,
    navController: NavController
) {
    // Load track points from the database when the screen or tripId changes
    LaunchedEffect(tripId) {
        viewModel.loadTrackPointsForTrip(tripId)
    }

    val trackPoints by viewModel.trackPoints.collectAsState()
    val latLngs = trackPoints.map { LatLng(it.lat, it.lon) }
    val startPoint = latLngs.firstOrNull()
    val endPoint = latLngs.lastOrNull()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(59.9139, 10.7522), 10f)
    }

    // Automatically adjust the camera bounds to fit the entire trip route
    LaunchedEffect(latLngs) {
        if (latLngs.size > 1) {
            val boundsBuilder = LatLngBounds.builder()
            latLngs.forEach { boundsBuilder.include(it) }
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 50))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                // Disable all UI interactions to keep the map as a static preview
                uiSettings = MapUiSettings(
                    compassEnabled = false,
                    zoomControlsEnabled = false,
                    zoomGesturesEnabled = false,
                    scrollGesturesEnabled = false,
                    scrollGesturesEnabledDuringRotateOrZoom = false,
                    tiltGesturesEnabled = false,
                    rotationGesturesEnabled = false
                )
            ) {
                if (latLngs.isNotEmpty()) {
                    Polyline(points = latLngs)
                }

                // Add marker for the start point
                startPoint?.let {
                    Marker(
                        state = rememberMarkerState(position = it),
                        title = "Start of Trip",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }

                // Add marker for the end point
                endPoint?.let {
                    Marker(
                        state = rememberMarkerState(position = it),
                        title = "End of Trip",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }
            // Overlay to handle clicks and navigate to a full trip summary
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { navController.navigate("trip_summary/$tripId") }
            )
        }
    }
}
