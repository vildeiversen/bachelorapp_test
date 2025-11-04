package no.oslomet.travelbehavior.ui.screens.tracking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import no.oslomet.travelbehavior.ui.theme.TextLight

@Composable
fun TripFeedbackSheet(
    onSave: (tripRating: Int, delayRating: Int, delayMinutes: Int?, delayComment: String) -> Unit
) {
    var tripRating by remember { mutableStateOf(0) }
    var delayRating by remember { mutableStateOf(0) }
    var delayMinutes by remember { mutableStateOf("") }
    var delayComment by remember { mutableStateOf("") }

    // HVA: Henter FocusManager.
    // HVORFOR: Gir oss kontroll til å fjerne fokus og lukke tastaturet.
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rate your trip", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Text("rate the trip (1 = bad, 5 = great)", modifier = Modifier.fillMaxWidth())
        StarRating(rating = tripRating, onRatingChanged = { tripRating = it })

        Spacer(modifier = Modifier.height(24.dp))

        Text("rate the delay (1 = huge delay, 5 = no delay)", modifier = Modifier.fillMaxWidth())
        StarRating(rating = delayRating, onRatingChanged = { delayRating = it })

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = delayMinutes,
            onValueChange = { delayMinutes = it.filter { c -> c.isDigit() } },
            label = { Text("How many minutes was the delay?") },
            // HVA: Legger til "Neste"-knapp på tastaturet.
            // HVORFOR: Forbedrer flyten slik at brukeren enkelt kan hoppe til neste felt.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = delayComment,
            onValueChange = { delayComment = it },
            label = { Text("Optional: Describe the delay") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            // HVA: Legger til "Ferdig"-knapp og en handling for den.
            // HVORFOR: Gir brukeren en klar måte å lukke tastaturet på når de er ferdige.
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val minutes = delayMinutes.toIntOrNull()
                onSave(tripRating, delayRating, minutes, delayComment)
            },
            enabled = tripRating > 0,
            colors = ButtonDefaults.buttonColors(
                contentColor = TextLight
            )
        ) {
            Text("Save Trip")
        }
    }
}

@Composable
fun StarRating(rating: Int, onRatingChanged: (Int) -> Unit) {
    Row {
        (1..5).forEach { index ->
            Icon(
                imageVector = if (index <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Star $index",
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onRatingChanged(index) },
                tint = if (index <= rating) Color(0xFFFFD700) else Color.Gray
            )
        }
    }
}
