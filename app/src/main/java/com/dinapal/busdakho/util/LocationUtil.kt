package com.dinapal.busdakho.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationUtil {
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val BACKGROUND_LOCATION_PERMISSION = arrayOf(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    fun hasLocationPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return BACKGROUND_LOCATION_PERMISSION.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    suspend fun checkLocationSettings(context: Context): Result<Unit> {
        val locationRequest = LocationRequest.Builder(10000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        return suspendCancellableCoroutine { continuation ->
            task.addOnSuccessListener {
                continuation.resume(Result.success(Unit))
            }.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    continuation.resume(Result.failure(exception))
                } else {
                    continuation.resume(Result.failure(exception))
                }
            }
        }
    }

    fun getLocationSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    }

    fun getRequiredPermissions(): Array<String> = REQUIRED_PERMISSIONS

    fun getBackgroundLocationPermission(): Array<String> = BACKGROUND_LOCATION_PERMISSION

    fun shouldShowBackgroundPermissionRationale(context: Context): Boolean {
        // On Android 10 (API level 29) and higher, background location permission
        // requires additional rationale
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    }

    data class LocationState(
        val hasLocationPermissions: Boolean = false,
        val hasBackgroundLocationPermission: Boolean = false,
        val isLocationEnabled: Boolean = false,
        val shouldShowPermissionRationale: Boolean = false
    )

    fun getLocationState(context: Context): LocationState {
        return LocationState(
            hasLocationPermissions = hasLocationPermissions(context),
            hasBackgroundLocationPermission = hasBackgroundLocationPermission(context),
            isLocationEnabled = isLocationEnabled(context),
            shouldShowPermissionRationale = shouldShowBackgroundPermissionRationale(context)
        )
    }

    sealed class LocationError : Exception() {
        object PermissionsDenied : LocationError()
        object LocationDisabled : LocationError()
        object NoGooglePlayServices : LocationError()
        data class Other(override val message: String?) : LocationError()
    }

    fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(10000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(5000L)
            .setMaxUpdateDelayMillis(15000L)
            .build()
    }

    fun createGeofencingRequest(geofenceList: List<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    fun createGeofence(
        id: String,
        latitude: Double,
        longitude: Double,
        radius: Float,
        expirationDuration: Long = Geofence.NEVER_EXPIRE
    ): Geofence {
        return Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(expirationDuration)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
    }

    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun isWithinRadius(
        currentLat: Double,
        currentLon: Double,
        targetLat: Double,
        targetLon: Double,
        radiusInMeters: Float
    ): Boolean {
        val distance = calculateDistance(currentLat, currentLon, targetLat, targetLon)
        return distance <= radiusInMeters
    }
}
