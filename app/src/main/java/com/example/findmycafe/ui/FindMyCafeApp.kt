package com.example.findmycafe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.findmycafe.data.Coordinate
import com.example.findmycafe.ui.components.Background
import com.example.findmycafe.ui.screens.DetailScreen
import com.example.findmycafe.ui.screens.FavoritesScreen
import com.example.findmycafe.ui.screens.HomeScreen
import com.example.findmycafe.ui.screens.LoginScreen
import com.example.findmycafe.ui.screens.MapScreen
import com.example.findmycafe.ui.screens.ProfileScreen
import com.example.findmycafe.ui.screens.RegisterScreen

private enum class Screen { LOGIN, REGISTER, HOME, MAP, FAVORITES, PROFILE, DETAIL }

@Composable
fun FindMyCafeApp(viewModel: CafeViewModel, hasLocationPermission: Boolean, requestLocation: () -> Unit, googleSignIn: ((String) -> Unit) -> Unit, openDirections: (Coordinate) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var screenName by rememberSaveable { mutableStateOf(if (state.isSignedIn) Screen.HOME.name else Screen.LOGIN.name) }
    var selectedId by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn && (screenName == Screen.LOGIN.name || screenName == Screen.REGISTER.name)) {
            screenName = Screen.HOME.name
        }
    }
    val screen = Screen.valueOf(screenName)
    val go: (Screen) -> Unit = { screenName = it.name }
    val navigate: (String) -> Unit = { screenName = it }
    val selected = state.cafes.firstOrNull { it.id == selectedId }
    Box(Modifier.fillMaxSize().background(Background)) {
        when (screen) {
            Screen.LOGIN -> LoginScreen(viewModel, { googleSignIn(it) }, { go(Screen.REGISTER) }, { go(Screen.HOME) })
            Screen.REGISTER -> RegisterScreen(viewModel, { go(Screen.HOME) }, { go(Screen.LOGIN) })
            Screen.HOME -> HomeScreen(state, { selectedId = it; go(Screen.DETAIL) }, viewModel::toggleFavorite, requestLocation, navigate)
            Screen.MAP -> MapScreen(state, hasLocationPermission, requestLocation, { selectedId = it; go(Screen.DETAIL) }, viewModel::toggleFavorite, navigate)
            Screen.FAVORITES -> FavoritesScreen(state, { selectedId = it; go(Screen.DETAIL) }, viewModel::toggleFavorite, navigate)
            Screen.PROFILE -> ProfileScreen(state, { go(Screen.FAVORITES) }, { go(Screen.MAP) }, { viewModel.signOut(); go(Screen.LOGIN) }, navigate)
            Screen.DETAIL -> selected?.let { DetailScreen(it, it.id in state.favoriteIds, { go(Screen.HOME) }, viewModel::toggleFavorite, openDirections) } ?: HomeScreen(state, { selectedId = it; go(Screen.DETAIL) }, viewModel::toggleFavorite, requestLocation, navigate)
        }
    }
}
