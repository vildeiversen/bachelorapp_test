package no.oslomet.travelbehavior.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DS_NAME = "consent_prefs"
val Context.consentDataStore by preferencesDataStore(DS_NAME)

private object Keys {
    val CONSENT_GIVEN = booleanPreferencesKey("consent_given")
    val CONSENT_VERSION = intPreferencesKey("consent_version_accepted")
}

data class ConsentState(val consentGiven: Boolean, val acceptedVersion: Int)

class ConsentRepository(private val context: Context) {

    val CURRENT_CONSENT_VERSION = 1

    val consentState: Flow<ConsentState> = context.consentDataStore.data.map { p ->
        val given = p[Keys.CONSENT_GIVEN] ?: false
        val version = p[Keys.CONSENT_VERSION] ?: 0
        ConsentState(given && version >= CURRENT_CONSENT_VERSION, version)
    }

    suspend fun accept() {
        context.consentDataStore.edit { p ->
            p[Keys.CONSENT_GIVEN] = true
            p[Keys.CONSENT_VERSION] = CURRENT_CONSENT_VERSION
        }
    }

    suspend fun revoke() {
        context.consentDataStore.edit { p ->
            p[Keys.CONSENT_GIVEN] = false
        }
    }
}
