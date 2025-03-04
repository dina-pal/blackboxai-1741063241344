package com.dinapal.busdakho.data.repository

import com.dinapal.busdakho.data.local.dao.UserDao
import com.dinapal.busdakho.data.local.entity.UserEntity
import com.dinapal.busdakho.domain.repository.UserRepository
import com.dinapal.busdakho.domain.repository.TravelHistoryEntry
import com.dinapal.busdakho.di.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.io.IOException

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val preferences: android.content.SharedPreferences
) : UserRepository {

    override fun getAllUsers(): Flow<List<UserEntity>> =
        userDao.getAllUsers()
            .catch { e ->
                emit(emptyList())
            }

    override suspend fun getUserById(userId: String): UserEntity? =
        try {
            userDao.getUserById(userId)
        } catch (e: Exception) {
            null
        }

    override suspend fun getUserByEmail(email: String): UserEntity? =
        try {
            userDao.getUserByEmail(email)
        } catch (e: Exception) {
            null
        }

    override suspend fun getUserByPhone(phone: String): UserEntity? =
        try {
            userDao.getUserByPhone(phone)
        } catch (e: Exception) {
            null
        }

    override suspend fun insertUser(user: UserEntity) =
        try {
            userDao.insertUser(user)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun insertUsers(users: List<UserEntity>) =
        try {
            userDao.insertUsers(users)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun updateUser(user: UserEntity) =
        try {
            userDao.updateUser(user)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun deleteUser(user: UserEntity) =
        try {
            userDao.deleteUser(user)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun deleteAllUsers() =
        try {
            userDao.deleteAllUsers()
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun updateFavoriteRoutes(userId: String, favoriteRoutes: List<String>) =
        try {
            userDao.updateFavoriteRoutes(userId, favoriteRoutes)
        } catch (e: Exception) {
            // Log error
        }

    override fun getFavoriteRoutes(userId: String): Flow<List<String>> =
        userDao.getFavoriteRoutes(userId)
            .catch { e ->
                emit(emptyList())
            }

    override suspend fun updateFavoriteStops(userId: String, favoriteStops: List<String>) =
        try {
            userDao.updateFavoriteStops(userId, favoriteStops)
        } catch (e: Exception) {
            // Log error
        }

    override fun getFavoriteStops(userId: String): Flow<List<String>> =
        userDao.getFavoriteStops(userId)
            .catch { e ->
                emit(emptyList())
            }

    override suspend fun updateLastUpdatedTimestamp(userId: String, timestamp: Long) =
        try {
            userDao.updateLastUpdatedTimestamp(userId, timestamp)
        } catch (e: Exception) {
            // Log error
        }

    override suspend fun getLastUpdatedTimestamp(userId: String): Long? =
        try {
            userDao.getLastUpdatedTimestamp(userId)
        } catch (e: Exception) {
            null
        }

    override suspend fun updateUserProfile(
        userId: String,
        name: String,
        email: String,
        phone: String
    ) = try {
        userDao.updateUserProfile(userId, name, email, phone)
    } catch (e: Exception) {
        // Log error
    }

    override fun searchUsers(query: String): Flow<List<UserEntity>> =
        userDao.searchUsers(query)
            .catch { e ->
                emit(emptyList())
            }

    override suspend fun userExists(userId: String): Boolean =
        try {
            userDao.userExists(userId)
        } catch (e: Exception) {
            false
        }

    override suspend fun emailExists(email: String): Boolean =
        try {
            userDao.emailExists(email)
        } catch (e: Exception) {
            false
        }

    override suspend fun phoneExists(phone: String): Boolean =
        try {
            userDao.phoneExists(phone)
        } catch (e: Exception) {
            false
        }

    override suspend fun syncUserData(userId: String): Result<Unit> =
        try {
            val userProfile = apiService.getUserProfile(userId)
            insertUser(UserEntity(
                userId = userProfile.userId,
                name = userProfile.name,
                email = userProfile.email,
                phone = userProfile.phone,
                favoriteRoutes = userProfile.favoriteRoutes,
                favoriteStops = userProfile.favoriteStops,
                lastUpdated = System.currentTimeMillis()
            ))
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun fetchUserProfile(userId: String): Result<UserEntity> =
        try {
            val userProfile = apiService.getUserProfile(userId)
            val userEntity = UserEntity(
                userId = userProfile.userId,
                name = userProfile.name,
                email = userProfile.email,
                phone = userProfile.phone,
                favoriteRoutes = userProfile.favoriteRoutes,
                favoriteStops = userProfile.favoriteStops,
                lastUpdated = System.currentTimeMillis()
            )
            insertUser(userEntity)
            Result.success(userEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun updateUserProfileOnServer(user: UserEntity): Result<Unit> =
        try {
            // TODO: Implement API call to update user profile on server
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun loginUser(email: String, password: String): Result<UserEntity> =
        try {
            // TODO: Implement actual login logic with API
            Result.failure(NotImplementedError("Login not implemented"))
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun registerUser(
        email: String,
        password: String,
        name: String,
        phone: String
    ): Result<UserEntity> =
        try {
            // TODO: Implement actual registration logic with API
            Result.failure(NotImplementedError("Registration not implemented"))
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun resetPassword(email: String): Result<Unit> =
        try {
            // TODO: Implement password reset logic with API
            Result.failure(NotImplementedError("Password reset not implemented"))
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun verifyPhone(phone: String, code: String): Result<Boolean> =
        try {
            // TODO: Implement phone verification logic with API
            Result.failure(NotImplementedError("Phone verification not implemented"))
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun updateNotificationPreferences(
        userId: String,
        preferences: Map<String, Boolean>
    ) {
        this.preferences.edit().apply {
            preferences.forEach { (key, value) ->
                putBoolean("notification_$userId$key", value)
            }
        }.apply()
    }

    override suspend fun getNotificationPreferences(
        userId: String
    ): Flow<Map<String, Boolean>> = flow {
        val prefs = mutableMapOf<String, Boolean>()
        // Add default notification preferences
        prefs["route_updates"] = this@UserRepositoryImpl.preferences
            .getBoolean("notification_${userId}route_updates", true)
        prefs["promotions"] = this@UserRepositoryImpl.preferences
            .getBoolean("notification_${userId}promotions", true)
        emit(prefs)
    }

    override suspend fun updateLanguagePreference(userId: String, languageCode: String) {
        preferences.edit().putString("language_$userId", languageCode).apply()
    }

    override suspend fun getLanguagePreference(userId: String): String =
        preferences.getString("language_$userId", "en") ?: "en"

    override suspend fun addToTravelHistory(
        userId: String,
        routeId: String,
        timestamp: Long
    ) {
        // TODO: Implement travel history storage
    }

    override fun getTravelHistory(userId: String): Flow<List<TravelHistoryEntry>> = flow {
        // TODO: Implement travel history retrieval
        emit(emptyList())
    }
}
