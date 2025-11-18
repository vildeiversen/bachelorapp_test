package no.oslomet.travelbehavior.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrackPointDao {
    @Insert
    suspend fun insert(trackPoint: TrackPoint)

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getTrackPointsForTrip(tripId: String): List<TrackPoint>

    // FIKS: Ny funksjon for å kun hente usynkroniserte punkter.
    // HVORFOR: Kritisk for gjenopptakbar synkronisering. Lar oss fortsette der vi slapp.
    @Query("SELECT * FROM track_points WHERE tripId = :tripId AND isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedTrackPointsForTrip(tripId: String): List<TrackPoint>

    // FIKS: Omdøpt og oppdatert for å markere punkter som synkronisert.
    // HVORFOR: Etter at en gruppe punkter er lastet opp, kaller vi denne for å oppdatere
    // deres status lokalt, slik at de ikke blir lastet opp på nytt.
    @Query("UPDATE track_points SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM track_points WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: String)
}
