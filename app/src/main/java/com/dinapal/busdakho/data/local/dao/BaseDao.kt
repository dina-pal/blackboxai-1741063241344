package com.dinapal.busdakho.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Base Data Access Object interface that provides common database operations
 * to be inherited by other DAOs
 */
interface BaseDao<T> {
    /**
     * Insert a single item
     * @param item the item to be inserted
     * @return the row ID of the newly inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: T): Long

    /**
     * Insert multiple items
     * @param items the list of items to be inserted
     * @return the list of row IDs of the newly inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<T>): List<Long>

    /**
     * Update a single item
     * @param item the item to be updated
     * @return the number of rows updated
     */
    @Update
    suspend fun update(item: T): Int

    /**
     * Update multiple items
     * @param items the list of items to be updated
     * @return the number of rows updated
     */
    @Update
    suspend fun updateAll(items: List<T>): Int

    /**
     * Delete a single item
     * @param item the item to be deleted
     * @return the number of rows deleted
     */
    @Delete
    suspend fun delete(item: T): Int

    /**
     * Delete multiple items
     * @param items the list of items to be deleted
     * @return the number of rows deleted
     */
    @Delete
    suspend fun deleteAll(items: List<T>): Int

    /**
     * Upsert operation: insert or update if exists
     * @param item the item to be upserted
     * @return the row ID of the upserted item
     */
    @Transaction
    suspend fun upsert(item: T) {
        val id = insert(item)
        if (id == -1L) {
            update(item)
        }
    }

    /**
     * Upsert operation for multiple items
     * @param items the list of items to be upserted
     */
    @Transaction
    suspend fun upsertAll(items: List<T>) {
        val insertResults = insertAll(items)
        val updateItems = items.filterIndexed { index, _ ->
            insertResults[index] == -1L
        }
        if (updateItems.isNotEmpty()) {
            updateAll(updateItems)
        }
    }

    /**
     * Delete all items and insert new ones in a single transaction
     * @param items the new items to be inserted after deletion
     */
    @Transaction
    suspend fun deleteAllAndInsert(items: List<T>) {
        deleteAll()
        insertAll(items)
    }

    /**
     * Delete all items from the table
     */
    @Query("DELETE FROM ${BaseDao.TABLE_NAME}")
    suspend fun deleteAll()

    /**
     * Get the count of items in the table
     * @return the number of items
     */
    @Query("SELECT COUNT(*) FROM ${BaseDao.TABLE_NAME}")
    suspend fun count(): Int

    /**
     * Check if the table is empty
     * @return true if the table is empty, false otherwise
     */
    @Query("SELECT COUNT(*) FROM ${BaseDao.TABLE_NAME}")
    suspend fun isEmpty(): Boolean = count() == 0

    /**
     * Get all items as a Flow
     * @return Flow of list of all items
     */
    @Query("SELECT * FROM ${BaseDao.TABLE_NAME}")
    fun getAllAsFlow(): Flow<List<T>>

    /**
     * Get all items
     * @return list of all items
     */
    @Query("SELECT * FROM ${BaseDao.TABLE_NAME}")
    suspend fun getAll(): List<T>

    companion object {
        const val TABLE_NAME = "override_table_name_in_implementing_dao"
    }
}

/**
 * Extension function to perform database operation within a transaction
 */
@Transaction
suspend inline fun <T> BaseDao<T>.withTransaction(crossinline block: suspend () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        throw e
    }
}

/**
 * Extension function to perform safe database operation with error handling
 */
suspend inline fun <T, R> BaseDao<T>.safeCall(crossinline block: suspend () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Extension function to perform database operation with retry mechanism
 */
suspend inline fun <T, R> BaseDao<T>.withRetry(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    crossinline block: suspend () -> R
): R {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            kotlinx.coroutines.delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return block() // last attempt
}

/**
 * Extension function to perform database operation with timeout
 */
suspend inline fun <T, R> BaseDao<T>.withTimeout(
    timeMillis: Long,
    crossinline block: suspend () -> R
): R = kotlinx.coroutines.withTimeout(timeMillis) {
    block()
}

/**
 * Extension function to perform database operation with result mapping
 */
suspend inline fun <T, R, M> BaseDao<T>.mapResult(
    crossinline block: suspend () -> R,
    crossinline mapper: (R) -> M
): Result<M> {
    return try {
        val result = block()
        Result.success(mapper(result))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
