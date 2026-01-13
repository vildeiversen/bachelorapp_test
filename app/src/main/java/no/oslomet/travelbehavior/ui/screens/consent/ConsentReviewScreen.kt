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
        Text("View Consent", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        ConsentFormCard(modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Click here to withdraw your consent, this action cannot be reversed.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { showConfirm = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Withdraw Consent")
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    scope.launch {
                        consentVm.revoke()
                        navController.navigate(Screen.Consent.route) { popUpTo(0) }
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            },
            title = { Text("Withdraw consent?") },
            text = { Text("This will delete local data and request deletion of any synced data (if implemented). You can accept again later to continue participating.") }
        )
    }
}
