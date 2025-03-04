package com.dinapal.busdakho.dispatcher

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Base class for handling coroutine dispatching
 */
abstract class BaseDispatcher {
    abstract val main: CoroutineDispatcher
    abstract val io: CoroutineDispatcher
    abstract val default: CoroutineDispatcher
    abstract val unconfined: CoroutineDispatcher

    /**
     * Scope for UI-related coroutines
     */
    abstract val mainScope: CoroutineScope

    /**
     * Scope for background operations
     */
    abstract val backgroundScope: CoroutineScope

    /**
     * Execute on main thread
     */
    suspend fun <T> onMain(block: suspend CoroutineScope.() -> T): T =
        withContext(main, block)

    /**
     * Execute on IO thread
     */
    suspend fun <T> onIO(block: suspend CoroutineScope.() -> T): T =
        withContext(io, block)

    /**
     * Execute on default thread
     */
    suspend fun <T> onDefault(block: suspend CoroutineScope.() -> T): T =
        withContext(default, block)

    /**
     * Execute with error handling
     */
    suspend fun <T> withErrorHandling(
        context: CoroutineContext = io,
        block: suspend CoroutineScope.() -> T,
        onError: suspend (Exception) -> Unit
    ): T? {
        return try {
            withContext(context) {
                block()
            }
        } catch (e: Exception) {
            onError(e)
            null
        }
    }

    /**
     * Launch a coroutine with error handling
     */
    fun launchWithErrorHandling(
        context: CoroutineContext = io,
        onError: suspend (Exception) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return backgroundScope.launch(context) {
            try {
                block()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /**
     * Execute multiple operations in parallel
     */
    suspend fun <T> parallel(
        dispatchers: List<CoroutineDispatcher> = listOf(io),
        block: suspend CoroutineScope.() -> T
    ): List<T> = coroutineScope {
        dispatchers.map { dispatcher ->
            async(dispatcher) {
                block()
            }
        }.awaitAll()
    }

    /**
     * Execute with retry
     */
    suspend fun <T> withRetry(
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
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return block() // last attempt
    }

    /**
     * Execute with timeout
     */
    suspend fun <T> withTimeout(
        timeMillis: Long,
        block: suspend CoroutineScope.() -> T
    ): T = kotlinx.coroutines.withTimeout(timeMillis) {
        block()
    }

    /**
     * Execute with cancellation
     */
    suspend fun <T> withCancellation(
        block: suspend CoroutineScope.() -> T
    ): T = coroutineScope {
        val job = async { block() }
        try {
            job.await()
        } catch (e: CancellationException) {
            job.cancel()
            throw e
        }
    }

    /**
     * Clean up resources
     */
    open fun cleanup() {
        mainScope.cancel()
        backgroundScope.cancel()
    }
}

/**
 * Interface for dispatcher provider
 */
interface DispatcherProvider {
    fun provideDispatcher(): BaseDispatcher
}

/**
 * Exception for dispatcher errors
 */
class DispatcherException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Extension function to run on main thread
 */
suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Main, block)

/**
 * Extension function to run on IO thread
 */
suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.IO, block)

/**
 * Extension function to run on default thread
 */
suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Default, block)

/**
 * Extension function to run unconfined
 */
suspend fun <T> withUnconfinedContext(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Unconfined, block)

/**
 * Extension function for parallel execution
 */
suspend fun <T> Collection<T>.forEachParallel(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    block: suspend (T) -> Unit
) {
    coroutineScope {
        map { item ->
            async(dispatcher) {
                block(item)
            }
        }.awaitAll()
    }
}

/**
 * Extension function for parallel map
 */
suspend fun <T,
