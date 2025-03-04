package com.dinapal.busdakho.data.repository

import com.dinapal.busdakho.data.local.dao.RouteDao
import com.dinapal.busdakho.data.local.entity.RouteEntity
import com.dinapal.busdakho.data.local.entity.ScheduleEntity
import com.dinapal.busdakho.data.local.entity.StopEntity
import com.dinapal.busdakho.domain.repository.RouteRepository
import com.dinapal.busdakho.di.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class RouteRepositoryImpl(
    private val routeDao: RouteDao,
    private val apiService: ApiService
) : RouteRepository {

    override fun getAllRoutes(): Flow<List<RouteEntity>> =
        routeDao.getAllRoutes()
            .catch { e ->
                emit(emptyList())
            }

    override suspend fun getRouteById(routeId: String): RouteEntity? =
        try {
            routeDao.getRouteById(routeId)
        } catch (e: Exception) {
            null
        }

    override suspend fun insertRoute(route: RouteEntity) =
        try {
            routeDao.insertRoute(route)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun insertRoutes(routes: List<RouteEntity>) =
        try {
            routeDao.insertRoutes(routes)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun updateRoute(route: RouteEntity) =
        try {
            routeDao.updateRoute(route)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun deleteRoute(route: RouteEntity) =
        try {
            routeDao.deleteRoute(route)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun deleteAllRoutes() =
        try {
            routeDao.deleteAllRoutes()
        } catch (e: Exception) {
            // Log error
        }

    override fun searchRoutes(query: String): Flow<List<RouteEntity>> =
        routeDao.searchRoutes(query)
            .catch { e ->
                emit(emptyList())
            }

    override fun getRoutesByMaxDistance(maxDistance: Double): Flow<List<RouteEntity>> =
        routeDao.getRoutesByMaxDistance(maxDistance)
            .catch { e ->
                emit(emptyList())
            }

    override fun getRoutesByMaxFare(maxFare: Double): Flow<List<RouteEntity>> =
        routeDao.getRoutesByMaxFare(maxFare)
            .catch { e ->
                emit(emptyList())
            }

    override fun getRoutesByIds(routeIds: List<String>): Flow<List<RouteEntity>> =
        routeDao.getRoutesByIds(routeIds)
            .catch { e ->
                emit(emptyList())
            }

    override fun getRoutesByStop(stopId: String): Flow<List<RouteEntity>> =
        routeDao.getRoutesByStop(stopId)
            .catch { e ->
                emit(emptyList())
            }

    override fun findRoutesBetweenStops(
        startStopId: String,
        endStopId: String
    ): Flow<List<RouteEntity>> =
        routeDao.findRoutesBetweenStops(startStopId, endStopId)
            .catch { e ->
                emit(emptyList())
            }

    override fun getAllStops(): Flow<List<StopEntity>> =
        routeDao.getAllStops()
            .catch { e ->
                emit(emptyList())
            }

    override fun getRoutesByOperatingDay(day: String): Flow<List<RouteEntity>> =
        routeDao.getRoutesByOperatingDay(day)
            .catch { e ->
                emit(emptyList())
            }

    override suspend fun fetchRoutes(): Result<List<RouteEntity>> =
        try {
            val routes = apiService.getRoutes()
            insertRoutes(routes)
            Result.success(routes)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun fetchRouteDetails(routeId: String): Result<RouteEntity> =
        try {
            val route = routeDao.getRouteById(routeId)
                ?: throw IllegalStateException("Route not found")
            Result.success(route)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun syncRouteData() {
        try {
            val routes = apiService.getRoutes()
            insertRoutes(routes)
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun calculateFare(
        routeId: String,
        startStopId: String,
        endStopId: String
    ): Result<Double> =
        try {
            val route = routeDao.getRouteById(routeId)
                ?: throw IllegalStateException("Route not found")
            
            // Simple fare calculation based on route's base fare
            // In a real app, this would be more complex
            Result.success(route.fare)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun getEstimatedTravelTime(
        routeId: String,
        startStopId: String,
        endStopId: String
    ): Result<Int> =
        try {
            val route = routeDao.getRouteById(routeId)
                ?: throw IllegalStateException("Route not found")
            
            // Simple estimation based on route's estimated time
            // In a real app, this would consider traffic, time of day, etc.
            Result.success(route.estimatedTime)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun getAlternativeRoutes(
        startStopId: String,
        endStopId: String,
        maxTransfers: Int
    ): Flow<List<List<RouteEntity>>> =
        routeDao.findRoutesBetweenStops(startStopId, endStopId)
            .map { directRoutes ->
                // For now, just return direct routes
                // In a real app, this would calculate routes with transfers
                listOf(directRoutes)
            }
            .catch { e ->
                emit(emptyList())
            }

    override suspend fun updateRouteSchedule(
        routeId: String,
        updatedSchedule: List<ScheduleEntity>
    ) {
        try {
            val route = routeDao.getRouteById(routeId)
                ?: throw IllegalStateException("Route not found")
            
            val updatedRoute = route.copy(schedule = updatedSchedule)
            routeDao.updateRoute(updatedRoute)
        } catch (e: Exception) {
            // Log error
        }
    }
}
