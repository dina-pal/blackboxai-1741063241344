package com.dinapal.busdakho.preferences

import android.content.Context
import com.dinapal.busdakho.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * User preferences
 */
class UserPreferences(context: Context) : BasePreferences(
    context = context,
    preferencesName = "user_preferences",
    encrypted = true
) {
    var userId: String?
        get() = getString(KEY_USER_ID)
        set(value) = putString(KEY_USER_ID, value)

    var authToken: String?
        get() = getString(KEY_AUTH_TOKEN)
        set(value) = putString(KEY_AUTH_TOKEN, value)

    var refreshToken: String?
        get() = getString(KEY_REFRESH_TOKEN)
        set(value) = putString(KEY_REFRESH_TOKEN, value)

    var lastLoginTime: Long
        get() = getLong(KEY_LAST_LOGIN)
        set(value) = putLong(KEY_LAST_LOGIN, value)

    fun observeAuthToken(): Flow<String?> = getFlow(KEY_AUTH_TOKEN).map { it as? String }

    fun clearAuth() {
        edit {
            remove(KEY_USER_ID)
            remove(KEY_AUTH_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_LAST_LOGIN)
        }
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_LAST_LOGIN = "last_login"
    }
}

/**
 * App preferences
 */
class AppPreferences(context: Context) : BasePreferences(
    context = context,
    preferencesName = "app_preferences"
) {
    var isDarkMode: Boolean
        get() = getBoolean(KEY_DARK_MODE)
        set(value) = putBoolean(KEY_DARK_MODE, value)

    var language: String
        get() = getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        set(value) = putString(KEY_LANGUAGE, value)

    var notificationsEnabled: Boolean
        get() = getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = putBoolean(KEY_NOTIFICATIONS_ENABLED, value)

    var lastSyncTimestamp: Long
        get() = getLong(KEY_LAST_SYNC)
        set(value) = putLong(KEY_LAST_SYNC, value)

    var onboardingCompleted: Boolean
        get() = getBoolean(KEY_ONBOARDING_COMPLETED)
        set(value) = putBoolean(KEY_ONBOARDING_COMPLETED, value)

    fun observeTheme(): Flow<Boolean> = getFlow(KEY_DARK_MODE).map { it as? Boolean ?: false }

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val DEFAULT_LANGUAGE = "en"
    }
}

/**
 * Location preferences
 */
class LocationPreferences(context: Context) : BasePreferences(
    context = context,
    preferencesName = "location_preferences"
) {
    var locationTrackingEnabled: Boolean
        get() = getBoolean(KEY_LOCATION_TRACKING)
        set(value) = putBoolean(KEY_LOCATION_TRACKING, value)

    var lastKnownLatitude: Double
        get() = getFloat(KEY_LAST_LATITUDE, 0f).toDouble()
        set(value) = putFloat(KEY_LAST_LATITUDE, value.toFloat())

    var lastKnownLongitude: Double
        get() = getFloat(KEY_LAST_LONGITUDE, 0f).toDouble()
        set(value) = putFloat(KEY_LAST_LONGITUDE, value.toFloat())

    var favoriteStops: Set<String>
        get() = getStringSet(KEY_FAVORITE_STOPS)
        set(value) = putStringSet(KEY_FAVORITE_STOPS, value)

    companion object {
        private const val KEY_LOCATION_TRACKING = "location_tracking"
        private const val KEY_LAST_LATITUDE = "last_latitude"
        private const val KEY_LAST_LONGITUDE = "last_longitude"
        private const val KEY_FAVORITE_STOPS = "favorite_stops"
    }
}

/**
 * Cache preferences
 */
class CachePreferences(context: Context) : BasePreferences(
    context = context,
    preferencesName = "cache_preferences"
) {
    var lastCacheCleanup: Long
        get() = getLong(KEY_LAST_CLEANUP)
        set(value) = putLong(KEY_LAST_CLEANUP, value)

    var cacheSize: Long
        get() = getLong(KEY_CACHE_SIZE)
        set(value) = putLong(KEY_CACHE_SIZE, value)

    fun shouldCleanCache(): Boolean {
        val timeSinceLastCleanup = System.currentTimeMillis() - lastCacheCleanup
        return timeSinceLastCleanup > Constants.CACHE_CLEANUP_INTERVAL ||
                cacheSize > Constants.CACHE_SIZE
    }

    companion object {
        private const val KEY_LAST_CLEANUP = "last_cleanup"
        private const val KEY_CACHE_SIZE = "cache_size"
    }
}

/**
 * Preferences manager to handle all preferences
 */
class PreferencesManager(context: Context) {
    val userPreferences = UserPreferences(context)
    val appPreferences = AppPreferences(context)
    val locationPreferences = LocationPreferences(context)
    val cachePreferences = CachePreferences(context)

    fun clearAll() {
        userPreferences.clear()
        appPreferences.clear()
        locationPreferences.clear()
        cachePreferences.clear()
    }

    companion object {
        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Extension functions for preferences
 */
fun Context.getPreferencesManager(): PreferencesManager {
    return PreferencesManager.getInstance(this)
}

val Context.userPreferences: UserPreferences
    get() = getPreferencesManager().userPreferences

val Context.appPreferences: AppPreferences
    get() = getPreferencesManager().appPreferences

val Context.locationPreferences: LocationPreferences
    get() = getPreferencesManager().locationPreferences

val Context.cachePreferences: CachePreferences
    get() = getPreferencesManager().cachePreferences
