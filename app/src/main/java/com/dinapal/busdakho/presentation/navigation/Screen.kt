package com.dinapal.busdakho.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object BusTracking : Screen(
        route = "bus_tracking",
        title = "Bus Tracking",
        icon = Icons.Default.DirectionsBus
    )

    object JourneyPlanning : Screen(
        route = "journey_planning",
        title = "Journey Planning",
        icon = Icons.Default.Map
    )

    object Profile : Screen(
        route = "profile",
        title = "Profile",
        icon = Icons.Default.Person
    )

    companion object {
        fun fromRoute(route: String?): Screen {
            return when (route) {
                BusTracking.route -> BusTracking
                JourneyPlanning.route -> JourneyPlanning
                Profile.route -> Profile
                else -> BusTracking
            }
        }

        fun bottomNavigationItems() = listOf(
            BusTracking,
            JourneyPlanning,
            Profile
        )
    }
}
