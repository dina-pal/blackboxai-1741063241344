package com.dinapal.busdakho.domain.repository

import com.dinapal.busdakho.data.local.entity.BusEntity
import kotlinx.coroutines.flow.Flow

interface BusRepository {
    fun getAllBuses(): Flow<List<BusEntity>>
    
    suspend fun getBusById(busId: String): BusEntity?
    
    fun getBusesByRoute(routeId: String): Flow<List<BusEntity>>
    
    suspend fun insertBus(bus: BusEntity)
    
    suspend fun insertBuses(buses: List<BusEntity>)
    
    suspend fun updateBus(bus: BusEntity)
    
    suspend fun deleteBus(bus: BusEntity)
    
    suspend fun deleteAllBuses()
    
    fun getBusesInArea(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Flow<List<BusEntity>>
    
    suspend fun updateBusLocation(busId: String, latitude: Double, longitude: Double, speed: Double, timestamp: Long)
    
    suspend fun updateBusOccupancy(busId: String, occupancy: Int)
    
    fun getBusesByStatus(status: String): Flow<List<BusEntity>>
    
    // Network operations
    suspend fun fetchRealTimeBusLocations(): Result<List<BusEntity>>
    
    suspend fun syncBusData()
}
