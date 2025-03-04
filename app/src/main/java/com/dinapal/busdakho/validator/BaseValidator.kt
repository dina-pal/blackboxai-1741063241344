package com.dinapal.busdakho.validator

import com.dinapal.busdakho.util.Constants
import java.util.regex.Pattern

/**
 * Base class for all validators
 */
abstract class BaseValidator<T> {
    /**
     * Validate the input
     */
    abstract fun validate(input: T): ValidationResult

    /**
     * Chain multiple validators
     */
    infix fun and(other: BaseValidator<T>): BaseValidator<T> {
        return object : BaseValidator<T>() {
            override fun validate(input: T): ValidationResult {
                val firstResult = this@BaseValidator.validate(input)
                return if (firstResult is ValidationResult.Success) {
                    other.validate(input)
                } else {
                    firstResult
                }
            }
        }
    }

    /**
     * Chain validators with OR logic
     */
    infix fun or(other: BaseValidator<T>): BaseValidator<T> {
        return object : BaseValidator<T>() {
            override fun validate(input: T): ValidationResult {
                val firstResult = this@BaseValidator.validate(input)
                return if (firstResult is ValidationResult.Success) {
                    firstResult
                } else {
                    other.validate(input)
                }
            }
        }
    }
}

/**
 * Validation result sealed class
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(
        val message: String,
        val errorCode: String? = null,
        val field: String? = null
    ) : ValidationResult()
}

/**
 * Common validators
 */
class RequiredValidator<T>(private val fieldName: String) : BaseValidator<T>() {
    override fun validate(input: T): ValidationResult {
        return when {
            input == null -> ValidationResult.Error(
                message = "$fieldName is required",
                errorCode = "REQUIRED",
                field = fieldName
            )
            input is String && input.isBlank() -> ValidationResult.Error(
                message = "$fieldName cannot be empty",
                errorCode = "EMPTY",
                field = fieldName
            )
            input is Collection<*> && input.isEmpty() -> ValidationResult.Error(
                message = "$fieldName cannot be empty",
                errorCode = "EMPTY",
                field = fieldName
            )
            else -> ValidationResult.Success
        }
    }
}

class EmailValidator : BaseValidator<String>() {
    private val emailPattern = Pattern.compile(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
                "@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )

    override fun validate(input: String): ValidationResult {
        return if (emailPattern.matcher(input).matches()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(
                message = "Invalid email address",
                errorCode = "INVALID_EMAIL",
                field = "email"
            )
        }
    }
}

class PhoneValidator : BaseValidator<String>() {
    private val phonePattern = Pattern.compile("^[+]?[0-9]{10,13}$")

    override fun validate(input: String): ValidationResult {
        return if (phonePattern.matcher(input).matches()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(
                message = "Invalid phone number",
                errorCode = "INVALID_PHONE",
                field = "phone"
            )
        }
    }
}

class PasswordValidator(
    private val minLength: Int = Constants.Validation.MIN_PASSWORD_LENGTH,
    private val requireUpperCase: Boolean = true,
    private val requireLowerCase: Boolean = true,
    private val requireDigit: Boolean = true,
    private val requireSpecialChar: Boolean = true
) : BaseValidator<String>() {
    override fun validate(input: String): ValidationResult {
        val errors = mutableListOf<String>()

        if (input.length < minLength) {
            errors.add("Password must be at least $minLength characters long")
        }
        if (requireUpperCase && !input.any { it.isUpperCase() }) {
            errors.add("Password must contain at least one uppercase letter")
        }
        if (requireLowerCase && !input.any { it.isLowerCase() }) {
            errors.add("Password must contain at least one lowercase letter")
        }
        if (requireDigit && !input.any { it.isDigit() }) {
            errors.add("Password must contain at least one digit")
        }
        if (requireSpecialChar && !input.any { !it.isLetterOrDigit() }) {
            errors.add("Password must contain at least one special character")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(
                message = errors.joinToString(". "),
                errorCode = "INVALID_PASSWORD",
                field = "password"
            )
        }
    }
}

class MinLengthValidator(
    private val minLength: Int,
    private val fieldName: String
) : BaseValidator<String>() {
    override fun validate(input: String): ValidationResult {
        return if (input.length >= minLength) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(
                message = "$fieldName must be at least $minLength characters long",
                errorCode = "MIN_LENGTH",
                field = fieldName
            )
        }
    }
}

class MaxLengthValidator(
    private val maxLength: Int,
    private val fieldName: String
) : BaseValidator<String>() {
    override fun validate(input: String): ValidationResult {
        return if (input.length <= maxLength) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(
                message = "$fieldName must not exceed $maxLength characters",
                errorCode = "MAX_LENGTH",
                field = fieldName
            )
        }
    }
}

class PatternValidator(
    private val pattern: Pattern,
    private val fieldName: String,
    private val errorMessage: String
) : BaseValidator<String>() {
    override fun validate(input: String): ValidationResult {
        return if (pattern.matcher(input).matches()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(
                message = errorMessage,
                errorCode = "PATTERN_MISMATCH",
                field = fieldName
            )
        }
    }
}

/**
 * Extension functions for validation
 */
fun ValidationResult.isValid(): Boolean = this is ValidationResult.Success

fun ValidationResult.getErrorMessage(): String? =
    if (this is ValidationResult.Error) message else null

fun ValidationResult.getErrorCode(): String? =
    if (this is ValidationResult.Error) errorCode else null

fun ValidationResult.getField(): String? =
    if (this is ValidationResult.Error) field else null
