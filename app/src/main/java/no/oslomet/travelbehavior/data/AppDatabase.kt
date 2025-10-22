package no.oslomet.travelbehavior.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TrackPoint::class, Trip::class], version = 2) // FIKS: Økt versjon
@TypeConverters(Converters::class) // FIKS: Forteller Room om vår nye oversetter
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
