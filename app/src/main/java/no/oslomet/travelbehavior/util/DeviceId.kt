package no.oslomet.travelbehavior.util

// Util klassene er statiske metoder og konstanter, de er ikke ment til å bli instansiert, og
// rollen deres er å ha et set med gjenbrukbare metoder for applikasjonen

import android.content.Context
import java.util.UUID

private const val PREFS = "app_prefs"
private const val KEY_DEVICE_ID = "device_id"

 // Pseudonym enhets-ID som overlever app-restarts, men ikke identifiserer brukeren
fun pseudoDeviceId(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
    val fresh = "dev-" + UUID.randomUUID().toString()
    prefs.edit().putString(KEY_DEVICE_ID, fresh).apply()
    return fresh
}
