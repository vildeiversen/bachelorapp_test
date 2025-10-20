package no.oslomet.travelbehavior.ui.screens.settings

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import no.oslomet.travelbehavior.ui.navigation.Screen
import no.oslomet.travelbehavior.ui.screens.consent.ConsentVMFactory
import no.oslomet.travelbehavior.ui.screens.consent.ConsentViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val app = (LocalContext.current.applicationContext as Application)
    val consentVm: ConsentViewModel = viewModel(factory = ConsentVMFactory(app))
    var showConfirm by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))

            Text(text = "Privacy", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Button(onClick = { showConfirm = true }) {
                Text("Delete data & withdraw consent")
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    // 1) Clear local consent (and stop any tracking you run)
                    consentVm.revoke()
                    // 2) Navigate to consent screen immediately
                    navController.navigate(Screen.Consent.route) { popUpTo(0) }
                    // 3) TODO: trigger cloud deletion by study ID if needed
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            },
            title = { Text("Delete all data?") },
            text = { Text("This will delete local data and request deletion of synced data. You can accept consent again later if you want to continue participating.") }
        )
    }
}
