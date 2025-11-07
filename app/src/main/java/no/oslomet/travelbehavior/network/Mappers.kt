package no.oslomet.travelbehavior.network

import no.oslomet.travelbehavior.data.TrackPoint

// FIKS: Oppdatert for å korrekt mappe fra TrackPoint til TrackPointDto.
// Den ignorerer nå de lokale feltene 'id' og 'tripId' som ikke skal til Firebase.
fun TrackPoint.toDto() = TrackPointDto(
    // FIKS: 'timestamp' er nå en Long (ms siden midnatt), så den brukes direkte.
    timestamp = this.timestamp,
    lat = this.lat,
    lon = this.lon,
    acc = this.acc
)
