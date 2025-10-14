package no.oslomet.travelbehavior.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncRepository(
    private val dao: TrackPointDao,
    private val remote: FirebaseRepository
) {
    /**
     * Laster opp pending punkter (uploaded=false) for en gitt tripId i små batcher,
     * markerer som uploaded når opplasting lykkes.
     */
    suspend fun syncPending(tripId: String, batchSize: Int = 200) = withContext(Dispatchers.IO) {
        while (true) {
            // OPPDATERT: Henter kun punkter for den spesifikke turen
            val batch = dao.getPendingForTrip(tripId, batchSize)
            if (batch.isEmpty()) break

            val uploadedIds = mutableListOf<Long>()
            for (p in batch) {
                try {
                    remote.addTrackPoint(tripId, p.toDto())
                    uploadedIds.add(p.id)
                } catch (e: Exception) {
                    // Logg og fortsett – ikke stopp hele syncen på én feil
                    e.printStackTrace()
                }
            }
            if (uploadedIds.isNotEmpty()) dao.markUploaded(uploadedIds)
        }
    }
}
