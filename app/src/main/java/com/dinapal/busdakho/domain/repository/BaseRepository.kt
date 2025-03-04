package com.dinapal.busdakho.domain.repository

import com.dinapal.busdakho.util.NetworkConnectivityManager
import com.dinapal.busdakho.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.io.IOException

interface BaseRepository {
    val networkManager: NetworkConnectivityManager

    /**
     * Executes a network call with proper error handling and resource state management
     */
    suspend fun <T> safeApiCall(
        apiCall: suspend () -> T
    ): Resource<T> {
        return try {
            if (!networkManager.isNetworkAvailable()) {
                Resource.Error(message = "No internet connection")
            } else {
                Resource.Success(apiCall())
            }
        } catch (e: IOException) {
            Resource.Error(message = "Network error occurred", error = e)
        } catch (e: Exception) {
            Resource.Error(message = e.message ?: "An unknown error occurred", error = e)
        }
    }

    /**
     * Executes a database operation with proper error handling
     */
    suspend fun <T> safeDbCall(
        dbCall: suspend () -> T
    ): Resource<T> {
        return try {
            Resource.Success(dbCall())
        } catch (e: Exception) {
            Resource.Error(message = e.message ?: "Database error occurred", error = e)
        }
    }

    /**
     * Combines network and database operations with caching strategy
     */
    fun <T> networkBoundResource(
        query: () -> Flow<T>,
        fetch: suspend () -> T,
        saveFetchResult: suspend (T) -> Unit,
        shouldFetch: (T?) -> Boolean = { true }
    ): Flow<Resource<T>> = query()
        .map { data -> 
            if (shouldFetch(data)) {
                try {
                    if (!networkManager.isNetworkAvailable()) {
                        Resource.Error(
                            message = "No internet connection. Showing cached data.",
                            data = data
                        )
                    } else {
                        val fetchedData = fetch()
                        saveFetchResult(fetchedData)
                        Resource.Success(fetchedData)
                    }
                } catch (e: Exception) {
                    Resource.Error(
                        message = e.message ?: "Error fetching data",
                        error = e,
                        data = data
                    )
                }
            } else {
                Resource.Success(data)
            }
        }
        .onStart { emit(Resource.Loading()) }
        .catch { e ->
            emit(Resource.Error(message = e.message ?: "Unknown error occurred", error = e))
        }

    /**
     * Executes a flow operation with proper error handling and resource state management
     */
    fun <T> safeFlow(
        flowCall: suspend () -> Flow<T>
    ): Flow<Resource<T>> = flow {
        try {
            flowCall().collect { data ->
                emit(Resource.Success(data))
            }
        } catch (e: Exception) {
            emit(Resource.Error(message = e.message ?: "Flow error occurred", error = e))
        }
    }.onStart { emit(Resource.Loading()) }

    /**
     * Retries an operation with exponential backoff
     */
    suspend fun <T> retryIO(
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
            } catch (e: IOException) {
                // Retry on IO Exception
            }
            kotlinx.coroutines.delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // last attempt
    }

    /**
     * Validates if the data is stale and needs refresh
     */
    fun isDataStale(lastUpdateTime: Long, maxAge: Long): Boolean {
        return System.currentTimeMillis() - lastUpdateTime > maxAge
    }

    companion object {
        private const val DEFAULT_CACHE_TIMEOUT = 5 * 60 * 1000L // 5 minutes
        private const val DEFAULT_RETRY_TIMES = 3
        private const val DEFAULT_INITIAL_RETRY_DELAY = 100L
        private const val DEFAULT_MAX_RETRY_DELAY = 1000L
        private const val DEFAULT_RETRY_FACTOR = 2.0
    }
}

// Extension function to convert Flow to Resource Flow
fun <T> Flow<T>.asResource(): Flow<Resource<T>> = this
    .map<T, Resource<T>> { Resource.Success(it) }
    .onStart { emit(Resource.Loading()) }
    .catch { e -> emit(Resource.Error(message = e.message ?: "Error occurred", error = e)) }

// Extension function to handle offline-first strategy
fun <T> Flow<T>.offlineFirst(
    networkManager: NetworkConnectivityManager,
    fetch: suspend () -> T
): Flow<Resource<T>> = this
    .map { cachedData ->
        if (networkManager.isNetworkAvailable()) {
            try {
                Resource.Success(fetch())
            } catch (e: Exception) {
                Resource.Error(
                    message = "Error fetching data. Showing cached data.",
                    error = e,
                    data = cachedData
                )
            }
        } else {
            Resource.Error(
                message = "No internet connection. Showing cached data.",
                data = cachedData
            )
        }
    }
    .onStart { emit(Resource.Loading()) }
    .catch { e ->
        emit(Resource.Error(message = e.message ?: "Unknown error occurred", error = e))
    }
