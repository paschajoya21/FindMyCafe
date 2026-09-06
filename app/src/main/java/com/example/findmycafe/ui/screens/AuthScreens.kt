package com.example.findmycafe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.findmycafe.ui.CafeViewModel
import com.example.findmycafe.ui.components.*

@Composable fun LoginScreen(viewModel: CafeViewModel, googleLogin: ((String) -> Unit) -> Unit, register: () -> Unit, success: () -> Unit) { var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }; Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Spacer(Modifier.height(70.dp)); Text("Find My Cafe", fontSize = 28.sp, color = Ink); Text("Temukan kafe favoritmu", color = Muted); Spacer(Modifier.height(48.dp)); OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }); Spacer(Modifier.height(12.dp)); OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation()); error?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red, fontSize = 11.sp) }; Spacer(Modifier.height(28.dp)); PrimaryButton("Login", email.isNotBlank() && password.isNotBlank()) { viewModel.signIn(email, password) { if (it) success() else error = viewModel.uiState.value.error } }; Spacer(Modifier.height(16.dp)); PrimaryButton("Continue with Google") { googleLogin { error = it } }; Spacer(Modifier.height(24.dp)); Text("Belum punya akun?", modifier = Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center); androidx.compose.material3.TextButton(register) { Text("Buat akun") } } }
@Composable fun RegisterScreen(viewModel: CafeViewModel, success: () -> Unit, login: () -> Unit) { var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Spacer(Modifier.height(48.dp)); Text("Buat akun", fontSize = 28.sp, color = Ink); Spacer(Modifier.height(32.dp)); OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nama lengkap") }); OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("Email") }); OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation()); Spacer(Modifier.height(24.dp)); PrimaryButton("Create Account", name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) { viewModel.register(name, email, password) { if (it) success() } }; androidx.compose.material3.TextButton(login) { Text("Login") } } }
