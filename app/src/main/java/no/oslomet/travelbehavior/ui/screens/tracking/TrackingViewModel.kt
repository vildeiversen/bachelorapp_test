package no.oslomet.travelbehavior.ui.screens.tracking

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.oslomet.travelbehavior.data.*
import no.oslomet.travelbehavior.location.LocationService
import java.util.UUID

// FIKS: UI State inneholder ikke lenger sanntidsdata for sporing
data class TrackingUiState(
    val activeTripId: String? = null,
    val isSaving: Boolean = false,
    // FIKS: isTracking og pathPoints er fjernet, de hentes nå fra servicen
)

class TrackingViewModel(private val app: Application) : AndroidViewModel(app) {

    // FIKS: ViewModelen har ikke lenger sin egen LocationClient
    private val dao: TrackPointDao = AppDatabase.getInstance(app).trackPointDao()
    private val firebaseRepo: FirebaseRepository = FirebaseRepository()
    private val syncRepo: SyncRepository = SyncRepository(dao, firebaseRepo)

    private val _uiState = MutableStateFlow(TrackingUiState())

    // FIKS: Kombinerer ViewModel-state med sanntidsdata fra LocationService
    val uiState = combine(
        _uiState,
        LocationService.isTracking,
        LocationService.pathPoints
    ) { vmState, isTracking, pathPoints ->
        Pair(vmState, Pair(isTracking, pathPoints))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(TrackingUiState(), Pair(false, emptyList())))


    init {
        ensureFirebaseLogin()
        // Sjekker om en tur allerede er aktiv når ViewModel-en opprettes
        _uiState.update { it.copy(activeTripId = TripManager.getTripId(app)) }
    }

    fun startTracking() {
        val localTripId = UUID.randomUUID().toString()
        Log.d("TrackingViewModel", "Requesting to start new trip with ID: $localTripId")
        TripManager.saveTripId(app, localTripId)
        _uiState.update { it.copy(activeTripId = localTripId) }

        // FIKS: Sender kommando til LocationService i stedet for å starte selv
        Intent(app, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            app.startService(this)
        }
    }

    fun stopTracking(): String? {
        val tripId = _uiState.value.activeTripId
        Log.d("TrackingViewModel", "Requesting to stop trip ID: $tripId")

        // FIKS: Sender kommando for å stoppe LocationService
        Intent(app, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
            app.startService(this)
        }

        // Vi nullstiller ikke isTracking her, det styres nå av servicen.
        return tripId
    }

    fun saveTrip(localTripId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                Log.d("TrackingViewModel", "Saving trip: $localTripId")
                val firebaseTripId = firebaseRepo.startTrip()
                syncRepo.syncPending(localTripId = localTripId, firebaseTripId = firebaseTripId)
                firebaseRepo.endTrip(firebaseTripId)
                dao.deleteByTripId(localTripId)
                TripManager.clearTripId(app)
                _uiState.update { it.copy(activeTripId = null) }
            } catch (e: Exception) {
                Log.e("TrackingViewModel", "Failed to save trip. Error: ${e.message}", e)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteTrip(localTripId: String) {
        viewModelScope.launch {
            Log.d("TrackingViewModel", "Deleting trip: $localTripId")
            dao.deleteByTripId(localTripId)
            TripManager.clearTripId(app)
            _uiState.update { it.copy(activeTripId = null) }
        }
    }

    private fun ensureFirebaseLogin() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            FirebaseAuth.getInstance().signInAnonymously()
        }
    }

    // onCleared er ikke lenger nødvendig for å stoppe sporing
}
