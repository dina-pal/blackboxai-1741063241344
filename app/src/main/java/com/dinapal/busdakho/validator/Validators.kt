package com.dinapal.busdakho.validator

import com.dinapal.busdakho.util.Constants
import java.util.regex.Pattern

/**
 * Form validator for handling multiple field validations
 */
class FormValidator {
    private val validations = mutableMapOf<String, BaseValidator<*>>()
    private val errors = mutableMapOf<String, String>()

    fun <T> addValidation(field: String, validator: BaseValidator<T>) {
        validations[field] = validator
    }

    fun validate(data: Map<String, Any?>): Boolean {
        errors.clear()
        var isValid = true

        data.forEach { (field, value) ->
            @Suppress("UNCHECKED_CAST")
            val validator = validations[field] as? BaseValidator<Any?> ?: return@forEach

            when (val result = validator.validate(value)) {
                is ValidationResult.Error -> {
                    errors[field] = result.message
                    isValid = false
                }
                is ValidationResult.Success -> {}
            }
        }

        return isValid
    }

    fun getErrors(): Map<String, String> = errors.toMap()
}

/**
 * User input validators
 */
object UserValidators {
    val nameValidator = RequiredValidator<String>("Name")
        .and(MinLengthValidator(2, "Name"))
        .and(MaxLengthValidator(50, "Name"))
        .and(PatternValidator(
            Pattern.compile("^[a-zA-Z ]*$"),
            "Name",
            "Name can only contain letters and spaces"
        ))

    val emailValidator = RequiredValidator<String>("Email")
        .and(EmailValidator())

    val phoneValidator = RequiredValidator<String>("Phone")
        .and(PhoneValidator())

    val passwordValidator = RequiredValidator<String>("Password")
        .and(PasswordValidator())
}

/**
 * Location validators
 */
object LocationValidators {
    val latitudeValidator = object : BaseValidator<Double>() {
        override fun validate(input: Double): ValidationResult {
            return if (input in -90.0..90.0) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(
                    message = "Invalid latitude value",
                    errorCode = "INVALID_LATITUDE",
                    field = "latitude"
                )
            }
        }
    }

    val longitudeValidator = object : BaseValidator<Double>() {
        override fun validate(input: Double): ValidationResult {
            return if (input in -180.0..180.0) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(
                    message = "Invalid longitude value",
                    errorCode = "INVALID_LONGITUDE",
                    field = "longitude"
                )
            }
        }
    }
}

/**
 * Bus route validators
 */
object RouteValidators {
    val routeIdValidator = RequiredValidator<String>("Route ID")
        .and(PatternValidator(
            Pattern.compile("^[A-Z0-9]{3,10}$"),
            "Route ID",
            "Route ID must be 3-10 characters long and contain only uppercase letters and numbers"
        ))

    val stopNameValidator = RequiredValidator<String>("Stop name")
        .and(MinLengthValidator(2, "Stop name"))
        .and(MaxLengthValidator(50, "Stop name"))

    val fareValidator = object : BaseValidator<Double>() {
        override fun validate(input: Double): ValidationResult {
            return if (input >= 0) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(
                    message = "Fare cannot be negative",
                    errorCode = "INVALID_FARE",
                    field = "fare"
                )
            }
        }
    }
}

/**
 * Time validators
 */
object TimeValidators {
    val timeValidator = object : BaseValidator<String>() {
        private val timePattern = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")

        override fun validate(input: String): ValidationResult {
            return if (timePattern.matcher(input).matches()) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(
                    message = "Invalid time format (HH:mm)",
                    errorCode = "INVALID_TIME",
                    field = "time"
                )
            }
        }
    }

    val durationValidator = object : BaseValidator<Int>() {
        override fun validate(input: Int): ValidationResult {
            return if (input > 0) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(
                    message = "Duration must be positive",
                    errorCode = "INVALID_DURATION",
                    field = "duration"
                )
            }
        }
    }
}

/**
 * Search validators
 */
object SearchValidators {
    val searchQueryValidator = object : BaseValidator<String>() {
        override fun validate(input: String): ValidationResult {
            return if (input.length >= Constants.Validation.MIN_SEARCH_QUERY_LENGTH) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(
                    message = "Search query too short",
                    errorCode = "INVALID_SEARCH_QUERY",
                    field = "query"
                )
            }
        }
    }
}

/**
 * Extension functions for validation
 */
fun Map<String, Any?>.validate(validator: FormValidator): Boolean {
    return validator.validate(this)
}

fun String.validateEmail(): ValidationResult {
    return UserValidators.emailValidator.validate(this)
}

fun String.validatePhone(): ValidationResult {
    return UserValidators.phoneValidator.validate(this)
}

fun String.validatePassword(): ValidationResult {
    return UserValidators.passwordValidator.validate(this)
}
