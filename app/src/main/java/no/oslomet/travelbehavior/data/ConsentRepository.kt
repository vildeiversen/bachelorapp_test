package no.oslomet.travelbehavior.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository that provides access to the user's consent status.
 */
interface ConsentRepository {
    /**
     * A flow that emits true if the user has given consent, false otherwise.
     */
    fun hasGivenConsent(): Flow<Boolean>

    /**
     * A flow that emits the version of the consent the user has agreed to.
     */
    fun getConsentVersion(): Flow<Int>

    /**
     * Saves the user's consent decision and the version they agreed to.
     */
    suspend fun saveConsent(given: Boolean, version: Int)
}
