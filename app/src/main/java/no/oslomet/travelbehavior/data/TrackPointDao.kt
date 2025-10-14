package no.oslomet.travelbehavior.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// Denne definerer "Data Access Object", altså hvordan vi leser/lagrer/oppdaterer data (for Room)
@Dao
interface TrackPointDao {
    @Insert
    suspend fun insert(trackPoint: TrackPoint)

    // Oppdatert for å hente punkter for en SPESIFIKK tur
    @Query("SELECT * FROM track_points WHERE tripId = :tripId AND uploaded = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingForTrip(tripId: String, limit: Int = 500): List<TrackPoint>

    @Query("UPDATE track_points SET uploaded = 1 WHERE id IN (:ids)")
    suspend fun markUploaded(ids: List<Long>)

    // NY: Sletter alle punkter knyttet til en spesifikk tur
    @Query("DELETE FROM track_points WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: String)
}
