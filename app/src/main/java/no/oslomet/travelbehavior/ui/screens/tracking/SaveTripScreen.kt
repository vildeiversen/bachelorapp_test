package no.oslomet.travelbehavior.ui.screens.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

@Composable
fun SaveTripScreen(
    navController: NavController,
    tripId: String?,
    viewModel: TrackingViewModel // FIKS: Mottar nå ViewModel, lager ikke sin egen
) {
    // FIKS: Henter hele UI-state for å lytte til isSaving
    val uiState by viewModel.uiState.collectAsState()

    // FIKS: Denne effekten håndterer navigasjonen på en sikker måte
    LaunchedEffect(uiState.isSaving, uiState.activeTripId) {
        // Navigerer tilbake KUN når lagring/sletting er ferdig (activeTripId er nullstilt)
        // OG vi ikke er midt i en ny lagringsprosess.
        if (!uiState.isSaving && uiState.activeTripId == null) {
            navController.popBackStack()
        }
    }

    if (tripId == null) {
        Text("Error: No trip ID found.")
        // Denne LaunchedEffect-en vil sørge for at vi navigerer tilbake hvis vi havner her
        // ved en feil, siden activeTripId i ViewModel-en mest sannsynlig er null.
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // FIKS: Viser enten knapper eller en lastespinner
        if (uiState.isSaving) {
            Text("Saving trip...")
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        } else {
            Text("Do you want to save the recorded trip?")
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveTrip(tripId) },
                enabled = !uiState.isSaving
            ) {
                Text("Save Trip")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.deleteTrip(tripId) },
                enabled = !uiState.isSaving
            ) {
                Text("Delete Trip")
            }
        }
    }
}
