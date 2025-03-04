package com.dinapal.busdakho.presentation.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.dinapal.busdakho.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    if (!state.isEditMode) {
                        IconButton(onClick = { viewModel.onEvent(ProfileEvent.ToggleEditMode) }) {
                            Icon(Icons.Default.Edit, "Edit profile")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Section
            item {
                ProfileSection(
                    state = state,
                    onEvent = viewModel::onEvent
                )
            }

            // Notification Preferences
            item {
                NotificationPreferencesSection(
                    preferences = state.notificationPreferences,
                    onPreferenceChanged = { key, enabled ->
                        viewModel.onEvent(ProfileEvent.UpdateNotificationPreference(key, enabled))
                    }
                )
            }

            // Favorite Routes
            item {
                FavoriteRoutesSection(
                    favoriteRoutes = state.favoriteRoutes,
                    onRemoveRoute = { routeId ->
                        viewModel.onEvent(ProfileEvent.RemoveFavoriteRoute(routeId))
                    }
                )
            }

            // Travel History
            item {
                Text(
                    text = "Travel History",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(state.travelHistory) { historyEntry ->
                TravelHistoryItem(historyEntry)
            }

            // Language Preference
            item {
                LanguagePreferenceSection(
                    currentLanguage = state.languagePreference,
                    onLanguageSelected = { languageCode ->
                        viewModel.onEvent(ProfileEvent.UpdateLanguage(languageCode))
                    }
                )
            }

            // Logout Button
            item {
                Button(
                    onClick = { viewModel.onEvent(ProfileEvent.Logout) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout")
                }
            }
        }

        // Loading indicator
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Error message
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSection(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit
) {
    var name by remember { mutableStateOf(state.user?.name ?: "") }
    var email by remember { mutableStateOf(state.user?.email ?: "") }
    var phone by remember { mutableStateOf(state.user?.phone ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            if (state.isEditMode) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { onEvent(ProfileEvent.ToggleEditMode) }) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onEvent(ProfileEvent.UpdateProfile(name, email, phone))
                        }
                    ) {
                        Text("Save")
                    }
                }
            } else {
                state.user?.let { user ->
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.phone,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationPreferencesSection(
    preferences: Map<String, Boolean>,
    onPreferenceChanged: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Notification Preferences",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            preferences.forEach { (key, enabled) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = key.replace("_", " ").capitalize(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onPreferenceChanged(key, it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteRoutesSection(
    favoriteRoutes: List<String>,
    onRemoveRoute: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Favorite Routes",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (favoriteRoutes.isEmpty()) {
                Text(
                    text = "No favorite routes yet",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                favoriteRoutes.forEach { routeId ->
                    ListItem(
                        headlineContent = { Text("Route $routeId") },
                        trailingContent = {
                            IconButton(onClick = { onRemoveRoute(routeId) }) {
                                Icon(Icons.Default.Close, "Remove route")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TravelHistoryItem(historyEntry: TravelHistoryEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Route ${historyEntry.routeId}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${historyEntry.startStop} → ${historyEntry.endStop}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Fare: ₹${historyEntry.fare}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(Date(historyEntry.timestamp)),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun LanguagePreferenceSection(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Language",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentLanguage == "en",
                    onClick = { onLanguageSelected("en") },
                    label = { Text("English") }
                )
                FilterChip(
                    selected = currentLanguage == "hi",
                    onClick = { onLanguageSelected("hi") },
                    label = { Text("हिंदी") }
                )
            }
        }
    }
}
