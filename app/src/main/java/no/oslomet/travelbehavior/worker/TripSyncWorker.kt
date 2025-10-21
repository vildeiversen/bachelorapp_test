package no.oslomet.travelbehavior.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import no.oslomet.travelbehavior.data.AppDatabase
import no.oslomet.travelbehavior.data.FirebaseRepository
import no.oslomet.travelbehavior.data.SyncRepository

// HVA: En bakgrunns-worker for å synkronisere turer.
// HVORFOR: WorkManager er designet for pålitelige bakgrunnsjobber som
// kjører selv om appen er lukket, og er perfekt for datasynkronisering.
class TripSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // HVA: Henter instanser av alle nødvendige repositories og DAOs.
    // HVORFOR: Worker-en trenger disse for å hente lokale data og kalle synk-logikken.
    private val tripDao = AppDatabase.getInstance(appContext).tripDao()
    private val trackPointDao = AppDatabase.getInstance(appContext).trackPointDao()
    private val firebaseRepo = FirebaseRepository()
    private val syncRepo = SyncRepository(tripDao, trackPointDao, firebaseRepo)

    override suspend fun doWork(): Result {
        Log.d("TripSyncWorker", "Worker started. Checking for unsynced trips.")

        // 1. Hent alle turer som ikke er synkronisert
        val unsyncedTrips = tripDao.getUnsyncedTrips()
        if (unsyncedTrips.isEmpty()) {
            Log.d("TripSyncWorker", "No unsynced trips found. Worker finishing.")
            return Result.success()
        }

        Log.d("TripSyncWorker", "Found ${unsyncedTrips.size} trips to sync.")

        // 2. Gå gjennom hver usynkroniserte tur og synkroniser den
        unsyncedTrips.forEach { trip ->
            try {
                // 3. Kaller den sentraliserte synk-logikken i SyncRepository
                syncRepo.syncSingleTrip(trip)
            } catch (e: Exception) {
                Log.e("TripSyncWorker", "Sync failed for trip ${trip.id}. Worker will retry later.", e)
                // Hvis noe feiler, vil WorkManager automatisk prøve jobben på nytt senere.
                return Result.failure()
            }
        }

        Log.d("TripSyncWorker", "Sync finished successfully for all trips.")
        return Result.success()
    }
}
