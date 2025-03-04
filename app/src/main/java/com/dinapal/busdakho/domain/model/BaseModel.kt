package com.dinapal.busdakho.domain.model

import java.util.UUID

/**
 * Base class for all domain models
 */
abstract class BaseModel {
    open val id: String = UUID.randomUUID().toString()
    open val createdAt: Long = System.currentTimeMillis()
    open val updatedAt: Long = System.currentTimeMillis()
    open val isDeleted: Boolean = false
    open val version: Int = 1
}

/**
 * Base class for models that need soft delete functionality
 */
abstract class SoftDeletableModel : BaseModel() {
    open val deletedAt: Long? = null
    open val deletedBy: String? = null
    open val deleteReason: String? = null
}

/**
 * Base class for models that need audit functionality
 */
abstract class AuditableModel : BaseModel() {
    open val createdBy: String? = null
    open val updatedBy: String? = null
    open val lastModifiedBy: String? = null
    open val lastModifiedAt: Long = System.currentTimeMillis()
}

/**
 * Base class for models that need both soft delete and audit functionality
 */
abstract class FullFeaturedModel : SoftDeletableModel() {
    open val createdBy: String? = null
    open val updatedBy: String? = null
    open val lastModifiedBy: String? = null
    open val lastModifiedAt: Long = System.currentTimeMillis()
}

/**
 * Interface for models that can be validated
 */
interface Validatable {
    fun validate(): ValidationResult
}

/**
 * Sealed class for validation results
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<ValidationError>) : ValidationResult()
}

/**
 * Data class for validation errors
 */
data class ValidationError(
    val field: String,
    val message: String,
    val code: String? = null
)

/**
 * Extension functions for validation
 */
fun ValidationResult.isValid(): Boolean = this is ValidationResult.Valid

fun ValidationResult.getErrors(): List<ValidationError> =
    when (this) {
        is ValidationResult.Valid -> emptyList()
        is ValidationResult.Invalid -> errors
    }

/**
 * Interface for models that can be compared for equality based on business rules
 */
interface BusinessComparable<T> {
    fun businessEquals(other: T): Boolean
}

/**
 * Interface for models that support versioning
 */
interface Versionable {
    val version: Int
    fun incrementVersion(): Versionable
}

/**
 * Interface for models that can be cloned
 */
interface Cloneable<T> {
    fun clone(): T
}

/**
 * Data class for model metadata
 */
data class ModelMetadata(
    val id: String,
    val version: Int,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Extension function to get model metadata
 */
fun BaseModel.getMetadata() = ModelMetadata(
    id = id,
    version = version,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Extension function for validation with multiple rules
 */
fun validate(vararg validations: Pair<Boolean, ValidationError>): ValidationResult {
    val errors = validations
        .filter { !it.first }
        .map { it.second }
    
    return if (errors.isEmpty()) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid(errors)
    }
}

/**
 * Extension function for common validation rules
 */
object ValidationRules {
    fun required(value: String?, fieldName: String): Pair<Boolean, ValidationError> =
        (value != null && value.isNotBlank()) to ValidationError(
            field = fieldName,
            message = "$fieldName is required",
            code = "REQUIRED"
        )

    fun minLength(value: String?, minLength: Int, fieldName: String): Pair<Boolean, ValidationError> =
        (value?.length ?: 0 >= minLength) to ValidationError(
            field = fieldName,
            message = "$fieldName must be at least $minLength characters long",
            code = "MIN_LENGTH"
        )

    fun maxLength(value: String?, maxLength: Int, fieldName: String): Pair<Boolean, ValidationError> =
        (value?.length ?: 0 <= maxLength) to ValidationError(
            field = fieldName,
            message = "$fieldName must not exceed $maxLength characters",
            code = "MAX_LENGTH"
        )

    fun pattern(value: String?, pattern: Regex, fieldName: String): Pair<Boolean, ValidationError> =
        (value?.matches(pattern) ?: false) to ValidationError(
            field = fieldName,
            message = "$fieldName has invalid format",
            code = "INVALID_FORMAT"
        )

    fun range(value: Number?, min: Number, max: Number, fieldName: String): Pair<Boolean, ValidationError> =
        (value != null && value.toDouble() in min.toDouble()..max.toDouble()) to ValidationError(
            field = fieldName,
            message = "$fieldName must be between $min and $max",
            code = "RANGE"
        )
}

/**
 * Extension function to create a copy of a model with updated timestamps
 */
fun <T : BaseModel> T.withUpdatedTimestamps(): T {
    return this::class.java.getDeclaredConstructor().newInstance().apply {
        this.javaClass.declaredFields.forEach { field ->
            field.isAccessible = true
            when (field.name) {
                "updatedAt" -> field.set(this, System.currentTimeMillis())
                else -> field.set(this, field.get(this@withUpdatedTimestamps))
            }
        }
    }
}
