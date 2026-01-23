package no.oslomet.travelbehavior.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for managing Trip entities in the local database.
 */
@Dao
interface TripDao {


    /**
     * Inserts or replaces a trip record in the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: Trip)


    /**
     * Retrieves a list of trips that haven't been synchronized with Firebase.
     */
    @Query("SELECT * FROM trips WHERE isSynced = 0")
    suspend fun getUnsyncedTrips(): List<Trip>

    /**
     * Updates the remote Firebase ID for a specific local trip.
     */
    @Query("UPDATE trips SET firebaseTripId = :firebaseId WHERE id = :localId")
    suspend fun setFirebaseId(localId: String, firebaseId: String)

    /**
     * Marks a trip as synchronized and updates its remote Firebase ID.
     */
    @Query("UPDATE trips SET isSynced = 1, firebaseTripId = :firebaseId WHERE id = :localId")
    suspend fun markAsSynced(localId: String, firebaseId: String)


    /**
     * Deletes a specific trip record by its ID.
     */
    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: String)
}
