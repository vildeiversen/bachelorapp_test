package no.oslomet.travelbehavior.network

import no.oslomet.travelbehavior.data.TrackPoint
import no.oslomet.travelbehavior.data.TrackPointDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// FIKS: Oppdatert for å korrekt mappe fra TrackPoint til TrackPointDto.
// Den ignorerer nå de lokale feltene 'id' og 'tripId' som ikke skal til Firebase.
fun TrackPoint.toDto(): TrackPointDto {
    // FIKS: Oppretter en formatter for å konvertere Long til en lesbar tid-streng.
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    return TrackPointDto(
        // FIKS: 'timestamp' er nå en Long (ms siden midnatt), så den brukes direkte.
        timestamp = this.timestamp,
        // FIKS: Legger til den formaterte, lesbare tid-strengen.
        timeString = timeFormatter.format(Date(this.timestamp)),
        lat = this.lat,
        lon = this.lon,
        acc = this.acc
    )
}
