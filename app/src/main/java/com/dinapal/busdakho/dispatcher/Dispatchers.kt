package com.dinapal.busdakho.dispatcher

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Default implementation of BaseDispatcher
 */
class AppDispatcher : BaseDispatcher() {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined

    override val mainScope: CoroutineScope = CoroutineScope(SupervisorJob() + main)
    override val backgroundScope: CoroutineScope = CoroutineScope(SupervisorJob() + io)
}

/**
 * Test dispatcher implementation
 */
class TestDispatcher : BaseDispatcher() {
    private val testDispatcher = kotlinx.coroutines.test.TestCoroutineDispatcher()

    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher

    override val mainScope: CoroutineScope = CoroutineScope(SupervisorJob() + testDispatcher)
    override val backgroundScope: CoroutineScope = CoroutineScope(SupervisorJob() + testDispatcher)

    override fun cleanup() {
        super.cleanup()
        testDispatcher.cleanupTestCoroutines()
    }
}

/**
 * Dispatcher provider implementation
 */
class DefaultDispatcherProvider : DispatcherProvider {
    override fun provideDispatcher(): BaseDispatcher = AppDispatcher()
}

/**
 * Dispatcher manager to handle dispatchers across the app
 */
class DispatcherManager private constructor() {
    private var dispatcher: BaseDispatcher = AppDispatcher()

    fun getDispatcher(): BaseDispatcher = dispatcher

    fun setDispatcher(newDispatcher: BaseDispatcher) {
        dispatcher.cleanup()
        dispatcher = newDispatcher
    }

    companion object {
        @Volatile
        private var instance: DispatcherManager? = null

        fun getInstance(): DispatcherManager {
            return instance ?: synchronized(this) {
                instance ?: DispatcherManager().also { instance = it }
            }
        }
    }
}

/**
 * Scoped dispatcher for specific use cases
 */
class ScopedDispatcher(
    private val parentScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(parentScope.coroutineContext + dispatcher)

    fun launch(
        context: CoroutineContext = dispatcher,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return scope.launch(context, start, block)
    }

    fun <T> async(
        context: CoroutineContext = dispatcher,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        return scope.async(context, start, block)
    }

    fun cancel() {
        scope.cancel()
    }
}

/**
 * Extension functions for coroutine scopes
 */
fun CoroutineScope.launchIO(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return launch(Dispatchers.IO, start, block)
}

fun CoroutineScope.launchMain(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return launch(Dispatchers.Main, start, block)
}

fun <T> CoroutineScope.asyncIO(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    return async(Dispatchers.IO, start, block)
}

fun <T> CoroutineScope.asyncMain(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    return async(Dispatchers.Main, start, block)
}

/**
 * Extension function for parallel map
 */
suspend fun <T, R> Iterable<T>.parallelMap(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    block: suspend (T) -> R
): List<R> = coroutineScope {
    map { item ->
        async(dispatcher) {
            block(item)
        }
    }.awaitAll()
}

/**
 * Extension function for parallel filter
 */
suspend fun <T> Iterable<T>.parallelFilter(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    predicate: suspend (T) -> Boolean
): List<T> = coroutineScope {
    map { item ->
        async(dispatcher) {
            item to predicate(item)
        }
    }.awaitAll()
        .filter { it.second }
        .map { it.first }
}

/**
 * Extension function to get dispatcher manager
 */
fun getDispatcherManager(): DispatcherManager = DispatcherManager.getInstance()

/**
 * Extension function to get current dispatcher
 */
fun getCurrentDispatcher(): BaseDispatcher = getDispatcherManager().getDispatcher()
