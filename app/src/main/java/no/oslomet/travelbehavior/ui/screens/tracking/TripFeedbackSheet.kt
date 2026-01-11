package no.oslomet.travelbehavior.ui.screens.tracking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rate your trip", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // TRIP RATING
        Text("How was your trip?", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium)
        StarRating(
            rating = tripRating,
            onRatingChanged = { tripRating = it },
            labelProvider = { index ->
                when(index) {
                    1 -> "Very Bad"
                    2 -> "Bad"
                    3 -> "Neutral"
                    4 -> "Good"
                    5 -> "Excellent"
                    else -> ""
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // DELAY RATING
        // HVA: Omvendt rekkefølge på labels (5 = No delay, 1 = Huge delay)
        // HVORFOR: Følger MMI-prinsippet om "Mental Models" – flere stjerner = mer positivt.
        Text("How would you rate the delay?", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium)
        StarRating(
            rating = delayRating,
            onRatingChanged = { delayRating = it },
            labelProvider = { index ->
                when(index) {
                    5 -> "No delay"
                    4 -> "Minor delay"
                    3 -> "Noticeable"
                    2 -> "Significant"
                    1 -> "Huge delay"
                    else -> ""
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = delayMinutes,
            onValueChange = { delayMinutes = it.filter { c -> c.isDigit() } },
            label = { Text("Delay in minutes (optional)") },
            placeholder = { Text("e.g. 5") },
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
            label = { Text("Comment on the delay (optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val minutes = delayMinutes.toIntOrNull()
                onSave(tripRating, delayRating, minutes, delayComment)
            },
            enabled = tripRating > 0,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = TextLight
            )
        ) {
            Text("Save Trip", fontSize = 18.sp)
        }
    }
}

@Composable
fun StarRating(
    rating: Int, 
    onRatingChanged: (Int) -> Unit,
    labelProvider: (Int) -> String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.selectableGroup()) {
            (1..5).forEach { index ->
                val isSelected = index <= rating
                val label = labelProvider(index)
                
                Icon(
                    imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Rate $index stars: $label",
                    modifier = Modifier
                        .size(48.dp)
                        .selectable(
                            selected = (index == rating),
                            onClick = { onRatingChanged(index) }
                        )
                        .padding(4.dp),
                    tint = if (isSelected) Color(0xFFFFD700) else Color.Gray
                )
            }
        }
        Text(
            text = if (rating > 0) labelProvider(rating) else "Select a rating",
            style = MaterialTheme.typography.labelMedium,
            color = if (rating > 0) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
