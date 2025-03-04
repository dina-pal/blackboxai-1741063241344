package com.dinapal.busdakho.error

import android.content.Context
import com.dinapal.busdakho.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Business logic errors
 */
sealed class BusinessError : BaseError() {
    data class InvalidOperation(
        override val userMessage: String,
        override val technicalMessage: String? = null
    ) : BusinessError() {
        override val code: String = "INVALID_OPERATION"
    }

    data class ResourceConflict(
        override val userMessage: String,
        override val technicalMessage: String? = null,
        override val data: Map<String, Any>? = null
    ) : BusinessError() {
        override val code: String = "RESOURCE_CONFLICT"
    }

    data class BusinessRuleViolation(
        override val userMessage: String,
        override val technicalMessage: String? = null,
        val rule: String
    ) : BusinessError() {
        override val code: String = "BUSINESS_RULE_VIOLATION"
    }
}

/**
 * Cache related errors
 */
sealed class CacheError : BaseError() {
    object CacheEmpty : CacheError() {
        override val code: String = "CACHE_EMPTY"
        override val userMessage: String = "No cached data available"
    }

    object CacheExpired : CacheError() {
        override val code: String = "CACHE_EXPIRED"
        override val userMessage: String = "Cached data has expired"
    }

    data class CacheWriteError(
        override val technicalMessage: String? = null
    ) : CacheError() {
        override val code: String = "CACHE_WRITE_ERROR"
        override val userMessage: String = "Failed to save data to cache"
    }
}

/**
 * Database errors
 */
sealed class DatabaseError : BaseError() {
    data class QueryError(
        override val userMessage: String = "Database query failed",
        override val technicalMessage: String? = null
    ) : DatabaseError() {
        override val code: String = "DB_QUERY_ERROR"
    }

    data class TransactionError(
        override val userMessage: String = "Database transaction failed",
        override val technicalMessage: String? = null
    ) : DatabaseError() {
        override val code: String = "DB_TRANSACTION_ERROR"
    }

    object ConnectionError : DatabaseError() {
        override val code: String = "DB_CONNECTION_ERROR"
        override val userMessage: String = "Database connection failed"
    }
}

/**
 * Error manager to handle different types of errors
 */
class ErrorManager(private val context: Context) {
    private val errorHandler = ErrorHandler.getInstance()
    private val strategies = mutableListOf<ErrorHandlingStrategy>()

    init {
        // Add default strategies
        addStrategy(LoggingErrorHandlingStrategy())
        addStrategy(DefaultErrorHandlingStrategy())
    }

    fun addStrategy(strategy: ErrorHandlingStrategy) {
        strategies.add(strategy)
    }

    fun handleError(error: BaseError) {
        CompositeErrorHandlingStrategy(strategies).handleError(error)
    }

    fun handleException(throwable: Throwable) {
        handleError(throwable.toBaseError())
    }

    fun getErrorMessage(error: BaseError): String {
        return when (error) {
            is NetworkError -> handleNetworkError(error)
            is AuthError -> handleAuthError(error)
            is DataError -> handleDataError(error)
            is BusinessError -> handleBusinessError(error)
            else -> error.userMessage
        }
    }

    private fun handleNetworkError(error: NetworkError): String {
        return when (error) {
            is NetworkError.NoInternet -> "Please check your internet connection"
            is NetworkError.Timeout -> "Request timed out. Please try again"
            is NetworkError.ServerError -> "Server error occurred. Please try again later"
            is NetworkError.ApiError -> error.userMessage
        }
    }

    private fun handleAuthError(error: AuthError): String {
        return when (error) {
            is AuthError.Unauthorized -> "Please log in to continue"
            is AuthError.InvalidCredentials -> "Invalid username or password"
            is AuthError.TokenExpired -> "Session expired. Please log in again"
            is AuthError.SessionExpired -> "Your session has expired. Please log in again"
        }
    }

    private fun handleDataError(error: DataError): String {
        return when (error) {
            is DataError.NotFound -> "Requested data not found"
            is DataError.InvalidData -> "Invalid data provided"
            is DataError.ValidationError -> error.userMessage
        }
    }

    private fun handleBusinessError(error: BusinessError): String {
        return when (error) {
            is BusinessError.InvalidOperation -> error.userMessage
            is BusinessError.ResourceConflict -> error.userMessage
            is BusinessError.BusinessRuleViolation -> error.userMessage
        }
    }

    companion object {
        @Volatile
        private var instance: ErrorManager? = null

        fun getInstance(context: Context): ErrorManager {
            return instance ?: synchronized(this) {
                instance ?: ErrorManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Extension functions for error handling
 */
fun <T> Flow<T>.handleErrors(
    errorManager: ErrorManager,
    onError: (BaseError) -> Unit = {}
): Flow<T> = this
    .catch { throwable ->
        val error = throwable.toBaseError()
        errorManager.handleError(error)
        onError(error)
        throw throwable
    }

fun <T> Flow<T>.logErrors(tag: String): Flow<T> = this
    .onEach { Logger.d(tag, "Emitting value: $it") }
    .catch { throwable ->
        Logger.e(tag, "Error in flow", throwable)
        throw throwable
    }

fun <T, R> Flow<T>.mapWithError(
    transform: (T) -> R,
    onError: (Throwable) -> R
): Flow<R> = this
    .map { try { transform(it) } catch (e: Throwable) { onError(e) } }

fun Context.getErrorManager(): ErrorManager = ErrorManager.getInstance(this)

fun Context.handleError(error: BaseError) = getErrorManager().handleError(error)

fun Context.handleException(throwable: Throwable) = getErrorManager().handleException(throwable)

fun Context.getErrorMessage(error: BaseError): String = getErrorManager().getErrorMessage(error)
