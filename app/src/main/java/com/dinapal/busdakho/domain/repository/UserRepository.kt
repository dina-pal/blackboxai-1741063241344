package com.dinapal.busdakho.domain.repository

import com.dinapal.busdakho.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getAllUsers(): Flow<List<UserEntity>>
    
    suspend fun getUserById(userId: String): UserEntity?
    
    suspend fun getUserByEmail(email: String): UserEntity?
    
    suspend fun getUserByPhone(phone: String): UserEntity?
    
    suspend fun insertUser(user: UserEntity)
    
    suspend fun insertUsers(users: List<UserEntity>)
    
    suspend fun updateUser(user: UserEntity)
    
    suspend fun deleteUser(user: UserEntity)
    
    suspend fun deleteAllUsers()
    
    suspend fun updateFavoriteRoutes(userId: String, favoriteRoutes: List<String>)
    
    fun getFavoriteRoutes(userId: String): Flow<List<String>>
    
    suspend fun updateFavoriteStops(userId: String, favoriteStops: List<String>)
    
    fun getFavoriteStops(userId: String): Flow<List<String>>
    
    suspend fun updateLastUpdatedTimestamp(userId: String, timestamp: Long)
    
    suspend fun getLastUpdatedTimestamp(userId: String): Long?
    
    suspend fun updateUserProfile(userId: String, name: String, email: String, phone: String)
    
    fun searchUsers(query: String): Flow<List<UserEntity>>
    
    suspend fun userExists(userId: String): Boolean
    
    suspend fun emailExists(email: String): Boolean
    
    suspend fun phoneExists(phone: String): Boolean
    
    // Network operations
    suspend fun syncUserData(userId: String): Result<Unit>
    
    suspend fun fetchUserProfile(userId: String): Result<UserEntity>
    
    suspend fun updateUserProfileOnServer(user: UserEntity): Result<Unit>
    
    // Authentication operations
    suspend fun loginUser(email: String, password: String): Result<UserEntity>
    
    suspend fun registerUser(email: String, password: String, name: String, phone: String): Result<UserEntity>
    
    suspend fun resetPassword(email: String): Result<Unit>
    
    suspend fun verifyPhone(phone: String, code: String): Result<Boolean>
    
    // Preferences and Settings
    suspend fun updateNotificationPreferences(userId: String, preferences: Map<String, Boolean>)
    
    suspend fun getNotificationPreferences(userId: String): Flow<Map<String, Boolean>>
    
    suspend fun updateLanguagePreference(userId: String, languageCode: String)
    
    suspend fun getLanguagePreference(userId: String): String
    
    // Travel History
    suspend fun addToTravelHistory(userId: String, routeId: String, timestamp: Long)
    
    fun getTravelHistory(userId: String): Flow<List<TravelHistoryEntry>>
}

data class TravelHistoryEntry(
    val routeId: String,
    val timestamp: Long,
    val startStop: String,
    val endStop: String,
    val fare: Double
)
