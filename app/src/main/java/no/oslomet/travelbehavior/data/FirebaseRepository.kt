package no.oslomet.travelbehavior.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles user-scoped trip and track-point synchronization with Firebase.
 */
class FirebaseRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Ensures the user is signed in anonymously before performing any Firebase operations.
     */
    private suspend fun ensureSignedInAnonymously() {
        if (auth.currentUser == null) {
            Log.d("FirebaseRepo", "User not signed in. Attempting anonymous sign-in...")
            auth.signInAnonymously().await()
            Log.d("FirebaseRepo", "Anonymous sign-in successful.")
        }
    }

    /**
     * Returns a reference to the current authenticated user's document in the "users" collection.
     */
    private fun userRoot(): com.google.firebase.firestore.DocumentReference {
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("Error: Cannot perform Firebase operation because user is not signed in.")
        return db.collection("users").document(userId)
    }

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    /**
     * Creates a new trip document in Firestore when a trip starts and returns its generated ID.
     */
    suspend fun startTrip(startTimestamp: Long): String {
        ensureSignedInAnonymously()
        val tripRef = userRoot().collection("trips").document()

        val trip = TripDTO(
            startedAt = startTimestamp,
            startedAtString = timeFormatter.format(Date(startTimestamp))
        )

        tripRef.set(trip).await()
        Log.d("FirebaseRepo", "Started new trip in Firebase with ID: ${tripRef.id}")
        return tripRef.id
    }

    /**
     * Finalizes an existing trip document with end time, ratings, and comments.
     */
    suspend fun endTrip(
        tripId: String,
        endTimestamp: Long,
        tripRating: Int,
        delayRating: Int,
        delayMinutes: Int?,
        delayComment: String?
    ) {
        ensureSignedInAnonymously()
        val tripUpdates = mapOf(
            "endedAt" to endTimestamp,
            "endedAtString" to timeFormatter.format(Date(endTimestamp)),
            "tripRating" to tripRating,
            "delayRating" to delayRating,
            "delayMinutes" to delayMinutes,
            "delayComment" to delayComment
        )
        userRoot().collection("trips").document(tripId)
            .update(tripUpdates).await()
        Log.d("FirebaseRepo", "Ended and rated trip in Firebase with ID: $tripId")
    }

    /**
     * Uploads track-points for a trip using Firestore batches (max 500 per batch).
     */
    suspend fun addTrackPointsBatch(tripId: String, points: List<TrackPointDto>) {
        ensureSignedInAnonymously()
        val tripPointsCollection = userRoot().collection("trips").document(tripId).collection("track_points")

        points.chunked(500).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { point ->
                val newPointRef = tripPointsCollection.document()
                batch.set(newPointRef, point)
            }
            batch.commit().await()
            Log.d("FirebaseRepo", "Committed a batch of ${chunk.size} points.")
        }
    }
}

/**
 * Data Transfer Object for trip data sent to Firebase.
 */
data class TripDTO(
    val startedAt: Long? = null,
    val startedAtString: String? = null,
    val endedAt: Long? = null,
    val endedAtString: String? = null,
    val tripRating: Int? = null,
    val delayRating: Int? = null,
    val delayMinutes: Int? = null,
    val delayComment: String? = null
)
