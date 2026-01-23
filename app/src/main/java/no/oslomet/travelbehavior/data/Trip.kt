package no.oslomet.travelbehavior.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a recorded trip.
 */
@Entity(tableName = "trips")
data class Trip(
    
    // Unique identifier for the trip
    @PrimaryKey
    val id: String,

    // Start and end timestamps of the journey
    val startTimestamp: Long,
    val endTimestamp: Long,

    // User feedback and delay information
    val overallRating: Int?,
    val delayRating: Int?,
    val delayMinutes: Int?,
    val delayComment: String?,

    // Synchronization status and remote reference
    var isSynced: Boolean = false,
    var firebaseTripId: String? = null
)
