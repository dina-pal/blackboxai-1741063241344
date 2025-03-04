package com.dinapal.busdakho.config

import android.content.Context
import com.dinapal.busdakho.BuildConfig
import com.dinapal.busdakho.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Base class for handling app configuration
 */
abstract class BaseConfig {
    protected val tag = this::class.java.simpleName
    protected val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
    }

    private val _configUpdated = MutableStateFlow(false)
    val configUpdated = _configUpdated.asStateFlow()

    /**
     * Load configuration
     */
    abstract fun load()

    /**
     * Save configuration
     */
    abstract fun save()

    /**
     * Reset configuration to defaults
     */
    abstract fun reset()

    /**
     * Validate configuration
     */
    abstract fun validate(): Boolean

    /**
     * Notify that config has been updated
     */
    protected fun notifyConfigUpdated() {
        _configUpdated.value = !_configUpdated.value
    }
}

/**
 * Base class for remote configuration
 */
abstract class RemoteConfig : BaseConfig() {
    abstract fun fetch()
    abstract fun activate()
    abstract fun fetchAndActivate()
    abstract fun setDefaults()
}

/**
 * Base class for local configuration
 */
abstract class LocalConfig(
    protected val context: Context,
    protected val fileName: String
) : BaseConfig() {

    protected val configFile: File by lazy {
        File(context.filesDir, fileName)
    }

    override fun load() {
        try {
            if (configFile.exists()) {
                val jsonString = configFile.readText()
                parseConfig(jsonString)
                Logger.d(tag, "Configuration loaded successfully")
            } else {
                Logger.d(tag, "No configuration file found, using defaults")
                reset()
            }
        } catch (e: Exception) {
            Logger.e(tag, "Error loading configuration", e)
            reset()
        }
    }

    override fun save() {
        try {
            val jsonString = createConfigJson()
            configFile.writeText(jsonString)
            Logger.d(tag, "Configuration saved successfully")
            notifyConfigUpdated()
        } catch (e: Exception) {
            Logger.e(tag, "Error saving configuration", e)
        }
    }

    protected abstract fun parseConfig(jsonString: String)
    protected abstract fun createConfigJson(): String
}

/**
 * Base class for encrypted configuration
 */
abstract class EncryptedConfig(
    context: Context,
    fileName: String
) : LocalConfig(context, fileName) {

    override fun save() {
        try {
            val jsonString = createConfigJson()
            val encrypted = encrypt(jsonString)
            configFile.writeBytes(encrypted)
            Logger.d(tag, "Encrypted configuration saved successfully")
            notifyConfigUpdated()
        } catch (e: Exception) {
            Logger.e(tag, "Error saving encrypted configuration", e)
        }
    }

    override fun load() {
        try {
            if (configFile.exists()) {
                val encrypted = configFile.readBytes()
                val decrypted = decrypt(encrypted)
                parseConfig(decrypted)
                Logger.d(tag, "Encrypted configuration loaded successfully")
            } else {
                Logger.d(tag, "No encrypted configuration file found, using defaults")
                reset()
            }
        } catch (e: Exception) {
            Logger.e(tag, "Error loading encrypted configuration", e)
            reset()
        }
    }

    protected abstract fun encrypt(data: String): ByteArray
    protected abstract fun decrypt(data: ByteArray): String
}

/**
 * Base configuration data class
 */
abstract class ConfigData {
    abstract fun validate(): Boolean
    abstract fun getDefaults(): ConfigData
}

/**
 * Base class for configuration migrations
 */
abstract class ConfigMigration(
    private val fromVersion: Int,
    private val toVersion: Int
) {
    fun canMigrate(currentVersion: Int): Boolean {
        return currentVersion == fromVersion
    }

    abstract fun migrate(config: ConfigData): ConfigData

    fun getNewVersion(): Int = toVersion
}

/**
 * Configuration exception
 */
class ConfigurationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Configuration validation exception
 */
class ConfigurationValidationException(
    message: String,
    val validationErrors: List<String>
) : Exception(message)

/**
 * Configuration migration exception
 */
class ConfigurationMigrationException(
    message: String,
    val fromVersion: Int,
    val toVersion: Int,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Build configuration helper
 */
object BuildConfig {
    val DEBUG = BuildConfig.DEBUG
    val VERSION_NAME = BuildConfig.VERSION_NAME
    val VERSION_CODE = BuildConfig.VERSION_CODE
    val APPLICATION_ID = BuildConfig.APPLICATION_ID
    val BUILD_TYPE = BuildConfig.BUILD_TYPE
    val FLAVOR = BuildConfig.FLAVOR
}
