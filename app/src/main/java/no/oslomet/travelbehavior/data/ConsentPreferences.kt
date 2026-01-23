package no.oslomet.travelbehavior.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Name of the DataStore file for consent preferences
private const val DS_NAME = "consent_prefs"

// Context extension to access consent DataStore
val Context.consentDataStore by preferencesDataStore(name = DS_NAME)

// Preference keys used for consent storage
private object Keys {
    val CONSENT_GIVEN = booleanPreferencesKey("consent_given")
    val CONSENT_VERSION = intPreferencesKey("consent_version_accepted")
}

/**
 * DataStore implementation of the ConsentRepository interface.
 * Used to store and retrieve user consent preferences.
 */
class ConsentRepositoryImpl(private val context: Context) : ConsentRepository {

    // The current version of the consent agreement
    private val CURRENT_CONSENT_VERSION: Int = 1

    // Checks if the user has accepted the most recent consent version
    override fun hasGivenConsent(): Flow<Boolean> {
        return context.consentDataStore.data.map { prefs ->
            val given = prefs[Keys.CONSENT_GIVEN] ?: false
            val version = prefs[Keys.CONSENT_VERSION] ?: 0
            given && version >= CURRENT_CONSENT_VERSION
        }
    }

    // Returns the version number of the accepted consent
    override fun getConsentVersion(): Flow<Int> {
        return context.consentDataStore.data.map { prefs ->
            prefs[Keys.CONSENT_VERSION] ?: 0
        }
    }

    // Saves the user's consent choice and version number
    override suspend fun saveConsent(given: Boolean, version: Int) {
        context.consentDataStore.edit { prefs ->
            prefs[Keys.CONSENT_GIVEN] = given
            if (given) {
                prefs[Keys.CONSENT_VERSION] = version
            }
        }
    }
}
