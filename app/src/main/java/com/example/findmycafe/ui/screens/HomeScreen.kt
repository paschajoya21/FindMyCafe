package com.example.findmycafe.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.findmycafe.data.Cafe
import com.example.findmycafe.data.DistanceCalculator
import com.example.findmycafe.ui.CafeUiState
import com.example.findmycafe.ui.components.*

@Composable fun HomeScreen(state: CafeUiState, open: (String) -> Unit, favorite: (String) -> Unit, requestLocation: () -> Unit, go: (String) -> Unit) { var query by remember { mutableStateOf("") }; val cafes = state.cafes.filter { it.name.contains(query, true) || it.address.contains(query, true) }.sortedBy { DistanceCalculator.kilometers(state.userLocation, it.location) ?: Double.MAX_VALUE }; Column(Modifier.fillMaxSize()) { TopBar("Find My Cafe"); LazyColumn(Modifier.weight(1f).padding(16.dp)) { item { Search(query, { query = it }, "Cari kafe, area, atau nama kafe"); Spacer(Modifier.height(18.dp)); Text("Kafe terdekat", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(if (state.userLocation == null) "Menunggu lokasi Anda..." else "Diurutkan berdasarkan jarak", color = Muted, fontSize = 11.sp); state.error?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red, fontSize = 11.sp) }; Spacer(Modifier.height(12.dp)) }; items(cafes) { CafeCard(it, state, open, favorite) } }; BottomNav("HOME", go) } }
@Composable fun CafeCard(cafe: Cafe, state: CafeUiState, open: (String) -> Unit, favorite: (String) -> Unit) = Card(Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { open(cafe.id) }) { Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(cafe.name, fontWeight = FontWeight.Bold, color = Ink); Text("★ %.1f".format(cafe.rating), color = Ink); Text("Cafe • ${DistanceCalculator.label(state.userLocation, cafe.location)}", color = Muted, fontSize = 12.sp); Text(cafe.address, color = Muted, fontSize = 11.sp) }; Text(if (cafe.id in state.favoriteIds) "♥" else "♡", color = Accent, fontSize = 26.sp, modifier = Modifier.clickable { favorite(cafe.id) }) } }
