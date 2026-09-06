package com.example.findmycafe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.findmycafe.data.Cafe
import com.example.findmycafe.data.Coordinate
import com.example.findmycafe.ui.components.*

@Composable fun DetailScreen(cafe: Cafe, favorite: Boolean, back: () -> Unit, toggleFavorite: (String) -> Unit, directions: (Coordinate) -> Unit) { Column(Modifier.fillMaxSize().padding(16.dp)) { TextButton(back) { Text("‹ Kembali") }; Text(cafe.name, fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("★ %.1f".format(cafe.rating)); Text(cafe.address, color = Muted); Spacer(Modifier.height(20.dp)); Text("Tentang", fontWeight = FontWeight.Bold); Text(cafe.description, color = Muted); Spacer(Modifier.height(24.dp)); PrimaryButton(if (favorite) "Hapus favorit" else "Simpan favorit") { toggleFavorite(cafe.id) }; cafe.location?.let { Spacer(Modifier.height(10.dp)); OutlinedButton({ directions(it) }, Modifier.fillMaxWidth()) { Text("Directions") } } } }
