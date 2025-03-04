package com.dinapal.busdakho.data.remote.interceptor

import com.dinapal.busdakho.data.preferences.PreferencesManager
import com.dinapal.busdakho.util.Constants
import com.dinapal.busdakho.util.NetworkConnectivityManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Interceptor to handle authentication
 */
class AuthInterceptor(
    private val preferencesManager: PreferencesManager
) : BaseInterceptor() {
    override fun modifyRequest(builder: Request.Builder): Request.Builder {
        val token = runBlocking { preferencesManager.authToken.first() }
        return if (!token.isNullOrEmpty()) {
            builder.addHeader("Authorization", "Bearer $token")
        } else {
            builder
        }
    }
}

/**
 * Interceptor to handle network connectivity
 */
class ConnectivityInterceptor(
    private val networkManager: NetworkConnectivityManager
) : BaseInterceptor() {
    override fun executeRequest(chain: Interceptor.Chain, request: Request): Response {
        if (!networkManager.isNetworkAvailable()) {
            throw NoConnectivityException()
        }
        return chain.proceed(request)
    }
}

/**
 * Interceptor to add common headers
 */
class HeaderInterceptor : BaseInterceptor() {
    override fun modifyRequest(builder: Request.Builder): Request.Builder {
        return builder
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "BusDakho-Android/${Constants.APP_VERSION}")
    }
}

/**
 * Interceptor to handle caching
 */
class CacheInterceptor(
    private val networkManager: NetworkConnectivityManager
) : BaseInterceptor() {
    override fun modifyResponse(response: Response): Response {
        val cacheControl = if (networkManager.isNetworkAvailable()) {
            "public, max-age=${Constants.CACHE_MAX_AGE}"
        } else {
            "public, only-if-cached, max-stale=${Constants.CACHE_MAX_STALE}"
        }

        return response.newBuilder()
            .header("Cache-Control", cacheControl)
            .build()
    }
}

/**
 * Interceptor to handle request timeouts
 */
class TimeoutInterceptor : BaseInterceptor() {
    override fun executeRequest(chain: Interceptor.Chain, request: Request): Response {
        return chain
            .withConnectTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .withReadTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .withWriteTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .proceed(request)
    }
}

/**
 * Interceptor to handle request retries
 */
class RetryInterceptor(
    private val maxRetries: Int = Constants.RETRY_ATTEMPTS
) : BaseInterceptor() {
    override fun executeRequest(chain: Interceptor.Chain, request: Request): Response {
        var response: Response? = null
        var retryCount = 0
        var exception: Exception? = null

        while (retryCount < maxRetries && (response == null || !response.isSuccessful)) {
            try {
                response?.close()
                response = chain.proceed(request)
                if (response.isSuccessful) {
                    return response
                }
            } catch (e: Exception) {
                exception = e
                retryCount++
                if (retryCount == maxRetries) {
                    throw e
                }
                // Exponential backoff
                Thread.sleep(getBackoffDelay(retryCount))
            }
        }

        throw exception ?: IllegalStateException("Unexpected retry failure")
    }

    private fun getBackoffDelay(retryCount: Int): Long {
        return (Math.pow(2.0, retryCount.toDouble()) * 1000).toLong()
            .coerceAtMost(Constants.MAX_RETRY_DELAY)
    }
}

/**
 * Interceptor to handle API versioning
 */
class ApiVersionInterceptor : BaseInterceptor() {
    override fun modifyRequest(builder: Request.Builder): Request.Builder {
        return builder.addHeader("Api-Version", Constants.API_VERSION)
    }
}

/**
 * Interceptor to handle request/response compression
 */
class CompressionInterceptor : BaseInterceptor() {
    override fun modifyRequest(builder: Request.Builder): Request.Builder {
        return builder.addHeader("Accept-Encoding", "gzip")
    }
}

/**
 * Custom exceptions
 */
class NoConnectivityException : java.io.IOException() {
    override val message: String
        get() = "No network connection"
}

class ApiVersionException : java.io.IOException() {
    override val message: String
        get() = "Incompatible API version"
}

class UnauthorizedException : java.io.IOException() {
    override val message: String
        get() = "Unauthorized access"
}

/**
 * Extension function to create a chain of interceptors
 */
fun List<Interceptor>.chain(): Interceptor {
    return Interceptor { chain ->
        var request = chain.request()
        forEach { interceptor ->
            request = interceptor.intercept(
                object : Interceptor.Chain {
                    override fun request(): Request = request
                    override fun proceed(request: Request): Response = chain.proceed(request)
                    override fun connection() = chain.connection()
                    override fun call() = chain.call()
                    override fun connectTimeoutMillis() = chain.connectTimeoutMillis()
                    override fun withConnectTimeout(timeout: Int, unit: TimeUnit) =
                        chain.withConnectTimeout(timeout, unit)
                    override fun readTimeoutMillis() = chain.readTimeoutMillis()
                    override fun withReadTimeout(timeout: Int, unit: TimeUnit) =
                        chain.withReadTimeout(timeout, unit)
                    override fun writeTimeoutMillis() = chain.writeTimeoutMillis()
                    override fun withWriteTimeout(timeout: Int, unit: TimeUnit) =
                        chain.withWriteTimeout(timeout, unit)
                }
            ).request()
        }
        chain.proceed(request)
    }
}
