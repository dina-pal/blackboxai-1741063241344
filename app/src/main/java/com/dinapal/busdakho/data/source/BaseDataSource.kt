package com.dinapal.busdakho.data.source

import com.dinapal.busdakho.util.Logger
import com.dinapal.busdakho.util.NetworkConnectivityManager
import com.dinapal.busdakho.util.NetworkError
import com.dinapal.busdakho.util.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

/**
 * Base class for all data sources
 */
abstract class BaseDataSource(
    protected val networkManager: NetworkConnectivityManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    protected val tag = this::class.java.simpleName

    /**
     * Safely executes a network call and returns a Resource
     */
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>
    ): Resource<T> = withContext(ioDispatcher) {
        try {
            if (!networkManager.isNetworkAvailable()) {
                return@withContext Resource.Error(
                    error = NetworkError.NoInternet,
                    message = "No internet connection"
                )
            }

            val response = apiCall()
            if (response.isSuccessful) {
                response.body()?.let {
                    return@withContext Resource.Success(it)
                } ?: return@withContext Resource.Error(
                    error = NetworkError.Unknown("Response body is null"),
                    message = "Empty response"
                )
            } else {
                return@withContext Resource.Error(
                    error = NetworkError.ApiError(
                        message = response.message(),
                        code = response.code(),
                        errorBody = response.errorBody()?.string()
                    ),
                    message = "API call failed with code ${response.code()}"
                )
            }
        } catch (e: Exception) {
            Logger.e(tag, "API call failed", e)
            return@withContext when (e) {
                is IOException -> Resource.Error(
                    error = NetworkError.NoInternet,
                    message = "Network error occurred"
                )
                else -> Resource.Error(
                    error = NetworkError.Unknown(e.message),
                    message = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Safely executes a database operation and returns a Resource
     */
    protected suspend fun <T> safeDbCall(
        dbCall: suspend () -> T
    ): Resource<T> = withContext(ioDispatcher) {
        try {
            Resource.Success(dbCall())
        } catch (e: Exception) {
            Logger.e(tag, "Database operation failed", e)
            Resource.Error(
                error = e,
                message = e.message ?: "Database error occurred"
            )
        }
    }

    /**
     * Combines network and database operations with caching strategy
     */
    protected fun <T> networkBoundResource(
        query: () -> Flow<T>,
        fetch: suspend () -> Response<T>,
        saveFetchResult: suspend (T) -> Unit,
        shouldFetch: (T?) -> Boolean = { true }
    ): Flow<Resource<T>> = flow {
        emit(Resource.Loading())

        val data = query().first()

        val flow = if (shouldFetch(data)) {
            emit(Resource.Loading(data))

            try {
                val response = fetch()
                if (response.isSuccessful) {
                    response.body()?.let { fetchedData ->
                        saveFetchResult(fetchedData)
                        query().map { Resource.Success(it) }
                    } ?: query().map {
                        Resource.Error(
                            message = "Response body is null",
                            data = it
                        )
                    }
                } else {
                    query().map {
                        Resource.Error(
                            message = "API call failed with code ${response.code()}",
                            data = it
                        )
                    }
                }
            } catch (e: Exception) {
                Logger.e(tag, "Network bound resource failed", e)
                query().map {
                    Resource.Error(
                        error = e,
                        message = e.message ?: "Error fetching data",
                        data = it
                    )
                }
            }
        } else {
            query().map { Resource.Success(it) }
        }

        emitAll(flow)
    }

    /**
     * Retries an operation with exponential backoff
     */
    protected suspend fun <T> retryIO(
        times: Int = 3,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                Logger.e(tag, "Operation failed, retrying...", e)
            }
            kotlinx.coroutines.delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // last attempt
    }

    /**
     * Extension function to convert Flow to Resource Flow
     */
    protected fun <T> Flow<T>.asResource(): Flow<Resource<T>> = this
        .map<T, Resource<T>> { Resource.Success(it) }
        .onStart { emit(Resource.Loading()) }
        .catch { e ->
            Logger.e(tag, "Flow operation failed", e)
            emit(Resource.Error(error = e, message = e.message))
        }

    /**
     * Extension function to handle offline-first strategy
     */
    protected fun <T> Flow<T>.offlineFirst(
        fetch: suspend () -> T
    ): Flow<Resource<T>> = flow {
        emit(Resource.Loading())

        // Emit cached data first
        emitAll(this@offlineFirst.map { Resource.Success(it) })

        // Then try to fetch fresh data if network is available
        if (networkManager.isNetworkAvailable()) {
            try {
                val freshData = fetch()
                emit(Resource.Success(freshData))
            } catch (e: Exception) {
                Logger.e(tag, "Failed to fetch fresh data", e)
                emit(Resource.Error(error = e, message = e.message))
            }
        }
    }
}
