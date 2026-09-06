package com.example.findmycafe.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import com.example.findmycafe.data.Coordinate
import com.example.findmycafe.ui.CafeUiState
import com.example.findmycafe.ui.components.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

@Composable fun MapScreen(state: CafeUiState, hasPermission: Boolean, requestLocation: () -> Unit, open: (String) -> Unit, favorite: (String) -> Unit, go: (String) -> Unit) { Box(Modifier.fillMaxSize()) { CafeMap(state, open); Column(Modifier.fillMaxSize()) { Spacer(Modifier.height(32.dp)); Search("", {}, "Cari kafe atau lokasi"); Spacer(Modifier.weight(1f)); BottomNav("MAP", go) }; Button(requestLocation, Modifier.align(Alignment.BottomEnd).padding(18.dp, 100.dp), shape = androidx.compose.foundation.shape.CircleShape) { Text("◎") } } }
@Composable private fun CafeMap(state: CafeUiState, open: (String) -> Unit) { val context = LocalContext.current; val view = remember { MapView(context).apply { onCreate(null); onStart(); onResume() } }; AndroidView(factory = { view }, modifier = Modifier.fillMaxSize()) { view.getMapAsync { map -> map.clear(); map.uiSettings.isMyLocationButtonEnabled = false; if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) map.isMyLocationEnabled = true; state.cafes.forEach { cafe -> cafe.location?.let { point -> map.addMarker(MarkerOptions().position(LatLng(point.latitude, point.longitude)).title(cafe.name))?.tag = cafe.id } }; map.setOnMarkerClickListener { marker -> (marker.tag as? String)?.let(open); true }; state.userLocation?.let { map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 13f)) } ?: state.cafes.firstOrNull()?.location?.let { map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 13f)) } } } }
