package com.example.findmycafe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.findmycafe.ui.CafeUiState
import com.example.findmycafe.ui.components.*

@Composable fun ProfileScreen(state: CafeUiState, favorites: () -> Unit, map: () -> Unit, logout: () -> Unit, go: (String) -> Unit) { Column(Modifier.fillMaxSize()) { TopBar("Profile"); Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(state.userName.ifBlank { "Pengguna" }, style = MaterialTheme.typography.headlineSmall); Text(state.userEmail, color = Muted); Spacer(Modifier.height(28.dp)); OutlinedButton(favorites, Modifier.fillMaxWidth()) { Text("Favorites") }; OutlinedButton(map, Modifier.fillMaxWidth()) { Text("Location & Map") }; Spacer(Modifier.weight(1f)); PrimaryButton("Logout", click = logout) }; BottomNav("PROFILE", go) } }
