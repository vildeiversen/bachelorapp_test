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
import android.app.Application
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import no.oslomet.travelbehavior.ui.screens.consent.ConsentScreen
import no.oslomet.travelbehavior.ui.screens.consent.ConsentViewModel
import no.oslomet.travelbehavior.ui.screens.consent.ConsentVMFactory
import no.oslomet.travelbehavior.ui.screens.consent.ConsentReviewScreen
import no.oslomet.travelbehavior.ui.screens.tracking.TripSummaryScreen

/**
 * App entry activity.
 * Sets theme and renders the root (AppShell).
 */
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

/**
 * root composable that own NavController, shared ViewModels,
 * and app-level Scaffold (top bar and bottom navigation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell() {
    val navController = rememberNavController()
    val app = (LocalContext.current.applicationContext as Application)

    // ViewModels for managing global state like consent and active tracking
    val consentVm: ConsentViewModel = viewModel(factory = ConsentVMFactory(app))
    val trackingVm: TrackingViewModel = viewModel()

    val ui = consentVm.ui.collectAsState().value
    val trackingState by trackingVm.uiState.collectAsState()

    // Determine starting screen based on loading and consent status
    val start = when {
        ui.isLoading       -> Screen.Splash.route
        ui.consentRequired -> Screen.Consent.route
        else               -> Screen.Home.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Dynamic title logic: Updates based on current route and tracking status
    val topBarTitle = when {
        currentRoute == Screen.Home.route -> "Travel Behaviour"
        currentRoute == "tracking_screen" -> {
            // Show active tracking status in title for better user feedback
            if (trackingState.isTracking) "Currently Tracking Route" else "Travel Behaviour"
        }
        currentRoute?.startsWith(Screen.SaveTrip.route.split("/")[0]) == true -> "Route Summary"
        currentRoute == Screen.Settings.route -> "Settings"
        currentRoute == Screen.ConsentReview.route -> "View Consent"
        else -> "Travel Behaviour"
    }

    // Controls visibility of navigation components
    val showBottomBar = currentRoute != Screen.Consent.route && currentRoute != Screen.Splash.route

    Scaffold(
        topBar = {
            // Only show TopAppBar on main content screens
            if (currentRoute != Screen.Consent.route && currentRoute != Screen.Splash.route) {
                TopAppBar(
                    title = { Text(topBarTitle) }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController, trackingViewModel = trackingVm)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Splash/Loading state
            composable(Screen.Splash.route) {
                androidx.compose.foundation.layout.Box(Modifier)
            }

            // Legal/Onboarding flow
            composable(Screen.Consent.route) {
                ConsentScreen(
                    agreeChecked = ui.agreeChecked,
                    onAgreeChange = consentVm::setAgree,
                    onAccept = {
                        consentVm.accept {
                            navController.navigate(Screen.Home.route) { popUpTo(0) }
                        }
                    },
                    // Callback for when the user declines the consent terms. Currently empty as the ConsentScreen component handles the app exit via onBackPressed.
                    // Reserved for future implementation (for example an exit message).
                    onDecline = { }
                )
            }

            // Review accepted terms from settings
            composable(Screen.ConsentReview.route) {
                ConsentReviewScreen(navController = navController)
            }

            // Main application screens
            composable(Screen.Home.route) { HomeScreen() }
            composable(route = Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }

            // Tracking sub-graph containing map and post-trip summary
            navigation(
                startDestination = "tracking_screen",
                route = Screen.Tracking.route
            ) {
                composable("tracking_screen") { backStackEntry ->
                    TrackingScreen(navController = navController, viewModel = trackingVm)
                }

                composable(
                    route = Screen.SaveTrip.route,
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString("tripId")
                    SaveTripScreen(navController = navController, tripId = tripId, viewModel = trackingVm)
                }

                composable(
                    route = "trip_summary/{tripId}",
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString("tripId")
                    TripSummaryScreen(navController = navController, tripId = tripId, viewModel = trackingVm)
                }
            }
        }
    }
}
