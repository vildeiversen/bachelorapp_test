package no.oslomet.travelbehavior.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main database configuration for the application.
 * Defines the entities (tables) and the version of the database.
 */
@Database(entities = [TrackPoint::class, Trip::class], version = 6)
abstract class AppDatabase : RoomDatabase() {
    
    // DAOs (Data Access Objects) to interact with the database tables
    abstract fun trackPointDao(): TrackPointDao
    abstract fun tripDao(): TripDao

    companion object {
        
        /**
         * Migration from version 5 to 6.
         * Renames the 'uploaded' column to 'isSynced'.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE track_points RENAME COLUMN uploaded TO isSynced")
            }
        }

        // Instance to ensure visibility across threads
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * Returns the database singleton.
         * If it doesn't exist, it creates it.
         */
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "travel_behavior.db"
                )
                .addMigrations(MIGRATION_5_6)
                .build().also { INSTANCE = it }
            }
    }
}
