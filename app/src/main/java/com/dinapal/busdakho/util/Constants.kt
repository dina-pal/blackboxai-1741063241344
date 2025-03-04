package com.dinapal.busdakho.util

object Constants {
    // App Configuration
    const val APP_NAME = "BusDakho"
    const val DATABASE_NAME = "busdakho_database"
    const val PREFERENCES_NAME = "busdakho_preferences"
    const val API_BASE_URL = "https://api.busdakho.com/" // Replace with actual API URL
    const val API_TIMEOUT = 30L // seconds

    // Location Constants
    const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds
    const val LOCATION_FASTEST_INTERVAL = 5000L // 5 seconds
    const val LOCATION_DISPLACEMENT = 10f // 10 meters
    const val DEFAULT_ZOOM_LEVEL = 15f
    const val MIN_ZOOM_LEVEL = 10f
    const val MAX_ZOOM_LEVEL = 20f
    const val DEFAULT_LATITUDE = 20.5937 // India center latitude
    const val DEFAULT_LONGITUDE = 78.9629 // India center longitude
    const val GEOFENCE_RADIUS = 100f // meters
    const val STOP_RADIUS = 50f // meters for bus stop proximity

    // Map Constants
    const val MAP_PADDING = 100 // pixels
    const val MAP_ANIMATION_DURATION = 300L // milliseconds
    const val MAP_CLUSTER_SIZE = 50 // pixels
    const val MAP_MARKER_SIZE = 40 // pixels
    const val MAP_LINE_WIDTH = 5f // pixels
    const val MAP_POLYLINE_WIDTH = 8f // pixels

    // Network Constants
    const val RETRY_ATTEMPTS = 3
    const val INITIAL_RETRY_DELAY = 1000L // milliseconds
    const val MAX_RETRY_DELAY = 5000L // milliseconds
    const val RETRY_BACKOFF_MULTIPLIER = 1.5
    
    // Cache Constants
    const val CACHE_SIZE = 10 * 1024 * 1024L // 10 MB
    const val CACHE_MAX_AGE = 7 * 24 * 60 * 60L // 7 days in seconds
    const val CACHE_MAX_STALE = 30 * 24 * 60 * 60L // 30 days in seconds

    // Notification Constants
    object NotificationChannels {
        const val BUS_ALERTS = "bus_alerts"
        const val JOURNEY_UPDATES = "journey_updates"
        const val GENERAL = "general_notifications"
    }

    object NotificationIds {
        const val BUS_ARRIVAL = 1001
        const val JOURNEY_UPDATE = 1002
        const val SERVICE_DISRUPTION = 1003
        const val LOCATION_SERVICE = 1004
    }

    // Validation Constants
    object Validation {
        const val MIN_PASSWORD_LENGTH = 8
        const val MAX_PASSWORD_LENGTH = 32
        const val MIN_NAME_LENGTH = 2
        const val MAX_NAME_LENGTH = 50
        const val PHONE_LENGTH = 10
        const val OTP_LENGTH = 6
        const val MAX_SEARCH_RESULTS = 20
    }

    // Time Constants
    object Time {
        const val SPLASH_DELAY = 2000L // milliseconds
        const val DEBOUNCE_TIME = 300L // milliseconds
        const val AUTO_REFRESH_INTERVAL = 30000L // 30 seconds
        const val SESSION_TIMEOUT = 30 * 60 * 1000L // 30 minutes
        const val CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 hours
    }

    // UI Constants
    object UI {
        const val ANIMATION_DURATION = 300L // milliseconds
        const val SNACKBAR_DURATION = 3000L // milliseconds
        const val MIN_CLICK_INTERVAL = 300L // milliseconds
        const val SHIMMER_DURATION = 1000L // milliseconds
        const val CROSSFADE_DURATION = 200L // milliseconds
        
        // Dimensions
        const val CARD_ELEVATION = 4f
        const val CARD_CORNER_RADIUS = 8f
        const val BUTTON_CORNER_RADIUS = 24f
        const val INPUT_FIELD_CORNER_RADIUS = 12f
        const val DIALOG_CORNER_RADIUS = 16f
    }

    // Error Messages
    object ErrorMessages {
        const val NO_INTERNET = "No internet connection"
        const val SERVER_ERROR = "Server error occurred"
        const val TIMEOUT_ERROR = "Request timed out"
        const val UNKNOWN_ERROR = "An unknown error occurred"
        const val LOCATION_PERMISSION_DENIED = "Location permission is required"
        const val LOCATION_DISABLED = "Location services are disabled"
        const val INVALID_CREDENTIALS = "Invalid email or password"
        const val WEAK_PASSWORD = "Password must be at least 8 characters"
        const val PASSWORDS_NOT_MATCH = "Passwords do not match"
        const val INVALID_EMAIL = "Invalid email address"
        const val INVALID_PHONE = "Invalid phone number"
    }

    // Intent Actions
    object IntentActions {
        const val NOTIFICATION_CLICKED = "com.dinapal.busdakho.NOTIFICATION_CLICKED"
        const val LOCATION_UPDATE = "com.dinapal.busdakho.LOCATION_UPDATE"
        const val BUS_ARRIVED = "com.dinapal.busdakho.BUS_ARRIVED"
        const val SERVICE_DISRUPTION = "com.dinapal.busdakho.SERVICE_DISRUPTION"
    }

    // Intent Extras
    object IntentExtras {
        const val BUS_ID = "bus_id"
        const val ROUTE_ID = "route_id"
        const val STOP_ID = "stop_id"
        const val NOTIFICATION_TYPE = "notification_type"
        const val LOCATION_LAT = "latitude"
        const val LOCATION_LNG = "longitude"
    }

    // Shared Preferences Keys
    object PreferenceKeys {
        const val USER_ID = "user_id"
        const val AUTH_TOKEN = "auth_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val DARK_THEME = "dark_theme"
        const val LANGUAGE = "language"
        const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val LAST_SYNC = "last_sync"
        const val ONBOARDING_COMPLETED = "onboarding_completed"
    }

    // API Endpoints
    object Endpoints {
        const val LOGIN = "auth/login"
        const val REGISTER = "auth/register"
        const val REFRESH_TOKEN = "auth/refresh"
        const val BUS_LOCATIONS = "buses/locations"
        const val ROUTES = "routes"
        const val STOPS = "stops"
        const val USER_PROFILE = "users/profile"
        const val NOTIFICATIONS = "notifications"
    }
}
