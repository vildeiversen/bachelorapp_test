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
    val CONSENT_GIVEN: Preferences.Key<Boolean> =
        booleanPreferencesKey("consent_given")
    val CONSENT_VERSION: Preferences.Key<Int> =
        intPreferencesKey("consent_version_accepted")
}

// ---------- Public model ----------
data class ConsentState(
    val consentGiven: Boolean,
    val acceptedVersion: Int
)

// ---------- Repository ----------
class ConsentRepository(private val context: Context) {

    /** Bump this when the consent text meaningfully changes */
    val CURRENT_CONSENT_VERSION: Int = 1

    val consentState: Flow<ConsentState> =
        context.consentDataStore.data.map { prefs ->
            val given = prefs[Keys.CONSENT_GIVEN] ?: false
            val version = prefs[Keys.CONSENT_VERSION] ?: 0
            ConsentState(
                consentGiven = given && version >= CURRENT_CONSENT_VERSION,
                acceptedVersion = version
            )
        }

    suspend fun accept() {
        context.consentDataStore.edit { prefs ->
            prefs[Keys.CONSENT_GIVEN] = true
            prefs[Keys.CONSENT_VERSION] = CURRENT_CONSENT_VERSION
        }
    }

    suspend fun revoke() {
        context.consentDataStore.edit { prefs ->
            prefs[Keys.CONSENT_GIVEN] = false
            // optionally: prefs[Keys.CONSENT_VERSION] = 0
        }
    }
}
