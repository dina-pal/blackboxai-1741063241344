package com.dinapal.busdakho.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dinapal.busdakho.data.local.dao.BusDao
import com.dinapal.busdakho.data.local.dao.RouteDao
import com.dinapal.busdakho.data.local.dao.UserDao
import com.dinapal.busdakho.data.local.entity.BusEntity
import com.dinapal.busdakho.data.local.entity.RouteEntity
import com.dinapal.busdakho.data.local.entity.UserEntity
import com.dinapal.busdakho.data.local.converter.Converters

@Database(
    entities = [
        BusEntity::class,
        RouteEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BusDakhoDatabase : RoomDatabase() {
    abstract fun busDao(): BusDao
    abstract fun routeDao(): RouteDao
    abstract fun userDao(): UserDao
}

// Entities
@kotlinx.serialization.Serializable
data class BusEntity(
    @androidx.room.PrimaryKey
    val busId: String,
    val routeId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val lastUpdated: Long,
    val status: String,
    val capacity: Int,
    val currentOccupancy: Int
)

@kotlinx.serialization.Serializable
data class RouteEntity(
    @androidx.room.PrimaryKey
    val routeId: String,
    val name: String,
    val stops: List<StopEntity>,
    val schedule: List<ScheduleEntity>,
    val fare: Double,
    val distance: Double,
    val estimatedTime: Int // in minutes
)

@kotlinx.serialization.Serializable
data class UserEntity(
    @androidx.room.PrimaryKey
    val userId: String,
    val name: String,
    val email: String,
    val phone: String,
    val favoriteRoutes: List<String>,
    val favoriteStops: List<String>,
    val lastUpdated: Long
)

@kotlinx.serialization.Serializable
data class StopEntity(
    val stopId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val amenities: List<String>
)

@kotlinx.serialization.Serializable
data class ScheduleEntity(
    val departureTime: String,
    val arrivalTime: String,
    val frequency: Int, // in minutes
    val daysOperating: List<String>
)
