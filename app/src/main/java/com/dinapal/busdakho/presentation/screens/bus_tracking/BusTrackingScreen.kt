package com.dinapal.busdakho.presentation.screens.bus_tracking

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter
import com.dinapal.busdakho.presentation.theme.*

@Composable
fun BusTrackingScreen(
    viewModel: BusTrackingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }
    val uiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = false)) }
    val defaultLocation = LatLng(20.5937, 78.9629) // India's center coordinates
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 5f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            properties = mapProperties,
            uiSettings = uiSettings,
            cameraPositionState = cameraPositionState,
            onMapLoaded = {
                // Map loaded callback
            }
        ) {
            // Draw bus markers
            state.buses.forEach { bus ->
                Marker(
                    state = MarkerState(
                        position = LatLng(bus.latitude, bus.longitude)
                    ),
                    title = "Bus ${bus.busId}",
                    snippet = "Route: ${bus.routeId}",
                    onClick = {
                        viewModel.onEvent(BusTrackingEvent.SelectBus(bus.busId))
                        true
                    }
                )
            }

            // Draw selected bus marker with different color if any
            state.selectedBus?.let { selectedBus ->
                Marker(
                    state = MarkerState(
                        position = LatLng(selectedBus.latitude, selectedBus.longitude)
                    ),
                    title = "Bus ${selectedBus.busId}",
                    snippet = "Route: ${selectedBus.routeId}",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                )
            }
        }

        // Floating action buttons
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.onEvent(BusTrackingEvent.RefreshBuses) },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }

            FloatingActionButton(
                onClick = {
                    state.userLocation?.let { location ->
                        cameraPositionState.move(
                            CameraUpdateFactory.newLatLngZoom(location, 15f)
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "My Location"
                )
            }
        }

        // Loading indicator
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
            )
        }

        // Error message
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(text = error)
            }
        }

        // Last updated time
        Text(
            text = "Last updated: ${state.lastUpdated.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Selected bus info
        state.selectedBus?.let { bus ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = CardShape
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Bus ${bus.busId}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Route: ${bus.routeId}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Occupancy: ${bus.currentOccupancy}/${bus.capacity}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: ${bus.status}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
