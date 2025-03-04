package com.dinapal.busdakho.presentation.screens.bus_tracking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinapal.busdakho.data.local.entity.BusEntity
import com.dinapal.busdakho.domain.repository.BusRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

data class BusTrackingState(
    val buses: List<BusEntity> = emptyList(),
    val selectedBus: BusEntity? = null,
    val userLocation: LatLng? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)

sealed class BusTrackingEvent {
    data class SelectBus(val busId: String) : BusTrackingEvent()
    data class UpdateUserLocation(val location: LatLng) : BusTrackingEvent()
    object RefreshBuses : BusTrackingEvent()
    object ClearSelection : BusTrackingEvent()
}

class BusTrackingViewModel(
    private val busRepository: BusRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BusTrackingState())
    val state: StateFlow<BusTrackingState> = _state.asStateFlow()

    private var currentBoundingBox: BoundingBox? by mutableStateOf(null)

    init {
        refreshBuses()
        startBusLocationUpdates()
    }

    fun onEvent(event: BusTrackingEvent) {
        when (event) {
            is BusTrackingEvent.SelectBus -> selectBus(event.busId)
            is BusTrackingEvent.UpdateUserLocation -> updateUserLocation(event.location)
            is BusTrackingEvent.RefreshBuses -> refreshBuses()
            BusTrackingEvent.ClearSelection -> clearSelection()
        }
    }

    private fun selectBus(busId: String) {
        viewModelScope.launch {
            val bus = busRepository.getBusById(busId)
            _state.value = _state.value.copy(selectedBus = bus)
        }
    }

    private fun updateUserLocation(location: LatLng) {
        _state.value = _state.value.copy(userLocation = location)
        updateVisibleBuses()
    }

    private fun refreshBuses() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                busRepository.fetchRealTimeBusLocations()
                    .onSuccess {
                        _state.value = _state.value.copy(
                            buses = it,
                            lastUpdated = LocalDateTime.now(),
                            isLoading = false
                        )
                    }
                    .onFailure {
                        _state.value = _state.value.copy(
                            error = it.message ?: "Failed to fetch buses",
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Unknown error occurred",
                    isLoading = false
                )
            }
        }
    }

    private fun clearSelection() {
        _state.value = _state.value.copy(selectedBus = null)
    }

    private fun startBusLocationUpdates() {
        viewModelScope.launch {
            while (true) {
                refreshBuses()
                kotlinx.coroutines.delay(30000) // Update every 30 seconds
            }
        }
    }

    fun updateMapBounds(boundingBox: BoundingBox) {
        currentBoundingBox = boundingBox
        updateVisibleBuses()
    }

    private fun updateVisibleBuses() {
        viewModelScope.launch {
            currentBoundingBox?.let { bounds ->
                busRepository.getBusesInArea(
                    minLat = bounds.southwest.latitude,
                    maxLat = bounds.northeast.latitude,
                    minLng = bounds.southwest.longitude,
                    maxLng = bounds.northeast.longitude
                ).collect { buses ->
                    _state.value = _state.value.copy(buses = buses)
                }
            }
        }
    }
}

data class BoundingBox(
    val southwest: LatLng,
    val northeast: LatLng
)
