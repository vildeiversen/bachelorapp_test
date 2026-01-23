package no.oslomet.travelbehavior.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines all navigation routes in the application.
 * Each screen has a route string, and optional label and icon for navigation UI.
 */
sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    
    // Main application screens accessible via bottom navigation
    object Tracking : Screen("tracking", "Tracking", Icons.Default.Timeline)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    // Onboarding and legal screens
    object Consent : Screen("consent", "Consent", null)
    object ConsentReview : Screen("consent_review", "View Consent", null)

    // Initial loading screen used while fetching data from DataStore
    object Splash : Screen("splash")

    /**
     * Screen for saving trip details. Includes a helper function to build 
     * the correct route for navigation for a given trip ID.
     */
    object SaveTrip : Screen("save_trip/{tripId}") {
        fun createRoute(tripId: String) = "save_trip/$tripId"
    }
}
