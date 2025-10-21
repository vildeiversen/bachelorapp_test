package no.oslomet.travelbehavior.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representerer en komplett tur med tilbakemeldinger, lagret lokalt i Room-databasen.
 * HVORFOR: Vi trenger et sted å lagre tur-metadata (som feedback) lokalt før vi er klare
 * til å synkronisere den til Firebase. Dette sikrer at ingen data går tapt ved nettverksfeil.
 */
@Entity(tableName = "trips")
data class Trip(

    // HVORFOR: Dette er den unike, lokale ID-en for turen. Vi bruker den som primærnøkkel
    // for å koble sammen TrackPoints og selve turen.
    @PrimaryKey
    val id: String,

    // HVORFOR: Tidsstempel for når turen ble avsluttet. Nyttig for sortering og feilsøking.
    val endTimestamp: Long,

    // --- FELT FOR TILBAKEMELDING ---
    val overallRating: Int?,
    val delayRating: Int?,
    val delayMinutes: Int?,
    val delayComment: String?,

    // HVORFOR: Et flagg for å vite om denne turen har blitt lastet opp til Firebase.
    // Bakgrunnssynkroniseringen vil se etter turer der isSynced = false.
    var isSynced: Boolean = false,

    // HVORFOR: ID-en fra Firebase. Denne blir satt når turen er vellykket synkronisert.
    // Kan være nyttig hvis vi senere vil koble lokale og eksterne data.
    var firebaseTripId: String? = null
)
