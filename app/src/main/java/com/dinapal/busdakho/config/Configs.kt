package com.dinapal.busdakho.config

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.dinapal.busdakho.util.Constants
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * App configuration data
 */
@Serializable
data class AppConfigData(
    val apiUrl: String = Constants.API_BASE_URL,
    val apiTimeout: Long = Constants.API_TIMEOUT,
    val cacheSize: Long = Constants.CACHE_SIZE,
    val cacheDuration: Long = Constants.CACHE_DURATION,
    val maxRetries: Int = Constants.RETRY_ATTEMPTS,
    val locationUpdateInterval: Long = Constants.LOCATION_UPDATE_INTERVAL,
    val version: Int = 1
) : ConfigData() {
    override fun validate(): Boolean {
        return apiUrl.isNotBlank() &&
                apiTimeout > 0 &&
                cacheSize > 0 &&
                cacheDuration > 0 &&
                maxRetries > 0 &&
                locationUpdateInterval > 0
    }

    override fun getDefaults(): ConfigData = AppConfigData()
}

/**
 * App configuration
 */
class AppConfig(context: Context) : LocalConfig(context, "app_config.json") {
    private var config = AppConfigData()

    val apiUrl: String get() = config.apiUrl
    val apiTimeout: Long get() = config.apiTimeout
    val cacheSize: Long get() = config.cacheSize
    val cacheDuration: Long get() = config.cacheDuration
    val maxRetries: Int get() = config.maxRetries
    val locationUpdateInterval: Long get() = config.locationUpdateInterval

    override fun parseConfig(jsonString: String) {
        config = json.decodeFromString(jsonString)
    }

    override fun createConfigJson(): String {
        return json.encodeToString(config)
    }

    override fun reset() {
        config = AppConfigData()
        save()
    }

    override fun validate(): Boolean = config.validate()
}

/**
 * Feature flags configuration data
 */
@Serializable
data class FeatureFlagsData(
    val enableNotifications: Boolean = true,
    val enableLocationTracking: Boolean = true,
    val enableOfflineMode: Boolean = true,
    val enableAnalytics: Boolean = true,
    val enableCrashReporting: Boolean = true,
    val version: Int = 1
) : ConfigData() {
    override fun validate(): Boolean = true
    override fun getDefaults(): ConfigData = FeatureFlagsData()
}

/**
 * Feature flags configuration
 */
class FeatureFlagsConfig(context: Context) : EncryptedConfig(context, "feature_flags.json") {
    private var config = FeatureFlagsData()

    val enableNotifications: Boolean get() = config.enableNotifications
    val enableLocationTracking: Boolean get() = config.enableLocationTracking
    val enableOfflineMode: Boolean get() = config.enableOfflineMode
    val enableAnalytics: Boolean get() = config.enableAnalytics
    val enableCrashReporting: Boolean get() = config.enableCrashReporting

    override fun parseConfig(jsonString: String) {
        config = json.decodeFromString(jsonString)
    }

    override fun createConfigJson(): String {
        return json.encodeToString(config)
    }

    override fun reset() {
        config = FeatureFlagsData()
        save()
    }

    override fun validate(): Boolean = config.validate()

    override fun encrypt(data: String): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            context,
            configFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val outputStream = ByteArrayOutputStream()
        encryptedFile.openFileOutput().use { fileOutputStream ->
            fileOutputStream.write(data.toByteArray())
        }
        return outputStream.toByteArray()
    }

    override fun decrypt(data: ByteArray): String {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            context,
            configFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return encryptedFile.openFileInput().bufferedReader().use { it.readText() }
    }
}

/**
 * Configuration manager to handle all configurations
 */
class ConfigManager(context: Context) {
    val appConfig = AppConfig(context)
    val featureFlags = FeatureFlagsConfig(context)

    init {
        loadAll()
    }

    fun loadAll() {
        appConfig.load()
        featureFlags.load()
    }

    fun saveAll() {
        appConfig.save()
        featureFlags.save()
    }

    fun resetAll() {
        appConfig.reset()
        featureFlags.reset()
    }

    fun validateAll(): Boolean {
        return appConfig.validate() && featureFlags.validate()
    }

    companion object {
        @Volatile
        private var instance: ConfigManager? = null

        fun getInstance(context: Context): ConfigManager {
            return instance ?: synchronized(this) {
                instance ?: ConfigManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Extension functions for configuration
 */
fun Context.getConfigManager(): ConfigManager = ConfigManager.getInstance(this)

val Context.appConfig: AppConfig get() = getConfigManager().appConfig

val Context.featureFlags: FeatureFlagsConfig get() = getConfigManager().featureFlags

/**
 * Utility functions for configuration
 */
fun String.sha256(): String {
    return MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }
}
