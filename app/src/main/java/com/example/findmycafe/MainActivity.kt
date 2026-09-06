package com.example.findmycafe

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.findmycafe.data.FusedLocationRepository
import com.example.findmycafe.ui.CafeViewModel
import com.example.findmycafe.ui.FindMyCafeApp
import com.example.findmycafe.ui.theme.FindMyCafeTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CafeViewModel
    private var hasLocationPermission by mutableStateOf(false)
    private val requestLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasLocationPermission = granted; if (granted) updateLocation() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        viewModel = ViewModelProvider(this, CafeViewModelFactory(this))[CafeViewModel::class.java]
        if (hasLocationPermission) updateLocation()
        enableEdgeToEdge()
        setContent { FindMyCafeTheme(darkTheme = false, dynamicColor = false) { FindMyCafeApp(viewModel, hasLocationPermission, { if (!hasLocationPermission) requestLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION) else updateLocation() }, ::signInWithGoogle, ::openDirections) } }
    }

    private fun updateLocation() = lifecycleScope.launch { viewModel.refreshLocation() }
    private fun openDirections(location: com.example.findmycafe.data.Coordinate) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${location.latitude},${location.longitude}&mode=d"))) }
    private fun signInWithGoogle(onError: (String) -> Unit) = lifecycleScope.launch {
        try {
            val option = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(getString(R.string.default_web_client_id)).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = CredentialManager.create(this@MainActivity).getCredential(this@MainActivity, request).credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) return@launch onError("Akun Google tidak dapat diproses.")
            viewModel.signInWithGoogle(GoogleIdTokenCredential.createFrom(credential.data).idToken) { if (!it) onError(viewModel.uiState.value.error ?: "Login Google gagal.") }
        } catch (e: Exception) { onError(e.localizedMessage ?: "Google Sign-In gagal.") }
    }
}

private class CafeViewModelFactory(activity: MainActivity) : ViewModelProvider.Factory {
    private val locationRepository = FusedLocationRepository(activity)
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST") return CafeViewModel(locationRepository = locationRepository) as T
    }
}
