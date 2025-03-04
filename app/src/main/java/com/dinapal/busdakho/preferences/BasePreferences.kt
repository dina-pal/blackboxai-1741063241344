package com.dinapal.busdakho.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dinapal.busdakho.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Base class for handling shared preferences
 */
abstract class BasePreferences(
    context: Context,
    preferencesName: String,
    encrypted: Boolean = false
) {
    protected val tag = this::class.java.simpleName
    private val json = Json { ignoreUnknownKeys = true }

    protected val preferences: SharedPreferences = if (encrypted) {
        createEncryptedPreferences(context, preferencesName)
    } else {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            key?.let { onPreferenceChanged(it) }
        }

    private val _preferencesFlow = MutableStateFlow<Map<String, Any?>>(emptyMap())
    val preferencesFlow = _preferencesFlow.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        updatePreferencesFlow()
    }

    private fun createEncryptedPreferences(
        context: Context,
        preferencesName: String
    ): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            preferencesName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    protected fun getString(key: String, defaultValue: String? = null): String? {
        return try {
            preferences.getString(key, defaultValue)
        } catch (e: Exception) {
            Logger.e(tag, "Error getting string preference: $key", e)
            defaultValue
        }
    }

    protected fun putString(key: String, value: String?) {
        try {
            preferences.edit().putString(key, value).apply()
        } catch (e: Exception) {
            Logger.e(tag, "Error putting string preference: $key", e)
        }
    }

    protected fun getInt(key: String, defaultValue: Int = 0): Int {
        return try {
            preferences.getInt(key, defaultValue)
        } catch (e: Exception) {
            Logger.e(tag, "Error getting int preference: $key", e)
            defaultValue
        }
    }

    protected fun putInt(key: String, value: Int) {
        try {
            preferences.edit().putInt(key, value).apply()
        } catch (e: Exception) {
            Logger.e(tag, "Error putting int preference: $key", e)
        }
    }

    protected fun getLong(key: String, defaultValue: Long = 0L): Long {
        return try {
            preferences.getLong(key, defaultValue)
        } catch (e: Exception) {
            Logger.e(tag, "Error getting long preference: $key", e)
            defaultValue
        }
    }

    protected fun putLong(key: String, value: Long) {
        try {
            preferences.edit().putLong(key, value).apply()
        } catch (e: Exception) {
            Logger.e(tag, "Error putting long preference: $key", e)
        }
    }

    protected fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            preferences.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            Logger.e(tag, "Error getting boolean preference: $key", e)
            defaultValue
        }
    }

    protected fun putBoolean(key: String, value: Boolean) {
        try {
            preferences.edit().putBoolean(key, value).apply()
        } catch (e: Exception) {
            Logger.e(tag, "Error putting boolean preference: $key", e)
        }
    }

    protected fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return try {
            preferences.getFloat(key, defaultValue)
        } catch (e: Exception) {
            Logger.e(tag, "Error getting float preference: $key", e)
            defaultValue
        }
    }

    protected fun putFloat(key: String, value: Float) {
        try {
            preferences.edit().putFloat(key, value).apply()
        } catch (e: Exception) {
            Logger.e(tag, "Error putting float preference: $key", e)
        }
    }

    protected fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        return try {
            preferences.getStringSet(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            Logger.e(tag, "Error getting string set preference: $key", e)
            defaultValue
        }
    }

    protected fun putStringSet(key: String, value: Set<String>) {
        try {
            preferences.edit().putStringSet(key, value).apply()
        } catch (e: Exception) {
            Logger.e(tag, "Error putting string set preference: $key", e)
        }
    }

    protected inline fun <reified T> getObject(key: String): T? {
        return try {
            getString(key)?.let { json.decodeFromString<T>(it) }
        } catch (e: Exception) {
            Logger.e(tag, "Error getting object preference: $key", e)
            null
        }
    }

    protected inline fun <reified T> putObject(key: String, value: T) {
        try {
            putString(key, json.encodeToString(T::class.serializer(), value))
        } catch (e: Exception) {
            Logger.e(tag, "Error putting object preference: $key", e)
        }
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    fun contains(key: String): Boolean {
        return preferences.contains(key)
    }

    protected fun getFlow(key: String): Flow<Any?> {
        return MutableStateFlow(preferences.all[key]).also { flow ->
            preferences.registerOnSharedPreferenceChangeListener { _, changedKey ->
                if (changedKey == key) {
                    flow.value = preferences.all[key]
                }
            }
        }
    }

    private fun onPreferenceChanged(key: String) {
        updatePreferencesFlow()
    }

    private fun updatePreferencesFlow() {
        _preferencesFlow.value = preferences.all
    }

    protected fun edit(action: SharedPreferences.Editor.() -> Unit) {
        preferences.edit().apply(action).apply()
    }
}

/**
 * Interface for preferences migration
 */
interface PreferencesMigration {
    fun migrate(preferences: SharedPreferences)
}

/**
 * Base class for preferences migration
 */
abstract class BasePreferencesMigration : PreferencesMigration {
    protected val tag = this::class.java.simpleName

    override fun migrate(preferences: SharedPreferences) {
        try {
            doMigration(preferences)
            Logger.d(tag, "Preferences migration completed successfully")
        } catch (e: Exception) {
            Logger.e(tag, "Error during preferences migration", e)
        }
    }

    protected abstract fun doMigration(preferences: SharedPreferences)
}
