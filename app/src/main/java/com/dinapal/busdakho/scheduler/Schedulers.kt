package com.dinapal.busdakho.scheduler

import android.content.Context
import androidx.work.*
import com.dinapal.busdakho.util.Constants
import com.dinapal.busdakho.util.NetworkConnectivityManager
import java.util.concurrent.TimeUnit

/**
 * Scheduler for data synchronization tasks
 */
class SyncScheduler(
    context: Context,
    private val networkManager: NetworkConnectivityManager
) : BaseScheduler(context) {

    fun scheduleBusLocationSync() {
        val constraints = createConstraints(
            requiresNetwork = NetworkType.CONNECTED,
            requiresBatteryNotLow = true
        )

        val workRequest = PeriodicWorkRequestBuilder<BusLocationSyncWorker>(
            repeatInterval = Constants.BUS_LOCATION_SYNC_INTERVAL,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setDefaultBackoffCriteria()
            .build()

        schedulePeriodicWork(
            workRequest = workRequest,
            uniqueWorkName = WORK_BUS_LOCATION_SYNC
        )
    }

    fun scheduleRouteDataSync() {
        val constraints = createConstraints(
            requiresNetwork = NetworkType.CONNECTED,
            requiresBatteryNotLow = true
        )

        val workRequest = PeriodicWorkRequestBuilder<RouteSyncWorker>(
            repeatInterval = Constants.ROUTE_SYNC_INTERVAL,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setDefaultBackoffCriteria()
            .build()

        schedulePeriodicWork(
            workRequest = workRequest,
            uniqueWorkName = WORK_ROUTE_SYNC
        )
    }

    companion object {
        private const val WORK_BUS_LOCATION_SYNC = "bus_location_sync"
        private const val WORK_ROUTE_SYNC = "route_sync"
    }
}

/**
 * Scheduler for maintenance tasks
 */
class MaintenanceScheduler(
    context: Context
) : BaseScheduler(context) {

    fun scheduleDataCleanup() {
        val constraints = createConstraints(
            requiresDeviceIdle = true,
            requiresBatteryNotLow = true,
            requiresStorageNotLow = true
        )

        val workRequest = PeriodicWorkRequestBuilder<DataCleanupWorker>(
            repeatInterval = Constants.DATA_CLEANUP_INTERVAL,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setDefaultBackoffCriteria()
            .build()

        schedulePeriodicWork(
            workRequest = workRequest,
            uniqueWorkName = WORK_DATA_CLEANUP
        )
    }

    fun scheduleBackup() {
        val constraints = createConstraints(
            requiresNetwork = NetworkType.UNMETERED,
            requiresCharging = true,
            requiresBatteryNotLow = true
        )

        val workRequest = PeriodicWorkRequestBuilder<DataBackupWorker>(
            repeatInterval = Constants.BACKUP_INTERVAL,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setDefaultBackoffCriteria()
            .build()

        schedulePeriodicWork(
            workRequest = workRequest,
            uniqueWorkName = WORK_BACKUP
        )
    }

    companion object {
        private const val WORK_DATA_CLEANUP = "data_cleanup"
        private const val WORK_BACKUP = "data_backup"
    }
}

/**
 * Scheduler for notification tasks
 */
class NotificationScheduler(
    context: Context
) : BaseScheduler(context) {

    fun scheduleReminder(
        busId: String,
        stopId: String,
        delayMinutes: Long
    ) {
        val data = workDataOf(
            "bus_id" to busId,
            "stop_id" to stopId
        )

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(data)
            .build()

        scheduleOneTimeWork(
            workRequest = workRequest,
            uniqueWorkName = "reminder_${busId}_${stopId}"
        )
    }

    fun cancelReminder(busId: String, stopId: String) {
        cancelWork("reminder_${busId}_${stopId}")
    }
}

/**
 * Scheduler manager to handle all schedulers
 */
class SchedulerManager(
    private val context: Context,
    private val networkManager: NetworkConnectivityManager
) {
    private val syncScheduler = SyncScheduler(context, networkManager)
    private val maintenanceScheduler = MaintenanceScheduler(context)
    private val notificationScheduler = NotificationScheduler(context)

    fun scheduleAllPeriodicTasks() {
        syncScheduler.scheduleBusLocationSync()
        syncScheduler.scheduleRouteDataSync()
        maintenanceScheduler.scheduleDataCleanup()
        maintenanceScheduler.scheduleBackup()
    }

    fun cancelAllTasks() {
        WorkManager.getInstance(context).cancelAllWork()
    }

    fun scheduleReminder(busId: String, stopId: String, delayMinutes: Long) {
        notificationScheduler.scheduleReminder(busId, stopId, delayMinutes)
    }

    fun cancelReminder(busId: String, stopId: String) {
        notificationScheduler.cancelReminder(busId, stopId)
    }

    companion object {
        @Volatile
        private var instance: SchedulerManager? = null

        fun getInstance(
            context: Context,
            networkManager: NetworkConnectivityManager
        ): SchedulerManager {
            return instance ?: synchronized(this) {
                instance ?: SchedulerManager(
                    context.applicationContext,
                    networkManager
                ).also { instance = it }
            }
        }
    }
}

/**
 * Extension functions for convenient scheduling
 */
fun Context.scheduleReminder(busId: String, stopId: String, delayMinutes: Long) {
    SchedulerManager.getInstance(
        this,
        NetworkConnectivityManager.getInstance(this)
    ).scheduleReminder(busId, stopId, delayMinutes)
}

fun Context.cancelReminder(busId: String, stopId: String) {
    SchedulerManager.getInstance(
        this,
        NetworkConnectivityManager.getInstance(this)
    ).cancelReminder(busId, stopId)
}
