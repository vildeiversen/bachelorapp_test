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

class ConsentViewModel(private val repo: ConsentRepository) : ViewModel() {

    companion object {
        const val CURRENT_CONSENT_VERSION = 1
    }

    private val _agreeChecked = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)

    private val _consentRequired: Flow<Boolean> = repo.hasGivenConsent().map { hasGiven ->
        _isLoading.value = false // Stop loading as soon as we get the first value
        !hasGiven
    }

    // Expose a StateFlow of the UI state
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
        started = SharingStarted.Eagerly, // Use Eagerly to make testing predictable
        initialValue = ConsentUiState()
    )

    /**
     * Called when the user checks or unchecks the agreement box.
     */
    fun setAgree(isAgreed: Boolean) {
        _agreeChecked.value = isAgreed
    }

    /**
     * Called when the user clicks the accept button.
     */
    fun accept(onDone: () -> Unit) {
        // Only allow accepting if the agreement box is checked
        if (_agreeChecked.value) {
            viewModelScope.launch {
                repo.saveConsent(given = true, version = CURRENT_CONSENT_VERSION)
                onDone()
            }
        }
    }

    /**
     * Revokes consent.
     */
    fun revoke() {
        viewModelScope.launch {
            repo.saveConsent(given = false, version = 0) // Setting version to 0 or less than current
        }
    }
}
