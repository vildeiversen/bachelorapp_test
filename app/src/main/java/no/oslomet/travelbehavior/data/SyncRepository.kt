package no.oslomet.travelbehavior.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.oslomet.travelbehavior.network.toDto

/**
 * Repository responsible for synchronizing local trip data with Firebase.
 */
class SyncRepository(
    private val tripDao: TripDao,
    private val trackPointDao: TrackPointDao,
    private val remote: FirebaseRepository
) {

    /**
     * Synchronizes a single trip by uploading its points and metadata to Firebase.
     * After a successful sync, the local data is deleted.
     */
    suspend fun syncSingleTrip(trip: Trip) = withContext(Dispatchers.IO) {
        Log.d("SyncRepository", "Starting resumable sync for local trip ID: ${trip.id}")

        try {
            // Step 1: Ensure the trip exists in Firebase and has a linked ID
            val firebaseTripId = trip.firebaseTripId ?: run {
                Log.d("SyncRepository", "No Firebase ID found. Creating new trip in Firebase.")
                val newId = remote.startTrip(trip.startTimestamp)
                
                tripDao.setFirebaseId(trip.id, newId)
                Log.d("SyncRepository", "Linked local trip ${trip.id} to Firebase trip $newId.")
                newId
            }

            // Step 2: Upload all unsynced track points in chunks
            val unsyncedPoints = trackPointDao.getUnsyncedTrackPointsForTrip(trip.id)
            if (unsyncedPoints.isNotEmpty()) {
                Log.d("SyncRepository", "Found ${unsyncedPoints.size} unsynced points for trip $firebaseTripId.")
                unsyncedPoints.chunked(100).forEach { chunk ->
                    val pointDtos = chunk.map { it.toDto() }
                    remote.addTrackPointsBatch(firebaseTripId, pointDtos)

                    // Mark points as synced locally to prevent duplicate uploads
                    val uploadedIds = chunk.map { it.id }
                    trackPointDao.markAsSynced(uploadedIds)
                    Log.d("SyncRepository", "Synced a chunk of ${chunk.size} points.")
                }
            }

            // Step 3: Finalize the trip in Firebase with ratings and end timestamp
            Log.d("SyncRepository", "All points synced. Ending trip $firebaseTripId in Firebase.")
            remote.endTrip(
                tripId = firebaseTripId,
                endTimestamp = trip.endTimestamp,
                tripRating = trip.overallRating ?: 0,
                delayRating = trip.delayRating ?: 0,
                delayMinutes = trip.delayMinutes,
                delayComment = trip.delayComment
            )

            // Step 4: Clean up local database after successful synchronization
            Log.d("SyncRepository", "Sync successful. Deleting local trip ${trip.id} and its points.")
            trackPointDao.deleteByTripId(trip.id)
            tripDao.deleteById(trip.id)
            Log.d("SyncRepository", "Successfully cleaned up local data.")

        } catch (e: Exception) {
            Log.e("SyncRepository", "Resumable sync failed for trip ${trip.id}. It will be retried later.", e)
            // Rethrow the error so WorkManager can retry the sync later
            throw e
        }
    }
}
