package no.oslomet.travelbehavior.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.oslomet.travelbehavior.network.toDto

class SyncRepository(
    private val tripDao: TripDao,
    private val trackPointDao: TrackPointDao,
    private val remote: FirebaseRepository
) {
    suspend fun syncSingleTrip(trip: Trip) = withContext(Dispatchers.IO) {
        Log.d("SyncRepository", "Starting sync for local trip ID: ${trip.id}")
        try {
            val firebaseTripId = remote.startTrip()
            Log.d("SyncRepository", "Created Firebase trip: $firebaseTripId")

            val trackPoints = trackPointDao.getTrackPointsForTrip(trip.id)
            Log.d("SyncRepository", "Found ${trackPoints.size} track points to upload.")

            // HVA: Optimalisert til å bruke batch-opplasting.
            // HVORFOR: I stedet for å sende hvert punkt enkeltvis, sendes alle punktene
            // i en eller noen få "pakker". Dette er dramatisk mye raskere og mer effektivt.
            if (trackPoints.isNotEmpty()) {
                val pointDtos = trackPoints.map { it.toDto() } // Konverterer alle til DTOs først
                remote.addTrackPointsBatch(firebaseTripId, pointDtos)
                Log.d("SyncRepository", "Finished uploading all track points in batches.")
            }

            remote.endTrip(
                tripId = firebaseTripId,
                tripRating = trip.overallRating ?: 0,
                delayRating = trip.delayRating ?: 0,
                delayMinutes = trip.delayMinutes,
                delayComment = trip.delayComment
            )
            Log.d("SyncRepository", "Ended Firebase trip with rating data.")

            tripDao.markAsSynced(localId = trip.id, firebaseId = firebaseTripId)
            Log.d("SyncRepository", "Marked local trip ${trip.id} as synced.")

        } catch (e: Exception) {
            Log.e("SyncRepository", "Sync failed for trip ${trip.id}. It will be retried later.", e)
            throw e
        }
    }
}
