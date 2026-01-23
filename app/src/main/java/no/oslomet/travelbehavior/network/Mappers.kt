package no.oslomet.travelbehavior.network

import no.oslomet.travelbehavior.data.TrackPoint
import no.oslomet.travelbehavior.data.TrackPointDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Extension function to convert a local TrackPoint entity into a TrackPointDto 
 * suitable for network transmission to Firebase.
 */
fun TrackPoint.toDto(): TrackPointDto {
    
    // Formatter for creating a human-readable time string
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    return TrackPointDto(
        timestamp = this.timestamp,
        // Convert the timestamp to a formatted string for better readability in the database
        timeString = timeFormatter.format(Date(this.timestamp)),
        lat = this.lat,
        lon = this.lon,
        acc = this.acc
    )
}
