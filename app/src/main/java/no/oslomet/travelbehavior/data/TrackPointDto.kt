package no.oslomet.travelbehavior.data

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Data Transfer Object (DTO) for uploading location points to Firebase Firestore.
 * The @IgnoreExtraProperties annotation ensures compatibility with potential future fields in Firestore.
 */
@IgnoreExtraProperties
data class TrackPointDto(
    val timestamp: Long = 0,    // Time of capture in milliseconds
    val timeString: String = "", // Formatted time string (e.g., HH:mm:ss)
    val lat: Double = 0.0,      // Latitude coordinate
    val lon: Double = 0.0,      // Longitude coordinate
    val acc: Float? = null      // GPS accuracy in meters (optional)
) {
    /**
     * Required empty constructor for Firebase Firestore deserialization.
     */
    constructor() : this(0, "", 0.0, 0.0, null)
}
