package no.oslomet.travelbehavior.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    // Ruter for bunnmenyen
    object Tracking : Screen("tracking", "Tracking", Icons.Default.Timeline)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    // Rute for lagring av tur, ingen label eller ikon nødvendig
    object SaveTrip : Screen("save_trip/{tripId}") {
        fun createRoute(tripId: String) = "save_trip/$tripId"
    }
}
