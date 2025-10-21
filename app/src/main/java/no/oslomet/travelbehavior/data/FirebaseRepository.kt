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

    suspend fun startTrip(): String {
        ensureSignedInAnonymously()
        val tripRef = userRoot().collection("trips").document()
        val trip = TripDTO(startedAt = Timestamp.now())
        tripRef.set(trip).await()
        Log.d("FirebaseRepo", "Started new trip in Firebase with ID: ${tripRef.id}")
        return tripRef.id
    }

    suspend fun endTrip(
        tripId: String,
        tripRating: Int,
        delayRating: Int,
        delayMinutes: Int?,
        delayComment: String?
    ) {
        ensureSignedInAnonymously()
        val tripUpdates = mapOf(
            "endedAt" to Timestamp.now(),
            "tripRating" to tripRating,
            "delayRating" to delayRating,
            "delayMinutes" to delayMinutes,
            "delayComment" to delayComment
        )
        userRoot().collection("trips").document(tripId)
            .update(tripUpdates).await()
        Log.d("FirebaseRepo", "Ended and rated trip in Firebase with ID: $tripId")
    }

    // HVA: En ny, optimalisert funksjon for å laste opp punkter i "batcher".
    // HVORFOR: Å sende mange punkter i ett kall er mye raskere og mer effektivt
    // enn å sende ett og ett. Dette reduserer nettverkstrafikk og batteribruk.
    suspend fun addTrackPointsBatch(tripId: String, points: List<TrackPointDto>) {
        ensureSignedInAnonymously()
        val tripPointsCollection = userRoot().collection("trips").document(tripId).collection("track_points")

        // Deler listen opp i biter på 500, siden det er maks for en Firestore-batch.
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


data class TripDTO(
    val startedAt: Timestamp = Timestamp.now(),
    val endedAt: Timestamp? = null,
    val note: String? = null,
    val tripRating: Int? = null,
    val delayRating: Int? = null,
    val delayMinutes: Int? = null,
    val delayComment: String? = null
)
