package no.oslomet.travelbehavior.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import no.oslomet.travelbehavior.network.TrackPointDto // FIKS: Importerer den ENE korrekte DTO-en

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

    // FIKS: Reversert tilbake til originalen
    suspend fun startTrip(): String {
        ensureSignedInAnonymously()
        val tripRef = userRoot().collection("trips").document()
        val trip = TripDTO(startedAt = Timestamp.now())
        tripRef.set(trip).await()
        Log.d("FirebaseRepo", "Started new trip in Firebase with ID: ${tripRef.id}")
        return tripRef.id
    }

    suspend fun endTrip(tripId: String) {
        ensureSignedInAnonymously()
        userRoot().collection("trips").document(tripId) // FIKS: Fjernet utilsiktet linjeskift
            .update("endedAt", Timestamp.now()).await()
        Log.d("FirebaseRepo", "Ended trip in Firebase with ID: $tripId")
    }

    suspend fun addTrackPoint(tripId: String, point: TrackPointDto) {
        ensureSignedInAnonymously()
        userRoot().collection("trips").document(tripId)
            .collection("track_points").add(point).await()
    }
}

// FIKS: Reversert tilbake til originalen
data class TripDTO(
    val startedAt: Timestamp = Timestamp.now(),
    val endedAt: Timestamp? = null,
    val note: String? = null
)
