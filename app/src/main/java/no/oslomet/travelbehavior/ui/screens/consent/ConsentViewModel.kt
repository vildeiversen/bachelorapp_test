package no.oslomet.travelbehavior.ui.screens.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import no.oslomet.travelbehavior.data.ConsentRepository

/**
 * ViewModel for managing user consent state.
 * It tracks whether consent is required, if the user has checked the agreement box,
 * and handles the logic for accepting or revoking consent.
 */
class ConsentViewModel(private val repo: ConsentRepository) : ViewModel() {

    companion object {
        // Increment this version number when terms change to re-prompt users
        const val CURRENT_CONSENT_VERSION = 1
    }

    // State for the "I agree" checkbox in the UI
    private val _agreeChecked = MutableStateFlow(false)
    
    // Tracks if the initial consent status is still being loaded from DataStore
    private val _isLoading = MutableStateFlow(true)

    // Determines if the user needs to see the consent screen (based on repository data)
    private val _consentRequired: Flow<Boolean> = repo.hasGivenConsent().map { hasGiven ->
        _isLoading.value = false // Data received, stop showing the loading state
        !hasGiven
    }

    /**
     * UI state for consent screen.
     */
    val ui: StateFlow<ConsentUiState> = combine(
        _isLoading,
        _consentRequired,
        _agreeChecked
    ) { isLoading, consentRequired, agreeChecked ->
        ConsentUiState(
            isLoading = isLoading,
            consentRequired = consentRequired,
            agreeChecked = agreeChecked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly, // Started immediately for predictable testing and UI behavior
        initialValue = ConsentUiState()
    )

    /**
     * Updates the local state when the user toggles the agreement checkbox.
     */
    fun setAgree(isAgreed: Boolean) {
        _agreeChecked.value = isAgreed
    }

    /**
     * Saves the user's consent with the current version and executes the completion callback.
     */
    fun accept(onDone: () -> Unit) {
        // Guard to ensure consent can't be submitted without checking the box
        if (_agreeChecked.value) {
            viewModelScope.launch {
                repo.saveConsent(given = true, version = CURRENT_CONSENT_VERSION)
                onDone()
            }
        }
    }

    /**
     * Revokes the user's consent by resetting the saved status in the repository.
     */
    fun revoke() {
        viewModelScope.launch {
            repo.saveConsent(given = false, version = 0)
        }
    }
}
