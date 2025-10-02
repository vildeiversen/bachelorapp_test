package no.oslomet.travelbehavior.data

import android.content.Context
import androidx.room.Database
import androidx.room.DatabaseView
import androidx.room.Room
import androidx.room.RoomDatabase

/*
* ENDRING: Flyttet @DatabaseView-annotasjonen og la til en tom klasse TrackPointsPretty.
* HVORFOR: En @DatabaseView må være knyttet til en @Database-annotasjon.
* Ved å flytte den hit og legge den til i `views`-arrayet i @Database-annotasjonen under,
* blir den gjenkjent av Room-biblioteket. En @DatabaseView trenger også en tilhørende klasse,
* selv om den er tom, for at koden skal kunne kompileres.
*/
@DatabaseView(
    viewName = "track_points_pretty",
    value = """
        SELECT 
          id,
          strftime('%H:%M:%S', timestamp/1000, 'unixepoch','localtime') AS klokkeslett,
          lon AS longitude,
          lat AS latitude
        FROM track_points
    """
)
class TrackPointsPretty

// Oppretter databasen og gir Room oversikt over hvilke entiteter (tabeller) og DAO-er som finnes
@Database(entities = [TrackPoint::class], views = [TrackPointsPretty::class], version = 1)
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