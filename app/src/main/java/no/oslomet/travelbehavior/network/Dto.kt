package no.oslomet.travelbehavior.network

// network pakken er for nettverks og API relaterte ting.

// DTO (Data Transfer Object) for et enkelt punkt
// DTO-en er en "ren" versjon av dataen uten metadata, men kun de feltne API-et trenger
data class TrackPointDto(
    val timestamp: Long,
    val lat: Double,
    val lon: Double,
    val acc: Float?
)

// Payload som sendes til API-et
// API-formatet: metadata + liste med DTO-er. Dette er det som blir til JSON-body i POST-requesten (selve pakken som vi sender til serveren)
// wrapper rundt flere punkter + metadata (hvilken enhet, hvilken appversjon).
data class UploadPayload(
    val deviceId: String,
    val appVersion: String,
    val points: List<TrackPointDto>
)
