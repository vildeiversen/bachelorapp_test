package no.oslomet.travelbehavior.ui.screens.tracking

import android.app.Application
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.oslomet.travelbehavior.data.*
import no.oslomet.travelbehavior.location.TrackingService
import no.oslomet.travelbehavior.worker.TripSyncWorker
import java.util.UUID

data class TrackingUiState(
    val isTracking: Boolean = false,
    val pathPoints: List<LatLng> = emptyList(),
    val activeTripId: String? = null,
    val isSaving: Boolean = false
)

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val tripDao: TripDao = AppDatabase.getInstance(getApplication()).tripDao()
    // HVA: Initialiserer TrackPointDao.
    // HVORFOR: Vi trenger tilgang til denne for å hente de lagrede punktene for en tur.
    private val trackPointDao: TrackPointDao = AppDatabase.getInstance(getApplication()).trackPointDao()
    private val workManager = WorkManager.getInstance(application)

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    // HVA: En ny StateFlow for å holde på listen av TrackPoint-objekter for oppsummeringsskjermen.
    // HVORFOR: Denne vil eksponere listen av punkter til TripSummaryScreen, slik at den
    // kan observere endringer og tegne kartet når dataen er klar.
    private val _trackPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()

    init {
        ensureFirebaseLogin()
        restoreTripIdIfActive() // FIKS: Gjenoppretter tur-ID ved oppstart

        TrackingService.pathPoints.onEach {
            _uiState.update { state -> state.copy(pathPoints = it) }
        }.launchIn(viewModelScope)

        TrackingService.isTracking.onEach {
            _uiState.update { state -> state.copy(isTracking = it) }
        }.launchIn(viewModelScope)
    }

    // HVA: En ny funksjon for å laste alle punktene for en spesifikk tur fra databasen.
    // HVORFOR: Denne funksjonen kalles fra TripSummaryScreen for å hente dataen som trengs
    // for å tegne ruten på kartet. Den kjører i en coroutine for ikke å blokkere UI-tråden.
    fun loadTrackPointsForTrip(tripId: String) {
        viewModelScope.launch {
            // Henter punktene fra databasen og oppdaterer _trackPoints-flowen.
            _trackPoints.value = trackPointDao.getTrackPointsForTrip(tripId)
        }
    }

    // HVA: En ny funksjon som sjekker om det finnes en pågående tur.
    // HVORFOR: Dette er avgjørende for å gjenopprette tilstanden hvis appen har
    // blitt lukket og åpnet igjen midt i en tur.
    private fun restoreTripIdIfActive() {
        val activeTripId = TripManager.getTripId(getApplication())
        if (activeTripId != null) {
            _uiState.update { it.copy(activeTripId = activeTripId) }
            Log.d("TrackingViewModel", "Restored active trip ID: $activeTripId")
        }
    }

    fun startTracking() {
        val localTripId = UUID.randomUUID().toString()
        Log.d("TrackingViewModel", "Starting new LOCAL-ONLY trip with ID: $localTripId")
        TripManager.saveTripId(getApplication(), localTripId)
        _uiState.update { it.copy(activeTripId = localTripId) }

        sendCommandToService(TrackingService.ACTION_START_SERVICE)
    }

    fun stopTracking(): String? {
        // FIKS: Henter ID-en FØR servicen stoppes
        val tripId = TripManager.getTripId(getApplication())
        sendCommandToService(TrackingService.ACTION_STOP_SERVICE)
        // TripManager.clearTripId() skjer nå kun etter vellykket lagring.
        return tripId
    }

    private fun sendCommandToService(action: String) {
        Intent(getApplication(), TrackingService::class.java).also {
            it.action = action
            getApplication<Application>().startService(it)
        }
    }

    fun saveTripAndRatings(localTripId: String, tripRating: Int, delayRating: Int, delayMinutes: Int?, delayComment: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val trip = Trip(
                    id = localTripId,
                    endTimestamp = System.currentTimeMillis(),
                    overallRating = tripRating,
                    delayRating = delayRating,
                    delayMinutes = delayMinutes,
                    delayComment = delayComment,
                    isSynced = false
                )
                tripDao.insert(trip)
                Log.d("TrackingViewModel", "Saved trip locally with ratings. Trip ID: $localTripId")
                Toast.makeText(getApplication(), "Turen er lagret!", Toast.LENGTH_SHORT).show()

                scheduleTripSync()

                // Først NÅ er det trygt å fjerne ID-en
                TripManager.clearTripId(getApplication())
                _uiState.update { it.copy(activeTripId = null, pathPoints = emptyList()) }

            } catch (e: Exception) {
                Log.e("TrackingViewModel", "Failed to save trip locally. Error: ${e.message}", e)
                Toast.makeText(getApplication(), "Feil: Kunne ikke lagre turen", Toast.LENGTH_LONG).show()
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun scheduleTripSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<TripSyncWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueue(syncRequest)
        Log.d("TrackingViewModel", "Trip sync worker has been enqueued.")
    }

    fun deleteTrip(localTripId: String) {
        viewModelScope.launch {
            Log.d("TrackingViewModel", "User chose to DELETE. Deleting local data for ID: $localTripId")
            tripDao.deleteById(localTripId)
            // Også trygt å fjerne ID-en her
            TripManager.clearTripId(getApplication())
            _uiState.update { it.copy(activeTripId = null, pathPoints = emptyList()) }
            Toast.makeText(getApplication(), "Turen ble slettet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ensureFirebaseLogin() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }
}
