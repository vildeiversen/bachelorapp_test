package no.oslomet.travelbehavior.ui.screens.consent

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import no.oslomet.travelbehavior.ui.navigation.Screen

@Composable
fun ConsentReviewScreen(
    navController: NavController
) {
    val app = (LocalContext.current.applicationContext as Application)
    val consentVm: ConsentViewModel = viewModel(factory = ConsentVMFactory(app))
    val scope = rememberCoroutineScope()
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Read and Review Consent", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        ConsentFormCard(modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))
        
        // HVA: Advarselstekst som bruker fargetemaet.
        // HVORFOR: UU-vennlig fargebruk som automatisk tilpasser seg lyst/mørkt tema.
        Text(
            text = "Click here to withdraw your consent. This action cannot be reversed.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center // Senterer for bedre balanse i UI-et
        ) {
            // HVA: Knapp med Material 3 Error-farger.
            // HVORFOR: Følger MMI-prinsippet om "Visual Affordance" – fargen signaliserer handlingens alvor.
            Button(
                onClick = { showConfirm = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp) // Stor touch-flate for UU
            ) {
                Text("Withdraw Consent")
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        scope.launch {
                            consentVm.revoke()
                            navController.navigate(Screen.Consent.route) { popUpTo(0) }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Withdraw") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            },
            title = { Text("Withdraw consent?") },
            text = { Text("This will delete local data and request deletion of any synced data. You can accept again later to continue participating.") }
        )
    }
}
