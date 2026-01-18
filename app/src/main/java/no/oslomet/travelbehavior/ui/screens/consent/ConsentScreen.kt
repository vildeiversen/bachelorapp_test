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

@Composable
fun ConsentScreen(
    agreeChecked: Boolean,
    onAgreeChange: (Boolean) -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val ctx = LocalContext.current
    val back = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

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

            // Scrollable area that contains the reusable consent card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                    .padding(12.dp)
            ) {
                // Reusable card (content defined in ConsentContent.kt)
                ConsentFormCard(
                    modifier = Modifier.fillMaxWidth(),
                    onOpenUrlFallback = ::openExternal
                )
            }

            Spacer(Modifier.height(12.dp))

            Row {
                Checkbox(
                    checked = agreeChecked,
                    onCheckedChange = onAgreeChange,
                    modifier = Modifier.testTag("agree_checkbox") // Added testTag
                )
                Spacer(Modifier.width(8.dp))
                Text("I have read and agree to the terms above.")
            }

            Spacer(Modifier.height(12.dp))

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
                    enabled = agreeChecked,
                    onClick = onAccept
                ) {
                    Text("Accept & Continue")
                }
            }
        }
    }
}
