package no.oslomet.travelbehavior.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Integration test that verifies the interaction between [ConsentRepositoryImpl]
 * and Android [DataStore]. This ensures that data is actually persisted to
 * disk and can be retrieved correctly. */

@RunWith(AndroidJUnit4::class)
class ConsentRepositoryIntegrationTest {

    private lateinit var context: Context
    private lateinit var repository: ConsentRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = ConsentRepositoryImpl(context)

        // Reset DataStore before each test to ensure isolation
        runBlocking {
            context.consentDataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }

    /** INT-01: Verifies that saving consent through the repository actually
     * persists the data to DataStore and can be retrieved correctly. */
    @Test
    fun saveConsent_persistsDataAndCanBeRetrieved() = runBlocking {
        // Verify that the initial state is that consent is not given
        val initialConsent = repository.hasGivenConsent().first()
        assertFalse("Consent should be false at start", initialConsent)

        // Save consent through the repository
        val testVersion = 1
        repository.saveConsent(given = true, version = testVersion)

        // Retrieve data again and verify it is updated correctly
        val updatedConsent = repository.hasGivenConsent().first()
        assertTrue("Consent should be true after saving", updatedConsent)

        // Confirm the version number was also persisted
        val versionResult = repository.getConsentVersion().first()
        assertTrue("Consent version should be saved correctly", versionResult == testVersion)
    }

    /** INT-02: Verifies that revoking consent correctly updates the underlying
     * storage to reflect that consent is no longer given. */
    @Test
    fun revokeConsent_updatesStorageCorrectly() = runBlocking {
        // Save an initial positive consent
        repository.saveConsent(given = true, version = 1)
        assertTrue(repository.hasGivenConsent().first())

        // Revoke the consent (set to false)
        repository.saveConsent(given = false, version = 0)

        // Verify that the storage now returns false
        val result = repository.hasGivenConsent().first()
        assertFalse("Consent should be false after it is revoked", result)
    }
}
