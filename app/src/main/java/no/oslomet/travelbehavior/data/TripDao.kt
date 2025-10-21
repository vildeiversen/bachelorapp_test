package no.oslomet.travelbehavior.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TripDao {

    // HVORFOR: Denne funksjonen lagrer et nytt Trip-objekt i databasen.
    // OnConflictStrategy.REPLACE betyr at hvis vi prøver å sette inn en tur
    // med en ID som allerede finnes, vil den gamle turen bli overskrevet.
    // Dette er nyttig for å oppdatere en eksisterende tur.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: Trip)

    // HVORFOR: Henter alle turer som ennå ikke er synkronisert med Firebase.
    // Dette er kjernen i vår offline-støtte. En bakgrunnsjobb vil kalle
    // denne funksjonen for å finne ut hvilke turer som må lastes opp.
    @Query("SELECT * FROM trips WHERE isSynced = 0")
    suspend fun getUnsyncedTrips(): List<Trip>

    // HVORFOR: Oppdaterer en liste med turer. Etter at turene er lastet opp
    // til Firebase, vil vi kalle denne for å sette isSynced = true for alle
    // de synkroniserte turene, slik at de ikke blir lastet opp på nytt.
    @Query("UPDATE trips SET isSynced = 1, firebaseTripId = :firebaseId WHERE id = :localId")
    suspend fun markAsSynced(localId: String, firebaseId: String)

    // HVA: Lagt til en funksjon for å slette en tur basert på ID.
    // HVORFOR: TrackingViewModel trenger en måte å slette en avbrutt tur på.
    // Denne spørringen fjerner en rad fra 'trips'-tabellen basert på primærnøkkelen.
    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: String)
}
