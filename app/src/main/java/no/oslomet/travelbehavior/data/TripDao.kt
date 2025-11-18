package no.oslomet.travelbehavior.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: Trip)

    @Query("SELECT * FROM trips WHERE isSynced = 0")
    suspend fun getUnsyncedTrips(): List<Trip>

    // FIKS: Ny, spisset funksjon for å kun sette Firebase-ID.
    // HVORFOR: Dette er kritisk for gjenopptakbar synkronisering. Vi må kunne lagre
    // koblingen til Firebase umiddelbart, uten å markere hele turen som ferdig.
    @Query("UPDATE trips SET firebaseTripId = :firebaseId WHERE id = :localId")
    suspend fun setFirebaseId(localId: String, firebaseId: String)

    @Query("UPDATE trips SET isSynced = 1, firebaseTripId = :firebaseId WHERE id = :localId")
    suspend fun markAsSynced(localId: String, firebaseId: String)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: String)
}
