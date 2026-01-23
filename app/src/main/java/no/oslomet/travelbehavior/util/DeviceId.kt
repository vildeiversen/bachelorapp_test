package no.oslomet.travelbehavior.util

import android.content.Context
import java.util.UUID

/**
 * Utility functions for device identification.
 * reusable methods and provide static-like functionality.
 */

private const val PREFS = "app_prefs"
private const val KEY_DEVICE_ID = "device_id"

/**
 * Generates or retrieves a persistent, pseudo-anonymous device ID.
 * This ID survives app restarts but does not personally identify the user.
 */
fun pseudoDeviceId(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    
    // Return existing ID if already stored
    prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
    
    // Otherwise, generate a new UUID, store it, and return it
    val fresh = "dev-" + UUID.randomUUID().toString()
    prefs.edit().putString(KEY_DEVICE_ID, fresh).apply()
    return fresh
}
