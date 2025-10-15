package no.oslomet.travelbehavior

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import no.oslomet.travelbehavior.ui.navigation.BottomNavigationBar
import no.oslomet.travelbehavior.ui.navigation.Screen
import no.oslomet.travelbehavior.ui.screens.home.HomeScreen
import no.oslomet.travelbehavior.ui.screens.settings.SettingsScreen
import no.oslomet.travelbehavior.ui.screens.tracking.SaveTripScreen
import no.oslomet.travelbehavior.ui.screens.tracking.TrackingScreen
import no.oslomet.travelbehavior.ui.screens.tracking.TrackingViewModel
import no.oslomet.travelbehavior.ui.theme.BachelorAppH2025Theme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BachelorAppH2025Theme {
                AppShell()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell() {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Travel Behaviour") }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }

            // FIKS: Nestet navigasjonsgraf for hele sporingsflyten
            navigation(
                startDestination = "tracking_screen", // Intern startdestinasjon for grafen
                route = Screen.Tracking.route      // Ruten som bunn-navigasjonen bruker
            ) {
                // Denne skjermen vises når man navigerer til Screen.Tracking.route
                composable("tracking_screen") { backStackEntry ->
                    // Henter delt ViewModel som er scopet til grafen "Screen.Tracking.route"
                    val sharedViewModel = backStackEntry.sharedViewModel<TrackingViewModel>(navController)
                    TrackingScreen(navController = navController, viewModel = sharedViewModel)
                }

                composable(
                    route = Screen.SaveTrip.route,
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType })
                ) { backStackEntry ->
                    // Henter den *samme* delte ViewModel-instansen
                    val sharedViewModel = backStackEntry.sharedViewModel<TrackingViewModel>(navController)
                    val tripId = backStackEntry.arguments?.getString("tripId")
                    SaveTripScreen(navController = navController, tripId = tripId, viewModel = sharedViewModel)
                }
            }
        }
    }
}

// FIKS: En hjelpefunksjon for å enkelt hente en delt ViewModel fra en navigasjonsgraf.
@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(navController: NavController): T {
    // Finner ruten til navigasjonsgrafen som denne skjermen tilhører
    val navGraphRoute = destination.parent?.route ?: return viewModel()
    // Finner backstack-entryen for hele grafen
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    // Returnerer en ViewModel som er scopet til den overordnede grafen
    return viewModel(parentEntry)
}
