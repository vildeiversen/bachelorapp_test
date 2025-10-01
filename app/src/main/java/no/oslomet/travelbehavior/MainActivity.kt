package no.oslomet.travelbehavior

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import no.oslomet.travelbehavior.location.LocationClient
import no.oslomet.travelbehavior.ui.theme.BachelorAppH2025Theme
import no.oslomet.travelbehavior.data.AppDatabase
import no.oslomet.travelbehavior.data.TrackPoint
import no.oslomet.travelbehavior.data.TrackPointDao
import no.oslomet.travelbehavior.network.buildUploadPayload
import no.oslomet.travelbehavior.util.pseudoDeviceId
import com.google.firebase.auth.FirebaseAuth

import no.oslomet.travelbehavior.data.FirebaseRepository
import no.oslomet.travelbehavior.data.TrackPointDto


class MainActivity : ComponentActivity() {


    // 1) Services/avhengigheter vi trenger på aktivitetsnivå
    private lateinit var locClient: LocationClient
    private lateinit var dao: TrackPointDao


    // 2) “Bro” fra ikke-Compose-kode til Compose-state (slik at vi kan skru på/av “tracking” fra callback)
    private var setTracking: ((Boolean) -> Unit)? = null

    // 3) Runtime-permissions for lokasjon. Når bruker sier “Allow”, starter vi sporing.
    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) startAfterPermission()
    }

    // 4) Sjekk om appen allerede har lokasjonstillatelser
    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    // 5) Start sporing (kalles når vi har tillatelser). Hver oppdatering lagres i Room.
    private fun startAfterPermission() {
        // start strøm av lokasjonspunkter
        locClient.start { lat, lon, acc ->
            // lagre til DB i bakgrunn (coroutine på lifecycleScope)
            lifecycleScope.launch {
                dao.insert(
                    TrackPoint(
                        timestamp = System.currentTimeMillis(),
                        lat = lat, lon = lon, acc = acc
                    )
                )
                val count = dao.count()
                android.util.Log.d("GPS", "Lagret punkt #$count: $lat, $lon")
            }
        }
        // fortell UI at vi nå “tracker”
        setTracking?.invoke(true)
    }

    private fun ensureFirebaseLogin() {
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            // Kjør røyk-testen hvis allerede logget inn
            lifecycleScope.launch {
                android.util.Log.d("FB", "UID = ${FirebaseAuth.getInstance().currentUser?.uid}")
                runSmokeTest()
            }
        } else {
            auth.signInAnonymously()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        android.util.Log.d("FB", "Firebase anonymous login ready")
                        // 🚀 Kjør røyk-testen etter innlogging
                        lifecycleScope.launch {
                            runSmokeTest()
                        }
                    } else {
                        android.util.Log.e("FB", "Anonymous sign-in failed", it.exception)
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("FB", "Anonymous sign-in failed", e)
                }
        }
    }

    /**
     * Selve røyk-testen: start en tur og legg inn ett punkt i Firestore.
     */
    private suspend fun runSmokeTest() {
        try {
            val repo = FirebaseRepository()
            val tripId = repo.startTrip()
            android.util.Log.d("FirebaseSmokeTest", "Started trip: $tripId")

            val testPoint = TrackPointDto(
                timestamp = com.google.firebase.Timestamp.now(),
                lat = 59.9139,
                lon = 10.7522,
                acc = 5.0f
            )
            repo.addTrackPoint(tripId, testPoint)
            android.util.Log.d("FirebaseSmokeTest", "Added one TrackPoint to trip $tripId")

            // repo.endTrip(tripId) // kan brukes hvis du vil avslutte turen
            android.util.Log.d("FirebaseSmokeTest", "Smoke test OK ✅ sjekk Firestore Console")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSmokeTest", "Smoke test failed", e)
        }
    }

    /* Anonym Autentisering: Koden bruker Firebase Anonymous Authentication.
        Dette er en smart måte å gi hver app-installasjon en unik, midlertidig bruker-ID i
        Firebase-systemet uten at brukeren trenger å oppgi e-post, passord eller logge inn med
        Google.

        Hvorfor er dette nyttig? For å kunne lagre data i Firebase (som i neste steg), må
        Firebase vite hvem som lagrer dataen. Anonym autentisering løser dette ved å opprette en
        "usynlig" brukerkonto for hver enhet. Denne brukeren får en unik ID (auth.currentUser.uid).

        onReady-funksjonen: Funksjonen er designet for å være asynkron. onReady() er en "callback"
        som kun kjøres etter at Firebase-innloggingen er garantert i orden.
        I onCreate kaller vi den slik:

        ensureFirebaseLogin {
        android.util.Log.d("FB", "Firebase anonymous login ready")
    }
    Dette sikrer at appen er klar til å kommunisere med Firebase før den eventuelt prøver
    å gjøre noe mer. Oppsummert: Gi appen en identitet i Firebase-økosystemet
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ensureFirebaseLogin()

        // Init av tjenester som trenger en Context
        locClient = LocationClient(this)
        dao = AppDatabase.getInstance(this).trackPointDao()

        setContent {
            BachelorAppH2025Theme {
                // Compose-state i UI (om vi sporer eller ikke)
                var tracking by remember { mutableStateOf(false) }
                setTracking = { tracking = it } // koble “broen”

                // Klikk på “Start” → enten spør om tillatelse, eller starter sporing direkte
                fun start() {
                    if (hasPermission()) {
                        startAfterPermission()
                    } else {
                        requestPerms.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }

                // Klikk på “Stop” → stopp strømmen
                fun stop() {
                    locClient.stop()
                    tracking = false
                }

                // Forhåndsvis payload (det vi ville sendt til Azure)
                // - Henter pending rader (uploaded=false)
                // - Bygger JSON
                // - Viser Toast + logger til Logcat (tag “API”)
                fun previewPayload() {
                    lifecycleScope.launch {
                        val pending: List<TrackPoint> = dao.getPending(limit = 500)
                        val payload = buildUploadPayload(
                            deviceId = pseudoDeviceId(this@MainActivity),   // anonym, stabil ID
                            appVersion = BuildConfig.VERSION_NAME,          // “1.0” fra build.gradle
                            points = pending
                        )

                        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                        val json = gson.toJson(payload)

                        // Alltid gi feedback – hvor mange punkter fant vi?
                        Toast.makeText(
                            this@MainActivity,
                            "Payload preview: ${pending.size} punkt(er)",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Logg uansett, så du kan se body i Logcat (filter: tag=API)
                        android.util.Log.d("API", "About to send ${pending.size} points")
                        android.util.Log.d("API", json)

                        // Tips: hvis 0 punkter – trykk Start tracking og spill en rute i emulatoren først :)
                    }
                }

                // Enkelt UI: start/stop + preview-knapp
                Box(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!tracking) {
                            Button(onClick = { start() }) { Text("Start tracking (foreground)") }
                        } else {
                            Button(onClick = { stop() }) { Text("Stop tracking") }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { previewPayload() }) { Text("Preview payload (JSON)") }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locClient.stop()
    }
}
