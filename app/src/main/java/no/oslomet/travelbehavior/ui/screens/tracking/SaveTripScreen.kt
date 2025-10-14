package no.oslomet.travelbehavior.ui.screens.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun SaveTripScreen(
    navController: NavController,
    tripId: String?,
    viewModel: TrackingViewModel = viewModel()
) {
    if (tripId == null) {
        // Håndter feilsituasjon, f.eks. naviger tilbake
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
        Text("Do you want to save the recorded trip?")
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            viewModel.saveTrip(tripId)
            // Gå tilbake til trackingskjermen (eller hjemskjermen)
            navController.popBackStack()
        }) {
            Text("Save Trip")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            viewModel.deleteTrip(tripId)
            // Gå tilbake til trackingskjermen
            navController.popBackStack()
        }) {
            Text("Delete Trip")
        }
    }
}
