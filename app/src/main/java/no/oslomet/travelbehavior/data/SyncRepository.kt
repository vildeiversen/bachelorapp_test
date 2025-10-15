package no.oslomet.travelbehavior.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.oslomet.travelbehavior.network.toDto

class SyncRepository(
    private val dao: TrackPointDao,
    private val remote: FirebaseRepository
) {
    suspend fun syncPending(localTripId: String, firebaseTripId: String, batchSize: Int = 200) = withContext(Dispatchers.IO) {
        var batchNum = 1
        while (true) {
            val batch = dao.getPendingForTrip(localTripId, batchSize)
            if (batch.isEmpty()) {
                Log.d("SyncRepository", "No more points to sync for local trip $localTripId. Sync complete.")
                break
            }

            Log.d("SyncRepository", "Syncing batch ${batchNum++} with ${batch.size} points for local trip $localTripId...")

            val uploadedIds = mutableListOf<Long>()
            for (p in batch) {
                try {
                    remote.addTrackPoint(firebaseTripId, p.toDto())
                    uploadedIds.add(p.id)
                } catch (e: Exception) {
                    Log.e("SyncRepository", "Failed to upload point ${p.id}. It will be retried later.", e)
                    // Fortsett til neste punkt selv om ett feiler
                }
            }

            // FIKS: Markerer punktene som er lastet opp, slik at de ikke lastes opp igjen.
            if (uploadedIds.isNotEmpty()) {
                Log.d("SyncRepository", "Successfully uploaded ${uploadedIds.size} points. Marking them as synced.")
                dao.markUploaded(uploadedIds)
            }
        }
    }
}
