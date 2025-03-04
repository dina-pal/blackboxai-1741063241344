package com.dinapal.busdakho.presentation.screens.journey_planning

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.dinapal.busdakho.presentation.theme.*
import com.dinapal.busdakho.data.local.entity.RouteEntity
import com.dinapal.busdakho.data.local.entity.StopEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyPlanningScreen(
    viewModel: JourneyPlanningViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // From Stop
                OutlinedTextField(
                    value = state.fromStop?.name ?: "",
                    onValueChange = { query ->
                        viewModel.onEvent(JourneyPlanningEvent.SearchStops(query))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("From") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, "From location")
                    },
                    shape = SearchBarShape
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Swap button
                IconButton(
                    onClick = { viewModel.onEvent(JourneyPlanningEvent.SwapStops) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.SwapVert, "Swap locations")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // To Stop
                OutlinedTextField(
                    value = state.toStop?.name ?: "",
                    onValueChange = { query ->
                        viewModel.onEvent(JourneyPlanningEvent.SearchStops(query))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("To") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, "To location")
                    },
                    shape = SearchBarShape
                )
            }
        }

        // Suggested Stops
        AnimatedVisibility(
            visible = state.suggestedStops.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = CardShape
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(state.suggestedStops) { stop ->
                        StopItem(
                            stop = stop,
                            onClick = {
                                if (state.isSearchingFrom) {
                                    viewModel.onEvent(JourneyPlanningEvent.SelectFromStop(stop))
                                } else {
                                    viewModel.onEvent(JourneyPlanningEvent.SelectToStop(stop))
                                }
                            }
                        )
                    }
                }
            }
        }

        // Available Routes
        if (state.fromStop != null && state.toStop != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Available Routes",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(state.availableRoutes) { route ->
                    RouteItem(
                        route = route,
                        isSelected = route == state.selectedRoute,
                        onClick = {
                            viewModel.onEvent(JourneyPlanningEvent.SelectRoute(route))
                        }
                    )
                }

                // Alternative routes with transfers
                if (state.alternativeRoutes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Alternative Routes (with transfers)",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(state.alternativeRoutes) { routeList ->
                        AlternativeRouteItem(
                            routes = routeList,
                            onClick = {
                                // Handle alternative route selection
                            }
                        )
                    }
                }
            }
        }

        // Loading indicator
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Error message
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopItem(
    stop: StopEntity,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = stop.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Icon(Icons.Default.LocationOn, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteItem(
    route: RouteEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onClick
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            supportingContent = {
                Text(
                    text = "Estimated time: ${route.estimatedTime} min • Fare: ₹${route.fare}",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingContent = {
                Icon(Icons.Default.DirectionsBus, contentDescription = null)
            },
            trailingContent = {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = "Selected")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlternativeRouteItem(
    routes: List<RouteEntity>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = CardShape,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            routes.forEachIndexed { index, route ->
                if (index > 0) {
                    Icon(
                        Icons.Default.SwapVert,
                        contentDescription = "Transfer",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total time: ${routes.sumOf { it.estimatedTime }} min",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
