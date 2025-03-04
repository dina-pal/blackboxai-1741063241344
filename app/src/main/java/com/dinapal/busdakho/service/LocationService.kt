package com.dinapal.busdakho.service

import android.app.*
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.dinapal.busdakho.domain.repository.BusRepository
import com.dinapal.busdakho.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.seconds

class LocationService : Service() {
    private val busRepository: BusRepository by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val notificationChannelId = "bus_tracking_channel"
    private val notificationId = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(notificationId, createNotification())
        startLocationUpdates()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            notificationChannelId,
            "Bus Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Used for tracking bus location"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Bus Tracking Active")
            .setContentText("Tracking bus location in background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun setupLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateBusLocation(location)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(10.seconds.inWholeMilliseconds)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle permission not granted
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateBusLocation(location: Location) {
        // In a real app, this would update the location for a specific bus
        // For demo purposes, we'll use a hardcoded bus ID
        val busId = "demo_bus"
        
        serviceScope.launchIn {
            try {
                busRepository.updateBusLocation(
                    busId = busId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speed = location.speed,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    companion object {
        private fun CoroutineScope.launchIn(block: suspend () -> Unit) {
            kotlinx.coroutines.flow.flow { emit(block()) }
                .catch { /* Handle error */ }
                .onEach { /* Handle success */ }
                .launchIn(this)
        }
    }
}
