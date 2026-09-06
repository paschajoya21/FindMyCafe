package com.example.findmycafe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Background = Color(0xFFF9F8F6); val Surface = Color.White; val Ink = Color(0xFF3C342E); val Muted = Color(0xFF796E65); val Line = Color(0xFFD9CFC7); val Sand = Color(0xFFEFE9E3); val Accent = Color(0xFFC9B59C)
@Composable fun TopBar(title: String) = Row(Modifier.fillMaxWidth().height(56.dp).background(Surface).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp) }
@Composable fun BottomNav(current: String, go: (String) -> Unit) { HorizontalDivider(color = Line); Row(Modifier.fillMaxWidth().height(64.dp).background(Surface), horizontalArrangement = Arrangement.SpaceAround) { listOf("HOME" to "⌂", "MAP" to "⌖", "FAVORITES" to "♡", "PROFILE" to "●").forEach { (screen, icon) -> Column(Modifier.width(76.dp).fillMaxHeight().clickable { go(screen) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(icon, color = if (screen == current) Ink else Muted, fontSize = 20.sp); Text(screen.lowercase().replaceFirstChar { it.uppercase() }, color = if (screen == current) Ink else Muted, fontSize = 10.sp) } } } }
@Composable fun PrimaryButton(text: String, enabled: Boolean = true, click: () -> Unit) = Button(click, Modifier.fillMaxWidth().height(50.dp), enabled = enabled, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text(text, color = Surface, fontWeight = FontWeight.Bold) }
@Composable fun Search(value: String, onValue: (String) -> Unit, hint: String) = OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Text("⌕") }, placeholder = { Text(hint) }, shape = CircleShape, colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Surface, unfocusedContainerColor = Surface, focusedBorderColor = Accent, unfocusedBorderColor = Line))
