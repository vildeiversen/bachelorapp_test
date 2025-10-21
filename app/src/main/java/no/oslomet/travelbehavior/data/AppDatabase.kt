package no.oslomet.travelbehavior.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// HVA: Lagt til Trip::class i listen over entiteter.
// HVORFOR: Room må vite om alle tabellene databasen skal inneholde.
@Database(entities = [TrackPoint::class, Trip::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackPointDao(): TrackPointDao

    // HVA: Lagt til den abstrakte funksjonen for TripDao.
    // HVORFOR: Dette gjør DAO-en tilgjengelig for resten av appen via en database-instans,
    // og løser feilen i TrackingViewModel.
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "travel_behavior.db"
                ).build().also { INSTANCE = it }
            }
    }
}
