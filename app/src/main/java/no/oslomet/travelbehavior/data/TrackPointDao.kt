package no.oslomet.travelbehavior.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrackPointDao {
    @Insert
    suspend fun insert(trackPoint: TrackPoint)

    // HVA: En ny funksjon for å hente ALLE punkter for en gitt tur.
    // HVORFOR: TripSyncWorker trenger dette for å laste opp alle punktene for en tur som skal synkroniseres.
    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getTrackPointsForTrip(tripId: String): List<TrackPoint>

    @Query("UPDATE track_points SET uploaded = 1 WHERE id IN (:ids)")
    suspend fun markUploaded(ids: List<Long>)

    @Query("DELETE FROM track_points WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: String)
}
