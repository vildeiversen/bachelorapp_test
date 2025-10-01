package no.oslomet.travelbehavior.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Her er logikken for å snakke med Firestore

class FirebaseRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private suspend fun ensureSignedInAnonymously() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    private fun userRoot() = db.collection("users").document(auth.currentUser!!.uid)

    suspend fun startTrip(): String {
        ensureSignedInAnonymously()
        val tripRef = userRoot().collection("trips").document()
        val trip = TripDTO(startedAt = Timestamp.now())
        tripRef.set(trip).await()
        return tripRef.id
    }

    suspend fun endTrip(tripId: String) {
        ensureSignedInAnonymously()
        userRoot().collection("trips").document(tripId)
            .update("endedAt", Timestamp.now()).await()
    }

    suspend fun addTrackPoint(tripId: String, point: TrackPointDto) {
        ensureSignedInAnonymously()
        userRoot().collection("trips").document(tripId)
            .collection("track_points").add(point).await()
    }
}

data class TripDTO(
    val startedAt: Timestamp = Timestamp.now(),
    val endedAt: Timestamp? = null,
    val note: String? = null
)
