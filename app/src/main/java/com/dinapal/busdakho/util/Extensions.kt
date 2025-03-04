package com.dinapal.busdakho.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.minutes

object Extensions {
    fun Context.isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }

    fun Long.toFormattedDateTime(pattern: String = "dd MMM yyyy, HH:mm"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
    }

    fun Long.toRelativeTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - this
        return when {
            diff < 1.minutes.inWholeMilliseconds -> "Just now"
            diff < 60.minutes.inWholeMilliseconds -> "${diff / (1000 * 60)} minutes ago"
            diff < 24 * 60.minutes.inWholeMilliseconds -> "${diff / (1000 * 60 * 60)} hours ago"
            else -> toFormattedDateTime("dd MMM yyyy")
        }
    }

    fun Double.formatDistance(): String {
        return when {
            this < 1000 -> "${this.toInt()}m"
            else -> String.format("%.1fkm", this / 1000)
        }
    }

    fun Int.formatDuration(): String {
        return when {
            this < 60 -> "${this}min"
            else -> "${this / 60}h ${this % 60}min"
        }
    }

    fun Double.formatFare(): String {
        return String.format("₹%.2f", this)
    }

    @Composable
    fun <T> Flow<T>.collectWithLifecycle(
        lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
        collector: suspend (T) -> Unit
    ) {
        LaunchedEffect(Unit) {
            flowWithLifecycle(lifecycleOwner.lifecycle, minActiveState)
                .collect { collector(it) }
        }
    }

    fun <T> retryIO(
        times: Int = 3,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): Flow<T> = flow {
        var currentDelay = initialDelay
        repeat(times) { attempt ->
            try {
                emit(block())
                return@flow
            } catch (e: Exception) {
                if (attempt == times - 1) throw e

                kotlinx.coroutines.delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
    }

    fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
        }
    }

    fun String.isValidEmail(): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }

    fun String.isValidPhone(): Boolean {
        return this.matches(Regex("^[+]?[0-9]{10,13}$"))
    }

    fun String.isValidName(): Boolean {
        return this.matches(Regex("^[a-zA-Z ]{2,30}$"))
    }

    fun String.maskPhone(): String {
        return if (this.length > 4) {
            "*".repeat(this.length - 4) + this.takeLast(4)
        } else {
            this
        }
    }

    fun String.maskEmail(): String {
        val parts = this.split("@")
        if (parts.size != 2) return this
        val (name, domain) = parts
        val maskedName = when {
            name.length <= 2 -> name
            name.length <= 4 -> name.take(2) + "*".repeat(name.length - 2)
            else -> name.take(2) + "*".repeat(name.length - 4) + name.takeLast(2)
        }
        return "$maskedName@$domain"
    }

    fun <T> List<T>.replaceAt(index: Int, item: T): List<T> {
        return toMutableList().apply {
            this[index] = item
        }
    }

    fun <T> List<T>.replaceIf(predicate: (T) -> Boolean, item: T): List<T> {
        return map { if (predicate(it)) item else it }
    }

    fun <T> List<T>.updateIf(predicate: (T) -> Boolean, transform: (T) -> T): List<T> {
        return map { if (predicate(it)) transform(it) else it }
    }

    fun <T> List<T>.findAndUpdate(predicate: (T) -> Boolean, transform: (T) -> T): List<T> {
        val index = indexOfFirst(predicate)
        return if (index != -1) {
            replaceAt(index, transform(get(index)))
        } else {
            this
        }
    }
}
