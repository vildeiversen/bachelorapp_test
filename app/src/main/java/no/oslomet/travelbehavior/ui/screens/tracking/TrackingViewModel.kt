package no.oslomet.travelbehavior.ui.screens.tracking

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.oslomet.travelbehavior.data.*
import no.oslomet.travelbehavior.location.LocationClient
import java.util.UUID

// UI State for the Tracking Screen
data class TrackingUiState(
    val isTracking: Boolean = false,
    val pathPoints: List<LatLng> = emptyList(),
    val activeTripId: String? = null, // Dette vil nå være en LOKAL UUID
    val isSaving: Boolean = false // FIKS: Ny tilstand for å vise lagringsstatus
)

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val locClient: LocationClient = LocationClient(getApplication())
    private val dao: TrackPointDao = AppDatabase.getInstance(getApplication()).trackPointDao()
    private val firebaseRepo: FirebaseRepository = FirebaseRepository()
    private val syncRepo: SyncRepository = SyncRepository(dao, firebaseRepo)

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    init {
        ensureFirebaseLogin()
    }

    fun startTracking() {
        val localTripId = UUID.randomUUID().toString()
        Log.d("TrackingViewModel", "Starting new LOCAL-ONLY trip with ID: $localTripId")
        TripManager.saveTripId(getApplication(), localTripId)

        _uiState.update { it.copy(isTracking = true, pathPoints = emptyList(), activeTripId = localTripId) }

        locClient.start { lat, lon, acc ->
            val newPoint = LatLng(lat, lon)
            _uiState.update { it.copy(pathPoints = it.pathPoints + newPoint) }

            viewModelScope.launch {
                dao.insert(TrackPoint(tripId = localTripId, timestamp = System.currentTimeMillis(), lat = lat, lon = lon, acc = acc))
            }
        }
    }

    fun stopTracking(): String? {
        locClient.stop()
        val tripId = _uiState.value.activeTripId
        Log.d("TrackingViewModel", "Local tracking stopped for trip ID: $tripId. Awaiting user action.")
        _uiState.update { it.copy(isTracking = false) }
        return tripId
    }

    fun saveTrip(localTripId: String) {
        viewModelScope.launch {
            // FIKS: Sett isSaving til true og bruk en finally-blokk
            _uiState.update { it.copy(isSaving = true) }
            try {
                Log.d("TrackingViewModel", "User chose to SAVE. Starting sync for local ID: $localTripId")

                val firebaseTripId = firebaseRepo.startTrip()
                syncRepo.syncPending(localTripId = localTripId, firebaseTripId = firebaseTripId)
                firebaseRepo.endTrip(firebaseTripId)

                Log.d("TrackingViewModel", "Sync complete. Deleting local data for ID: $localTripId")
                dao.deleteByTripId(localTripId)
                TripManager.clearTripId(getApplication())
                // Fjerner path-punktene, men venter med å nullstille alt til isSaving er false
                _uiState.update { it.copy(activeTripId = null, pathPoints = emptyList()) }

            } catch (e: Exception) {
                Log.e("TrackingViewModel", "Failed to save trip. The trip remains on the device. Error: ${e.message}", e)
            } finally {
                // Denne vil *alltid* kjøre, selv om jobben kanselleres
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteTrip(localTripId: String) {
        viewModelScope.launch {
            Log.d("TrackingViewModel", "User chose to DELETE. Deleting local data for ID: $localTripId")
            dao.deleteByTripId(localTripId)
            TripManager.clearTripId(getApplication())
            _uiState.update { it.copy(activeTripId = null, pathPoints = emptyList()) }
        }
    }

    private fun ensureFirebaseLogin() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }

    override fun onCleared() {
        super.onCleared()
        locClient.stop()
    }
}
