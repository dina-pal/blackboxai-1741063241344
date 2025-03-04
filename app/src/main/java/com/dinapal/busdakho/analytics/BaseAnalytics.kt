package com.dinapal.busdakho.analytics

import com.dinapal.busdakho.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Base class for analytics tracking
 */
abstract class BaseAnalytics {
    protected val tag = this::class.java.simpleName
    private val analyticsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _events = MutableSharedFlow<AnalyticsEvent>()
    val events = _events.asSharedFlow()

    /**
     * Track an analytics event
     */
    fun track(event: AnalyticsEvent) {
        analyticsScope.launch {
            try {
                Logger.d(tag, "Tracking event: ${event.name}")
                _events.emit(event)
                processEvent(event)
            } catch (e: Exception) {
                Logger.e(tag, "Failed to track event: ${event.name}", e)
            }
        }
    }

    /**
     * Process the analytics event
     */
    protected abstract suspend fun processEvent(event: AnalyticsEvent)

    /**
     * Base analytics event
     */
    interface AnalyticsEvent {
        val name: String
        val parameters: Map<String, Any>
        val timestamp: Long
            get() = System.currentTimeMillis()
    }

    /**
     * Screen view event
     */
    data class ScreenViewEvent(
        override val name: String,
        val screenClass: String,
        override val parameters: Map<String, Any> = emptyMap()
    ) : AnalyticsEvent

    /**
     * User action event
     */
    data class UserActionEvent(
        override val name: String,
        val action: String,
        override val parameters: Map<String, Any> = emptyMap()
    ) : AnalyticsEvent

    /**
     * Error event
     */
    data class ErrorEvent(
        override val name: String = "error",
        val error: Throwable,
        val errorMessage: String? = null,
        override val parameters: Map<String, Any> = emptyMap()
    ) : AnalyticsEvent

    /**
     * Performance event
     */
    data class PerformanceEvent(
        override val name: String,
        val duration: Long,
        override val parameters: Map<String, Any> = emptyMap()
    ) : AnalyticsEvent

    /**
     * User property event
     */
    data class UserPropertyEvent(
        override val name: String = "user_property",
        val properties: Map<String, Any>,
        override val parameters: Map<String, Any> = emptyMap()
    ) : AnalyticsEvent

    companion object {
        // Common event names
        const val EVENT_SCREEN_VIEW = "screen_view"
        const val EVENT_BUTTON_CLICK = "button_click"
        const val EVENT_USER_LOGIN = "user_login"
        const val EVENT_USER_LOGOUT = "user_logout"
        const val EVENT_SEARCH = "search"
        const val EVENT_ERROR = "error"
        const val EVENT_PERFORMANCE = "performance"

        // Common parameter keys
        const val PARAM_SCREEN_NAME = "screen_name"
        const val PARAM_SCREEN_CLASS = "screen_class"
        const val PARAM_BUTTON_ID = "button_id"
        const val PARAM_SEARCH_TERM = "search_term"
        const val PARAM_ERROR_MESSAGE = "error_message"
        const val PARAM_ERROR_TYPE = "error_type"
        const val PARAM_DURATION = "duration"
        const val PARAM_SUCCESS = "success"
        const val PARAM_USER_ID = "user_id"
        const val PARAM_SOURCE = "source"

        // Helper function to create parameter maps
        fun createParams(vararg pairs: Pair<String, Any>): Map<String, Any> = pairs.toMap()
    }
}

/**
 * Extension functions for analytics tracking
 */
fun BaseAnalytics.trackScreenView(
    screenName: String,
    screenClass: String,
    additionalParams: Map<String, Any> = emptyMap()
) {
    track(
        BaseAnalytics.ScreenViewEvent(
            name = BaseAnalytics.EVENT_SCREEN_VIEW,
            screenClass = screenClass,
            parameters = BaseAnalytics.createParams(
                BaseAnalytics.PARAM_SCREEN_NAME to screenName,
                BaseAnalytics.PARAM_SCREEN_CLASS to screenClass
            ) + additionalParams
        )
    )
}

fun BaseAnalytics.trackUserAction(
    action: String,
    additionalParams: Map<String, Any> = emptyMap()
) {
    track(
        BaseAnalytics.UserActionEvent(
            name = action,
            action = action,
            parameters = additionalParams
        )
    )
}

fun BaseAnalytics.trackError(
    error: Throwable,
    additionalParams: Map<String, Any> = emptyMap()
) {
    track(
        BaseAnalytics.ErrorEvent(
            error = error,
            errorMessage = error.message,
            parameters = BaseAnalytics.createParams(
                BaseAnalytics.PARAM_ERROR_TYPE to error::class.java.simpleName,
                BaseAnalytics.PARAM_ERROR_MESSAGE to (error.message ?: "Unknown error")
            ) + additionalParams
        )
    )
}

fun BaseAnalytics.trackPerformance(
    name: String,
    duration: Long,
    additionalParams: Map<String, Any> = emptyMap()
) {
    track(
        BaseAnalytics.PerformanceEvent(
            name = name,
            duration = duration,
            parameters = BaseAnalytics.createParams(
                BaseAnalytics.PARAM_DURATION to duration
            ) + additionalParams
        )
    )
}

fun BaseAnalytics.trackUserProperties(
    properties: Map<String, Any>
) {
    track(
        BaseAnalytics.UserPropertyEvent(
            properties = properties
        )
    )
}
