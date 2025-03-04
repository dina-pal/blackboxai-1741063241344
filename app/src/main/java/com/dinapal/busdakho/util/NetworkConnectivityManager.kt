package com.dinapal.busdakho.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class NetworkStatus {
    object Available : NetworkStatus()
    object Unavailable : NetworkStatus()
}

class NetworkConnectivityManager(context: Context) {
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val networkStatusCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onUnavailable() {
                launch { send(NetworkStatus.Unavailable) }
            }

            override fun onAvailable(network: Network) {
                launch { send(NetworkStatus.Available) }
            }

            override fun onLost(network: Network) {
                launch { send(NetworkStatus.Unavailable) }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        connectivityManager.registerNetworkCallback(request, networkStatusCallback)

        // Set initial status
        val currentStatus = getCurrentConnectivityStatus()
        send(currentStatus)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkStatusCallback)
        }
    }.distinctUntilChanged()

    fun getCurrentConnectivityStatus(): NetworkStatus {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkStatus.Unavailable
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            return if (capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) {
                NetworkStatus.Available
            } else {
                NetworkStatus.Unavailable
            }
        } else {
            @Suppress("DEPRECATION")
            return if (connectivityManager.activeNetworkInfo?.isConnected == true) {
                NetworkStatus.Available
            } else {
                NetworkStatus.Unavailable
            }
        }
    }

    fun isNetworkAvailable(): Boolean {
        return getCurrentConnectivityStatus() is NetworkStatus.Available
    }

    fun getConnectionType(): ConnectionType {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return ConnectionType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionType.NONE

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
                else -> ConnectionType.NONE
            }
        } else {
            @Suppress("DEPRECATION")
            return when (connectivityManager.activeNetworkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> ConnectionType.WIFI
                ConnectivityManager.TYPE_MOBILE -> ConnectionType.CELLULAR
                ConnectivityManager.TYPE_ETHERNET -> ConnectionType.ETHERNET
                else -> ConnectionType.NONE
            }
        }
    }

    fun getNetworkStrength(): NetworkStrength {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkStrength.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkStrength.NONE

            return when {
                capabilities.hasSignalStrength(NetworkCapabilities.SIGNAL_STRENGTH_EXCELLENT) -> NetworkStrength.EXCELLENT
                capabilities.hasSignalStrength(NetworkCapabilities.SIGNAL_STRENGTH_GOOD) -> NetworkStrength.GOOD
                capabilities.hasSignalStrength(NetworkCapabilities.SIGNAL_STRENGTH_MODERATE) -> NetworkStrength.MODERATE
                capabilities.hasSignalStrength(NetworkCapabilities.SIGNAL_STRENGTH_POOR) -> NetworkStrength.POOR
                else -> NetworkStrength.NONE
            }
        }
        return NetworkStrength.UNKNOWN
    }

    enum class ConnectionType {
        WIFI,
        CELLULAR,
        ETHERNET,
        NONE
    }

    enum class NetworkStrength {
        EXCELLENT,
        GOOD,
        MODERATE,
        POOR,
        NONE,
        UNKNOWN
    }

    companion object {
        @Volatile
        private var instance: NetworkConnectivityManager? = null

        fun getInstance(context: Context): NetworkConnectivityManager {
            return instance ?: synchronized(this) {
                instance ?: NetworkConnectivityManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
