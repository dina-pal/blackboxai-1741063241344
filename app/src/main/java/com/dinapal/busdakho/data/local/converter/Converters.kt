package com.dinapal.busdakho.data.local.converter

import androidx.room.TypeConverter
import com.dinapal.busdakho.data.local.entity.ScheduleEntity
import com.dinapal.busdakho.data.local.entity.StopEntity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    @TypeConverter
    fun fromStopList(value: List<StopEntity>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStopList(value: String): List<StopEntity> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromScheduleList(value: List<ScheduleEntity>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toScheduleList(value: String): List<ScheduleEntity> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
