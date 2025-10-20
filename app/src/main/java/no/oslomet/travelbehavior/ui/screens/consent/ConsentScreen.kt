package no.oslomet.travelbehavior.ui.screens.consent

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ConsentScreen(
    agreeChecked: Boolean,
    onAgreeChange: (Boolean) -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val ctx = LocalContext.current
    val back = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val text = consentAnnotatedText()

    fun handleClick(str: AnnotatedString, offset: Int) {
        str.getStringAnnotations(start = offset, end = offset).firstOrNull()?.let { ann ->
            val intent = when (ann.tag) {
                "URL"  -> Intent(Intent.ACTION_VIEW, Uri.parse(ann.item))
                "MAIL" -> Intent(Intent.ACTION_SENDTO, Uri.parse(ann.item))
                else -> null
            }
            intent?.let { ctx.startActivity(it) }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Consent to the collection and use of travel data", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))

            ClickableText(
                text = text,
                onClick = { handleClick(text, it) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                    .padding(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            Row {
                Checkbox(checked = agreeChecked, onCheckedChange = onAgreeChange)
                Spacer(Modifier.width(8.dp))
                Text("I have read and agree to the terms above.")
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                    back?.onBackPressed(); onDecline()
                }) { Text("Decline") }

                Button(modifier = Modifier.weight(1f), enabled = agreeChecked, onClick = onAccept) {
                    Text("Accept & Continue")
                }
            }
        }
    }
}

//In case an contact email is added
private const val CONTACT_EMAIL: String = "name@email.com"
//In case of automatic deletion of data
private const val RETENTION_DAYS: Int = 180

//In case a link to privacy statement website is needed
private const val PRIVACY_URL: String = ""


@Composable
//WIP
private fun consentAnnotatedText(): AnnotatedString = buildAnnotatedString {

    appendLine("This app collects anonymous travel data to support research on travel behaviour. Data is used only for research purposes and is handled securely and anonymously.")
    appendLine()

    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold)); appendLine("Data that is collected"); pop()
    appendLine("• Location (latitude, longitude).")
    appendLine("• Speed.")
    appendLine("• Timestamp of travel activity.")
    appendLine("• Anonymized travel records.")
    appendLine()

    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold)); appendLine("How the data is stored"); pop()
    appendLine("• The data is stored anonymously without any identifiers such as names, email, phone numbers or other identifiers. Each user is assigned a random ID, so no data can be traced back to an individual.")
    appendLine("• Your data is stored locally on your device using a secure database (Room) and may be synced to our cloud database (Firebase) when you have internet connectivity.")
    appendLine()

    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold)); appendLine("Purpose of the data collection"); pop()
    appendLine("• The purpose of the data is to analyze travel patterns for the research of users' travel behaviour, and how it can be used to contribute to research on sustainable transport.")
    appendLine("• The data will not be used for commercial purposes.")
    appendLine()

    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold)); appendLine("Voluntary participation and right to withdraw"); pop()
    appendLine("• Participation is voluntary. The user can withdraw consent to the use of data at any time, all local data will be permanently deleted.")
    appendLine("• You can withdraw by pressing the “withdraw consent” button in the app’s setting. This will delete local copies of your data. ")
    appendLine()

    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold)); appendLine("Data retention and deletion"); pop()
    appendLine("• Data is retained for $RETENTION_DAYS days after collection and then will be deleted automatically.") //WIP
    appendLine()

    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold)); appendLine("Contact"); pop()
    append("• Contact person: ")
    pushStringAnnotation("MAIL", "mailto:$CONTACT_EMAIL")
    pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium))
    append(CONTACT_EMAIL)
    pop(); pop()
    appendLine()
    appendLine("• Institution: Oslo Metropolitan University")

    //WIP
    if (!PRIVACY_URL.isNullOrBlank()) {
        appendLine()
        append("Read full policy: ")
        pushStringAnnotation("URL", PRIVACY_URL!!)
        pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium))
        append("Privacy Policy")
        pop(); pop()
    }
}
