package com.dinapal.busdakho.presentation.base

import com.dinapal.busdakho.util.Resource

/**
 * Base interface for UI states
 */
interface BaseState {
    val isLoading: Boolean
    val error: String?
}

/**
 * Base implementation of UI state
 */
data class UiState<T>(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val data: T? = null,
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = false,
    val lastUpdated: Long? = null
) : BaseState

/**
 * Base class for list-based UI states
 */
data class ListUiState<T>(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val items: List<T> = emptyList(),
    val isRefreshing: Boolean = false,
    val hasMoreItems: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val lastUpdated: Long? = null
) : BaseState

/**
 * Base class for form-based UI states
 */
data class FormUiState<T>(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val data: T? = null,
    val isValid: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false
) : BaseState

/**
 * Extension functions for UiState
 */
fun <T> UiState<T>.copyWithLoading(isLoading: Boolean = true): UiState<T> =
    copy(isLoading = isLoading, error = null)

fun <T> UiState<T>.copyWithError(error: String?): UiState<T> =
    copy(isLoading = false, error = error)

fun <T> UiState<T>.copyWithData(data: T?): UiState<T> =
    copy(isLoading = false, error = null, data = data, isEmpty = data == null)

fun <T> UiState<T>.copyWithRefreshing(isRefreshing: Boolean = true): UiState<T> =
    copy(isRefreshing = isRefreshing)

/**
 * Extension functions for ListUiState
 */
fun <T> ListUiState<T>.copyWithLoading(isLoading: Boolean = true): ListUiState<T> =
    copy(isLoading = isLoading, error = null)

fun <T> ListUiState<T>.copyWithError(error: String?): ListUiState<T> =
    copy(isLoading = false, error = error)

fun <T> ListUiState<T>.copyWithItems(
    items: List<T>,
    hasMore: Boolean = false,
    currentPage: Int = this.currentPage,
    totalPages: Int = this.totalPages
): ListUiState<T> =
    copy(
        isLoading = false,
        error = null,
        items = items,
        hasMoreItems = hasMore,
        currentPage = currentPage,
        totalPages = totalPages,
        lastUpdated = System.currentTimeMillis()
    )

fun <T> ListUiState<T>.appendItems(
    newItems: List<T>,
    hasMore: Boolean = false,
    currentPage: Int = this.currentPage + 1,
    totalPages: Int = this.totalPages
): ListUiState<T> =
    copy(
        isLoading = false,
        error = null,
        items = items + newItems,
        hasMoreItems = hasMore,
        currentPage = currentPage,
        totalPages = totalPages,
        lastUpdated = System.currentTimeMillis()
    )

/**
 * Extension functions for FormUiState
 */
fun <T> FormUiState<T>.copyWithLoading(isLoading: Boolean = true): FormUiState<T> =
    copy(isLoading = isLoading, error = null)

fun <T> FormUiState<T>.copyWithError(error: String?): FormUiState<T> =
    copy(isLoading = false, error = error)

fun <T> FormUiState<T>.copyWithFieldError(field: String, error: String): FormUiState<T> =
    copy(fieldErrors = fieldErrors + (field to error))

fun <T> FormUiState<T>.copyWithSubmitting(isSubmitting: Boolean = true): FormUiState<T> =
    copy(isSubmitting = isSubmitting)

fun <T> FormUiState<T>.copyWithSuccess(data: T? = null): FormUiState<T> =
    copy(
        isLoading = false,
        error = null,
        data = data,
        isSubmitting = false,
        isSuccess = true
    )

/**
 * Extension function to convert Resource to UiState
 */
fun <T> Resource<T>.toUiState(): UiState<T> = when (this) {
    is Resource.Loading -> UiState(isLoading = true)
    is Resource.Success -> UiState(data = data)
    is Resource.Error -> UiState(error = message)
}

/**
 * Extension function to convert Resource to ListUiState
 */
fun <T> Resource<List<T>>.toListUiState(): ListUiState<T> = when (this) {
    is Resource.Loading -> ListUiState(isLoading = true)
    is Resource.Success -> ListUiState(items = data ?: emptyList())
    is Resource.Error -> ListUiState(error = message)
}

/**
 * Extension function to convert Resource to FormUiState
 */
fun <T> Resource<T>.toFormUiState(): FormUiState<T> = when (this) {
    is Resource.Loading -> FormUiState(isLoading = true)
    is Resource.Success -> FormUiState(data = data)
    is Resource.Error -> FormUiState(error = message)
}

/**
 * Utility class for handling paginated data
 */
data class PaginationState(
    val currentPage: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Int = 0,
    val hasMoreItems: Boolean = false
) {
    val totalPages: Int
        get() = if (totalItems == 0) 1 else (totalItems + pageSize - 1) / pageSize

    fun nextPage(): PaginationState =
        if (hasMoreItems) copy(currentPage = currentPage + 1) else this

    fun reset(): PaginationState =
        copy(currentPage = 1, totalItems = 0, hasMoreItems = false)
}
