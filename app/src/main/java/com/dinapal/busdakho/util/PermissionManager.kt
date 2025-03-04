package com.dinapal.busdakho.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class PermissionManager(private val context: Context) {

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: Flow<PermissionState> = _permissionState

    init {
        updatePermissionState()
    }

    fun updatePermissionState() {
        _permissionState.update { currentState ->
            currentState.copy(
                locationPermissionGranted = hasLocationPermission(),
                backgroundLocationPermissionGranted = hasBackgroundLocationPermission(),
                notificationPermissionGranted = hasNotificationPermission(),
                cameraPermissionGranted = hasCameraPermission()
            )
        }
    }

    fun hasLocationPermission(): Boolean {
        return LOCATION_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldShowLocationPermissionRationale(): Boolean {
        return LOCATION_PERMISSIONS.any { permission ->
            shouldShowRequestPermissionRationale(permission)
        }
    }

    fun shouldShowBackgroundLocationPermissionRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            false
        }
    }

    fun shouldShowNotificationPermissionRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            false
        }
    }

    fun shouldShowCameraPermissionRationale(): Boolean {
        return shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
    }

    private fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return if (context is android.app.Activity) {
            context.shouldShowRequestPermissionRationale(permission)
        } else {
            false
        }
    }

    fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        if (!hasLocationPermission()) {
            permissions.addAll(LOCATION_PERMISSIONS)
        }

        if (!hasBackgroundLocationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (!hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (!hasCameraPermission()) {
            permissions.add(Manifest.permission.CAMERA)
        }

        return permissions
    }

    fun getAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    companion object {
        private val LOCATION_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        @Volatile
        private var instance: PermissionManager? = null

        fun getInstance(context: Context): PermissionManager {
            return instance ?: synchronized(this) {
                instance ?: PermissionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    data class PermissionState(
        val locationPermissionGranted: Boolean = false,
        val backgroundLocationPermissionGranted: Boolean = false,
        val notificationPermissionGranted: Boolean = false,
        val cameraPermissionGranted: Boolean = false
    )

    sealed class PermissionResult {
        object Granted : PermissionResult()
        object Denied : PermissionResult()
        object ShowRationale : PermissionResult()
        object PermanentlyDenied : PermissionResult()
    }

    fun getPermissionText(permission: String): PermissionText {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> PermissionText(
                title = "Location Permission Required",
                description = "This app needs access to location to show nearby buses and stops.",
                rationaleText = "Location permission is essential for showing nearby buses and calculating arrival times. Please grant this permission to use these features."
            )
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> PermissionText(
                title = "Background Location Required",
                description = "Allow the app to access location in the background for real-time bus tracking.",
                rationaleText = "Background location is needed to track buses and provide notifications when they're approaching your stop, even when the app is not open."
            )
            Manifest.permission.POST_NOTIFICATIONS -> PermissionText(
                title = "Notifications Permission Required",
                description = "Enable notifications to receive updates about bus arrivals and service changes.",
                rationaleText = "Notifications help you stay informed about bus arrivals, delays, and service updates. Please enable notifications to get the most out of the app."
            )
            Manifest.permission.CAMERA -> PermissionText(
                title = "Camera Permission Required",
                description = "Camera access is needed to scan QR codes for quick access to bus information.",
                rationaleText = "The camera is used to scan QR codes at bus stops for quick access to schedules and real-time information."
            )
            else -> PermissionText(
                title = "Permission Required",
                description = "This permission is required for app functionality.",
                rationaleText = "Please grant this permission to use all features of the app."
            )
        }
    }

    data class PermissionText(
        val title: String,
        val description: String,
        val rationaleText: String
    )
}
