package com.dinapal.busdakho.analytics

import android.content.Context
import android.os.Bundle
import com.dinapal.busdakho.BuildConfig
import com.dinapal.busdakho.util.Logger
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach

/**
 * Firebase Analytics implementation
 */
class FirebaseAnalyticsTracker(
    private val context: Context
) : BaseAnalytics() {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override suspend fun processEvent(event: AnalyticsEvent) {
        val bundle = Bundle().apply {
            event.parameters.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
            // Add common parameters
            putString("app_version", BuildConfig.VERSION_NAME)
            putLong("timestamp", event.timestamp)
        }

        firebaseAnalytics.logEvent(event.name, bundle)

        // Log errors to Crashlytics
        if (event is ErrorEvent) {
            crashlytics.recordException(event.error)
        }
    }
}

/**
 * Custom analytics implementation for local logging
 */
class DebugAnalyticsTracker : BaseAnalytics() {
    override suspend fun processEvent(event: AnalyticsEvent) {
        Logger.d(
            tag,
            """
            Analytics Event:
            Name: ${event.name}
            Parameters: ${event.parameters}
            Timestamp: ${event.timestamp}
            """.trimIndent()
        )
    }
}

/**
 * Composite analytics tracker that delegates to multiple trackers
 */
class CompositeAnalyticsTracker(
    private val trackers: List<BaseAnalytics>
) : BaseAnalytics() {
    override suspend fun processEvent(event: AnalyticsEvent) {
        trackers.forEach { tracker ->
            try {
                tracker.track(event)
            } catch (e: Exception) {
                Logger.e(tag, "Failed to track event in ${tracker::class.java.simpleName}", e)
            }
        }
    }
}

/**
 * Analytics manager to handle all analytics tracking
 */
class AnalyticsManager(context: Context) {
    private val trackers = listOf(
        FirebaseAnalyticsTracker(context),
        if (BuildConfig.DEBUG) DebugAnalyticsTracker() else null
    ).filterNotNull()

    private val compositeTracker = CompositeAnalyticsTracker(trackers)

    fun track(event: BaseAnalytics.AnalyticsEvent) {
        compositeTracker.track(event)
    }

    fun trackScreenView(screenName: String, screenClass: String) {
        compositeTracker.trackScreenView(screenName, screenClass)
    }

    fun trackUserAction(action: String, params: Map<String, Any> = emptyMap()) {
        compositeTracker.trackUserAction(action, params)
    }

    fun trackError(error: Throwable, params: Map<String, Any> = emptyMap()) {
        compositeTracker.trackError(error, params)
    }

    fun trackPerformance(name: String, duration: Long, params: Map<String, Any> = emptyMap()) {
        compositeTracker.trackPerformance(name, duration, params)
    }

    fun trackUserProperties(properties: Map<String, Any>) {
        compositeTracker.trackUserProperties(properties)
    }

    fun getEvents(): Flow<BaseAnalytics.AnalyticsEvent> {
        return compositeTracker.events
            .onEach { event ->
                Logger.d(tag, "Analytics event emitted: ${event.name}")
            }
            .catch { error ->
                Logger.e(tag, "Error in analytics event flow", error)
            }
    }

    companion object {
        private const val tag = "AnalyticsManager"

        @Volatile
        private var instance: AnalyticsManager? = null

        fun getInstance(context: Context): AnalyticsManager {
            return instance ?: synchronized(this) {
                instance ?: AnalyticsManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Extension functions for convenient analytics tracking
 */
fun Context.trackScreenView(screenName: String, screenClass: String) {
    AnalyticsManager.getInstance(this).trackScreenView(screenName, screenClass)
}

fun Context.trackUserAction(action: String, params: Map<String, Any> = emptyMap()) {
    AnalyticsManager.getInstance(this).trackUserAction(action, params)
}

fun Context.trackError(error: Throwable, params: Map<String, Any> = emptyMap()) {
    AnalyticsManager.getInstance(this).trackError(error, params)
}

fun Context.trackPerformance(name: String, duration: Long, params: Map<String, Any> = emptyMap()) {
    AnalyticsManager.getInstance(this).trackPerformance(name, duration, params)
}

fun Context.trackUserProperties(properties: Map<String, Any>) {
    AnalyticsManager.getInstance(this).trackUserProperties(properties)
}
