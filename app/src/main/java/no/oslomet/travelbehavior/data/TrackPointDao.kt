package no.oslomet.travelbehavior.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Data Access Object for track_points table.
 * Handles database operations for GPS location points.
 */
@Dao
interface TrackPointDao {
    
    // Inserts a single track point into the database
    @Insert
    suspend fun insert(trackPoint: TrackPoint)

    // Retrieves all track points for a specific trip, ordered by time
    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getTrackPointsForTrip(tripId: String): List<TrackPoint>

    // Retrieves points for a trip that have not yet been synced to the server
    @Query("SELECT * FROM track_points WHERE tripId = :tripId AND isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedTrackPointsForTrip(tripId: String): List<TrackPoint>

    // Marks a list of track points as synced after successful upload
    @Query("UPDATE track_points SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    // Deletes all track points associated with a specific trip
    @Query("DELETE FROM track_points WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: String)
}
