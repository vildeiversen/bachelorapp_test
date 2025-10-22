package no.oslomet.travelbehavior.data

import android.content.Context

object TripManager {
    private const val PREF = "trip_prefs"
    private const val KEY_TRIP_ID = "current_trip_id"
    // FIKS: Endret nøkkel til å lagre det absolutte start-tidspunktet (ankeret).
    private const val KEY_TRIP_START_TIME = "current_trip_start_time"

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

    // FIKS: Lagrer nå det faktiske, absolutte starttidspunktet (System.currentTimeMillis()).
    fun saveTripStartTime(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putLong(KEY_TRIP_START_TIME, System.currentTimeMillis()).apply()
    }

    // FIKS: Henter det absolutte starttidspunktet.
    fun getTripStartTime(context: Context): Long {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getLong(KEY_TRIP_START_TIME, 0L)
    }

    // FIKS: Sletter det absolutte starttidspunktet.
    fun clearTripStartTime(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(KEY_TRIP_START_TIME).apply()
    }
}
