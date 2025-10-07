package no.oslomet.travelbehavior.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Oppretter databasen og gir Room oversikt over hvilke entiteter (tabeller) og DAO-er som finnes
// Binder DAO og entities sammen
@Database(entities = [TrackPoint::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackPointDao(): TrackPointDao

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
