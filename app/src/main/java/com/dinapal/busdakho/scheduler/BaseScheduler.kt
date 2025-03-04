package com.dinapal.busdakho.scheduler

import android.content.Context
import androidx.work.*
import com.dinapal.busdakho.util.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Base class for scheduling tasks
 */
abstract class BaseScheduler(
    protected val context: Context,
    private val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()
) {
    protected val tag = this::class.java.simpleName
    protected val scope = CoroutineScope(coroutineContext)

    private val _events = MutableSharedFlow<SchedulerEvent>()
    val events = _events.asSharedFlow()

    /**
     * Schedule a one-time task
     */
    protected fun scheduleOneTimeWork(
        workRequest: OneTimeWorkRequest,
        uniqueWorkName: String? = null,
        existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE
    ) {
        try {
            val workManager = WorkManager.getInstance(context)
            if (uniqueWorkName != null) {
                workManager.enqueueUniqueWork(
                    uniqueWorkName,
                    existingWorkPolicy,
                    workRequest
                )
            } else {
                workManager.enqueue(workRequest)
            }
            Logger.d(tag, "Scheduled one-time work: ${workRequest.id}")
        } catch (e: Exception) {
            Logger.e(tag, "Failed to schedule one-time work", e)
            handleError(e)
        }
    }

    /**
     * Schedule a periodic task
     */
    protected fun schedulePeriodicWork(
        workRequest: PeriodicWorkRequest,
        uniqueWorkName: String,
        existingWorkPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.REPLACE
    ) {
        try {
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    uniqueWorkName,
                    existingWorkPolicy,
                    workRequest
                )
            Logger.d(tag, "Scheduled periodic work: ${workRequest.id}")
        } catch (e: Exception) {
            Logger.e(tag, "Failed to schedule periodic work", e)
            handleError(e)
        }
    }

    /**
     * Cancel a scheduled task
     */
    protected fun cancelWork(uniqueWorkName: String) {
        try {
            WorkManager.getInstance(context)
                .cancelUniqueWork(uniqueWorkName)
            Logger.d(tag, "Cancelled work: $uniqueWorkName")
        } catch (e: Exception) {
            Logger.e(tag, "Failed to cancel work", e)
            handleError(e)
        }
    }

    /**
     * Cancel all scheduled tasks
     */
    protected fun cancelAllWork() {
        try {
            WorkManager.getInstance(context).cancelAllWork()
            Logger.d(tag, "Cancelled all work")
        } catch (e: Exception) {
            Logger.e(tag, "Failed to cancel all work", e)
            handleError(e)
        }
    }

    /**
     * Get work info for a task
     */
    protected fun getWorkInfo(uniqueWorkName: String): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(uniqueWorkName)
    }

    /**
     * Create work constraints
     */
    protected fun createConstraints(
        requiresCharging: Boolean = false,
        requiresDeviceIdle: Boolean = false,
        requiresStorageNotLow: Boolean = false,
        requiresBatteryNotLow: Boolean = false,
        requiresNetwork: NetworkType = NetworkType.NOT_REQUIRED
    ): Constraints {
        return Constraints.Builder()
            .setRequiresCharging(requiresCharging)
            .setRequiresDeviceIdle(requiresDeviceIdle)
            .setRequiresStorageNotLow(requiresStorageNotLow)
            .setRequiresBatteryNotLow(requiresBatteryNotLow)
            .setRequiredNetworkType(requiresNetwork)
            .build()
    }

    /**
     * Handle scheduler errors
     */
    protected open fun handleError(error: Exception) {
        scope.launch {
            _events.emit(SchedulerEvent.Error(error))
        }
    }

    /**
     * Clean up resources
     */
    open fun cleanup() {
        scope.cancel()
    }
}

/**
 * Scheduler events
 */
sealed class SchedulerEvent {
    data class Success(val workName: String) : SchedulerEvent()
    data class Error(val error: Exception) : SchedulerEvent()
    data class Progress(val workName: String, val progress: Int) : SchedulerEvent()
    data class Status(val workName: String, val status: WorkInfo.State) : SchedulerEvent()
}

/**
 * Extension functions for work request builders
 */
fun OneTimeWorkRequest.Builder.setDefaultBackoffCriteria(): OneTimeWorkRequest.Builder {
    return setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS
    )
}

fun PeriodicWorkRequest.Builder.setDefaultBackoffCriteria(): PeriodicWorkRequest.Builder {
    return setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS
    )
}

/**
 * Extension function to create work data
 */
fun workDataOf(vararg pairs: Pair<String, Any?>): Data {
    return Data.Builder().apply {
        pairs.forEach { (key, value) ->
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Double -> putDouble(key, value)
                is String -> putString(key, value)
                is Array<*> -> when {
                    value.isArrayOf<String>() -> putStringArray(key, value as Array<String>)
                    value.isArrayOf<Boolean>() -> putBooleanArray(key, value as Array<Boolean>)
                    value.isArrayOf<Int>() -> putIntArray(key, value as Array<Int>)
                    value.isArrayOf<Long>() -> putLongArray(key, value as Array<Long>)
                    value.isArrayOf<Float>() -> putFloatArray(key, value as Array<Float>)
                    value.isArrayOf<Double>() -> putDoubleArray(key, value as Array<Double>)
                }
            }
        }
    }.build()
}
