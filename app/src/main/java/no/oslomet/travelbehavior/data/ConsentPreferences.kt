package no.oslomet.travelbehavior.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ---------- DataStore singleton (TOP-LEVEL!) ----------
private const val DS_NAME = "consent_prefs"
// NOTE: this must be top-level (outside any class)
val Context.consentDataStore by preferencesDataStore(name = DS_NAME)

// ---------- Keys ----------
private object Keys {
    val CONSENT_GIVEN = booleanPreferencesKey("consent_given")
    val CONSENT_VERSION = intPreferencesKey("consent_version_accepted")
}

/**
 * The concrete implementation of the [ConsentRepository] that uses DataStore.
 * This class is now the "real" repository that talks to the device storage.
 */
class ConsentRepositoryImpl(private val context: Context) : ConsentRepository {

    private val CURRENT_CONSENT_VERSION: Int = 1

    override fun hasGivenConsent(): Flow<Boolean> {
        return context.consentDataStore.data.map { prefs ->
            val given = prefs[Keys.CONSENT_GIVEN] ?: false
            val version = prefs[Keys.CONSENT_VERSION] ?: 0
            given && version >= CURRENT_CONSENT_VERSION
        }
    }

    override fun getConsentVersion(): Flow<Int> {
        return context.consentDataStore.data.map { prefs ->
            prefs[Keys.CONSENT_VERSION] ?: 0
        }
    }

    override suspend fun saveConsent(given: Boolean, version: Int) {
        context.consentDataStore.edit { prefs ->
            prefs[Keys.CONSENT_GIVEN] = given
            if (given) {
                prefs[Keys.CONSENT_VERSION] = version
            }
        }
    }
}
