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

// UI State for the Tracking Screen
data class TrackingUiState(
    val isTracking: Boolean = false,
    val pathPoints: List<LatLng> = emptyList(),
    val activeTripId: String? = null
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
        // FIKS: Vi fjerner logikken som automatisk satte isTracking=true.
        // Sporing skal kun starte når brukeren trykker på knappen.
        // En eventuell gammel/forlatt tripId vil bli overskrevet neste gang brukeren starter en ny tur.
    }

    fun startTracking() {
        viewModelScope.launch {
            val newTripId = firebaseRepo.startTrip()
            TripManager.saveTripId(getApplication(), newTripId)
            Log.d("TRIP", "Started and saved new trip: $newTripId")

            _uiState.update { it.copy(isTracking = true, pathPoints = emptyList(), activeTripId = newTripId) }

            locClient.start { lat, lon, acc ->
                val newPoint = LatLng(lat, lon)
                _uiState.update { it.copy(pathPoints = it.pathPoints + newPoint) }

                viewModelScope.launch {
                    // NY: Lagrer med tripId
                    dao.insert(TrackPoint(tripId = newTripId, timestamp = System.currentTimeMillis(), lat = lat, lon = lon, acc = acc))
                }
            }
        }
    }

    // Stopper KUN sporingen lokalt
    fun stopTracking(): String? {
        locClient.stop()
        val tripId = _uiState.value.activeTripId
        _uiState.update { it.copy(isTracking = false) }
        return tripId
    }

    // NY: Lagrer turen til Firebase
    fun saveTrip(tripId: String) {
        viewModelScope.launch {
            Log.d("SYNC", "Saving trip to Firebase: $tripId")
            syncRepo.syncPending(tripId)
            firebaseRepo.endTrip(tripId)
            TripManager.clearTripId(getApplication())
            _uiState.update { it.copy(activeTripId = null, pathPoints = emptyList()) }
        }
    }

    // NY: Sletter turen fra Room
    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            Log.d("SYNC", "Deleting local trip data for: $tripId")
            dao.deleteByTripId(tripId)
            TripManager.clearTripId(getApplication())
            _uiState.update { it.copy(activeTripId = null, pathPoints = emptyList()) }
        }
    }

    private fun ensureFirebaseLogin() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener { Log.d("FB_AUTH", "Anonymous login successful.") }
                .addOnFailureListener { e -> Log.e("FB_AUTH", "Anonymous login failed", e) }
        } else {
            Log.d("FB_AUTH", "Already logged in.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        locClient.stop()
    }
}
