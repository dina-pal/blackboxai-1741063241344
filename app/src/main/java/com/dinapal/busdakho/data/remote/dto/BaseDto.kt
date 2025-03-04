package com.dinapal.busdakho.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Base class for all DTOs (Data Transfer Objects)
 */
@Serializable
abstract class BaseDto {
    @SerialName("id")
    open val id: String = UUID.randomUUID().toString()

    @SerialName("created_at")
    open val createdAt: Long = System.currentTimeMillis()

    @SerialName("updated_at")
    open val updatedAt: Long = System.currentTimeMillis()

    @SerialName("version")
    open val version: Int = 1
}

/**
 * Base class for DTOs that include soft delete information
 */
@Serializable
abstract class SoftDeletableDto : BaseDto() {
    @SerialName("is_deleted")
    open val isDeleted: Boolean = false

    @SerialName("deleted_at")
    open val deletedAt: Long? = null

    @SerialName("deleted_by")
    open val deletedBy: String? = null

    @SerialName("delete_reason")
    open val deleteReason: String? = null
}

/**
 * Base class for DTOs that include audit information
 */
@Serializable
abstract class AuditableDto : BaseDto() {
    @SerialName("created_by")
    open val createdBy: String? = null

    @SerialName("updated_by")
    open val updatedBy: String? = null

    @SerialName("last_modified_by")
    open val lastModifiedBy: String? = null

    @SerialName("last_modified_at")
    open val lastModifiedAt: Long = System.currentTimeMillis()
}

/**
 * Base response wrapper for paginated data
 */
@Serializable
data class PaginatedResponseDto<T>(
    @SerialName("data")
    val data: List<T>,

    @SerialName("page")
    val page: Int,

    @SerialName("per_page")
    val perPage: Int,

    @SerialName("total")
    val total: Int,

    @SerialName("total_pages")
    val totalPages: Int
)

/**
 * Base response wrapper for single item responses
 */
@Serializable
data class SingleResponseDto<T>(
    @SerialName("data")
    val data: T,

    @SerialName("message")
    val message: String? = null
)

/**
 * Base error response
 */
@Serializable
data class ErrorResponseDto(
    @SerialName("code")
    val code: String,

    @SerialName("message")
    val message: String,

    @SerialName("details")
    val details: Map<String, String>? = null
)

/**
 * Base request wrapper for create operations
 */
@Serializable
data class CreateRequestDto<T>(
    @SerialName("data")
    val data: T
)

/**
 * Base request wrapper for update operations
 */
@Serializable
data class UpdateRequestDto<T>(
    @SerialName("data")
    val data: T,

    @SerialName("version")
    val version: Int
)

/**
 * Base request wrapper for batch operations
 */
@Serializable
data class BatchRequestDto<T>(
    @SerialName("items")
    val items: List<T>
)

/**
 * Base response wrapper for batch operations
 */
@Serializable
data class BatchResponseDto<T>(
    @SerialName("successful")
    val successful: List<T>,

    @SerialName("failed")
    val failed: List<BatchFailureDto>
)

/**
 * DTO for batch operation failures
 */
@Serializable
data class BatchFailureDto(
    @SerialName("index")
    val index: Int,

    @SerialName("id")
    val id: String?,

    @SerialName("error")
    val error: ErrorResponseDto
)

/**
 * Base metadata DTO
 */
@Serializable
data class MetadataDto(
    @SerialName("version")
    val version: Int,

    @SerialName("created_at")
    val createdAt: Long,

    @SerialName("updated_at")
    val updatedAt: Long
)

/**
 * Extension function to get metadata from BaseDto
 */
fun BaseDto.getMetadata() = MetadataDto(
    version = version,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Extension function to create a copy with updated timestamp
 */
fun <T : BaseDto> T.withUpdatedTimestamp(): T {
    return this::class.java.getDeclaredConstructor().newInstance().apply {
        this.javaClass.declaredFields.forEach { field ->
            field.isAccessible = true
            when (field.name) {
                "updatedAt" -> field.set(this, System.currentTimeMillis())
                else -> field.set(this, field.get(this@withUpdatedTimestamp))
            }
        }
    }
}

/**
 * Extension function to create a paginated response
 */
fun <T> List<T>.toPaginatedResponse(
    page: Int,
    perPage: Int
): PaginatedResponseDto<T> {
    val startIndex = (page - 1) * perPage
    val endIndex = minOf(startIndex + perPage, size)
    val pageData = if (startIndex < size) subList(startIndex, endIndex) else emptyList()

    return PaginatedResponseDto(
        data = pageData,
        page = page,
        perPage = perPage,
        total = size,
        totalPages = (size + perPage - 1) / perPage
    )
}

/**
 * Extension function to wrap single item in response
 */
fun <T> T.toSingleResponse(message: String? = null): SingleResponseDto<T> {
    return SingleResponseDto(
        data = this,
        message = message
    )
}
