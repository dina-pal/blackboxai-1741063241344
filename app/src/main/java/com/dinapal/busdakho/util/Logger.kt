package com.dinapal.busdakho.util

import android.util.Log
import com.dinapal.busdakho.BuildConfig
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.*

object Logger {
    private const val TAG = "BusDakho"
    private var isDebugEnabled = BuildConfig.DEBUG
    private var isFileLoggingEnabled = false
    private var logFile: File? = null
    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private var logJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(debugEnabled: Boolean = BuildConfig.DEBUG, fileLoggingEnabled: Boolean = false, logDir: File? = null) {
        isDebugEnabled = debugEnabled
        isFileLoggingEnabled = fileLoggingEnabled

        if (isFileLoggingEnabled && logDir != null) {
            setupFileLogging(logDir)
        }
    }

    private fun setupFileLogging(logDir: File) {
        try {
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            logFile = File(logDir, "busdakho_$timestamp.log")

            // Start log processing job
            startLogProcessing()
        } catch (e: Exception) {
            e("Logger", "Failed to setup file logging", e)
            isFileLoggingEnabled = false
        }
    }

    private fun startLogProcessing() {
        logJob?.cancel()
        logJob = coroutineScope.launch {
            while (isActive) {
                processLogQueue()
                delay(1000) // Process logs every second
            }
        }
    }

    private suspend fun processLogQueue() {
        if (!isFileLoggingEnabled || logFile == null) return

        try {
            val entries = mutableListOf<LogEntry>()
            while (true) {
                val entry = logQueue.poll() ?: break
                entries.add(entry)
            }

            if (entries.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    FileWriter(logFile!!, true).use { writer ->
                        entries.forEach { entry ->
                            writer.append(entry.toString())
                            writer.append('\n')
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write logs to file", e)
        }
    }

    fun d(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.d("$TAG:$tag", message)
            logToFile(LogLevel.DEBUG, tag, message)
        }
    }

    fun i(tag: String, message: String) {
        Log.i("$TAG:$tag", message)
        logToFile(LogLevel.INFO, tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$TAG:$tag", message, throwable)
        logToFile(LogLevel.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG:$tag", message, throwable)
        logToFile(LogLevel.ERROR, tag, message, throwable)
    }

    fun wtf(tag: String, message: String, throwable: Throwable? = null) {
        Log.wtf("$TAG:$tag", message, throwable)
        logToFile(LogLevel.ASSERT, tag, message, throwable)
    }

    private fun logToFile(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (!isFileLoggingEnabled) return

        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        logQueue.offer(entry)
    }

    fun clearLogs() {
        logFile?.delete()
    }

    fun getLogs(): List<String> {
        return try {
            logFile?.readLines() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read logs", e)
            emptyList()
        }
    }

    private enum class LogLevel {
        DEBUG, INFO, WARN, ERROR, ASSERT
    }

    private data class LogEntry(
        val timestamp: Long,
        val level: LogLevel,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    ) {
        override fun toString(): String {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(Date(timestamp))
            val throwableMessage = throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
            return "[$time] ${level.name}/$tag: $message$throwableMessage"
        }
    }

    // Extension functions for common logging scenarios
    fun logNetworkCall(tag: String, url: String, method: String, responseCode: Int? = null) {
        if (isDebugEnabled) {
            d(tag, "Network Call: $method $url${responseCode?.let { " (Response: $it)" } ?: ""}")
        }
    }

    fun logNetworkError(tag: String, url: String, error: Throwable) {
        e(tag, "Network Error: $url", error)
    }

    fun logLifecycle(tag: String, lifecycle: String) {
        if (isDebugEnabled) {
            d(tag, "Lifecycle: $lifecycle")
        }
    }

    fun logNavigation(tag: String, from: String, to: String) {
        if (isDebugEnabled) {
            d(tag, "Navigation: $from -> $to")
        }
    }

    fun logUserAction(tag: String, action: String) {
        if (isDebugEnabled) {
            d(tag, "User Action: $action")
        }
    }

    fun logDatabaseOperation(tag: String, operation: String, result: String) {
        if (isDebugEnabled) {
            d(tag, "Database: $operation - $result")
        }
    }

    fun logPermission(tag: String, permission: String, granted: Boolean) {
        if (isDebugEnabled) {
            d(tag, "Permission: $permission - ${if (granted) "Granted" else "Denied"}")
        }
    }

    fun logLocationUpdate(tag: String, latitude: Double, longitude: Double) {
        if (isDebugEnabled) {
            d(tag, "Location Update: Lat: $latitude, Lng: $longitude")
        }
    }

    fun logServiceEvent(tag: String, event: String) {
        if (isDebugEnabled) {
            d(tag, "Service Event: $event")
        }
    }
}
