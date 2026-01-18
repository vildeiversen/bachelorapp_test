package no.oslomet.travelbehavior.data

import android.content.Context
import android.util.Log
import java.util.Calendar

object TripManager {
    private const val PREF = "trip_prefs"
    private const val KEY_TRIP_ID = "current_trip_id"
    private const val KEY_TRIP_START_TIME = "current_trip_start_time"
    private const val KEY_TRIP_END_TIME = "current_trip_end_time"
    private const val KEY_TRIP_START_DAY_MIDNIGHT = "current_trip_start_day_midnight"

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

    // FIKS: Lagrer nå millisekunder siden startdagens midnatt.
    fun saveTripStartTime(context: Context) {
        val midnight = getTripStartDayMidnight(context)
        if (midnight == 0L) {
            Log.e("TripManager", "Kan ikke lagre starttid, midnatt-anker er ikke satt.")
            return
        }
        val relativeTime = System.currentTimeMillis() - midnight
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putLong(KEY_TRIP_START_TIME, relativeTime).apply()
    }

    fun getTripStartTime(context: Context): Long {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getLong(KEY_TRIP_START_TIME, 0L)
    }

    fun clearTripStartTime(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(KEY_TRIP_START_TIME).apply()
    }

    // FIKS: Lagrer nå millisekunder siden startdagens midnatt.
    fun saveTripEndTime(context: Context) {
        val midnight = getTripStartDayMidnight(context)
        if (midnight == 0L) {
            Log.e("TripManager", "Kan ikke lagre slutttid, midnatt-anker er ikke satt.")
            return
        }
        val relativeTime = System.currentTimeMillis() - midnight
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putLong(KEY_TRIP_END_TIME, relativeTime).apply()
    }

    fun getTripEndTime(context: Context): Long {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getLong(KEY_TRIP_END_TIME, 0L)
    }

    fun clearTripEndTime(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(KEY_TRIP_END_TIME).apply()
    }

    fun saveTripStartDayMidnight(context: Context) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putLong(KEY_TRIP_START_DAY_MIDNIGHT, cal.timeInMillis).apply()
    }

    fun getTripStartDayMidnight(context: Context): Long {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getLong(KEY_TRIP_START_DAY_MIDNIGHT, 0L)
    }

    fun clearTripStartDayMidnight(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(KEY_TRIP_START_DAY_MIDNIGHT).apply()
    }
}
