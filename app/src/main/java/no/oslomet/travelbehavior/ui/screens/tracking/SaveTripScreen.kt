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
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaving, uiState.activeTripId) {
        if (!uiState.isSaving && uiState.activeTripId == null) {
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
        if (uiState.isSaving) {
            Text("Saving trip...")
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        } else {
            // HVA: Oppdatert lambdaen til å motta alle fire verdiene.
            // HVORFOR: Dette kobler det nye UI-feltet til ViewModel-funksjonen.
            TripFeedbackSheet {
                tripRating, delayRating, delayMinutes, delayComment ->
                viewModel.saveTripAndRatings(tripId, tripRating, delayRating, delayMinutes, delayComment)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.deleteTrip(tripId) },
                enabled = !uiState.isSaving,
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
