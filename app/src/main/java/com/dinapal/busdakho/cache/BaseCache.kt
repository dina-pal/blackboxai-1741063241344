package com.dinapal.busdakho.cache

import com.dinapal.busdakho.util.Constants
import com.dinapal.busdakho.util.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Base class for all caching implementations
 */
abstract class BaseCache<K, V> {
    protected val tag = this::class.java.simpleName
    protected val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val mutex = Mutex()

    /**
     * Put an item in the cache
     */
    suspend fun put(
        key: K,
        value: V,
        expirationTime: Long = Constants.CACHE_DURATION
    ) = mutex.withLock {
        try {
            cache[key] = CacheEntry(
                value = value,
                timestamp = System.currentTimeMillis(),
                expirationTime = expirationTime
            )
            Logger.d(tag, "Cached item with key: $key")
        } catch (e: Exception) {
            Logger.e(tag, "Failed to cache item with key: $key", e)
        }
    }

    /**
     * Get an item from the cache
     */
    suspend fun get(key: K): V? = mutex.withLock {
        try {
            val entry = cache[key]
            return when {
                entry == null -> {
                    Logger.d(tag, "Cache miss for key: $key")
                    null
                }
                entry.isExpired() -> {
                    Logger.d(tag, "Cache expired for key: $key")
                    cache.remove(key)
                    null
                }
                else -> {
                    Logger.d(tag, "Cache hit for key: $key")
                    entry.value
                }
            }
        } catch (e: Exception) {
            Logger.e(tag, "Failed to get cached item with key: $key", e)
            null
        }
    }

    /**
     * Get an item from the cache or compute if not present
     */
    suspend fun getOrPut(
        key: K,
        expirationTime: Long = Constants.CACHE_DURATION,
        compute: suspend () -> V
    ): V = mutex.withLock {
        get(key) ?: compute().also { value ->
            put(key, value, expirationTime)
        }
    }

    /**
     * Remove an item from the cache
     */
    suspend fun remove(key: K) = mutex.withLock {
        try {
            cache.remove(key)
            Logger.d(tag, "Removed item from cache with key: $key")
        } catch (e: Exception) {
            Logger.e(tag, "Failed to remove item from cache with key: $key", e)
        }
    }

    /**
     * Clear all items from the cache
     */
    suspend fun clear() = mutex.withLock {
        try {
            cache.clear()
            Logger.d(tag, "Cleared cache")
        } catch (e: Exception) {
            Logger.e(tag, "Failed to clear cache", e)
        }
    }

    /**
     * Remove expired items from the cache
     */
    suspend fun cleanExpired() = mutex.withLock {
        try {
            val expiredKeys = cache.entries
                .filter { it.value.isExpired() }
                .map { it.key }

            expiredKeys.forEach { key ->
                cache.remove(key)
            }

            if (expiredKeys.isNotEmpty()) {
                Logger.d(tag, "Removed ${expiredKeys.size} expired items from cache")
            }
        } catch (e: Exception) {
            Logger.e(tag, "Failed to clean expired items from cache", e)
        }
    }

    /**
     * Get the size of the cache
     */
    fun size(): Int = cache.size

    /**
     * Check if the cache is empty
     */
    fun isEmpty(): Boolean = cache.isEmpty()

    /**
     * Check if the cache contains a key
     */
    fun containsKey(key: K): Boolean = cache.containsKey(key)

    /**
     * Get all keys in the cache
     */
    fun keys(): Set<K> = cache.keys

    /**
     * Get all values in the cache
     */
    fun values(): Collection<V> = cache.values.map { it.value }

    /**
     * Cache entry with metadata
     */
    protected data class CacheEntry<V>(
        val value: V,
        val timestamp: Long,
        val expirationTime: Long
    ) {
        fun isExpired(): Boolean =
            System.currentTimeMillis() - timestamp > expirationTime
    }
}

/**
 * Interface for cache invalidation strategies
 */
interface CacheInvalidationStrategy {
    fun shouldInvalidate(timestamp: Long): Boolean
}

/**
 * Time-based cache invalidation strategy
 */
class TimeBasedInvalidationStrategy(
    private val maxAge: Long = Constants.CACHE_MAX_AGE
) : CacheInvalidationStrategy {
    override fun shouldInvalidate(timestamp: Long): Boolean =
        System.currentTimeMillis() - timestamp > maxAge
}

/**
 * Size-based cache invalidation strategy
 */
class SizeBasedInvalidationStrategy(
    private val maxSize: Int = Constants.CACHE_SIZE.toInt()
) : CacheInvalidationStrategy {
    fun shouldInvalidate(currentSize: Int): Boolean =
        currentSize >= maxSize
}

/**
 * Composite cache invalidation strategy
 */
class CompositeCacheInvalidationStrategy(
    private val strategies: List<CacheInvalidationStrategy>
) : CacheInvalidationStrategy {
    override fun shouldInvalidate(timestamp: Long): Boolean =
        strategies.any { it.shouldInvalidate(timestamp) }
}

/**
 * Extension functions for caching
 */
suspend fun <K, V> BaseCache<K, V>.getOrDefault(
    key: K,
    defaultValue: V
): V = get(key) ?: defaultValue

suspend fun <K, V> BaseCache<K, V>.getOrNull(key: K): V? = get(key)

suspend fun <K, V> BaseCache<K, V>.putIfAbsent(
    key: K,
    value: V,
    expirationTime: Long = Constants.CACHE_DURATION
): V? = get(key).also { if (it == null) put(key, value, expirationTime) }

suspend fun <K, V> BaseCache<K, V>.computeIfAbsent(
    key: K,
    expirationTime: Long = Constants.CACHE_DURATION,
    compute: suspend () -> V
): V = getOrPut(key, expirationTime, compute)
