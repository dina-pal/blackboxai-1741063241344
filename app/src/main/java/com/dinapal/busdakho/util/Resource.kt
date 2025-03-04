package com.dinapal.busdakho.util

sealed class Resource<T>(
    val data: T? = null,
    val error: Throwable? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data = data)
    class Error<T>(
        error: Throwable? = null,
        message: String? = null,
        data: T? = null
    ) : Resource<T>(
        error = error,
        message = message,
        data = data
    )
    class Loading<T>(data: T? = null) : Resource<T>(data = data)

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun <R> map(transform: (T?) -> R): Resource<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> Error(error, message, transform(data))
            is Loading -> Loading(transform(data))
        }
    }

    companion object {
        fun <T> success(data: T): Resource<T> = Success(data)
        
        fun <T> error(
            message: String? = null,
            error: Throwable? = null,
            data: T? = null
        ): Resource<T> = Error(error, message, data)
        
        fun <T> loading(data: T? = null): Resource<T> = Loading(data)

        fun <T> fromResult(result: Result<T>): Resource<T> {
            return result.fold(
                onSuccess = { Success(it) },
                onFailure = { Error(error = it, message = it.message) }
            )
        }
    }

    suspend fun onSuccess(action: suspend (T) -> Unit): Resource<T> {
        if (this is Success && data != null) {
            action(data)
        }
        return this
    }

    suspend fun onError(action: suspend (Throwable?) -> Unit): Resource<T> {
        if (this is Error) {
            action(error)
        }
        return this
    }

    suspend fun onLoading(action: suspend () -> Unit): Resource<T> {
        if (this is Loading) {
            action()
        }
        return this
    }

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrDefault(defaultValue: T): T = when (this) {
        is Success -> data ?: defaultValue
        else -> defaultValue
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data ?: throw IllegalStateException("Data is null")
        is Error -> throw error ?: IllegalStateException(message ?: "Unknown error")
        is Loading -> throw IllegalStateException("Resource is in loading state")
    }
}

sealed class NetworkError : Exception() {
    object NoInternet : NetworkError()
    object ServerError : NetworkError()
    object Timeout : NetworkError()
    data class Unknown(override val message: String?) : NetworkError()
    data class ApiError(
        override val message: String?,
        val code: Int,
        val errorBody: String?
    ) : NetworkError()
}

sealed class ValidationError : Exception() {
    object EmptyField : ValidationError()
    object InvalidEmail : ValidationError()
    object InvalidPhone : ValidationError()
    object InvalidPassword : ValidationError()
    object PasswordMismatch : ValidationError()
    data class Custom(override val message: String) : ValidationError()
}

fun <T> Result<T>.toResource(): Resource<T> = fold(
    onSuccess = { Resource.success(it) },
    onFailure = { Resource.error(error = it, message = it.message) }
)

suspend fun <T> safeApiCall(
    apiCall: suspend () -> T
): Resource<T> = try {
    Resource.success(apiCall())
} catch (e: Exception) {
    when (e) {
        is java.net.UnknownHostException -> Resource.error(
            error = NetworkError.NoInternet,
            message = "No internet connection"
        )
        is java.net.SocketTimeoutException -> Resource.error(
            error = NetworkError.Timeout,
            message = "Request timed out"
        )
        else -> Resource.error(
            error = NetworkError.Unknown(e.message),
            message = e.message
        )
    }
}

fun validateEmail(email: String): Result<Unit> = when {
    email.isEmpty() -> Result.failure(ValidationError.EmptyField)
    !email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$")) -> 
        Result.failure(ValidationError.InvalidEmail)
    else -> Result.success(Unit)
}

fun validatePhone(phone: String): Result<Unit> = when {
    phone.isEmpty() -> Result.failure(ValidationError.EmptyField)
    !phone.matches(Regex("^[+]?[0-9]{10,13}\$")) -> 
        Result.failure(ValidationError.InvalidPhone)
    else -> Result.success(Unit)
}

fun validatePassword(password: String): Result<Unit> = when {
    password.isEmpty() -> Result.failure(ValidationError.EmptyField)
    password.length < 8 -> Result.failure(ValidationError.InvalidPassword)
    !password.matches(Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=]).*\$")) ->
        Result.failure(ValidationError.InvalidPassword)
    else -> Result.success(Unit)
}

fun validatePasswordMatch(password: String, confirmPassword: String): Result<Unit> = when {
    password != confirmPassword -> Result.failure(ValidationError.PasswordMismatch)
    else -> Result.success(Unit)
}
