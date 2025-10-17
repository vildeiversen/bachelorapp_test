package no.oslomet.travelbehavior.ui.screens.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import no.oslomet.travelbehavior.ui.theme.AccentRed
import no.oslomet.travelbehavior.ui.theme.TextLight

@Composable
fun SaveTripScreen(
    navController: NavController,
    tripId: String?,
    viewModel: TrackingViewModel
) {
    val combinedState by viewModel.uiState.collectAsState()
    val vmState = combinedState.first

    LaunchedEffect(vmState.isSaving, vmState.activeTripId) {
        if (!vmState.isSaving && vmState.activeTripId == null) {
            navController.popBackStack()
        }
    }

    if (tripId == null) {
        Text("Error: No trip ID found.")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (vmState.isSaving) {
            Text("Saving trip...")
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        } else {
            Text("Do you want to save the recorded trip?")
            Spacer(modifier = Modifier.height(24.dp))

            // FIKS: Fjernet overflødig 'enabled'-parameter
            Button(
                onClick = { viewModel.saveTrip(tripId) },
                colors = ButtonDefaults.buttonColors(
                    contentColor = TextLight
                )
            ) {
                Text("Save Trip")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FIKS: Fjernet overflødig 'enabled'-parameter
            Button(
                onClick = { viewModel.deleteTrip(tripId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed,
                    contentColor = TextLight
                )
            ) {
                Text("Delete Trip")
            }
        }
    }
}
