package no.oslomet.travelbehavior.ui.screens.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import no.oslomet.travelbehavior.data.ConsentRepository

data class ConsentUiState(
    val isLoading: Boolean = true,
    val consentRequired: Boolean = true,
    val agreeChecked: Boolean = false
)

class ConsentViewModel(private val repo: ConsentRepository) : ViewModel() {
    private val agree = MutableStateFlow(false)
    private val loading = MutableStateFlow(true)
    private val required = MutableStateFlow(true)

    val ui: StateFlow<ConsentUiState> = combine(loading, required, agree) { l, r, a ->
        ConsentUiState(l, r, a)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConsentUiState())

    init {
        viewModelScope.launch {
            repo.consentState.collect { s ->
                required.value = !s.consentGiven
                loading.value = false
            }
        }
    }

    fun setAgree(v: Boolean) { agree.value = v }

    fun accept(onDone: () -> Unit) = viewModelScope.launch {
        repo.accept()
        onDone()
    }

    fun revoke() = viewModelScope.launch { repo.revoke() }
}
