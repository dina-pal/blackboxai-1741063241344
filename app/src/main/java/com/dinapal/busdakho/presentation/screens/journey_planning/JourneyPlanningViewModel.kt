package com.dinapal.busdakho.presentation.screens.journey_planning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinapal.busdakho.data.local.entity.RouteEntity
import com.dinapal.busdakho.data.local.entity.StopEntity
import com.dinapal.busdakho.domain.repository.RouteRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class JourneyPlanningState(
    val fromStop: StopEntity? = null,
    val toStop: StopEntity? = null,
    val availableRoutes: List<RouteEntity> = emptyList(),
    val alternativeRoutes: List<List<RouteEntity>> = emptyList(),
    val searchQuery: String = "",
    val suggestedStops: List<StopEntity> = emptyList(),
    val selectedRoute: RouteEntity? = null,
    val estimatedTime: Int? = null,
    val fare: Double? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSearchingFrom: Boolean = true
)

sealed class JourneyPlanningEvent {
    data class SearchStops(val query: String) : JourneyPlanningEvent()
    data class SelectFromStop(val stop: StopEntity) : JourneyPlanningEvent()
    data class SelectToStop(val stop: StopEntity) : JourneyPlanningEvent()
    data class SelectRoute(val route: RouteEntity) : JourneyPlanningEvent()
    object SwapStops : JourneyPlanningEvent()
    object ClearRoute : JourneyPlanningEvent()
    object ToggleSearchMode : JourneyPlanningEvent()
}

@OptIn(FlowPreview::class)
class JourneyPlanningViewModel(
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(JourneyPlanningState())
    val state: StateFlow<JourneyPlanningState> = _state.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.length >= 2) {
                        routeRepository.getAllStops()
                            .map { stops ->
                                stops.filter { stop ->
                                    stop.name.contains(query, ignoreCase = true)
                                }
                            }
                    } else {
                        flow { emit(emptyList()) }
                    }
                }
                .catch { e ->
                    _state.value = _state.value.copy(
                        error = e.message ?: "Error searching stops",
                        isLoading = false
                    )
                }
                .collect { stops ->
                    _state.value = _state.value.copy(
                        suggestedStops = stops,
                        isLoading = false
                    )
                }
        }
    }

    fun onEvent(event: JourneyPlanningEvent) {
        when (event) {
            is JourneyPlanningEvent.SearchStops -> {
                _state.value = _state.value.copy(isLoading = true)
                searchQuery.value = event.query
            }
            is JourneyPlanningEvent.SelectFromStop -> {
                _state.value = _state.value.copy(
                    fromStop = event.stop,
                    suggestedStops = emptyList()
                )
                findRoutes()
            }
            is JourneyPlanningEvent.SelectToStop -> {
                _state.value = _state.value.copy(
                    toStop = event.stop,
                    suggestedStops = emptyList()
                )
                findRoutes()
            }
            is JourneyPlanningEvent.SelectRoute -> {
                selectRoute(event.route)
            }
            JourneyPlanningEvent.SwapStops -> {
                _state.value = _state.value.copy(
                    fromStop = _state.value.toStop,
                    toStop = _state.value.fromStop
                )
                findRoutes()
            }
            JourneyPlanningEvent.ClearRoute -> {
                _state.value = _state.value.copy(
                    selectedRoute = null,
                    estimatedTime = null,
                    fare = null
                )
            }
            JourneyPlanningEvent.ToggleSearchMode -> {
                _state.value = _state.value.copy(
                    isSearchingFrom = !_state.value.isSearchingFrom,
                    suggestedStops = emptyList()
                )
            }
        }
    }

    private fun findRoutes() {
        val fromStop = _state.value.fromStop
        val toStop = _state.value.toStop

        if (fromStop != null && toStop != null) {
            viewModelScope.launch {
                _state.value = _state.value.copy(isLoading = true)
                try {
                    routeRepository.findRoutesBetweenStops(fromStop.stopId, toStop.stopId)
                        .collect { routes ->
                            _state.value = _state.value.copy(
                                availableRoutes = routes,
                                isLoading = false
                            )
                        }

                    // Get alternative routes with one transfer
                    routeRepository.getAlternativeRoutes(fromStop.stopId, toStop.stopId)
                        .collect { alternativeRoutes ->
                            _state.value = _state.value.copy(
                                alternativeRoutes = alternativeRoutes
                            )
                        }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(
                        error = e.message ?: "Error finding routes",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun selectRoute(route: RouteEntity) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val fromStop = _state.value.fromStop
                val toStop = _state.value.toStop

                if (fromStop != null && toStop != null) {
                    val fareResult = routeRepository.calculateFare(
                        route.routeId,
                        fromStop.stopId,
                        toStop.stopId
                    )
                    val timeResult = routeRepository.getEstimatedTravelTime(
                        route.routeId,
                        fromStop.stopId,
                        toStop.stopId
                    )

                    _state.value = _state.value.copy(
                        selectedRoute = route,
                        fare = fareResult.getOrNull(),
                        estimatedTime = timeResult.getOrNull(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Error selecting route",
                    isLoading = false
                )
            }
        }
    }
}
