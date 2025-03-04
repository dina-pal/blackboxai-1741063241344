package com.dinapal.busdakho.cache

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import com.dinapal.busdakho.util.Constants
import com.dinapal.busdakho.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Memory cache implementation using LruCache
 */
class MemoryCache<K, V>(
    maxSize: Int = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
) : BaseCache<K, V>() {
    private val lruCache = object : LruCache<K, V>(maxSize) {
        override fun sizeOf(key: K, value: V): Int {
            return when (value) {
                is Bitmap -> value.byteCount / 1024
                is String -> value.length
                is ByteArray -> value.size / 1024
                else -> 1
            }
        }
    }

    override suspend fun put(key: K, value: V, expirationTime: Long) {
        withContext(Dispatchers.Default) {
            lruCache.put(key, value)
        }
    }

    override suspend fun get(key: K): V? {
        return withContext(Dispatchers.Default) {
            lruCache.get(key)
        }
    }

    override suspend fun remove(key: K) {
        withContext(Dispatchers.Default) {
            lruCache.remove(key)
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.Default) {
            lruCache.evictAll()
        }
    }
}

/**
 * Disk cache implementation
 */
class DiskCache(
    context: Context,
    private val directory: String = "cache"
) : BaseCache<String, ByteArray>() {
    private val cacheDir = File(context.cacheDir, directory).apply {
        if (!exists()) {
            mkdirs()
        }
    }

    override suspend fun put(key: String, value: ByteArray, expirationTime: Long) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, key.hashCode().toString())
                FileOutputStream(file).use { fos ->
                    fos.write(value)
                }
                Logger.d(tag, "Saved file to disk cache: $key")
            } catch (e: Exception) {
                Logger.e(tag, "Failed to save file to disk cache: $key", e)
            }
        }
    }

    override suspend fun get(key: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, key.hashCode().toString())
                if (file.exists()) {
                    file.readBytes()
                } else {
                    null
                }
            } catch (e: Exception) {
                Logger.e(tag, "Failed to read file from disk cache: $key", e)
                null
            }
        }
    }

    override suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, key.hashCode().toString())
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Logger.e(tag, "Failed to delete file from disk cache: $key", e)
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            try {
                cacheDir.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) {
                Logger.e(tag, "Failed to clear disk cache", e)
            }
        }
    }

    suspend fun getCacheSize(): Long {
        return withContext(Dispatchers.IO) {
            cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        }
    }
}

/**
 * Two-level cache implementation combining memory and disk cache
 */
class TwoLevelCache<K>(
    context: Context,
    private val memoryCache: MemoryCache<K, ByteArray>,
    private val diskCache: DiskCache
) : BaseCache<K, ByteArray>() {

    override suspend fun put(key: K, value: ByteArray, expirationTime: Long) {
        memoryCache.put(key, value, expirationTime)
        diskCache.put(key.toString(), value, expirationTime)
    }

    override suspend fun get(key: K): ByteArray? {
        return memoryCache.get(key) ?: diskCache.get(key.toString())?.also { value ->
            memoryCache.put(key, value, Constants.CACHE_DURATION)
        }
    }

    override suspend fun remove(key: K) {
        memoryCache.remove(key)
        diskCache.remove(key.toString())
    }

    override suspend fun clear() {
        memoryCache.clear()
        diskCache.clear()
    }
}

/**
 * Cache manager to handle different types of caches
 */
class CacheManager(context: Context) {
    private val memoryCache = MemoryCache<String, ByteArray>()
    private val diskCache = DiskCache(context)
    private val twoLevelCache = TwoLevelCache(context, memoryCache, diskCache)

    suspend fun put(key: String, value: ByteArray, useMemoryOnly: Boolean = false) {
        if (useMemoryOnly) {
            memoryCache.put(key, value)
        } else {
            twoLevelCache.put(key, value)
        }
    }

    suspend fun get(key: String, useMemoryOnly: Boolean = false): ByteArray? {
        return if (useMemoryOnly) {
            memoryCache.get(key)
        } else {
            twoLevelCache.get(key)
        }
    }

    suspend fun remove(key: String, useMemoryOnly: Boolean = false) {
        if (useMemoryOnly) {
            memoryCache.remove(key)
        } else {
            twoLevelCache.remove(key)
        }
    }

    suspend fun clear(useMemoryOnly: Boolean = false) {
        if (useMemoryOnly) {
            memoryCache.clear()
        } else {
            twoLevelCache.clear()
        }
    }

    companion object {
        @Volatile
        private var instance: CacheManager? = null

        fun getInstance(context: Context): CacheManager {
            return instance ?: synchronized(this) {
                instance ?: CacheManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
