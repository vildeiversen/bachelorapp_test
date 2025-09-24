package no.oslomet.travelbehavior

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import no.oslomet.travelbehavior.BuildConfig


class MainActivity : ComponentActivity() {

    private lateinit var locClient: LocationClient
    private lateinit var dao: TrackPointDao

    private var setTracking: ((Boolean) -> Unit)? = null

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) startAfterPermission()
    }

    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun startAfterPermission() {
        locClient.start { lat, lon, acc ->
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
        setTracking?.invoke(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locClient = LocationClient(this)
        dao = AppDatabase.getInstance(this).trackPointDao()

        setContent {
            BachelorAppH2025Theme {
                var tracking by remember { mutableStateOf(false) }
                setTracking = { tracking = it }

                fun start() {
                    if (hasPermission()) startAfterPermission()
                    else requestPerms.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }

                fun stop() { locClient.stop(); tracking = false }

                fun previewPayload() {
                    lifecycleScope.launch {
                        val pending: List<TrackPoint> = dao.getPending(limit = 500)
                        val payload = buildUploadPayload(
                            deviceId = pseudoDeviceId(this@MainActivity),
                            appVersion = BuildConfig.VERSION_NAME,
                            points = pending
                        )
                        val json = com.google.gson.GsonBuilder()
                            .setPrettyPrinting().create().toJson(payload)
                        android.util.Log.d("API", json)  // For å se i Logcat (taggen er "API")
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
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
