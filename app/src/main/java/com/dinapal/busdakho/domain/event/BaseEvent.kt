package com.dinapal.busdakho.domain.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

/**
 * Base class for all events in the application
 */
sealed class BaseEvent {
    val id: String = UUID.randomUUID().toString()
    val timestamp: Long = System.currentTimeMillis()
}

/**
 * Base class for UI events
 */
sealed class UiEvent : BaseEvent() {
    data class ShowSnackbar(
        val message: String,
        val action: String? = null,
        val duration: SnackbarDuration = SnackbarDuration.SHORT
    ) : UiEvent()

    data class ShowToast(
        val message: String,
        val duration: ToastDuration = ToastDuration.SHORT
    ) : UiEvent()

    data class Navigate(
        val route: String,
        val popUpTo: String? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = false
    ) : UiEvent()

    object NavigateBack : UiEvent()

    data class ShowDialog(
        val title: String,
        val message: String,
        val positiveButton: String? = null,
        val negativeButton: String? = null,
        val onPositiveClick: (() -> Unit)? = null,
        val onNegativeClick: (() -> Unit)? = null,
        val cancelable: Boolean = true
    ) : UiEvent()

    data class ShowLoading(val message: String? = null) : UiEvent()
    object HideLoading : UiEvent()
}

/**
 * Base class for domain events
 */
sealed class DomainEvent : BaseEvent() {
    data class DataChanged<T>(
        val data: T,
        val source: String
    ) : DomainEvent()

    data class Error(
        val error: Throwable,
        val message: String? = null
    ) : DomainEvent()

    data class Success(
        val message: String
    ) : DomainEvent()
}

/**
 * Base class for analytics events
 */
sealed class AnalyticsEvent : BaseEvent() {
    abstract val eventName: String
    abstract val parameters: Map<String, Any>

    data class ScreenView(
        val screenName: String,
        val screenClass: String? = null,
        override val parameters: Map<String, Any> = emptyMap()
    ) : AnalyticsEvent() {
        override val eventName: String = "screen_view"
    }

    data class UserAction(
        val action: String,
        override val parameters: Map<String, Any> = emptyMap()
    ) : AnalyticsEvent() {
        override val eventName: String = "user_action"
    }

    data class Error(
        val error: Throwable,
        val errorMessage: String? = null,
        override val parameters: Map<String, Any> = emptyMap()
    ) : AnalyticsEvent() {
        override val eventName: String = "error"
    }
}

/**
 * Event bus for handling events across the application
 */
object EventBus {
    private val _events = MutableSharedFlow<BaseEvent>()
    val events = _events.asSharedFlow()

    suspend fun emit(event: BaseEvent) {
        _events.emit(event)
    }
}

/**
 * Enums for UI event durations
 */
enum class SnackbarDuration {
    SHORT,
    LONG,
    INDEFINITE
}

enum class ToastDuration {
    SHORT,
    LONG
}

/**
 * Extension functions for event handling
 */
fun BaseEvent.toAnalyticsEvent(): AnalyticsEvent {
    return when (this) {
        is UiEvent.Navigate -> AnalyticsEvent.ScreenView(
            screenName = route,
            parameters = mapOf(
                "pop_up_to" to (popUpTo ?: ""),
                "inclusive" to inclusive,
                "single_top" to singleTop
            )
        )
        is UiEvent.ShowSnackbar -> AnalyticsEvent.UserAction(
            action = "show_snackbar",
            parameters = mapOf(
                "message" to message,
                "action" to (action ?: ""),
                "duration" to duration.name
            )
        )
        is DomainEvent.Error -> AnalyticsEvent.Error(
            error = error,
            errorMessage = message,
            parameters = mapOf(
                "error_type" to error::class.java.simpleName,
                "timestamp" to timestamp
            )
        )
        else -> AnalyticsEvent.UserAction(
            action = this::class.java.simpleName,
            parameters = mapOf(
                "timestamp" to timestamp,
                "event_id" to id
            )
        )
    }
}

/**
 * Interface for event handlers
 */
interface EventHandler<T : BaseEvent> {
    suspend fun handle(event: T)
}

/**
 * Abstract base class for event processors
 */
abstract class BaseEventProcessor<T : BaseEvent> : EventHandler<T> {
    override suspend fun handle(event: T) {
        try {
            process(event)
        } catch (e: Exception) {
            handleError(e, event)
        }
    }

    protected abstract suspend fun process(event: T)

    protected open suspend fun handleError(error: Exception, event: T) {
        EventBus.emit(DomainEvent.Error(error))
    }
}

/**
 * Extension function to create event parameters
 */
fun eventParamsOf(vararg pairs: Pair<String, Any>): Map<String, Any> = pairs.toMap()
