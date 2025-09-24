package no.oslomet.travelbehavior.network

import no.oslomet.travelbehavior.data.TrackPoint

/**
 *  Hvorfor mapper vi til DTO?
 *  Skiller intern lagringsmodell (Room) fra API-kontrakten.
 *  Vi lekker ikke interne felt som id / uploaded til serveren.
 *  Vi kan gjøre transformasjoner (formatere tid, endre feltnavn) uten å endre DB-skjema.
 */

// ÉN rad: Room-entity -> DTO som kan sendes som JSON
fun TrackPoint.toDto(): TrackPointDto =
    TrackPointDto(
        timestamp = this.timestamp,  // behold epoch ms; backend kan formatere
        lat = this.lat,
        lon = this.lon,
        acc = this.acc
    )

// Liste: praktisk når man henter mange rader fra Room
fun List<TrackPoint>.toDtoList(): List<TrackPointDto> = this.map { it.toDto() }

// Bygger hele payloaden som API-et forventer (kan bytte ut felt når vi får spesifikasjon fra Hedda)
// og returnerer UploadPayload
fun buildUploadPayload(
    deviceId: String,
    appVersion: String,
    points: List<TrackPoint>
): UploadPayload {
    return UploadPayload(
        deviceId = deviceId,
        appVersion = appVersion,
        points = points.toDtoList()
    )
}
