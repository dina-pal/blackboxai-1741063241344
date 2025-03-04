package com.dinapal.busdakho.domain.usecase

import com.dinapal.busdakho.util.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

/**
 * Base class for Use Cases that return a single value
 */
abstract class BaseUseCase<in P, R>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(parameters: P): Resource<R> {
        return try {
            withContext(dispatcher) {
                execute(parameters).let { result ->
                    Resource.Success(result)
                }
            }
        } catch (e: Exception) {
            Resource.Error(error = e, message = e.message)
        }
    }

    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): R
}

/**
 * Base class for Use Cases that return a Flow
 */
abstract class BaseFlowUseCase<in P, R>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    operator fun invoke(parameters: P): Flow<Resource<R>> {
        return execute(parameters)
            .map<R, Resource<R>> { Resource.Success(it) }
            .onStart { emit(Resource.Loading()) }
            .catch { e -> emit(Resource.Error(error = e, message = e.message)) }
            .flowOn(dispatcher)
    }

    protected abstract fun execute(parameters: P): Flow<R>
}

/**
 * Base class for Use Cases that don't require parameters
 */
abstract class BaseNoParamsUseCase<R>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(): Resource<R> {
        return try {
            withContext(dispatcher) {
                execute().let { result ->
                    Resource.Success(result)
                }
            }
        } catch (e: Exception) {
            Resource.Error(error = e, message = e.message)
        }
    }

    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(): R
}

/**
 * Base class for Use Cases that don't require parameters and return a Flow
 */
abstract class BaseNoParamsFlowUseCase<R>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    operator fun invoke(): Flow<Resource<R>> {
        return execute()
            .map<R, Resource<R>> { Resource.Success(it) }
            .onStart { emit(Resource.Loading()) }
            .catch { e -> emit(Resource.Error(error = e, message = e.message)) }
            .flowOn(dispatcher)
    }

    protected abstract fun execute(): Flow<R>
}

/**
 * Base class for Use Cases that perform operations without returning a value
 */
abstract class BaseCompletableUseCase<in P>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(parameters: P): Resource<Unit> {
        return try {
            withContext(dispatcher) {
                execute(parameters)
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            Resource.Error(error = e, message = e.message)
        }
    }

    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P)
}

/**
 * Wrapper class for use case parameters
 */
abstract class UseCaseParams

/**
 * Object to represent empty parameters
 */
object None : UseCaseParams()

/**
 * Extension functions for Resource handling
 */
suspend fun <T> Resource<T>.onSuccess(action: suspend (T) -> Unit): Resource<T> {
    if (this is Resource.Success) {
        action(data!!)
    }
    return this
}

suspend fun <T> Resource<T>.onError(action: suspend (String?) -> Unit): Resource<T> {
    if (this is Resource.Error) {
        action(message)
    }
    return this
}

suspend fun <T> Resource<T>.onLoading(action: suspend () -> Unit): Resource<T> {
    if (this is Resource.Loading) {
        action()
    }
    return this
}

/**
 * Extension function to map Resource data
 */
fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> {
    return when (this) {
        is Resource.Success -> Resource.Success(transform(data!!))
        is Resource.Error -> Resource.Error(message, error, data?.let { transform(it) })
        is Resource.Loading -> Resource.Loading(data?.let { transform(it) })
    }
}

/**
 * Extension function to combine multiple Resources
 */
fun <T1, T2, R> combine(
    resource1: Resource<T1>,
    resource2: Resource<T2>,
    transform: (T1, T2) -> R
): Resource<R> {
    return when {
        resource1 is Resource.Error -> Resource.Error(resource1.message, resource1.error)
        resource2 is Resource.Error -> Resource.Error(resource2.message, resource2.error)
        resource1 is Resource.Loading || resource2 is Resource.Loading -> Resource.Loading()
        resource1 is Resource.Success && resource2 is Resource.Success ->
            Resource.Success(transform(resource1.data!!, resource2.data!!))
        else -> Resource.Error("Invalid state")
    }
}

/**
 * Extension function to retry a use case execution
 */
suspend fun <P, R> BaseUseCase<P, R>.retry(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    parameters: P
): Resource<R> {
    var currentDelay = initialDelay
    repeat(times - 1) { attempt ->
        val result = invoke(parameters)
        if (result is Resource.Success) {
            return result
        }
        kotlinx.coroutines.delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return invoke(parameters) // last attempt
}
