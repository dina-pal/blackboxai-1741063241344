package com.dinapal.busdakho.error

import com.dinapal.busdakho.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base class for all errors in the application
 */
sealed class BaseError : Exception() {
    abstract val code: String
    abstract val userMessage: String
    open val technicalMessage: String? = null
    open val data: Map<String, Any>? = null

    override val message: String
        get() = userMessage
}

/**
 * Network related errors
 */
sealed class NetworkError : BaseError() {
    object NoInternet : NetworkError() {
        override val code: String = "NO_INTERNET"
        override val userMessage: String = "No internet connection available"
    }

    object Timeout : NetworkError() {
        override val code: String = "TIMEOUT"
        override val userMessage: String = "Request timed out"
    }

    object ServerError : NetworkError() {
        override val code: String = "SERVER_ERROR"
        override val userMessage: String = "Server error occurred"
    }

    data class ApiError(
        override val code: String,
        override val userMessage: String,
        override val technicalMessage: String? = null,
        val statusCode: Int,
        override val data: Map<String, Any>? = null
    ) : NetworkError()
}

/**
 * Authentication related errors
 */
sealed class AuthError : BaseError() {
    object Unauthorized : AuthError() {
        override val code: String = "UNAUTHORIZED"
        override val userMessage: String = "Please log in to continue"
    }

    object InvalidCredentials : AuthError() {
        override val code: String = "INVALID_CREDENTIALS"
        override val userMessage: String = "Invalid username or password"
    }

    object TokenExpired : AuthError() {
        override val code: String = "TOKEN_EXPIRED"
        override val userMessage: String = "Session expired, please log in again"
    }

    object SessionExpired : AuthError() {
        override val code: String = "SESSION_EXPIRED"
        override val userMessage: String = "Your session has expired"
    }
}

/**
 * Data related errors
 */
sealed class DataError : BaseError() {
    object NotFound : DataError() {
        override val code: String = "NOT_FOUND"
        override val userMessage: String = "Requested data not found"
    }

    object InvalidData : DataError() {
        override val code: String = "INVALID_DATA"
        override val userMessage: String = "Invalid data provided"
    }

    data class ValidationError(
        override val userMessage: String,
        val field: String? = null,
        override val data: Map<String, Any>? = null
    ) : DataError() {
        override val code: String = "VALIDATION_ERROR"
    }
}

/**
 * Permission related errors
 */
sealed class PermissionError : BaseError() {
    data class Missing(
        val permission: String,
        override val userMessage: String = "Required permission not granted"
    ) : PermissionError() {
        override val code: String = "PERMISSION_MISSING"
    }

    data class Denied(
        val permission: String,
        override val userMessage: String = "Permission denied"
    ) : PermissionError() {
        override val code: String = "PERMISSION_DENIED"
    }
}

/**
 * Location related errors
 */
sealed class LocationError : BaseError() {
    object Disabled : LocationError() {
        override val code: String = "LOCATION_DISABLED"
        override val userMessage: String = "Location services are disabled"
    }

    object NoGps : LocationError() {
        override val code: String = "NO_GPS"
        override val userMessage: String = "GPS signal not found"
    }

    object Timeout : LocationError() {
        override val code: String = "LOCATION_TIMEOUT"
        override val userMessage: String = "Location request timed out"
    }
}

/**
 * Error handler to manage errors across the app
 */
class ErrorHandler {
    private val _currentError = MutableStateFlow<BaseError?>(null)
    val currentError = _currentError.asStateFlow()

    fun handleError(error: BaseError) {
        Logger.e("ErrorHandler", "Error occurred: ${error.code}", error)
        _currentError.value = error
    }

    fun clearError() {
        _currentError.value = null
    }

    companion object {
        @Volatile
        private var instance: ErrorHandler? = null

        fun getInstance(): ErrorHandler {
            return instance ?: synchronized(this) {
                instance ?: ErrorHandler().also { instance = it }
            }
        }
    }
}

/**
 * Extension functions for error handling
 */
fun Throwable.toBaseError(): BaseError {
    return when (this) {
        is BaseError -> this
        is java.net.UnknownHostException -> NetworkError.NoInternet
        is java.net.SocketTimeoutException -> NetworkError.Timeout
        else -> DataError.InvalidData
    }
}

fun BaseError.log() {
    Logger.e(
        "Error",
        """
        Error occurred:
        Code: $code
        User Message: $userMessage
        Technical Message: $technicalMessage
        Data: $data
        """.trimIndent(),
        this
    )
}

/**
 * Interface for error handling strategies
 */
interface ErrorHandlingStrategy {
    fun handleError(error: BaseError)
}

/**
 * Default error handling strategy
 */
class DefaultErrorHandlingStrategy : ErrorHandlingStrategy {
    override fun handleError(error: BaseError) {
        ErrorHandler.getInstance().handleError(error)
    }
}

/**
 * Logging error handling strategy
 */
class LoggingErrorHandlingStrategy : ErrorHandlingStrategy {
    override fun handleError(error: BaseError) {
        error.log()
    }
}

/**
 * Composite error handling strategy
 */
class CompositeErrorHandlingStrategy(
    private val strategies: List<ErrorHandlingStrategy>
) : ErrorHandlingStrategy {
    override fun handleError(error: BaseError) {
        strategies.forEach { it.handleError(error) }
    }
}
