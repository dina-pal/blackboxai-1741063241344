package com.dinapal.busdakho.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dinapal.busdakho.presentation.MainActivity
import com.dinapal.busdakho.data.local.entity.BusEntity
import com.dinapal.busdakho.data.local.entity.StopEntity

object NotificationUtil {
    private const val CHANNEL_ID_BUS_ALERTS = "bus_alerts"
    private const val CHANNEL_ID_JOURNEY_UPDATES = "journey_updates"
    private const val CHANNEL_ID_GENERAL = "general_notifications"

    private const val NOTIFICATION_ID_BUS_ARRIVAL = 1
    private const val NOTIFICATION_ID_JOURNEY_UPDATE = 2
    private const val NOTIFICATION_ID_SERVICE_DISRUPTION = 3

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_BUS_ALERTS,
                    "Bus Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications about bus arrivals and delays"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_ID_JOURNEY_UPDATES,
                    "Journey Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Updates about your planned journeys"
                },
                NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "General app notifications"
                }
            )

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun showBusArrivalNotification(
        context: Context,
        bus: BusEntity,
        stop: StopEntity,
        estimatedTimeMinutes: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("busId", bus.busId)
            putExtra("stopId", stop.stopId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUS_ALERTS)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle("Bus Arriving Soon")
            .setContentText("Bus ${bus.busId} arriving at ${stop.name} in $estimatedTimeMinutes minutes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        showNotification(context, NOTIFICATION_ID_BUS_ARRIVAL, notification)
    }

    fun showJourneyUpdateNotification(
        context: Context,
        title: String,
        message: String,
        routeId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            routeId?.let { putExtra("routeId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_JOURNEY_UPDATES)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        showNotification(context, NOTIFICATION_ID_JOURNEY_UPDATE, notification)
    }

    fun showServiceDisruptionNotification(
        context: Context,
        title: String,
        message: String,
        routeIds: List<String>
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("showDisruptions", true)
            putStringArrayListExtra("affectedRoutes", ArrayList(routeIds))
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUS_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        showNotification(context, NOTIFICATION_ID_SERVICE_DISRUPTION, notification)
    }

    private fun showNotification(context: Context, notificationId: Int, notification: android.app.Notification) {
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, notification)
            }
        } catch (e: SecurityException) {
            // Handle notification permission not granted
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun getNotificationPermissionRequest(): String {
        return android.Manifest.permission.POST_NOTIFICATIONS
    }

    // Notification IDs for public access
    object NotificationIds {
        const val BUS_ARRIVAL = NOTIFICATION_ID_BUS_ARRIVAL
        const val JOURNEY_UPDATE = NOTIFICATION_ID_JOURNEY_UPDATE
        const val SERVICE_DISRUPTION = NOTIFICATION_ID_SERVICE_DISRUPTION
    }

    // Channel IDs for public access
    object ChannelIds {
        const val BUS_ALERTS = CHANNEL_ID_BUS_ALERTS
        const val JOURNEY_UPDATES = CHANNEL_ID_JOURNEY_UPDATES
        const val GENERAL = CHANNEL_ID_GENERAL
    }
}
