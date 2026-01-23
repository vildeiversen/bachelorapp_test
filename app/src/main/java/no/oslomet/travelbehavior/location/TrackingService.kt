package no.oslomet.travelbehavior.location

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.oslomet.travelbehavior.data.AppDatabase
import no.oslomet.travelbehavior.data.TrackPoint
import no.oslomet.travelbehavior.data.TrackPointDao
import no.oslomet.travelbehavior.data.TripManager

/**
 * Foreground service that tracks user location in the background.
 * It manages a persistent notification and saves location data to the database.
 */
class TrackingService : LifecycleService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var trackPointDao: TrackPointDao

    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        const val NOTIFICATION_CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Tracking"
        const val NOTIFICATION_ID = 1

        // StateFlows to expose tracking status and path points to the UI
        private val _isTracking = MutableStateFlow(false)
        val isTracking = _isTracking.asStateFlow()

        private val _pathPoints = MutableStateFlow<List<LatLng>>(emptyList())
        val pathPoints = _pathPoints.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        trackPointDao = AppDatabase.getInstance(this).trackPointDao()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    /**
     * Handles incoming intents to start or stop the tracking service.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_SERVICE -> {
                    Log.i("TrackingService", "Tracking service started.")
                    startForegroundService()
                    _isTracking.value = true
                    _pathPoints.value = emptyList()
                }
                ACTION_STOP_SERVICE -> {
                    stopService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Stops location updates and shuts down the foreground service.
     */
    private fun stopService() {
        Log.i("TrackingService", "Tracking service stopped.")
        _isTracking.value = false
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        stopForeground(true)
        stopSelf()
    }

    /**
     * Configures and starts location tracking with specific intervals and accuracy.
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.create().apply {
            interval = 8000L
            fastestInterval = 4000L
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationProviderClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /**
     * Callback receiving location updates, updating UI state, and saving points to the database.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (_isTracking.value) {
                result.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    _pathPoints.value += latLng

                    serviceScope.launch {
                        val tripId = TripManager.getTripId(applicationContext) ?: return@launch
                        
                        // Retrieves the midnight anchor for calculating relative timestamps
                        val midnight = TripManager.getTripStartDayMidnight(applicationContext)
                        if (midnight == 0L) {
                            Log.e("TrackingService", "Failed to get midnight anchor. Aborting point storage.")
                            return@launch
                        }

                        // Calculates milliseconds since midnight of the start day
                        val millisSinceStartDayMidnight = location.time - midnight

                        trackPointDao.insert(
                            TrackPoint(
                                tripId = tripId,
                                timestamp = millisSinceStartDayMidnight,
                                lat = location.latitude,
                                lon = location.longitude,
                                acc = location.accuracy
                            )
                        )
                        Log.d("TrackingService", "New TrackPoint saved locally for trip ID: $tripId")
                    }
                }
            }
        }
    }

    /**
     * Initializes the foreground service and shows a persistent notification.
     */
    private fun startForegroundService() {
        startLocationUpdates()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Travel Behavior")
            .setContentText("Tracking location...")

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    /**
     * Creates a notification channel required for Android O and above.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}
