package no.oslomet.travelbehavior.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Lager en Entity (tabell) for å lagre datapunkter (altså definerer datamodellen vår)
@Entity(tableName = "track_points", indices = [Index("timestamp"), Index("tripId")])
data class TrackPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: String, // NY: For å gruppere punkter per tur
    val timestamp: Long,
    val lat: Double,
    val lon: Double,
    val acc: Float?,
    val uploaded: Boolean = false
)
