package no.oslomet.travelbehavior.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import no.oslomet.travelbehavior.ui.navigation.Screen

@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))

            Text(text = "Privacy", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // New: navigate to a read-only consent review screen
            Button(onClick = { navController.navigate(Screen.ConsentReview.route) }) {
                Text("View Consent Settings")
            }
        }
    }
}

