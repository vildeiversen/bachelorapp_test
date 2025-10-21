package no.oslomet.travelbehavior.ui.screens.tracking

import android.app.Application
import android.util.Log
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.oslomet.travelbehavior.data.*
import no.oslomet.travelbehavior.location.LocationClient
import no.oslomet.travelbehavior.worker.TripSyncWorker
import java.util.UUID

// UI State for the Tracking Screen
data class TrackingUiState(
    val isTracking: Boolean = false,
    val pathPoints: List<LatLng> = emptyList(),
    val activeTripId: String? = null, // Dette vil nå være en LOKAL UUID
    val isSaving: Boolean = false
)

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val locClient: LocationClient = LocationClient(getApplication())
    private val trackPointDao: TrackPointDao = AppDatabase.getInstance(getApplication()).trackPointDao()
    private val tripDao: TripDao = AppDatabase.getInstance(getApplication()).tripDao()
    private val workManager = WorkManager.getInstance(application)

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
                trackPointDao.insert(TrackPoint(tripId = localTripId, timestamp = System.currentTimeMillis(), lat = lat, lon = lon, acc = acc))
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

    // HVA: Oppdatert funksjonen til å motta delayMinutes.
    // HVORFOR: For å kunne ta imot den nye verdien fra UI-et.
    fun saveTripAndRatings(localTripId: String, tripRating: Int, delayRating: Int, delayMinutes: Int?, delayComment: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val trip = Trip(
                    id = localTripId,
                    endTimestamp = System.currentTimeMillis(),
                    overallRating = tripRating,
                    delayRating = delayRating,
                    // HVA: Lagrer den nye verdien i Trip-objektet.
                    // HVORFOR: Sørger for at minutt-forsinkelsen blir lagret lokalt.
                    delayMinutes = delayMinutes,
                    delayComment = delayComment,
                    isSynced = false
                )
                tripDao.insert(trip)
                Log.d("TrackingViewModel", "Saved trip locally with ratings. Trip ID: $localTripId")

                scheduleTripSync()

                TripManager.clearTripId(getApplication())
                _uiState.update { it.copy(activeTripId = null, pathPoints = emptyList()) }

            } catch (e: Exception) {
                Log.e("TrackingViewModel", "Failed to save trip locally. Error: ${e.message}", e)
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
            trackPointDao.deleteByTripId(localTripId)
            tripDao.deleteById(localTripId)
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
