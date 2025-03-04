package com.dinapal.busdakho.data.local.dao

import androidx.room.*
import com.dinapal.busdakho.data.local.entity.BusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusDao {
    @Query("SELECT * FROM BusEntity")
    fun getAllBuses(): Flow<List<BusEntity>>

    @Query("SELECT * FROM BusEntity WHERE busId = :busId")
    suspend fun getBusById(busId: String): BusEntity?

    @Query("SELECT * FROM BusEntity WHERE routeId = :routeId")
    fun getBusesByRoute(routeId: String): Flow<List<BusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBus(bus: BusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuses(buses: List<BusEntity>)

    @Update
    suspend fun updateBus(bus: BusEntity)

    @Delete
    suspend fun deleteBus(bus: BusEntity)

    @Query("DELETE FROM BusEntity")
    suspend fun deleteAllBuses()

    @Query("SELECT * FROM BusEntity WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng")
    fun getBusesInArea(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Flow<List<BusEntity>>

    @Query("UPDATE BusEntity SET latitude = :latitude, longitude = :longitude, speed = :speed, lastUpdated = :timestamp WHERE busId = :busId")
    suspend fun updateBusLocation(busId: String, latitude: Double, longitude: Double, speed: Double, timestamp: Long)

    @Query("UPDATE BusEntity SET currentOccupancy = :occupancy WHERE busId = :busId")
    suspend fun updateBusOccupancy(busId: String, occupancy: Int)

    @Query("SELECT * FROM BusEntity WHERE status = :status")
    fun getBusesByStatus(status: String): Flow<List<BusEntity>>
}
