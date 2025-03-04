package com.dinapal.busdakho.worker

import android.content.Context
import androidx.work.*
import com.dinapal.busdakho.data.preferences.PreferencesManager
import com.dinapal.busdakho.domain.repository.BusRepository
import com.dinapal.busdakho.domain.repository.RouteRepository
import com.dinapal.busdakho.util.Constants
import com.dinapal.busdakho.util.NetworkConnectivityManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Worker to sync bus locations periodically
 */
class BusLocationSyncWorker(
    context: Context,
    params: WorkerParameters,
    networkManager: NetworkConnectivityManager,
    private val busRepository: BusRepository
) : BaseWorker(context, params, networkManager) {

    override suspend fun executeWork(): Result {
        return try {
            busRepository.syncBusLocations()
            Result.success()
        } catch (e: Exception) {
            if (e is RetryableException) Result.retry() else Result.failure()
        }
    }

    override fun requiresNetwork(): Boolean = true

    companion object {
        private const val UNIQUE_WORK_NAME = "bus_location_sync"
        private const val SYNC_INTERVAL = 1L // minutes

        fun schedule(context: Context) {
            val request = createPeriodicWork<BusLocationSyncWorker>(
                repeatInterval = SYNC_INTERVAL,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                constraints = networkConstraints(),
                tags = listOf(UNIQUE_WORK_NAME)
            )

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )
        }
    }
}

/**
 * Worker to sync route data periodically
 */
class RouteSyncWorker(
    context: Context,
    params: WorkerParameters,
    networkManager: NetworkConnectivityManager,
    private val routeRepository: RouteRepository,
    private val preferencesManager: PreferencesManager
) : BaseWorker(context, params, networkManager) {

    override suspend fun executeWork(): Result {
        return try {
            val lastSync = preferencesManager.lastSyncTimestamp.first()
            if (System.currentTimeMillis() - lastSync > Constants.SYNC_INTERVAL) {
                routeRepository.syncRoutes()
                preferencesManager.setLastSyncTimestamp(System.currentTimeMillis())
            }
            Result.success()
        } catch (e: Exception) {
            if (e is RetryableException) Result.retry() else Result.failure()
        }
    }

    override fun requiresNetwork(): Boolean = true

    companion object {
        private const val UNIQUE_WORK_NAME = "route_sync"
        private const val SYNC_INTERVAL = 24L // hours

        fun schedule(context: Context) {
            val request = createPeriodicWork<RouteSyncWorker>(
                repeatInterval = SYNC_INTERVAL,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                constraints = networkConstraints(),
                tags = listOf(UNIQUE_WORK_NAME)
            )

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}

/**
 * Worker to clean up old data
 */
class DataCleanupWorker(
    context: Context,
    params: WorkerParameters,
    networkManager: NetworkConnectivityManager,
    private val busRepository: BusRepository,
    private val routeRepository: RouteRepository
) : BaseWorker(context, params, networkManager) {

    override suspend fun executeWork(): Result {
        return try {
            val threshold = System.currentTimeMillis() - Constants.DATA_RETENTION_PERIOD
            busRepository.deleteOldData(threshold)
            routeRepository.deleteOldData(threshold)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "data_cleanup"
        private const val CLEANUP_INTERVAL = 7L // days

        fun schedule(context: Context) {
            val request = createPeriodicWork<DataCleanupWorker>(
                repeatInterval = CLEANUP_INTERVAL,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
                constraints = storageConstraints(),
                tags = listOf(UNIQUE_WORK_NAME)
            )

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}

/**
 * Worker to handle data backup
 */
class DataBackupWorker(
    context: Context,
    params: WorkerParameters,
    networkManager: NetworkConnectivityManager,
    private val busRepository: BusRepository,
    private val routeRepository: RouteRepository
) : BaseWorker(context, params, networkManager) {

    override suspend fun executeWork(): Result {
        return try {
            // Backup data to cloud storage
            Result.success()
        } catch (e: Exception) {
            if (e is RetryableException) Result.retry() else Result.failure()
        }
    }

    override fun requiresNetwork(): Boolean = true

    companion object {
        private const val UNIQUE_WORK_NAME = "data_backup"
        private const val BACKUP_INTERVAL = 24L // hours

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = createPeriodicWork<DataBackupWorker>(
                repeatInterval = BACKUP_INTERVAL,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                constraints = constraints,
                tags = listOf(UNIQUE_WORK_NAME)
            )

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}

/**
 * Extension function to schedule all workers
 */
fun Context.scheduleAllWorkers() {
    BusLocationSyncWorker.schedule(this)
    RouteSyncWorker.schedule(this)
    DataCleanupWorker.schedule(this)
    DataBackupWorker.schedule(this)
}

/**
 * Extension function to cancel all workers
 */
fun Context.cancelAllWorkers() {
    WorkManager.getInstance(this).cancelAllWork()
}
