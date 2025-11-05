package no.oslomet.travelbehavior.data

import android.content.Context

object TripManager {
    private const val PREF = "trip_prefs"
    private const val KEY_TRIP_ID = "current_trip_id"
    private const val KEY_TRIP_START_TIME = "current_trip_start_time"
    private const val KEY_TRIP_END_TIME = "current_trip_end_time" // NY

    fun saveTripId(context: Context, tripId: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_TRIP_ID, tripId).apply()
    }

    fun getTripId(context: Context): String? =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_TRIP_ID, null)

    fun clearTripId(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(KEY_TRIP_ID).apply()
    }

    fun saveTripStartTime(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putLong(KEY_TRIP_START_TIME, System.currentTimeMillis()).apply()
    }

    fun getTripStartTime(context: Context): Long {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getLong(KEY_TRIP_START_TIME, 0L)
    }

    fun clearTripStartTime(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(KEY_TRIP_START_TIME).apply()
    }

    // NYE FUNKSJONER FOR SLUTT-TID
    fun saveTripEndTime(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putLong(KEY_TRIP_END_TIME, System.currentTimeMillis()).apply()
    }

    fun getTripEndTime(context: Context): Long {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getLong(KEY_TRIP_END_TIME, 0L)
    }

    fun clearTripEndTime(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(KEY_TRIP_END_TIME).apply()
    }
}
