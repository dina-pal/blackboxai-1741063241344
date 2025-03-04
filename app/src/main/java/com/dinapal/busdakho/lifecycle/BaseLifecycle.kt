package com.dinapal.busdakho.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.dinapal.busdakho.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Base class for lifecycle management
 */
abstract class BaseLifecycle : LifecycleOwner {
    protected val tag = this::class.java.simpleName
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val jobs = mutableListOf<Job>()

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    /**
     * Start the lifecycle
     */
    protected fun start() {
        Logger.d(tag, "Lifecycle started")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    /**
     * Stop the lifecycle
     */
    protected fun stop() {
        Logger.d(tag, "Lifecycle stopped")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    /**
     * Create the lifecycle
     */
    protected fun create() {
        Logger.d(tag, "Lifecycle created")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    /**
     * Destroy the lifecycle
     */
    protected fun destroy() {
        Logger.d(tag, "Lifecycle destroyed")
        jobs.forEach { it.cancel() }
        jobs.clear()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    /**
     * Resume the lifecycle
     */
    protected fun resume() {
        Logger.d(tag, "Lifecycle resumed")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /**
     * Pause the lifecycle
     */
    protected fun pause() {
        Logger.d(tag, "Lifecycle paused")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    /**
     * Launch a coroutine in the lifecycle scope
     */
    protected fun launchInLifecycle(block: suspend CoroutineScope.() -> Unit): Job {
        return lifecycleScope.launch {
            block()
        }.also { jobs.add(it) }
    }

    /**
     * Check if the lifecycle is in a specific state
     */
    protected fun isInState(state: Lifecycle.State): Boolean {
        return lifecycle.currentState.isAtLeast(state)
    }
}

/**
 * Interface for lifecycle callbacks
 */
interface LifecycleCallbacks {
    fun onCreate()
    fun onStart()
    fun onResume()
    fun onPause()
    fun onStop()
    fun onDestroy()
}

/**
 * Base implementation of lifecycle callbacks
 */
abstract class BaseLifecycleCallbacks : LifecycleCallbacks {
    override fun onCreate() {}
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
}

/**
 * Lifecycle observer for handling lifecycle events
 */
abstract class LifecycleObserver(
    private val lifecycleOwner: LifecycleOwner
) {
    protected val tag = this::class.java.simpleName
    private var isObserving = false

    protected abstract fun onStateChanged(state: Lifecycle.State)

    fun startObserving() {
        if (!isObserving) {
            lifecycleOwner.lifecycle.addObserver(androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_CREATE -> onCreate()
                    Lifecycle.Event.ON_START -> onStart()
                    Lifecycle.Event.ON_RESUME -> onResume()
                    Lifecycle.Event.ON_PAUSE -> onPause()
                    Lifecycle.Event.ON_STOP -> onStop()
                    Lifecycle.Event.ON_DESTROY -> onDestroy()
                    else -> {}
                }
                onStateChanged(lifecycleOwner.lifecycle.currentState)
            })
            isObserving = true
        }
    }

    fun stopObserving() {
        if (isObserving) {
            lifecycleOwner.lifecycle.removeObserver(androidx.lifecycle.LifecycleEventObserver { _, _ -> })
            isObserving = false
        }
    }

    protected open fun onCreate() {
        Logger.d(tag, "onCreate")
    }

    protected open fun onStart() {
        Logger.d(tag, "onStart")
    }

    protected open fun onResume() {
        Logger.d(tag, "onResume")
    }

    protected open fun onPause() {
        Logger.d(tag, "onPause")
    }

    protected open fun onStop() {
        Logger.d(tag, "onStop")
    }

    protected open fun onDestroy() {
        Logger.d(tag, "onDestroy")
        stopObserving()
    }
}

/**
 * Extension functions for lifecycle management
 */
fun LifecycleOwner.launchWhenStarted(block: suspend CoroutineScope.() -> Unit): Job {
    return lifecycleScope.launch {
        lifecycle.whenStarted {
            block()
        }
    }
}

fun LifecycleOwner.launchWhenResumed(block: suspend CoroutineScope.() -> Unit): Job {
    return lifecycleScope.launch {
        lifecycle.whenResumed {
            block()
        }
    }
}

fun LifecycleOwner.launchWhenCreated(block: suspend CoroutineScope.() -> Unit): Job {
    return lifecycleScope.launch {
        lifecycle.whenCreated {
            block()
        }
    }
}

/**
 * Extension property to check lifecycle state
 */
val LifecycleOwner.isActive: Boolean
    get() = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
