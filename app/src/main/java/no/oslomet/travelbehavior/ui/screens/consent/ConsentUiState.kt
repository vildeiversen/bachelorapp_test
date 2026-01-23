package no.oslomet.travelbehavior.ui.screens.consent

/**
 * Represents the UI state for the consent screen.
 */
data class ConsentUiState(
    val isLoading: Boolean = true,  // True while loading consent data
    val consentRequired: Boolean = false, // whether consent is required before proceeding
    val agreeChecked: Boolean = false   // Whether the user has agreed to the terms
)
