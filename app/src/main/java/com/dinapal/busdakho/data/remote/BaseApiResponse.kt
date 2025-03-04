package com.dinapal.busdakho.data.remote

import com.dinapal.busdakho.util.Logger
import com.dinapal.busdakho.util.NetworkError
import com.dinapal.busdakho.util.Resource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import java.io.IOException

@Serializable
sealed class BaseApiResponse<T> {
    @Serializable
    data class Success<T>(
        @SerialName("data") val data: T,
        @SerialName("message") val message: String? = null
    ) : BaseApiResponse<T>()

    @Serializable
    data class Error<T>(
        @SerialName("error") val error: ApiError,
        @SerialName("data") val data: T? = null
    ) : BaseApiResponse<T>()
}

@Serializable
data class ApiError(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String,
    @SerialName("details") val details: Map<String, String>? = null
)

@Serializable
data class ApiResponse<T>(
    @SerialName("status") val status: Boolean,
    @SerialName("data") val data: T? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("error") val error: ApiError? = null
)

/**
 * Abstract base class for API response handling
 */
abstract class BaseRemoteDataSource {
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<ApiResponse<T>>
    ): Resource<T> {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                body?.let { apiResponse ->
                    return when {
                        apiResponse.status && apiResponse.data != null -> {
                            Resource.Success(apiResponse.data)
                        }
                        !apiResponse.status && apiResponse.error != null -> {
                            Resource.Error(
                                error = NetworkError.ApiError(
                                    message = apiResponse.error.message,
                                    code = apiResponse.error.code,
                                    errorBody = apiResponse.error.details?.toString()
                                )
                            )
                        }
                        else -> {
                            Resource.Error(
                                error = NetworkError.Unknown("Invalid response format")
                            )
                        }
                    }
                }
            }
            
            return Resource.Error(
                error = NetworkError.ApiError(
                    message = response.message(),
                    code = response.code(),
                    errorBody = response.errorBody()?.string()
                )
            )
        } catch (e: Exception) {
            Logger.e("BaseRemoteDataSource", "API call failed", e)
            return when (e) {
                is IOException -> Resource.Error(
                    error = NetworkError.NoInternet,
                    message = "No internet connection"
                )
                is retrofit2.HttpException -> Resource.Error(
                    error = NetworkError.ServerError,
                    message = "Server error occurred"
                )
                else -> Resource.Error(
                    error = NetworkError.Unknown(e.message),
                    message = e.message ?: "An unknown error occurred"
                )
            }
        }
    }

    protected suspend fun <T> safeApiCallWithRetry(
        times: Int = 3,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        apiCall: suspend () -> Response<ApiResponse<T>>
    ): Resource<T> {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            when (val result = safeApiCall(apiCall)) {
                is Resource.Success -> return result
                is Resource.Error -> {
                    if (shouldRetry(result.error)) {
                        kotlinx.coroutines.delay(currentDelay)
                        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                    } else {
                        return result
                    }
                }
                is Resource.Loading -> {} // Should not happen in this context
            }
        }
        return safeApiCall(apiCall) // last attempt
    }

    private fun shouldRetry(error: Throwable?): Boolean {
        return when (error) {
            is NetworkError.NoInternet,
            is NetworkError.Timeout,
            is NetworkError.ServerError -> true
            is NetworkError.ApiError -> error.code in 500..599
            else -> false
        }
    }
}

/**
 * Extension function to convert Response<T> to Resource<T>
 */
fun <T> Response<T>.toResource(): Resource<T> {
    return try {
        if (isSuccessful) {
            body()?.let {
                Resource.Success(it)
            } ?: Resource.Error(
                error = NetworkError.Unknown("Response body is null"),
                message = "Empty response"
            )
        } else {
            Resource.Error(
                error = NetworkError.ApiError(
                    message = message(),
                    code = code(),
                    errorBody = errorBody()?.string()
                )
            )
        }
    } catch (e: Exception) {
        Resource.Error(
            error = NetworkError.Unknown(e.message),
            message = e.message ?: "An unknown error occurred"
        )
    }
}

/**
 * Extension function to handle API pagination
 */
@Serializable
data class PaginatedResponse<T>(
    @SerialName("data") val data: List<T>,
    @SerialName("page") val page: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_items") val totalItems: Int
)

/**
 * Extension function to handle API metadata
 */
@Serializable
data class MetadataResponse<T>(
    @SerialName("data") val data: T,
    @SerialName("metadata") val metadata: Map<String, String>
)
