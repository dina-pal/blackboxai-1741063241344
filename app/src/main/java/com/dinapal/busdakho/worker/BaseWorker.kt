package com.dinapal.busdakho.worker

import android.content.Context
import androidx.work.*
import com.dinapal.busdakho.util.Logger
import com.dinapal.busdakho.util.NetworkConnectivityManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Base class for all workers in the application
 */
abstract class BaseWorker(
    context: Context,
    workerParams: WorkerParameters,
    protected val networkManager: NetworkConnectivityManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CoroutineWorker(context, workerParams) {

    protected val tag = this::class.java.simpleName

    override suspend fun doWork(): Result = withContext(dispatcher) {
        try {
            Logger.d(tag, "Starting work")
            
            if (requiresNetwork() && !networkManager.isNetworkAvailable()) {
                Logger.w(tag, "Network required but not available")
                return@withContext Result.retry()
            }

            val result = executeWork()
            
            Logger.d(tag, "Work completed successfully")
            result
        } catch (e: Exception) {
            Logger.e(tag, "Work failed", e)
            handleError(e)
        }
    }

    /**
     * Execute the actual work
     */
    protected abstract suspend fun executeWork(): Result

    /**
     * Whether this worker requires network connectivity
     */
    protected open fun requiresNetwork(): Boolean = false

    /**
     * Handle errors that occur during work execution
     */
    protected open fun handleError(error: Exception): Result {
        return when (error) {
            is RetryableException -> Result.retry()
            else -> Result.failure(workDataOf("error" to error.message))
        }
    }

    /**
     * Base class for worker data
     */
    abstract class WorkerData {
        abstract fun toWorkData(): Data
        abstract fun validate(): Boolean
    }

    companion object {
        /**
         * Create a OneTimeWorkRequest with default configuration
         */
        inline fun <reified T : ListenableWorker> createOneTimeWork(
            data: Data? = null,
            constraints: Constraints? = null,
            backoffPolicy: BackoffPolicy = BackoffPolicy.LINEAR,
            backoffDelay: Long = WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
            tags: List<String> = emptyList()
        ): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<T>().apply {
                data?.let { setInputData(it) }
                constraints?.let { setConstraints(it) }
                setBackoffCriteria(backoffPolicy, backoffDelay, TimeUnit.MILLISECONDS)
                tags.forEach { addTag(it) }
            }.build()
        }

        /**
         * Create a PeriodicWorkRequest with default configuration
         */
        inline fun <reified T : ListenableWorker> createPeriodicWork(
            repeatInterval: Long,
            repeatIntervalTimeUnit: TimeUnit,
            data: Data? = null,
            constraints: Constraints? = null,
            backoffPolicy: BackoffPolicy = BackoffPolicy.LINEAR,
            backoffDelay: Long = WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
            tags: List<String> = emptyList()
        ): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<T>(repeatInterval, repeatIntervalTimeUnit).apply {
                data?.let { setInputData(it) }
                constraints?.let { setConstraints(it) }
                setBackoffCriteria(backoffPolicy, backoffDelay, TimeUnit.MILLISECONDS)
                tags.forEach { addTag(it) }
            }.build()
        }

        /**
         * Default network constraints
         */
        fun networkConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Default charging constraints
         */
        fun chargingConstraints() = Constraints.Builder()
            .setRequiresCharging(true)
            .build()

        /**
         * Default battery not low constraints
         */
        fun batteryConstraints() = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        /**
         * Default storage not low constraints
         */
        fun storageConstraints() = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()
    }
}

/**
 * Exception indicating that the work should be retried
 */
class RetryableException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

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

/**
 * Extension function to get typed value from work data
 */
inline fun <reified T> Data.getValue(key: String): T? {
    return when (T::class) {
        Boolean::class -> getBoolean(key, false) as T
        Int::class -> getInt(key, 0) as T
        Long::class -> getLong(key, 0L) as T
        Float::class -> getFloat(key, 0f) as T
        Double::class -> getDouble(key, 0.0) as T
        String::class -> getString(key) as T?
        Array<Boolean>::class -> getBooleanArray(key) as T?
        Array<Int>::class -> getIntArray(key) as T?
        Array<Long>::class -> getLongArray(key) as T?
        Array<Float>::class -> getFloatArray(key) as T?
        Array<Double>::class -> getDoubleArray(key) as T?
        Array<String>::class -> getStringArray(key) as T?
        else -> null
    }
}
