package no.oslomet.travelbehavior.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single GPS location point recorded during a trip.
 * This class defines the "track_points" table in the Room database.
 * Indexes are added on timestamp and tripId for faster querying.
 */
@Entity(tableName = "track_points", indices = [Index("timestamp"), Index("tripId")])
data class TrackPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Unique identifier for each point
    val tripId: String, // ID of the trip this point belongs to
    
    val timestamp: Long, // Time when the point was captured
    val lat: Double,     // Latitude coordinate
    val lon: Double,     // Longitude coordinate
    val acc: Float?,    // Accuracy of the location in meters
    
    val isSynced: Boolean = false // Indicates if the point has been uploaded to the server
)
