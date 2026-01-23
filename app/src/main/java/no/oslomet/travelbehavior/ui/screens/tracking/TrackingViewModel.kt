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

/**
 * Represents the state of the tracking UI, including the active path and current trip status.
 */
data class TrackingUiState(
    val isTracking: Boolean = false,
    val pathPoints: List<LatLng> = emptyList(),
    val activeTripId: String? = null,
    val isSaving: Boolean = false
)

/**
 * ViewModel responsible for managing GPS tracking logic, local data storage, 
 * and synchronization with the background TrackingService.
 */
class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val tripDao: TripDao = AppDatabase.getInstance(getApplication()).tripDao()
    private val trackPointDao: TrackPointDao = AppDatabase.getInstance(getApplication()).trackPointDao()
    private val workManager = WorkManager.getInstance(application)

    // Main UI state observed by the TrackingScreen
    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    // Holds track points for the current trip preview
    private val _trackPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()

    init {
        ensureFirebaseLogin()
        restoreTripIdIfActive()

        // Sync local UI state with updates from the background TrackingService
        TrackingService.pathPoints.onEach {
            _uiState.update { state -> state.copy(pathPoints = it) }
        }.launchIn(viewModelScope)

        TrackingService.isTracking.onEach {
            _uiState.update { state -> state.copy(isTracking = it) }
        }.launchIn(viewModelScope)
    }

    /**
     * Loads all location points for a specific trip to be displayed on a map.
     */
    fun loadTrackPointsForTrip(tripId: String) {
        viewModelScope.launch {
            _trackPoints.value = trackPointDao.getTrackPointsForTrip(tripId)
        }
    }

    /**
     * Checks if a trip was previously in progress or pending save after an app restart.
     */
    private fun restoreTripIdIfActive() {
        val activeTripId = TripManager.getTripId(getApplication())
        if (activeTripId != null) {
            _uiState.update { it.copy(activeTripId = activeTripId) }
            Log.d("TrackingViewModel", "Restored active trip ID: $activeTripId")
        }
    }

    /**
     * Starts a new tracking session, generates a unique ID, and initializes time anchors.
     */
    fun startTracking() {
        // Prevent starting a new trip if one is pending confirmation/save
        if (_uiState.value.activeTripId != null && !_uiState.value.isTracking) {
            Toast.makeText(getApplication(), "You must save or delete the previous trip first.", Toast.LENGTH_LONG).show()
            return
        }

        val localTripId = UUID.randomUUID().toString()
        Log.i("Tracking", "User started a new trip. Local ID: $localTripId")
        TripManager.saveTripId(getApplication(), localTripId)

        // Save the midnight anchor first to ensure consistent relative timestamp calculations
        TripManager.saveTripStartDayMidnight(getApplication())
        // Record the start time of the trip relative to the midnight anchor
        TripManager.saveTripStartTime(getApplication())

        _uiState.update { it.copy(activeTripId = localTripId) }

        sendCommandToService(TrackingService.ACTION_START_SERVICE)
    }

    /**
     * Stops the tracking service and records the trip end time.
     */
    fun stopTracking(): String? {
        val tripId = TripManager.getTripId(getApplication())
        Log.i("Tracking", "User stopped trip ID: $tripId")
        TripManager.saveTripEndTime(getApplication())
        sendCommandToService(TrackingService.ACTION_STOP_SERVICE)
        return tripId
    }

    /**
     * sends control commands (Start/Stop) to the background TrackingService.
     */
    private fun sendCommandToService(action: String) {
        Intent(getApplication(), TrackingService::class.java).also {
            it.action = action
            getApplication<Application>().startService(it)
        }
    }

    /**
     * Saves the trip and ratings locally and schedules a background sync.
     */
    fun saveTripAndRatings(localTripId: String, tripRating: Int, delayRating: Int, delayMinutes: Int?, delayComment: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val startTime = TripManager.getTripStartTime(getApplication())
                val endTime = TripManager.getTripEndTime(getApplication())
                val midnight = TripManager.getTripStartDayMidnight(getApplication())

                // Calculate relative time as a fallback if specific timestamps are missing.
                // Ensures timestamps are always relative to the midnight anchor.
                val relativeNow = if (midnight != 0L) System.currentTimeMillis() - midnight else 0L

                val trip = Trip(
                    id = localTripId,
                    startTimestamp = if (startTime != 0L) startTime else relativeNow,
                    endTimestamp = if (endTime != 0L) endTime else relativeNow,
                    overallRating = tripRating,
                    delayRating = delayRating,
                    delayMinutes = delayMinutes,
                    delayComment = delayComment,
                    isSynced = false
                )
                tripDao.insert(trip)
                Log.d("TrackingViewModel", "Saved trip locally with ratings. Trip ID: $localTripId")
                Toast.makeText(getApplication(), "Trip saved successfully!", Toast.LENGTH_SHORT).show()

                scheduleTripSync()

                // Clean up all temporary trip-related state from persistent storage
                clearLocalTripState()
                _uiState.update { it.copy(activeTripId = null, pathPoints = emptyList()) }

            } catch (e: Exception) {
                Log.e("TrackingViewModel", "Failed to save trip locally. Error: ${e.message}", e)
                Toast.makeText(getApplication(), "Error: The trip could not be saved.", Toast.LENGTH_LONG).show()
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    /**
     * Background task to sync unsynced trips to the server when network is available.
     */
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

    /**
     * Permanently deletes a trip and its associated track points from the local database.
     */
    fun deleteTrip(localTripId: String) {
        viewModelScope.launch {
            Log.d("TrackingViewModel", "User chose to DELETE. Deleting local data for ID: $localTripId")
            trackPointDao.deleteByTripId(localTripId)
            tripDao.deleteById(localTripId)

            clearLocalTripState()
            _uiState.update { it.copy(activeTripId = null, pathPoints = emptyList()) }
            Toast.makeText(getApplication(), "Trip has been deleted.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Clears all temporary trip metadata from TripManager.
     */
    private fun clearLocalTripState() {
        TripManager.clearTripId(getApplication())
        TripManager.clearTripStartTime(getApplication())
        TripManager.clearTripEndTime(getApplication())
        TripManager.clearTripStartDayMidnight(getApplication())
    }

    /**
     * Ensures the user is authenticated anonymously with Firebase for cloud synchronization.
     */
    private fun ensureFirebaseLogin() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }
}
