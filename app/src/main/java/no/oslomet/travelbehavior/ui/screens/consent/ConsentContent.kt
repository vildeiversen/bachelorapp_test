package no.oslomet.travelbehavior.ui.screens.consent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Reusable card that renders the consent text. 
 * Used in both the initial onboarding and the settings review screen.
 */
@Composable
fun ConsentFormCard(
    modifier: Modifier = Modifier,
    onOpenUrlFallback: (String) -> Unit = {} // Fallback if the URI handler fails
) {
    val uriHandler = LocalUriHandler.current
    val text = consentAnnotatedText()

    Card(
        modifier = modifier,
        // Set container color to surface to maintain theme consistency (avoiding default surfaceVariant)
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            ClickableText(
                text = text,
                // Explicitly set color as ClickableText does not inherit it automatically
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { offset ->
                    // Handle regular URL clicks
                    text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { ann ->
                            val url = ann.item
                            if (url.isNotBlank()) {
                                runCatching { uriHandler.openUri(url) }
                                    .onFailure { onOpenUrlFallback(url) }
                            }
                        }

                    // Handle email link clicks
                    text.getStringAnnotations(tag = "MAIL", start = offset, end = offset)
                        .firstOrNull()?.let { ann ->
                            val mail = ann.item
                            if (mail.isNotBlank()) {
                                runCatching { uriHandler.openUri(mail) }
                                    .onFailure { onOpenUrlFallback(mail) }
                            }
                        }
                }
            )
        }
    }
}

/* ------- Content configuration (edit here to update all consent views) ------- */

private const val CONTACT_EMAIL: String = "name@email.com"
private const val RETENTION_DAYS: Int = 180
private const val PRIVACY_URL: String = "" // Optional: link to a full privacy policy

/**
 * Builds the annotated string containing the consent information,
 * including clickable links for email and external policies.
 */
@Composable
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
    appendLine("• Data is retained for $RETENTION_DAYS days after collection and then will be deleted automatically.")
    appendLine()

    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold)); appendLine("Contact"); pop()
    append("• Contact person: ")
    pushStringAnnotation(tag = "MAIL", annotation = "mailto:$CONTACT_EMAIL")
    pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium))
    append(CONTACT_EMAIL)
    pop(); pop()
    appendLine()
    appendLine("• Institution: Oslo Metropolitan University")

    if (PRIVACY_URL.isNotBlank()) {
        appendLine()
        append("Read full policy: ")
        pushStringAnnotation(tag = "URL", annotation = PRIVACY_URL)
        pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium))
        append("Privacy Policy")
        pop(); pop()
    }
}
