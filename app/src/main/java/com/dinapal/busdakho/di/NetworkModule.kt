package com.dinapal.busdakho.di

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.observer.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun createKtorClient(): HttpClient {
    return HttpClient(Android) {
        // Install required plugins
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("Ktor", message)
                }
            }
            level = LogLevel.ALL
        }

        install(ResponseObserver) {
            onResponse { response ->
                Log.d("Ktor", "HTTP status: ${response.status.value}")
            }
        }

        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        // Configure client
        engine {
            connectTimeout = 60_000
            socketTimeout = 60_000
        }

        // Handle exceptions
        HttpResponseValidator {
            validateResponse { response ->
                val statusCode = response.status.value
                when (statusCode) {
                    in 300..399 -> throw RedirectResponseException(response, "Redirect Error")
                    in 400..499 -> throw ClientRequestException(response, "Client Error")
                    in 500..599 -> throw ServerResponseException(response, "Server Error")
                }
            }
        }
    }
}

fun createApiService(client: HttpClient): ApiService {
    return ApiServiceImpl(client)
}

interface ApiService {
    suspend fun getBusLocations(): List<BusLocation>
    suspend fun getRoutes(): List<Route>
    suspend fun getUserProfile(userId: String): UserProfile
    // Add more API endpoints as needed
}

class ApiServiceImpl(private val client: HttpClient) : ApiService {
    companion object {
        private const val BASE_URL = "https://api.busdakho.com" // Replace with actual API URL
    }

    override suspend fun getBusLocations(): List<BusLocation> {
        // TODO: Implement actual API call
        return emptyList()
    }

    override suspend fun getRoutes(): List<Route> {
        // TODO: Implement actual API call
        return emptyList()
    }

    override suspend fun getUserProfile(userId: String): UserProfile {
        // TODO: Implement actual API call
        return UserProfile()
    }
}

// Data classes for API responses
@kotlinx.serialization.Serializable
data class BusLocation(
    val busId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val timestamp: Long
)

@kotlinx.serialization.Serializable
data class Route(
    val routeId: String,
    val name: String,
    val stops: List<Stop>,
    val schedule: List<Schedule>
)

@kotlinx.serialization.Serializable
data class Stop(
    val stopId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

@kotlinx.serialization.Serializable
data class Schedule(
    val departureTime: String,
    val arrivalTime: String,
    val frequency: Int // in minutes
)

@kotlinx.serialization.Serializable
data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val favoriteRoutes: List<String> = emptyList(),
    val favoriteStops: List<String> = emptyList()
)
