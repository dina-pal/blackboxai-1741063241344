package com.dinapal.busdakho.domain.repository

import com.dinapal.busdakho.data.local.entity.RouteEntity
import com.dinapal.busdakho.data.local.entity.StopEntity
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    fun getAllRoutes(): Flow<List<RouteEntity>>
    
    suspend fun getRouteById(routeId: String): RouteEntity?
    
    suspend fun insertRoute(route: RouteEntity)
    
    suspend fun insertRoutes(routes: List<RouteEntity>)
    
    suspend fun updateRoute(route: RouteEntity)
    
    suspend fun deleteRoute(route: RouteEntity)
    
    suspend fun deleteAllRoutes()
    
    fun searchRoutes(query: String): Flow<List<RouteEntity>>
    
    fun getRoutesByMaxDistance(maxDistance: Double): Flow<List<RouteEntity>>
    
    fun getRoutesByMaxFare(maxFare: Double): Flow<List<RouteEntity>>
    
    fun getRoutesByIds(routeIds: List<String>): Flow<List<RouteEntity>>
    
    fun getRoutesByStop(stopId: String): Flow<List<RouteEntity>>
    
    fun findRoutesBetweenStops(startStopId: String, endStopId: String): Flow<List<RouteEntity>>
    
    fun getAllStops(): Flow<List<StopEntity>>
    
    fun getRoutesByOperatingDay(day: String): Flow<List<RouteEntity>>
    
    // Network operations
    suspend fun fetchRoutes(): Result<List<RouteEntity>>
    
    suspend fun fetchRouteDetails(routeId: String): Result<RouteEntity>
    
    suspend fun syncRouteData()
    
    // Additional operations
    suspend fun calculateFare(routeId: String, startStopId: String, endStopId: String): Result<Double>
    
    suspend fun getEstimatedTravelTime(routeId: String, startStopId: String, endStopId: String): Result<Int>
    
    fun getAlternativeRoutes(startStopId: String, endStopId: String, maxTransfers: Int = 1): Flow<List<List<RouteEntity>>>
    
    suspend fun updateRouteSchedule(routeId: String, updatedSchedule: List<com.dinapal.busdakho.data.local.entity.ScheduleEntity>)
}
