package no.oslomet.travelbehavior.ui.screens.consent

import android.content.Intent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

/**
 * Screen displayed during onboarding to obtain user consent for data collection.
 */
@Composable
fun ConsentScreen(
    agreeChecked: Boolean,           // State of the agreement checkbox
    onAgreeChange: (Boolean) -> Unit, // Callback when checkbox state changes
    onAccept: () -> Unit,             // Callback when the user accepts
    onDecline: () -> Unit             // Callback when the user declines
) {
    val ctx = LocalContext.current
    val back = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    /**
     * Helper to open links (emails or websites) from the consent text.
     */
    fun openExternal(uri: String) {
        val intent = when {
            uri.startsWith("mailto:", ignoreCase = true) ->
                Intent(Intent.ACTION_SENDTO, uri.toUri())
            else ->
                Intent(Intent.ACTION_VIEW, uri.toUri())
        }
        ctx.startActivity(intent)
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text(
                "Consent to the Collection and Use of Travel Data",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))

            // Scrollable area that contains the reusable consent card.
            // A border is added to clearly separate the legal text from the rest of the UI.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                    .padding(12.dp)
            ) {
                // Reusable card component (defined in ConsentContent.kt)
                ConsentFormCard(
                    modifier = Modifier.fillMaxWidth(),
                    onOpenUrlFallback = ::openExternal
                )
            }

            Spacer(Modifier.height(12.dp))

            // Checkbox for the user to confirm they have read the terms
            Row {
                Checkbox(
                    checked = agreeChecked,
                    onCheckedChange = onAgreeChange,
                    modifier = Modifier.testTag("agree_checkbox")
                )
                Spacer(Modifier.width(8.dp))
                Text("I have read and agree to the terms above.")
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons: Decline exits/goes back, Accept continues if checkbox is checked
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        back?.onBackPressed(); onDecline()
                    }
                ) { Text("Decline") }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = agreeChecked, // Button is only clickable if terms are agreed to
                    onClick = onAccept
                ) {
                    Text("Accept & Continue")
                }
            }
        }
    }
}
