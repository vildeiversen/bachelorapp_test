package no.oslomet.travelbehavior.data

import android.content.Context

object TripManager {
    private const val PREF = "trip_prefs"
    private const val KEY_TRIP_ID = "current_trip_id"

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
}
