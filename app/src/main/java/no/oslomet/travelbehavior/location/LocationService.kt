package no.oslomet.travelbehavior.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.oslomet.travelbehavior.R
import no.oslomet.travelbehavior.data.AppDatabase
import no.oslomet.travelbehavior.data.TrackPoint
import no.oslomet.travelbehavior.data.TripManager

class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    inner class LocationServiceBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    private val binder = LocationServiceBinder()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationClient(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        // Sikrer at tjenesten restartes hvis systemet stopper den
        return START_STICKY
    }

    private fun start() {
        Log.d("LocationService", "Service started")
        _isTracking.value = true
        _pathPoints.value = emptyList()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // FIKS: Kjører kun på API 26+ for å unngå krasj på eldre enheter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location",
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Travel Behavior")
            .setContentText("Tracking is active.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Du bør bytte dette ikonet
            .setOngoing(true)

        val dao = AppDatabase.getInstance(application).trackPointDao()
        val localTripId = TripManager.getTripId(this) ?: run {
            Log.e("LocationService", "Could not start tracking, no trip ID found.")
            return
        }

        locationClient.start { lat, lon, acc ->
            val newPoint = LatLng(lat, lon)
            _pathPoints.value = _pathPoints.value + newPoint

            serviceScope.launch {
                dao.insert(TrackPoint(tripId = localTripId, timestamp = System.currentTimeMillis(), lat = lat, lon = lon, acc = acc))
            }
        }

        startForeground(1, notification.build())
    }

    private fun stop() {
        Log.d("LocationService", "Service stopped")
        _isTracking.value = false
        locationClient.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
         const val ACTION_START = "ACTION_START"
         const val ACTION_STOP = "ACTION_STOP"

        private val _isTracking = MutableStateFlow(false)
        val isTracking = _isTracking.asStateFlow()

        private val _pathPoints = MutableStateFlow<List<LatLng>>(emptyList())
        val pathPoints = _pathPoints.asStateFlow()
    }
}
