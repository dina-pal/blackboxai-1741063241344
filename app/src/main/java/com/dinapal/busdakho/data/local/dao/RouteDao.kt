package com.dinapal.busdakho.data.local.dao

import androidx.room.*
import com.dinapal.busdakho.data.local.entity.RouteEntity
import com.dinapal.busdakho.data.local.entity.StopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM RouteEntity")
    fun getAllRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM RouteEntity WHERE routeId = :routeId")
    suspend fun getRouteById(routeId: String): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<RouteEntity>)

    @Update
    suspend fun updateRoute(route: RouteEntity)

    @Delete
    suspend fun deleteRoute(route: RouteEntity)

    @Query("DELETE FROM RouteEntity")
    suspend fun deleteAllRoutes()

    @Transaction
    @Query("SELECT * FROM RouteEntity WHERE " +
           "name LIKE '%' || :query || '%' OR " +
           "routeId LIKE '%' || :query || '%'")
    fun searchRoutes(query: String): Flow<List<RouteEntity>>

    @Query("SELECT * FROM RouteEntity WHERE distance <= :maxDistance")
    fun getRoutesByMaxDistance(maxDistance: Double): Flow<List<RouteEntity>>

    @Query("SELECT * FROM RouteEntity WHERE fare <= :maxFare")
    fun getRoutesByMaxFare(maxFare: Double): Flow<List<RouteEntity>>

    @Transaction
    @Query("SELECT * FROM RouteEntity WHERE routeId IN (:routeIds)")
    fun getRoutesByIds(routeIds: List<String>): Flow<List<RouteEntity>>

    // Custom queries for finding routes by stops
    @Query("SELECT * FROM RouteEntity WHERE :stopId IN (SELECT stopId FROM json_each(stops) WHERE json_extract(value, '$.stopId') = :stopId)")
    fun getRoutesByStop(stopId: String): Flow<List<RouteEntity>>

    @Query("SELECT * FROM RouteEntity WHERE " +
           ":startStopId IN (SELECT stopId FROM json_each(stops) WHERE json_extract(value, '$.stopId') = :startStopId) AND " +
           ":endStopId IN (SELECT stopId FROM json_each(stops) WHERE json_extract(value, '$.stopId') = :endStopId)")
    fun findRoutesBetweenStops(startStopId: String, endStopId: String): Flow<List<RouteEntity>>

    // Query to get all unique stops across all routes
    @Query("SELECT DISTINCT value FROM RouteEntity, json_each(stops)")
    fun getAllStops(): Flow<List<StopEntity>>

    // Query to get routes operating on specific days
    @Query("SELECT * FROM RouteEntity WHERE :day IN (SELECT value FROM json_each(schedule) WHERE json_extract(value, '$.daysOperating') LIKE '%' || :day || '%')")
    fun getRoutesByOperatingDay(day: String): Flow<List<RouteEntity>>
}
