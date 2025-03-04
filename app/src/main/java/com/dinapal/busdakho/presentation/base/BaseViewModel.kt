package com.dinapal.busdakho.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinapal.busdakho.util.Logger
import com.dinapal.busdakho.util.NetworkConnectivityManager
import com.dinapal.busdakho.util.NetworkStatus
import com.dinapal.busdakho.util.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class BaseViewModel : ViewModel() {
    
    private val tag = this::class.java.simpleName
    
    // Error handling
    private val _error = Channel<String>()
    val error = _error.receiveAsFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Network status
    private var networkStatus: NetworkStatus = NetworkStatus.Available
    private var activeJobs = mutableMapOf<String, Job>()

    protected fun <T> launchSafeIO(
        context: CoroutineContext = Dispatchers.IO,
        block: suspend () -> T
    ): Job {
        return viewModelScope.launch(context) {
            try {
                _isLoading.value = true
                block()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    protected fun <T> Flow<T>.launchAndCollect(
        key: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        onError: (Throwable) -> Unit = { handleError(it) },
        onComplete: () -> Unit = {},
        block: suspend (T) -> Unit
    ) {
        activeJobs[key]?.cancel()
        activeJobs[key] = viewModelScope.launch(dispatcher) {
            try {
                collect { value ->
                    try {
                        block(value)
                    } catch (e: Exception) {
                        handleError(e)
                    }
                }
                onComplete()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    protected suspend fun <T> safeApiCall(
        block: suspend () -> T
    ): Resource<T> {
        return try {
            if (networkStatus is NetworkStatus.Unavailable) {
                Resource.Error(message = "No internet connection")
            } else {
                Resource.Success(block())
            }
        } catch (e: Exception) {
            handleError(e)
            Resource.Error(error = e)
        }
    }

    protected fun handleError(error: Throwable) {
        Logger.e(tag, "Error occurred", error)
        viewModelScope.launch {
            _error.send(error.message ?: "An unknown error occurred")
        }
    }

    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    protected fun showError(message: String) {
        viewModelScope.launch {
            _error.send(message)
        }
    }

    fun updateNetworkStatus(status: NetworkStatus) {
        networkStatus = status
        Logger.d(tag, "Network status updated: $status")
    }

    override fun onCleared() {
        super.onCleared()
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }

    protected fun <T> Flow<Resource<T>>.onResourceSuccess(
        action: suspend (T) -> Unit
    ): Flow<Resource<T>> = onEach { resource ->
        if (resource is Resource.Success) {
            resource.data?.let { action(it) }
        }
    }

    protected fun <T> Flow<Resource<T>>.onResourceError(
        action: suspend (String?) -> Unit
    ): Flow<Resource<T>> = onEach { resource ->
        if (resource is Resource.Error) {
            action(resource.message)
        }
    }

    protected fun <T> Flow<Resource<T>>.onResourceLoading(
        action: suspend (Boolean) -> Unit
    ): Flow<Resource<T>> = onEach { resource ->
        action(resource.isLoading)
    }

    protected fun <T> Flow<T>.stateInViewModel(initialValue: T): StateFlow<T> {
        return stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialValue
        )
    }

    protected fun cancelJob(key: String) {
        activeJobs[key]?.cancel()
        activeJobs.remove(key)
    }

    protected fun cancelAllJobs() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }

    protected fun isJobActive(key: String): Boolean {
        return activeJobs[key]?.isActive == true
    }

    companion object {
        private const val DEFAULT_TIMEOUT = 5000L
    }
}

// Extension function to convert Flow<T> to StateFlow<Resource<T>>
fun <T> Flow<T>.asResourceStateFlow(
    scope: Flow<NetworkStatus>,
    initialValue: T? = null
): StateFlow<Resource<T>> {
    return combine(scope) { data, networkStatus ->
        when (networkStatus) {
            is NetworkStatus.Available -> Resource.Success(data)
            is NetworkStatus.Unavailable -> Resource.Error(message = "No internet connection")
        }
    }.stateIn(
        scope = scope as kotlinx.coroutines.CoroutineScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialValue?.let { Resource.Success(it) } ?: Resource.Loading()
    )
}

// Extension function to handle Resource states
fun <T> Resource<T>.handle(
    onSuccess: (T) -> Unit,
    onError: (String?) -> Unit,
    onLoading: () -> Unit = {}
) {
    when (this) {
        is Resource.Success -> data?.let { onSuccess(it) }
        is Resource.Error -> onError(message)
        is Resource.Loading -> onLoading()
    }
}
