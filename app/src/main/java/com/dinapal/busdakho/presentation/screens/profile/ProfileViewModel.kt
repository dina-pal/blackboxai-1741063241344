package com.dinapal.busdakho.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinapal.busdakho.data.local.entity.UserEntity
import com.dinapal.busdakho.domain.repository.TravelHistoryEntry
import com.dinapal.busdakho.domain.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileState(
    val user: UserEntity? = null,
    val favoriteRoutes: List<String> = emptyList(),
    val favoriteStops: List<String> = emptyList(),
    val travelHistory: List<TravelHistoryEntry> = emptyList(),
    val notificationPreferences: Map<String, Boolean> = emptyMap(),
    val languagePreference: String = "en",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)

sealed class ProfileEvent {
    data class UpdateProfile(
        val name: String,
        val email: String,
        val phone: String
    ) : ProfileEvent()
    
    data class UpdateNotificationPreference(
        val key: String,
        val enabled: Boolean
    ) : ProfileEvent()
    
    data class UpdateLanguage(val languageCode: String) : ProfileEvent()
    data class AddFavoriteRoute(val routeId: String) : ProfileEvent()
    data class RemoveFavoriteRoute(val routeId: String) : ProfileEvent()
    data class AddFavoriteStop(val stopId: String) : ProfileEvent()
    data class RemoveFavoriteStop(val stopId: String) : ProfileEvent()
    object ToggleEditMode : ProfileEvent()
    object RefreshProfile : ProfileEvent()
    object Logout : ProfileEvent()
}

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    // Hardcoded user ID for demo - in real app, this would come from auth system
    private val currentUserId = "demo_user"

    init {
        loadUserProfile()
        loadNotificationPreferences()
        loadTravelHistory()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.UpdateProfile -> updateProfile(
                event.name,
                event.email,
                event.phone
            )
            is ProfileEvent.UpdateNotificationPreference -> updateNotificationPreference(
                event.key,
                event.enabled
            )
            is ProfileEvent.UpdateLanguage -> updateLanguage(event.languageCode)
            is ProfileEvent.AddFavoriteRoute -> addFavoriteRoute(event.routeId)
            is ProfileEvent.RemoveFavoriteRoute -> removeFavoriteRoute(event.routeId)
            is ProfileEvent.AddFavoriteStop -> addFavoriteStop(event.stopId)
            is ProfileEvent.RemoveFavoriteStop -> removeFavoriteStop(event.stopId)
            ProfileEvent.ToggleEditMode -> toggleEditMode()
            ProfileEvent.RefreshProfile -> loadUserProfile()
            ProfileEvent.Logout -> logout()
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                userRepository.fetchUserProfile(currentUserId)
                    .onSuccess { user ->
                        _state.value = _state.value.copy(
                            user = user,
                            isLoading = false
                        )
                        loadFavorites()
                    }
                    .onFailure { e ->
                        _state.value = _state.value.copy(
                            error = e.message ?: "Failed to load profile",
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Unknown error occurred",
                    isLoading = false
                )
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            userRepository.getFavoriteRoutes(currentUserId)
                .collect { routes ->
                    _state.value = _state.value.copy(favoriteRoutes = routes)
                }
        }
        viewModelScope.launch {
            userRepository.getFavoriteStops(currentUserId)
                .collect { stops ->
                    _state.value = _state.value.copy(favoriteStops = stops)
                }
        }
    }

    private fun loadNotificationPreferences() {
        viewModelScope.launch {
            userRepository.getNotificationPreferences(currentUserId)
                .collect { preferences ->
                    _state.value = _state.value.copy(notificationPreferences = preferences)
                }
        }
    }

    private fun loadTravelHistory() {
        viewModelScope.launch {
            userRepository.getTravelHistory(currentUserId)
                .collect { history ->
                    _state.value = _state.value.copy(travelHistory = history)
                }
        }
    }

    private fun updateProfile(name: String, email: String, phone: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                userRepository.updateUserProfile(currentUserId, name, email, phone)
                _state.value = _state.value.copy(
                    isLoading = false,
                    isEditMode = false
                )
                loadUserProfile()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to update profile",
                    isLoading = false
                )
            }
        }
    }

    private fun updateNotificationPreference(key: String, enabled: Boolean) {
        viewModelScope.launch {
            val updatedPreferences = _state.value.notificationPreferences.toMutableMap()
            updatedPreferences[key] = enabled
            userRepository.updateNotificationPreferences(currentUserId, updatedPreferences)
        }
    }

    private fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            userRepository.updateLanguagePreference(currentUserId, languageCode)
            _state.value = _state.value.copy(languagePreference = languageCode)
        }
    }

    private fun addFavoriteRoute(routeId: String) {
        viewModelScope.launch {
            val updatedRoutes = _state.value.favoriteRoutes + routeId
            userRepository.updateFavoriteRoutes(currentUserId, updatedRoutes)
        }
    }

    private fun removeFavoriteRoute(routeId: String) {
        viewModelScope.launch {
            val updatedRoutes = _state.value.favoriteRoutes - routeId
            userRepository.updateFavoriteRoutes(currentUserId, updatedRoutes)
        }
    }

    private fun addFavoriteStop(stopId: String) {
        viewModelScope.launch {
            val updatedStops = _state.value.favoriteStops + stopId
            userRepository.updateFavoriteStops(currentUserId, updatedStops)
        }
    }

    private fun removeFavoriteStop(stopId: String) {
        viewModelScope.launch {
            val updatedStops = _state.value.favoriteStops - stopId
            userRepository.updateFavoriteStops(currentUserId, updatedStops)
        }
    }

    private fun toggleEditMode() {
        _state.value = _state.value.copy(isEditMode = !_state.value.isEditMode)
    }

    private fun logout() {
        viewModelScope.launch {
            // Clear local data and navigate to login
            // Implementation depends on authentication system
        }
    }
}
