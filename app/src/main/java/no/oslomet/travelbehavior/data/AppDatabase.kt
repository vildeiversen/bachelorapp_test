package no.oslomet.travelbehavior.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// @Database tells Room which tables (entities) are contained in the database
// and the version number of the database.
@Database(entities = [TrackPoint::class, Trip::class], version = 5) // FIKS: Økt versjon pga. fjerning av TypeConverters
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackPointDao(): TrackPointDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "travel_behavior.db"
                )
                .fallbackToDestructiveMigration() // FIKS: Hindrer krasj ved versjonsendring
                .build().also { INSTANCE = it }
            }
    }
}
