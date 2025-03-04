package com.dinapal.busdakho.data.local.dao

import androidx.room.*
import com.dinapal.busdakho.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM UserEntity")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM UserEntity WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM UserEntity WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM UserEntity WHERE phone = :phone")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM UserEntity")
    suspend fun deleteAllUsers()

    // Favorite routes operations
    @Query("UPDATE UserEntity SET favoriteRoutes = :favoriteRoutes WHERE userId = :userId")
    suspend fun updateFavoriteRoutes(userId: String, favoriteRoutes: List<String>)

    @Query("SELECT favoriteRoutes FROM UserEntity WHERE userId = :userId")
    fun getFavoriteRoutes(userId: String): Flow<List<String>>

    // Favorite stops operations
    @Query("UPDATE UserEntity SET favoriteStops = :favoriteStops WHERE userId = :userId")
    suspend fun updateFavoriteStops(userId: String, favoriteStops: List<String>)

    @Query("SELECT favoriteStops FROM UserEntity WHERE userId = :userId")
    fun getFavoriteStops(userId: String): Flow<List<String>>

    // Last updated timestamp operations
    @Query("UPDATE UserEntity SET lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun updateLastUpdatedTimestamp(userId: String, timestamp: Long)

    @Query("SELECT lastUpdated FROM UserEntity WHERE userId = :userId")
    suspend fun getLastUpdatedTimestamp(userId: String): Long?

    // Profile update operations
    @Query("UPDATE UserEntity SET name = :name, email = :email, phone = :phone WHERE userId = :userId")
    suspend fun updateUserProfile(userId: String, name: String, email: String, phone: String)

    // Search operations
    @Query("SELECT * FROM UserEntity WHERE " +
           "name LIKE '%' || :query || '%' OR " +
           "email LIKE '%' || :query || '%' OR " +
           "phone LIKE '%' || :query || '%'")
    fun searchUsers(query: String): Flow<List<UserEntity>>

    // Check if user exists
    @Query("SELECT EXISTS(SELECT 1 FROM UserEntity WHERE userId = :userId)")
    suspend fun userExists(userId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM UserEntity WHERE email = :email)")
    suspend fun emailExists(email: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM UserEntity WHERE phone = :phone)")
    suspend fun phoneExists(phone: String): Boolean
}
