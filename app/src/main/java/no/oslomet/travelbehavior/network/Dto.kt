package no.oslomet.travelbehavior.network

// Data Transfer Objects - klasser som definerer hvordan data ser ut når de sendes til Firebase.

// FIKS: Fjernet 'uploaded'-feltet. Dette feltet er kun for lokal bruk i Room.
data class TrackPointDto(
    val timestamp: Long,
    val lat: Double,
    val lon: Double,
    val acc: Float?
)
