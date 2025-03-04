package com.dinapal.busdakho.data.repository

import com.dinapal.busdakho.data.local.dao.BusDao
import com.dinapal.busdakho.data.local.entity.BusEntity
import com.dinapal.busdakho.domain.repository.BusRepository
import com.dinapal.busdakho.di.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class BusRepositoryImpl(
    private val busDao: BusDao,
    private val apiService: ApiService
) : BusRepository {

    override fun getAllBuses(): Flow<List<BusEntity>> =
        busDao.getAllBuses()
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
            }

    override suspend fun getBusById(busId: String): BusEntity? =
        try {
            busDao.getBusById(busId)
        } catch (e: Exception) {
            null
        }

    override fun getBusesByRoute(routeId: String): Flow<List<BusEntity>> =
        busDao.getBusesByRoute(routeId)
            .catch { e ->
                emit(emptyList())
            }

    override suspend fun insertBus(bus: BusEntity) =
        try {
            busDao.insertBus(bus)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun insertBuses(buses: List<BusEntity>) =
        try {
            busDao.insertBuses(buses)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun updateBus(bus: BusEntity) =
        try {
            busDao.updateBus(bus)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun deleteBus(bus: BusEntity) =
        try {
            busDao.deleteBus(bus)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun deleteAllBuses() =
        try {
            busDao.deleteAllBuses()
        } catch (e: Exception) {
            // Log error
        }

    override fun getBusesInArea(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<BusEntity>> =
        busDao.getBusesInArea(minLat, maxLat, minLng, maxLng)
            .catch { e ->
                emit(emptyList())
            }

    override suspend fun updateBusLocation(
        busId: String,
        latitude: Double,
        longitude: Double,
        speed: Double,
        timestamp: Long
    ) = try {
        busDao.updateBusLocation(busId, latitude, longitude, speed, timestamp)
    } catch (e: Exception) {
        // Log error
    }

    override suspend fun updateBusOccupancy(busId: String, occupancy: Int) =
        try {
            busDao.updateBusOccupancy(busId, occupancy)
        } catch (e: Exception) {
            // Log error
        }

    override fun getBusesByStatus(status: String): Flow<List<BusEntity>> =
        busDao.getBusesByStatus(status)
            .catch { e ->
                emit(emptyList())
            }

    override suspend fun fetchRealTimeBusLocations(): Result<List<BusEntity>> =
        try {
            val buses = apiService.getBusLocations()
            insertBuses(buses)
            Result.success(buses)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun syncBusData() {
        try {
            val buses = apiService.getBusLocations()
            insertBuses(buses)
        } catch (e: Exception) {
            // Log error
        }
    }
}
