package no.oslomet.travelbehavior.ui.screens.tracking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

/**
 * Screen displaying a full-screen map with the recorded route for a completed trip.
 * Provides a visual summary and a back button to return to the previous screen.
 */
@Composable
fun TripSummaryScreen(
    navController: NavController,
    tripId: String?,
    viewModel: TrackingViewModel
) {
    // If tripId is missing, we cannot fetch data; return to the previous screen.
    if (tripId == null) {
        navController.popBackStack()
        return
    }

    // Trigger loading of track points from the ViewModel when the tripId changes.
    LaunchedEffect(tripId) {
        viewModel.loadTrackPointsForTrip(tripId)
    }

    // Observe the list of track points. UI will update automatically when data is loaded.
    val trackPoints by viewModel.trackPoints.collectAsState()

    // Map track point entities to LatLng objects for Google Maps.
    val latLngs = trackPoints.map { LatLng(it.lat, it.lon) }
    val startPoint = latLngs.firstOrNull()
    val endPoint = latLngs.lastOrNull()

    // State for managing camera position, zoom, and animations.
    val cameraPositionState = rememberCameraPositionState {
        // Default initial position (Oslo area) before data loads.
        position = CameraPosition.fromLatLngZoom(LatLng(59.9139, 10.7522), 10f)
    }

    // Automatically adjust the camera to fit the entire route whenever points are loaded.
    LaunchedEffect(latLngs) {
        if (latLngs.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            latLngs.forEach { boundsBuilder.include(it) }
            // Animate camera to fit all points within the specified bounds and padding.
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100),
                durationMs = 1000
            )
        }
    }

    Scaffold(
        topBar = {
            // Navigation back button.
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // Enable standard map UI controls for better usability.
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    compassEnabled = true
                )
            ) {
                // Visualize the travel path with a Polyline.
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
        }
    }
}
