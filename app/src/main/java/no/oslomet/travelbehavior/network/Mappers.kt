package no.oslomet.travelbehavior.network

import no.oslomet.travelbehavior.data.TrackPoint

// FIKS: Oppdatert for å korrekt mappe fra TrackPoint til TrackPointDto.
// Den ignorerer nå de lokale feltene 'id' og 'tripId' som ikke skal til Firebase.
fun TrackPoint.toDto() = TrackPointDto(
    // FIKS: Konverterer Date-objektet til Long med .time
    timestamp = this.timestamp.time,
    lat = this.lat,
    lon = this.lon,
    acc = this.acc
)
