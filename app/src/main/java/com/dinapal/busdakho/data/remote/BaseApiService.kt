package com.dinapal.busdakho.data.remote

import com.dinapal.busdakho.util.Constants
import com.dinapal.busdakho.util.Logger
import com.dinapal.busdakho.util.NetworkConnectivityManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

interface BaseApiService {
    companion object {
        inline fun <reified T> createService(
            baseUrl: String = Constants.API_BASE_URL,
            networkManager: NetworkConnectivityManager,
            tokenProvider: () -> String? = { null },
            noinline headerProvider: (() -> Map<String, String>)? = null
        ): T {
            val okHttpClient = createOkHttpClient(networkManager, tokenProvider, headerProvider)
            
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(T::class.java)
        }

        fun createOkHttpClient(
            networkManager: NetworkConnectivityManager,
            tokenProvider: () -> String?,
            headerProvider: (() -> Map<String, String>)? = null
        ): OkHttpClient {
            return OkHttpClient.Builder().apply {
                // Timeouts
                connectTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
                readTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
                writeTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)

                // Add interceptors
                addInterceptor(createNetworkInterceptor(networkManager))
                addInterceptor(createAuthInterceptor(tokenProvider))
                addInterceptor(createHeaderInterceptor(headerProvider))
                addInterceptor(createLoggingInterceptor())

                // Error handling interceptor
                addInterceptor(createErrorInterceptor())
            }.build()
        }

        private fun createNetworkInterceptor(
            networkManager: NetworkConnectivityManager
        ): Interceptor {
            return Interceptor { chain ->
                if (!networkManager.isNetworkAvailable()) {
                    throw NoConnectivityException()
                }
                chain.proceed(chain.request())
            }
        }

        private fun createAuthInterceptor(tokenProvider: () -> String?): Interceptor {
            return Interceptor { chain ->
                val request = chain.request()
                val token = tokenProvider()

                val newRequest = if (token != null) {
                    request.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    request
                }

                chain.proceed(newRequest)
            }
        }

        private fun createHeaderInterceptor(
            headerProvider: (() -> Map<String, String>)?
        ): Interceptor {
            return Interceptor { chain ->
                val request = chain.request()
                val headers = headerProvider?.invoke() ?: emptyMap()

                val newRequest = request.newBuilder().apply {
                    headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                    // Add default headers
                    addHeader("Accept", "application/json")
                    addHeader("Content-Type", "application/json")
                }.build()

                chain.proceed(newRequest)
            }
        }

        private fun createLoggingInterceptor(): HttpLoggingInterceptor {
            return HttpLoggingInterceptor { message ->
                Logger.d("OkHttp", message)
            }.apply {
                level = if (Constants.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        }

        private fun createErrorInterceptor(): Interceptor {
            return Interceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)

                when (response.code) {
                    401 -> {
                        // Handle unauthorized
                        Logger.e("API", "Unauthorized request: ${request.url}")
                    }
                    403 -> {
                        // Handle forbidden
                        Logger.e("API", "Forbidden request: ${request.url}")
                    }
                    404 -> {
                        // Handle not found
                        Logger.e("API", "Resource not found: ${request.url}")
                    }
                    in 500..599 -> {
                        // Handle server error
                        Logger.e("API", "Server error: ${response.code}")
                    }
                }

                response
            }
        }
    }
}

class NoConnectivityException : java.io.IOException() {
    override val message: String
        get() = "No network connection"
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

interface ApiService {
    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_ACCEPT = "Accept"
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val HEADER_USER_AGENT = "User-Agent"
        
        const val CONTENT_TYPE_JSON = "application/json"
        const val CONTENT_TYPE_FORM = "application/x-www-form-urlencoded"
        
        const val ERROR_NETWORK = "Network error occurred"
        const val ERROR_TIMEOUT = "Request timed out"
        const val ERROR_UNKNOWN = "Unknown error occurred"
        const val ERROR_SERVER = "Server error occurred"
        const val ERROR_UNAUTHORIZED = "Unauthorized access"
    }
}
