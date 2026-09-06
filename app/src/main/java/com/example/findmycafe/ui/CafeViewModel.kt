package com.example.findmycafe.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findmycafe.data.AuthRepository
import com.example.findmycafe.data.Cafe
import com.example.findmycafe.data.CafeRepository
import com.example.findmycafe.data.Coordinate
import com.example.findmycafe.data.FirebaseAuthRepository
import com.example.findmycafe.data.FirebaseCafeRepository
import com.example.findmycafe.data.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CafeUiState(
    val userName: String = "",
    val userEmail: String = "",
    val isSignedIn: Boolean = false,
    val cafes: List<Cafe> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val userLocation: Coordinate? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CafeViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val cafeRepository: CafeRepository = FirebaseCafeRepository(),
    private val locationRepository: LocationRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(CafeUiState())
    val uiState: StateFlow<CafeUiState> = _uiState.asStateFlow()

    init { refreshSession() }

    fun refreshSession() {
        val user = authRepository.currentUser
        _uiState.value = _uiState.value.copy(
            isSignedIn = user != null,
            userName = user?.displayName.orEmpty(),
            userEmail = user?.email.orEmpty(),
            error = null
        )
        if (user != null) observeUserData(user.uid)
    }

    private fun observeUserData(uid: String) {
        viewModelScope.launch {
            cafeRepository.observeCafes().catch { showError(it) }.collectLatest { cafes ->
                _uiState.value = _uiState.value.copy(cafes = cafes, isLoading = false)
            }
        }
        viewModelScope.launch {
            cafeRepository.observeFavoriteIds(uid).catch { showError(it) }.collectLatest { ids ->
                _uiState.value = _uiState.value.copy(favoriteIds = ids)
            }
        }
    }

    fun signIn(email: String, password: String, onComplete: (Boolean) -> Unit = {}) = viewModelScope.launch {
        runCatching { authRepository.signIn(email, password) }.onSuccess { refreshSession(); onComplete(true) }.onFailure { showError(it); onComplete(false) }
    }
    fun register(name: String, email: String, password: String, onComplete: (Boolean) -> Unit = {}) = viewModelScope.launch {
        runCatching { authRepository.register(name, email, password) }.onSuccess { refreshSession(); onComplete(true) }.onFailure { showError(it); onComplete(false) }
    }
    fun signInWithGoogle(token: String, onComplete: (Boolean) -> Unit = {}) = viewModelScope.launch {
        runCatching { authRepository.signInWithGoogle(token) }.onSuccess { refreshSession(); onComplete(true) }.onFailure { showError(it); onComplete(false) }
    }
    fun toggleFavorite(cafeId: String) = viewModelScope.launch {
        val user = authRepository.currentUser ?: return@launch
        runCatching { cafeRepository.setFavorite(user.uid, cafeId, cafeId !in _uiState.value.favoriteIds) }.onFailure(::showError)
    }
    fun updateLocation(location: Coordinate?) { _uiState.value = _uiState.value.copy(userLocation = location) }
    suspend fun refreshLocation() {
        locationRepository?.let { repository ->
            runCatching { repository.currentLocation() }.onSuccess(::updateLocation).onFailure(::showError)
        }
    }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun signOut() { authRepository.signOut(); _uiState.value = CafeUiState() }
    private fun showError(error: Throwable) { _uiState.value = _uiState.value.copy(isLoading = false, error = error.localizedMessage ?: "Terjadi kesalahan.") }
}
