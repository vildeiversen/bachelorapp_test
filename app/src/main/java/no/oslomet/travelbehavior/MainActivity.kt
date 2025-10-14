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
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import no.oslomet.travelbehavior.ui.navigation.BottomNavigationBar
import no.oslomet.travelbehavior.ui.navigation.Screen
import no.oslomet.travelbehavior.ui.screens.home.HomeScreen
import no.oslomet.travelbehavior.ui.screens.settings.SettingsScreen
import no.oslomet.travelbehavior.ui.screens.tracking.SaveTripScreen
import no.oslomet.travelbehavior.ui.screens.tracking.TrackingScreen
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
            composable(Screen.Tracking.route) { TrackingScreen(navController = navController) } // Pass NavController
            composable(Screen.Settings.route) { SettingsScreen() }

            composable(
                route = Screen.SaveTrip.route,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId")
                SaveTripScreen(navController = navController, tripId = tripId)
            }
        }
    }
}
