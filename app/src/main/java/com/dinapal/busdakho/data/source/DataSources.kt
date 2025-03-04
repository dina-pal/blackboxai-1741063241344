package com.dinapal.busdakho.data.source

import com.dinapal.busdakho.data.local.BusDakhoDatabase
import com.dinapal.busdakho.data.preferences.PreferencesManager
import com.dinapal.busdakho.util.Constants
import com.dinapal.busdakho.util.NetworkConnectivityManager
import com.dinapal.busdakho.util.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/**
 * Base class for remote data sources
 */
abstract class RemoteDataSource(
    networkManager: NetworkConnectivityManager,
    private val preferencesManager: PreferencesManager,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseDataSource(networkManager, ioDispatcher) {

    protected suspend fun getAuthToken(): String? {
        return preferencesManager.authToken.first()
    }

    protected suspend fun isTokenValid(): Boolean {
        val token = getAuthToken()
        return !token.isNullOrEmpty()
    }

    protected suspend fun <T> withAuth(block: suspend (String) -> T): Resource<T> {
        val token = getAuthToken()
        return if (token != null) {
            try {
                Resource.Success(block(token))
            } catch (e: Exception) {
                Resource.Error(error = e, message = e.message)
            }
        } else {
            Resource.Error(message = "Authentication required")
        }
    }

    protected fun <T> cacheResponse(
        cacheKey: String,
        response: T,
        expirationTime: Long = Constants.CACHE_DURATION
    ) {
        // Implement caching mechanism
    }

    protected suspend fun <T> getCachedResponse(cacheKey: String): T? {
        // Implement cache retrieval
        return null
    }

    protected suspend fun clearCache() {
        // Implement cache clearing
    }
}

/**
 * Base class for local data sources
 */
abstract class LocalDataSource(
    protected val database: BusDakhoDatabase,
    private val preferencesManager: PreferencesManager,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    protected val tag = this::class.java.simpleName

    protected suspend fun <T> withTransaction(block: suspend () -> T): T {
        return database.withTransaction { block() }
    }

    protected suspend fun <T> safeDbCall(
        block: suspend () -> T
    ): Resource<T> = withContext(Dispatchers.IO) {
        try {
            Resource.Success(block())
        } catch (e: Exception) {
            Resource.Error(error = e, message = e.message)
        }
    }

    protected fun <T> observeData(
        query: () -> Flow<T>
    ): Flow<Resource<T>> = flow {
        emit(Resource.Loading())
        try {
            query().collect { data ->
                emit(Resource.Success(data))
            }
        } catch (e: Exception) {
            emit(Resource.Error(error = e, message = e.message))
        }
    }

    protected suspend fun clearAllTables() {
        database.clearAllTables()
    }

    protected suspend fun getUserId(): String? {
        return preferencesManager.userId.first()
    }

    protected suspend fun getLastSyncTimestamp(): Long {
        return preferencesManager.lastSyncTimestamp.first()
    }

    protected suspend fun updateLastSyncTimestamp() {
        preferencesManager.setLastSyncTimestamp(System.currentTimeMillis())
    }
}

/**
 * Base class for memory cache data source
 */
abstract class MemoryCacheDataSource<K, V> {
    protected val cache = mutableMapOf<K, CacheEntry<V>>()

    protected fun put(key: K, value: V, expirationTime: Long = Constants.CACHE_DURATION) {
        cache[key] = CacheEntry(value, System.currentTimeMillis() + expirationTime)
    }

    protected fun get(key: K): V? {
        val entry = cache[key]
        return if (entry?.isValid == true) entry.value else null
    }

    protected fun remove(key: K) {
        cache.remove(key)
    }

    protected fun clear() {
        cache.clear()
    }

    protected fun cleanExpired() {
        val iterator = cache.entries.iterator()
        while (iterator.hasNext()) {
            if (!iterator.next().value.isValid) {
                iterator.remove()
            }
        }
    }

    private data class CacheEntry<V>(
        val value: V,
        val expirationTime: Long
    ) {
        val isValid: Boolean
            get() = System.currentTimeMillis() < expirationTime
    }
}

/**
 * Interface for data synchronization
 */
interface SyncableDataSource {
    suspend fun sync(): Resource<Unit>
    suspend fun isSyncNeeded(): Boolean
    suspend fun getLastSyncTimestamp(): Long
    suspend fun markSynced()
}

/**
 * Interface for paginated data source
 */
interface PaginatedDataSource<T> {
    suspend fun getPage(page: Int, pageSize: Int): Resource<List<T>>
    suspend fun getTotal(): Int
    suspend fun hasNextPage(currentPage: Int): Boolean
    suspend fun refresh()
}

/**
 * Extension functions for data sources
 */
fun <T> Resource<T>.requireData(): T {
    return when (this) {
        is Resource.Success -> data!!
        is Resource.Error -> throw IllegalStateException(message ?: "Data required but not available")
        is Resource.Loading -> throw IllegalStateException("Data required but still loading")
    }
}

fun <T> Flow<Resource<T>>.filterSuccess(): Flow<T> = transform { resource ->
    if (resource is Resource.Success && resource.data != null) {
        emit(resource.data)
    }
}

fun <T> Flow<Resource<T>>.filterError(): Flow<String> = transform { resource ->
    if (resource is Resource.Error && resource.message != null) {
        emit(resource.message)
    }
}

fun <T> Flow<Resource<T>>.doOnSuccess(action: suspend (T) -> Unit): Flow<Resource<T>> = onEach {
    if (it is Resource.Success && it.data != null) {
        action(it.data)
    }
}

fun <T> Flow<Resource<T>>.doOnError(action: suspend (String?) -> Unit): Flow<Resource<T>> = onEach {
    if (it is Resource.Error) {
        action(it.message)
    }
}
