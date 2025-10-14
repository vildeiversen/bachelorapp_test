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
        // Sørg for at Firebase er logget inn når ViewModel-en opprettes
        ensureFirebaseLogin()

        // Gjenopprett aktiv tur-ID ved oppstart
        viewModelScope.launch {
            val tripId = TripManager.getTripId(application)
            if (tripId != null) {
                _uiState.update { it.copy(activeTripId = tripId) }
            }
        }
    }

    fun startTracking() {
        viewModelScope.launch {
            // Opprett en ny tur i Firebase og lagre ID-en
            val newTripId = firebaseRepo.startTrip()
            TripManager.saveTripId(getApplication(), newTripId)
            Log.d("TRIP", "Started and saved new trip: $newTripId")

            _uiState.update { it.copy(isTracking = true, pathPoints = emptyList(), activeTripId = newTripId) }

            // Start innsamling av lokasjonspoeng
            locClient.start { lat, lon, acc ->
                val newPoint = LatLng(lat, lon)
                _uiState.update { currentState ->
                    currentState.copy(pathPoints = currentState.pathPoints + newPoint)
                }

                viewModelScope.launch {
                    dao.insert(TrackPoint(timestamp = System.currentTimeMillis(), lat = lat, lon = lon, acc = acc))
                }
            }
        }
    }

    fun stopTracking() {
        locClient.stop()

        viewModelScope.launch {
            val tripId = _uiState.value.activeTripId
            if (tripId != null) {
                Log.d("SYNC", "Stopping track and syncing trip: $tripId")
                syncRepo.syncPending(tripId)
                firebaseRepo.endTrip(tripId)
                TripManager.clearTripId(getApplication())
            } else {
                Log.w("SYNC", "stopTracking called but no active tripId found.")
            }
            _uiState.update { it.copy(isTracking = false, activeTripId = null) }
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
