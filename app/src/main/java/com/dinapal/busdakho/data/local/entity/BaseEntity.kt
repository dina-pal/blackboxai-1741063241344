package com.dinapal.busdakho.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Base class for all database entities
 */
abstract class BaseEntity {
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: String = UUID.randomUUID().toString()

    @ColumnInfo(name = "created_at")
    var createdAt: Long = System.currentTimeMillis()

    @ColumnInfo(name = "updated_at")
    var updatedAt: Long = System.currentTimeMillis()

    @ColumnInfo(name = "is_deleted")
    var isDeleted: Boolean = false

    @ColumnInfo(name = "sync_status")
    var syncStatus: SyncStatus = SyncStatus.PENDING

    @ColumnInfo(name = "last_sync_timestamp")
    var lastSyncTimestamp: Long? = null

    @ColumnInfo(name = "version")
    var version: Int = 1
}

/**
 * Base class for entities that need soft delete functionality
 */
abstract class SoftDeleteEntity : BaseEntity() {
    @ColumnInfo(name = "deleted_at")
    var deletedAt: Long? = null

    @ColumnInfo(name = "deleted_by")
    var deletedBy: String? = null

    @ColumnInfo(name = "delete_reason")
    var deleteReason: String? = null
}

/**
 * Base class for entities that need audit functionality
 */
abstract class AuditableEntity : BaseEntity() {
    @ColumnInfo(name = "created_by")
    var createdBy: String? = null

    @ColumnInfo(name = "updated_by")
    var updatedBy: String? = null

    @ColumnInfo(name = "last_modified_by")
    var lastModifiedBy: String? = null

    @ColumnInfo(name = "last_modified_at")
    var lastModifiedAt: Long = System.currentTimeMillis()
}

/**
 * Base class for entities that need both soft delete and audit functionality
 */
abstract class FullFeaturedEntity : SoftDeleteEntity() {
    @ColumnInfo(name = "created_by")
    var createdBy: String? = null

    @ColumnInfo(name = "updated_by")
    var updatedBy: String? = null

    @ColumnInfo(name = "last_modified_by")
    var lastModifiedBy: String? = null

    @ColumnInfo(name = "last_modified_at")
    var lastModifiedAt: Long = System.currentTimeMillis()
}

/**
 * Enum class to represent the sync status of an entity
 */
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
    CONFLICT
}

/**
 * Interface for entities that can be synced
 */
interface Syncable {
    fun markAsSynced()
    fun markAsPending()
    fun markAsFailed()
    fun markAsConflict()
    fun needsSync(): Boolean
}

/**
 * Extension functions for BaseEntity
 */
fun BaseEntity.updateTimestamps() {
    updatedAt = System.currentTimeMillis()
}

fun BaseEntity.markAsDeleted() {
    isDeleted = true
    updateTimestamps()
}

fun BaseEntity.incrementVersion() {
    version++
    updateTimestamps()
}

fun BaseEntity.updateSyncStatus(status: SyncStatus) {
    syncStatus = status
    if (status == SyncStatus.SYNCED) {
        lastSyncTimestamp = System.currentTimeMillis()
    }
    updateTimestamps()
}

/**
 * Extension functions for SoftDeleteEntity
 */
fun SoftDeleteEntity.softDelete(userId: String? = null, reason: String? = null) {
    isDeleted = true
    deletedAt = System.currentTimeMillis()
    deletedBy = userId
    deleteReason = reason
    updateTimestamps()
}

fun SoftDeleteEntity.restore() {
    isDeleted = false
    deletedAt = null
    deletedBy = null
    deleteReason = null
    updateTimestamps()
}

/**
 * Extension functions for AuditableEntity
 */
fun AuditableEntity.updateAuditInfo(userId: String?) {
    lastModifiedBy = userId
    lastModifiedAt = System.currentTimeMillis()
    updateTimestamps()
}

/**
 * Base class for entity metadata
 */
data class EntityMetadata(
    val id: String,
    val version: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val lastSyncTimestamp: Long?
)

/**
 * Extension function to get entity metadata
 */
fun BaseEntity.getMetadata() = EntityMetadata(
    id = id,
    version = version,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    lastSyncTimestamp = lastSyncTimestamp
)

/**
 * Interface for entities that support optimistic locking
 */
interface OptimisticLockable {
    fun getVersion(): Int
    fun incrementVersion()
    fun checkVersion(version: Int): Boolean
}

/**
 * Extension function to implement optimistic locking
 */
fun BaseEntity.withOptimisticLock(version: Int, block: () -> Unit) {
    if (this.version != version) {
        throw OptimisticLockException("Version mismatch: expected $version but was ${this.version}")
    }
    block()
    this.version++
    updateTimestamps()
}

class OptimisticLockException(message: String) : RuntimeException(message)
