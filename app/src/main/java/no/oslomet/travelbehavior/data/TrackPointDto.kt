package no.oslomet.travelbehavior.data

import com.google.firebase.firestore.IgnoreExtraProperties

// HVA: En DTO (Data Transfer Object) for TrackPoint.
// HVORFOR: Vi bruker en egen modell for Firebase for å ha full kontroll over
// hva som sendes til databasen. Dette gjør det enklere å håndtere
// fremtidige endringer uten å måtte endre den lokale databasemodellen (TrackPoint).
@IgnoreExtraProperties
data class TrackPointDto(
    val timestamp: Long = 0,
    val timeString: String = "", // FIKS: Nytt felt for lesbar tid
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val acc: Float? = null
) {
    // Tom konstruktør er påkrevd av Firebase
    constructor() : this(0, "", 0.0, 0.0, null)
}
