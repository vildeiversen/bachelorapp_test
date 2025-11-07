package no.oslomet.travelbehavior.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import no.oslomet.travelbehavior.network.TrackPointDto

class FirebaseRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private suspend fun ensureSignedInAnonymously() {
        if (auth.currentUser == null) {
            Log.d("FirebaseRepo", "User not signed in. Attempting anonymous sign-in...")
            auth.signInAnonymously().await()
            Log.d("FirebaseRepo", "Anonymous sign-in successful.")
        }
    }

    private fun userRoot(): com.google.firebase.firestore.DocumentReference {
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("Error: Cannot perform Firebase operation because user is not signed in.")
        return db.collection("users").document(userId)
    }

    suspend fun startTrip(startTimestamp: Long): String {
        ensureSignedInAnonymously()
        val tripRef = userRoot().collection("trips").document()
        // FIKS: Lagrer den relative Long-verdien direkte, uten konvertering.
        val trip = TripDTO(startedAt = startTimestamp)
        tripRef.set(trip).await()
        Log.d("FirebaseRepo", "Started new trip in Firebase with ID: ${tripRef.id}")
        return tripRef.id
    }

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
            // FIKS: Lagrer den relative Long-verdien direkte.
            "endedAt" to endTimestamp,
            "tripRating" to tripRating,
            "delayRating" to delayRating,
            "delayMinutes" to delayMinutes,
            "delayComment" to delayComment
        )
        userRoot().collection("trips").document(tripId)
            .update(tripUpdates).await()
        Log.d("FirebaseRepo", "Ended and rated trip in Firebase with ID: $tripId")
    }

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

// FIKS: Endret tidspunkter fra Timestamp til Long for å lagre rå millisekund-verdi.
data class TripDTO(
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val note: String? = null,
    val tripRating: Int? = null,
    val delayRating: Int? = null,
    val delayMinutes: Int? = null,
    val delayComment: String? = null
)
