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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import no.oslomet.travelbehavior.ui.theme.AccentRed
import no.oslomet.travelbehavior.ui.theme.TextLight

@Composable
fun SaveTripScreen(
    navController: NavController,
    tripId: String?,
    viewModel: TrackingViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    // HVA: Navigerer tilbake til hovedskjermen når turen er lagret eller slettet.
    // HVORFOR: Etter at handlingen er fullført (lagring/sletting), har ikke brukeren
    // lenger noe å gjøre på denne skjermen. `activeTripId` blir nullstilt, og
    // vi returnerer til forrige skjerm.
    LaunchedEffect(uiState.isSaving, uiState.activeTripId) {
        if (!uiState.isSaving && uiState.activeTripId == null) {
            navController.popBackStack()
        }
    }

    if (tripId == null) {
        Text("Error: No trip ID found.")
        return
    }

    // HVA: Gjør hele kolonnen rullbar.
    // HVORFOR: Sikrer at alt innhold, inkludert kartet og knappene, er synlig
    // selv på små skjermer eller når tastaturet er oppe.
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
            Text("Saving trip...")
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        } else {
            // HVA: En ny seksjon for kart-forhåndsvisningen.
            // HVORFOR: Viser en visuell representasjon av turen direkte på lagringsskjermen.
            // Kartet er gjort klikkbart for å navigere til en detaljert visning.
            Text("Din reise:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            TripSummaryPreview(tripId = tripId, viewModel = viewModel, navController = navController)

            Spacer(modifier = Modifier.height(24.dp))

            // HVA: Tilbakemeldingsskjema for turen.
            // HVORFOR: Lar brukeren vurdere og gi kommentarer om reisen sin.
            TripFeedbackSheet {
                tripRating, delayRating, delayMinutes, delayComment ->
                viewModel.saveTripAndRatings(tripId, tripRating, delayRating, delayMinutes, delayComment)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // HVA: Knappen for å slette turen.
            // HVORFOR: Gir brukeren en mulighet til å forkaste turen hvis de ikke ønsker å lagre den.
            Button(
                onClick = { viewModel.deleteTrip(tripId) },
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed,
                    contentColor = TextLight
                )
            ) {
                Text("Slett tur")
            }
        }
    }
}

// HVA: En ny Composable for å vise en liten, klikkbar forhåndsvisning av kartet.
// HVORFOR: Gjenbrukbarhet og lesbarhet. Ved å trekke ut logikken for kart-previewet
// i en egen Composable, blir `SaveTripScreen` renere og enklere å forstå.
@Composable
private fun TripSummaryPreview(
    tripId: String,
    viewModel: TrackingViewModel,
    navController: NavController
) {
    // HVA: Laster inn punktene for turen.
    // HVORFOR: Dette kallet sørger for at ViewModel henter dataen vi trenger for å tegne kartet.
    LaunchedEffect(tripId) {
        viewModel.loadTrackPointsForTrip(tripId)
    }

    val trackPoints by viewModel.trackPoints.collectAsState()
    // FIKS: Endret fra it.latitude og it.longitude til it.lat og it.lon for å matche TrackPoint-dataklassen.
    val latLngs = trackPoints.map { LatLng(it.lat, it.lon) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(59.9139, 10.7522), 10f)
    }

    // HVA: Justerer kameraet for å vise hele ruten når punktene er lastet.
    // HVORFOR: Sikrer at hele turen er synlig i det lille forhåndsvisningsvinduet.
    LaunchedEffect(latLngs) {
        if (latLngs.size > 1) {
            val boundsBuilder = LatLngBounds.builder()
            latLngs.forEach { boundsBuilder.include(it) }
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 50))
        }
    }

    // HVA: En Surface-boks som fungerer som en container for kartet.
    // HVORFOR: `Surface` gir oss skygge og avrundede hjørner for et pent design.
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
                // HVA: Deaktiverer alle brukerinteraksjoner med kartet.
                // HVORFOR: Dette er kun en forhåndsvisning. Brukeren skal ikke kunne
                // zoome eller panorere her, kun klikke på hele feltet.
                properties = MapProperties(isMyLocationEnabled = false),
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
            }
            // HVA: En transparent Box som ligger over kartet for å fange trykk.
            // HVORFOR: GoogleMap-komponenten kan "stjele" trykk-hendelser selv om
            // interaksjon er deaktivert. Ved å legge en usynlig, klikkbar boks over,
            // sikrer vi at trykket blir fanget opp og at navigasjonen alltid fungerer.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { navController.navigate("trip_summary/$tripId") }
            )
        }
    }
}

