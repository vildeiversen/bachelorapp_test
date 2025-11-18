package no.oslomet.travelbehavior.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TrackPoint::class, Trip::class], version = 6) // FIKS: Økt versjon for å støtte gjenopptakbar synkronisering
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackPointDao(): TrackPointDao
    abstract fun tripDao(): TripDao

    companion object {
        // FIKS: Definerer en migrering for å omdøpe 'uploaded' til 'isSynced'
        // HVORFOR: Dette sikrer at eksisterende data ikke går tapt når vi oppdaterer
        // databasestrukturen. Uten dette ville appen krasjet eller slettet dataen.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE track_points RENAME COLUMN uploaded TO isSynced")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "travel_behavior.db"
                )
                // FIKS: Erstatter den destruktive snarveien med en spesifikk oppgradering
                .addMigrations(MIGRATION_5_6)
                .build().also { INSTANCE = it }
            }
    }
}
