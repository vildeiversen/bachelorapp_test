package no.oslomet.travelbehavior.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// Denne definerer "Data Access Object", altså hvordan vi leser/lagrer/oppdaterer data
@Dao
interface TrackPointDao {
    @Insert
    suspend fun insert(trackPoint: TrackPoint)

    @Query("SELECT COUNT(*) FROM track_points")
    suspend fun count(): Int

    @Query("SELECT * FROM track_points WHERE uploaded = 0 ORDER BY id LIMIT :limit")
    suspend fun getPending(limit: Int = 500): List<TrackPoint>

    @Query("UPDATE track_points SET uploaded = 1 WHERE id IN (:ids)")
    suspend fun markUploaded(ids: List<Long>)
}
