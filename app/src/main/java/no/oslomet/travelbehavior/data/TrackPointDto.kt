package no.oslomet.travelbehavior.data

import com.google.firebase.Timestamp
import java.util.Date

// dette er vår Firestore-modell (hvordan dokumentene lagres i skyen).
/**
 * Modellen slik den lagres i Firestore.
 * Bruker Firebase Timestamp i stedet for Long.
 */
// Firestore-modell for et track point
data class TrackPointDto(
    val timestamp: Timestamp = Timestamp.now(),
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val acc: Float? = null
)

// Mapper fra vår lokale Room-entity til Firestore-modellen
fun TrackPoint.toDto(): TrackPointDto {
    // FIKS: Bruker .time for å hente ut millisekund-tallet fra Date-objektet før vi regner på det.
    val timeInMillis = this.timestamp.time
    return TrackPointDto(
        timestamp = Timestamp(timeInMillis / 1000, ((timeInMillis % 1000) * 1_000_000).toInt()),
        lat = lat,
        lon = lon,
        acc = acc
    )
}
