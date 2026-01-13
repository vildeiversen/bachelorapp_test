package no.oslomet.travelbehavior.ui.screens.consent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import no.oslomet.travelbehavior.data.ConsentRepository

/** A fake implementation of a consent repository for testing the ConsentViewModel.
 * It allows setting the initial consent state and simulates data storage in memory. */

class FakeConsentRepository(
    startGiven: Boolean,
    startVersion: Int
) : ConsentRepository {
    // In-memory state flows to simulate storage
    private val _consentGiven = MutableStateFlow(startGiven)
    private val _consentVersion = MutableStateFlow(startVersion)

    // Observable flows for the UI
    override fun hasGivenConsent(): Flow<Boolean> = _consentGiven
    override fun getConsentVersion(): Flow<Int> = _consentVersion

    // Updates the in-memory state
    override suspend fun saveConsent(given: Boolean, version: Int) {
        _consentGiven.value = given
        _consentVersion.value = version
    }
}
