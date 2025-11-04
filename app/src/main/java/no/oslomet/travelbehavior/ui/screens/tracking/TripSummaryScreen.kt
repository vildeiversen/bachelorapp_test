// HVA: En ny Composable-skjerm for å vise et kart med ruten for en avsluttet tur.
// HVORFOR: Gir brukeren en visuell bekreftelse og et sammendrag av hvor de har reist.
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

// HVA: Selve Composable-funksjonen for oppsummeringsskjermen.
// HVORFOR: Denne funksjonen setter sammen UI-elementene for skjermen,
// inkludert kartet, ruten og en tilbake-knapp.
@Composable
fun TripSummaryScreen(
    navController: NavController,
    tripId: String?,
    viewModel: TrackingViewModel
) {
    // HVA: Sjekker om tripId er gyldig.
    // HVORFOR: Uten en tripId kan vi ikke hente punktene for turen.
    // Navigerer tilbake hvis den mangler.
    if (tripId == null) {
        navController.popBackStack()
        return
    }

    // HVA: Kaller en funksjon i ViewModel for å starte innlasting av punkter for turen.
    // HVORFOR: LaunchedEffect sørger for at dette kallet kun skjer én gang når
    // tripId endres, og henter dataen vi trenger for å tegne kartet.
    LaunchedEffect(tripId) {
        viewModel.loadTrackPointsForTrip(tripId)
    }

    // HVA: Observerer listen med TrackPoint-objekter fra ViewModel.
    // HVORFOR: `collectAsState` gjør at Composable-en automatisk tegnes på nytt
    // når listen med punkter er ferdig lastet fra databasen.
    val trackPoints by viewModel.trackPoints.collectAsState()

    // FIKS: Endret fra it.latitude og it.longitude til it.lat og it.lon for å matche TrackPoint-dataklassen.
    val latLngs = trackPoints.map { LatLng(it.lat, it.lon) }

    // HVA: Oppretter og husker en tilstand for kameraposisjonen.
    // HVORFOR: Dette gir oss kontroll over hvor kartet skal sentreres og zoomes.
    val cameraPositionState = rememberCameraPositionState {
        // Starter med et standardpunkt (Oslo) før data er lastet.
        position = CameraPosition.fromLatLngZoom(LatLng(59.9139, 10.7522), 10f)
    }

    // HVA: En ny LaunchedEffect som reagerer når listen med punkter (latLngs) endres.
    // HVORFOR: Når punktene er lastet, vil denne koden kjøre for å zoome og panorere
    // kameraet slik at hele ruten blir synlig på skjermen.
    LaunchedEffect(latLngs) {
        if (latLngs.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            latLngs.forEach { boundsBuilder.include(it) }
            // Animerer kameraet til å vise alle punktene med en 100dp padding.
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100),
                durationMs = 1000
            )
        }
    }

    // HVA: Bruker en Scaffold for å enkelt legge til en tilbake-knapp.
    // HVORFOR: Gir en standard layout-struktur og gjør det enkelt å plassere
    // elementer som en "app bar" øverst.
    Scaffold(
        topBar = {
            // HVA: Tilbake-knapp.
            // HVORFOR: Lar brukeren enkelt navigere tilbake til forrige skjerm.
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    ) { paddingValues ->
        // HVA: En Box som holder på GoogleMap-komponenten.
        // HVORFOR: Gir en container for kartet som fyller hele skjermen.
        Box(modifier = Modifier.padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // HVA: Setter diverse UI-innstillinger for kartet.
                // HVORFOR: Gjør kartet mer brukervennlig ved å vise zoom-knapper og kompass.
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    compassEnabled = true
                )
            ) {
                // HVA: Tegner en Polyline (linje) mellom alle punktene.
                // HVORFOR: Dette er selve visualiseringen av ruten brukeren har reist.
                if (latLngs.isNotEmpty()) {
                    Polyline(points = latLngs)
                }
            }
        }
    }
}
