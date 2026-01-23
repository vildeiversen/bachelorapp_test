package no.oslomet.travelbehavior.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import no.oslomet.travelbehavior.ui.screens.tracking.TrackingViewModel

/**
 * Bottom navigation bar for the application, providing access to main screens.
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    trackingViewModel: TrackingViewModel
) {
    // List of screens to be displayed in the navigation bar
    val items = listOf(
        Screen.Tracking,
        Screen.Home,
        Screen.Settings
    )

    // Observe current navigation state and tracking state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val trackingState by trackingViewModel.uiState.collectAsState()

    // Hide the navigation bar if the user is on the Consent screen
    if (currentRoute == Screen.Consent.route) return

    NavigationBar { 
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    screen.icon?.let { icon ->
                        Icon(imageVector = icon, contentDescription = screen.label)
                    }
                },
                label = {
                    screen.label?.let { label ->
                        Text(text = label)
                    }
                },
                selected = currentRoute == screen.route,
                onClick = {
                    // Redirect to SaveTrip screen if a trip is finished but not yet saved
                    val destinationRoute = if (screen == Screen.Tracking && trackingState.activeTripId != null && !trackingState.isTracking) {
                        Screen.SaveTrip.route.replace("{tripId}", trackingState.activeTripId!!)
                    } else {
                        screen.route
                    }

                    // Perform navigation without duplicating screens (backstack)
                    navController.navigate(destinationRoute) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
