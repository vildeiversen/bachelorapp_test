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

    // FIKS: Fullstendig omskriving for å støtte gjenopptakbar synkronisering.
    // HVORFOR: Forhindrer dobbeltlagring ved nettverksfeil. Hvis prosessen
    // feiler halvveis, vil den fortsette der den slapp neste gang.
    suspend fun syncSingleTrip(trip: Trip) = withContext(Dispatchers.IO) {
        Log.d("SyncRepository", "Starting resumable sync for local trip ID: ${trip.id}")

        try {
            // STEG 1: Hent eller opprett Firebase ID
            val firebaseTripId = trip.firebaseTripId ?: run {
                Log.d("SyncRepository", "No Firebase ID found. Creating new trip in Firebase.")
                val newId = remote.startTrip(trip.startTimestamp)
                // Kritisk: Lagre koblingen lokalt umiddelbart!
                tripDao.setFirebaseId(trip.id, newId)
                Log.d("SyncRepository", "Linked local trip ${trip.id} to Firebase trip $newId.")
                newId
            }

            // STEG 2: Last opp usynkroniserte punkter i grupper
            val unsyncedPoints = trackPointDao.getUnsyncedTrackPointsForTrip(trip.id)
            Log.d("SyncRepository", "Found ${unsyncedPoints.size} unsynced points for trip $firebaseTripId.")

            if (unsyncedPoints.isNotEmpty()) {
                // Laster opp i grupper for å unngå minneproblemer og store enkelt-opplastinger
                unsyncedPoints.chunked(100).forEach { chunk ->
                    val pointDtos = chunk.map { it.toDto() }
                    remote.addTrackPointsBatch(firebaseTripId, pointDtos)

                    // Kritisk: Marker denne gruppen som synkronisert lokalt
                    val uploadedIds = chunk.map { it.id }
                    trackPointDao.markAsSynced(uploadedIds)
                    Log.d("SyncRepository", "Synced a chunk of ${chunk.size} points.")
                }
            }

            // STEG 3: Fullfør turen i Firebase og lokalt
            Log.d("SyncRepository", "All points synced. Ending trip $firebaseTripId in Firebase.")
            remote.endTrip(
                tripId = firebaseTripId,
                endTimestamp = trip.endTimestamp,
                tripRating = trip.overallRating ?: 0,
                delayRating = trip.delayRating ?: 0,
                delayMinutes = trip.delayMinutes,
                delayComment = trip.delayComment
            )

            // Siste steg: Marker hele den lokale turen som ferdig
            tripDao.markAsSynced(localId = trip.id, firebaseId = firebaseTripId)
            Log.d("SyncRepository", "Successfully marked local trip ${trip.id} as fully synced.")

        } catch (e: Exception) {
            Log.e("SyncRepository", "Resumable sync failed for trip ${trip.id}. It will be retried later.", e)
            // Viktig: Kast unntaket videre slik at TripSyncWorker vet at den må prøve på nytt
            throw e
        }
    }
}
