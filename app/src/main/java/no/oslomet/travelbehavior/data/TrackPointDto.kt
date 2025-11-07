package no.oslomet.travelbehavior.data

/**
 * FIKS: Endret fra Timestamp til Long for å lagre rå millisekund-verdi i Firestore.
 * Dette gjør modellen konsistent med TripDTO.
 */
data class TrackPointDto(
    val timestamp: Long = 0,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val acc: Float? = null
)

/**
 * Mapper fra vår lokale Room-entity til Firestore-modellen.
 */
fun TrackPoint.toDto(): TrackPointDto {
    // FIKS: Bruker nå den relative Long-verdien direkte, uten konvertering.
    return TrackPointDto(
        timestamp = this.timestamp,
        lat = lat,
        lon = lon,
        acc = acc
    )
}
