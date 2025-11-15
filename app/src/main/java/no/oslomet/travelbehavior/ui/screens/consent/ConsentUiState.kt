package no.oslomet.travelbehavior.ui.screens.consent

/**
 * Represents the UI state for the consent screen.
 */
data class ConsentUiState(
    val isLoading: Boolean = true,
    val consentRequired: Boolean = false,
    val agreeChecked: Boolean = false
)
