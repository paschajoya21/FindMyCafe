package com.example.findmycafe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.findmycafe.data.DistanceCalculator
import com.example.findmycafe.ui.CafeUiState
import com.example.findmycafe.ui.components.*

@Composable fun FavoritesScreen(state: CafeUiState, open: (String) -> Unit, favorite: (String) -> Unit, go: (String) -> Unit) { val cafes = state.cafes.filter { it.id in state.favoriteIds }.sortedBy { DistanceCalculator.kilometers(state.userLocation, it.location) ?: Double.MAX_VALUE }; Column(Modifier.fillMaxSize()) { TopBar("Favorites"); if (cafes.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Belum ada kafe favorit") } else LazyColumn(Modifier.weight(1f).padding(16.dp)) { items(cafes) { CafeCard(it, state, open, favorite) } }; BottomNav("FAVORITES", go) } }
