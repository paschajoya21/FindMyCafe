package com.example.findmycafe

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.findmycafe.ui.theme.FindMyCafeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch

private val Background = Color(0xFFF9F8F6); private val Surface = Color.White; private val Ink = Color(0xFF3C342E)
private val Muted = Color(0xFF796E65); private val Line = Color(0xFFD9CFC7); private val Sand = Color(0xFFEFE9E3)
private val Accent = Color(0xFFC9B59C); private val MapBase = Color(0xFFE5DED7)
private enum class Screen { LOGIN, REGISTER, HOME, MAP, FAVORITES, PROFILE, DETAIL }
private data class Cafe(
    val id: String = "",
    val name: String = "",
    val rating: Double = 0.0,
    val address: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    val distance: String get() = "Lihat peta"
    val ratingText: String get() = "%.1f".format(rating)

    fun distanceInKm(from: LatLng?): Double? {
        if (from == null || (latitude == 0.0 && longitude == 0.0)) return null
        val earthRadiusKm = 6371.0
        val latitudeDifference = Math.toRadians(latitude - from.latitude)
        val longitudeDifference = Math.toRadians(longitude - from.longitude)
        val a = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2) +
            Math.cos(Math.toRadians(from.latitude)) * Math.cos(Math.toRadians(latitude)) *
            Math.sin(longitudeDifference / 2) * Math.sin(longitudeDifference / 2)
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    fun distanceText(from: LatLng?): String = distanceInKm(from)?.let { km ->
        if (km < 1) "${(km * 1000).toInt()} m" else "%.1f km".format(km)
    } ?: "Lokasi tidak tersedia"
}

private fun cafeFromDocument(document: com.google.firebase.firestore.DocumentSnapshot): Cafe {
    val location = document.getGeoPoint("location")
    return Cafe(
        id = document.getString("id") ?: document.id,
        name = document.getString("name") ?: "Kafe tanpa nama",
        rating = document.getDouble("rating") ?: 0.0,
        address = document.getString("address") ?: "",
        description = document.getString("description") ?: "",
        latitude = location?.latitude ?: 0.0,
        longitude = location?.longitude ?: 0.0
    )
}

class MainActivity : ComponentActivity() {
    private var locationPermissionGranted by mutableStateOf(false)
    private val requestLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> locationPermissionGranted = granted }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); locationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED; enableEdgeToEdge(); setContent { FindMyCafeTheme(darkTheme = false, dynamicColor = false) { CafeApp(onRequestLocation = { if (!locationPermissionGranted) requestLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION) }, hasLocationPermission = locationPermissionGranted, onGoogleSignIn = ::signInWithGoogle, onDirections = ::openDirections) } } }

    private fun openDirections(cafe: Cafe) {
        if (cafe.latitude == 0.0 && cafe.longitude == 0.0) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${cafe.latitude},${cafe.longitude}&mode=d"))
        startActivity(intent)
    }

    private fun signInWithGoogle(onSuccess: (FirebaseUser) -> Unit, onError: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                val credential = CredentialManager.create(this@MainActivity).getCredential(this@MainActivity, request).credential
                if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    onError("Akun Google tidak dapat diproses.")
                    return@launch
                }
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                    .addOnSuccessListener { result -> result.user?.let(onSuccess) ?: onError("Login Google gagal.") }
                    .addOnFailureListener { onError(it.localizedMessage ?: "Login Google gagal.") }
            } catch (error: Exception) {
                onError(error.localizedMessage ?: "Google Sign-In dibatalkan atau gagal.")
            }
        }
    }
}

@Composable private fun CafeApp(onRequestLocation: () -> Unit, hasLocationPermission: Boolean, onGoogleSignIn: ((FirebaseUser) -> Unit, (String) -> Unit) -> Unit, onDirections: (Cafe) -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var page by rememberSaveable { mutableStateOf(if (auth.currentUser == null) Screen.LOGIN.name else Screen.HOME.name) }
    var favorites by remember { mutableStateOf(setOf<String>()) }
    var selectedId by rememberSaveable { mutableStateOf("") }
    var user by remember { mutableStateOf(auth.currentUser) }
    var cafeList by remember { mutableStateOf<List<Cafe>>(emptyList()) }
    var cafeError by remember { mutableStateOf<String?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(user?.uid, hasLocationPermission) {
        if (user != null && hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) userLocation = LatLng(location.latitude, location.longitude)
                else {
                    val tokenSource = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
                        .addOnSuccessListener { current -> userLocation = current?.let { LatLng(it.latitude, it.longitude) } }
                }
            }
        }
    }
    LaunchedEffect(page, user?.uid, hasLocationPermission) {
        if (page == Screen.HOME.name && user != null && !hasLocationPermission) onRequestLocation()
    }
    LaunchedEffect(user?.uid) {
        if (user == null) { cafeList = emptyList(); return@LaunchedEffect }
        firestore.collection("cafeList").get()
            .addOnSuccessListener { snapshot -> cafeList = snapshot.documents.map(::cafeFromDocument); cafeError = null }
            .addOnFailureListener { cafeError = it.localizedMessage ?: "Gagal memuat data kafe." }
    }
    DisposableEffect(user?.uid) {
        val uid = user?.uid
        if (uid == null) {
            favorites = emptySet()
            onDispose { }
        } else {
            val listener = firestore.collection("users").document(uid).collection("favorites")
                .addSnapshotListener { snapshot, error ->
                    if (error == null) favorites = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                }
            onDispose { listener.remove() }
        }
    }
    val go: (Screen) -> Unit = { page = it.name }
    val selected = cafeList.firstOrNull { it.id == selectedId } ?: Cafe()
    val signIn: (String, String, (String?) -> Unit) -> Unit = { email, password, result ->
        auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
            if (task.isSuccessful) { user = auth.currentUser; result(null); go(Screen.HOME) }
            else result(task.exception?.localizedMessage ?: "Login gagal.")
        }
    }
    val createAccount: (String, String, String, (String?) -> Unit) -> Unit = { name, email, password, result ->
        auth.createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                auth.currentUser?.updateProfile(userProfileChangeRequest { displayName = name })
                user = auth.currentUser; result(null); go(Screen.HOME)
            } else result(task.exception?.localizedMessage ?: "Pendaftaran gagal.")
        }
    }
    val toggleFavorite: (String) -> Unit = { cafeId ->
        user?.uid?.let { uid ->
            val favoriteRef = firestore.collection("users").document(uid).collection("favorites").document(cafeId)
            if (cafeId in favorites) favoriteRef.delete()
            else favoriteRef.set(mapOf("cafeId" to cafeId, "savedAt" to FieldValue.serverTimestamp()))
        }
    }
    Box(Modifier.fillMaxSize().background(Background)) { when (Screen.valueOf(page)) {
        Screen.LOGIN -> Login(signIn, { showError -> onGoogleSignIn({ user = it; go(Screen.HOME) }, showError) }, { go(Screen.REGISTER) })
        Screen.REGISTER -> Register(createAccount, { go(Screen.LOGIN) })
        Screen.HOME -> Home(cafeList, cafeError, userLocation, favorites, { selectedId = it.id; go(Screen.DETAIL) }, toggleFavorite, go)
        Screen.MAP -> Map(cafeList, favorites, selected, { selectedId = it.id }, { go(Screen.DETAIL) }, toggleFavorite, onRequestLocation, hasLocationPermission, go)
        Screen.FAVORITES -> Favorites(cafeList, userLocation, favorites, { selectedId = it.id; go(Screen.DETAIL) }, toggleFavorite, go)
        Screen.PROFILE -> Profile(user, { go(Screen.FAVORITES) }, { go(Screen.MAP) }, { auth.signOut(); user = null; favorites = emptySet(); go(Screen.LOGIN) }, go)
        Screen.DETAIL -> Detail(selected, selected.id in favorites, { go(Screen.HOME) }, { toggleFavorite(selected.id) }, { onDirections(selected) })
    } }
}
private fun Set<String>.toggle(id: String) = if (id in this) this - id else this + id

@Composable private fun Login(login: (String, String, (String?) -> Unit) -> Unit, googleLogin: ((String) -> Unit) -> Unit, register: () -> Unit) { var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }; AuthFrame { Spacer(Modifier.height(64.dp)); Brand("Find My Cafe", "Temukan kafe favoritmu"); Spacer(Modifier.height(48.dp)); Field("Email", email) { email = it }; Spacer(Modifier.height(12.dp)); Field("Password", password, true) { password = it }; if (error != null) Text(error!!, color = Color(0xFFB3261E), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)); Spacer(Modifier.height(30.dp)); PrimaryButton("Login", email.isNotBlank() && password.isNotBlank()) { login(email, password) { error = it } }; Spacer(Modifier.height(24.dp)); CenterText("atau"); Spacer(Modifier.height(16.dp)); SecondaryButton("Continue with Google") { googleLogin { error = it } }; Spacer(Modifier.height(38.dp)); CenterText("Belum punya akun?"); Link("Register", register) } }
@Composable private fun Register(create: (String, String, String, (String?) -> Unit) -> Unit, login: () -> Unit) { var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var pass by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }; AuthFrame { Spacer(Modifier.height(26.dp)); Brand("Buat akun", "Mulai cari dan simpan kafe favoritmu"); Spacer(Modifier.height(44.dp)); Field("Nama lengkap", name) { name = it }; Spacer(Modifier.height(12.dp)); Field("Email", email) { email = it }; Spacer(Modifier.height(12.dp)); Field("Password", pass, true) { pass = it }; Spacer(Modifier.height(12.dp)); Field("Konfirmasi password", confirm, true) { confirm = it }; if (error != null) Text(error!!, color = Color(0xFFB3261E), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)); Spacer(Modifier.height(16.dp)); PrimaryButton("Create Account", name.isNotBlank() && email.isNotBlank() && pass.isNotBlank() && pass == confirm) { create(name, email, pass) { error = it } }; Spacer(Modifier.height(26.dp)); CenterText("Dengan mendaftar, Anda menyetujui Terms & Privacy", 10.sp); Spacer(Modifier.height(36.dp)); CenterText("Sudah punya akun?"); Link("Login", login) } }
@Composable private fun AuthFrame(content: @Composable ColumnScope.() -> Unit) = Column(Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, content = content)
@Composable private fun Brand(title: String, sub: String) { Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 28.sp); Spacer(Modifier.height(9.dp)); Text(sub, color = Muted, fontSize = 12.sp) }
@Composable private fun Field(label: String, value: String, secret: Boolean = false, onValue: (String) -> Unit) = OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), singleLine = true, label = { Text(label) }, visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None, colors = fieldColors())
@Composable private fun CenterText(text: String, size: androidx.compose.ui.unit.TextUnit = 12.sp) = Text(text, color = Muted, fontSize = size, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
@Composable private fun Link(text: String, action: () -> Unit) = Text(text, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = action), textAlign = TextAlign.Center)

@Composable private fun Home(cafes: List<Cafe>, cafeError: String?, userLocation: LatLng?, favs: Set<String>, open: (Cafe) -> Unit, toggle: (String) -> Unit, go: (Screen) -> Unit) { var query by remember { mutableStateOf("") }; val nearbyCafes = cafes.filter { it.name.contains(query, true) || it.address.contains(query, true) }.sortedBy { it.distanceInKm(userLocation) ?: Double.MAX_VALUE }; Column(Modifier.fillMaxSize()) { Top("Find My Cafe", "⋮"); LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) { item { Spacer(Modifier.height(10.dp)); Search("Cari kafe, area, atau nama kafe", query) { query = it }; Spacer(Modifier.height(20.dp)); Text("Kafe terdekat", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink); Text(if (userLocation == null) "Menunggu lokasi Anda..." else "Diurutkan berdasarkan jarak dari lokasi Anda", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 4.dp)); if (cafeError != null) Text(cafeError, color = Color(0xFFB3261E), fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp)); Spacer(Modifier.height(12.dp)); Filters(); Spacer(Modifier.height(12.dp)) }; items(nearbyCafes) { CardCafe(it, userLocation, it.id in favs, { open(it) }, { toggle(it.id) }) }; if (cafes.isEmpty() && cafeError == null) item { Text("Memuat data kafe...", color = Muted, modifier = Modifier.padding(vertical = 24.dp)) } }; Bottom(Screen.HOME, go) } }
@Composable private fun Filters() = Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Filter", "Jarak", "Rating", "Buka").forEach { Text(it, color = Ink, fontSize = 11.sp, modifier = Modifier.clip(CircleShape).background(Sand).padding(horizontal = 17.dp, vertical = 9.dp)) } }
@Composable private fun Favorites(cafes: List<Cafe>, userLocation: LatLng?, favs: Set<String>, open: (Cafe) -> Unit, toggle: (String) -> Unit, go: (Screen) -> Unit) { Column(Modifier.fillMaxSize()) { Top("Favorites"); if (favs.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth(), Arrangement.Center, Alignment.CenterHorizontally) { Text("♡", fontSize = 64.sp, color = Accent); Text("Belum ada kafe favorit", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(10.dp)); Text("Simpan kafe yang Anda sukai\nagar mudah ditemukan kembali.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center); Spacer(Modifier.height(24.dp)); Button({ go(Screen.HOME) }, colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = CircleShape) { Text("Explore Cafes", Modifier.padding(horizontal = 16.dp), color = Surface) } } else LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) { item { Spacer(Modifier.height(10.dp)); Search("Cari favorite...", "") {}; Spacer(Modifier.height(12.dp)) }; items(cafes.filter { it.id in favs }.sortedBy { it.distanceInKm(userLocation) ?: Double.MAX_VALUE }) { CardCafe(it, userLocation, true, { open(it) }, { toggle(it.id) }) } }; Bottom(Screen.FAVORITES, go) } }
@Composable private fun CardCafe(cafe: Cafe, userLocation: LatLng?, fav: Boolean, open: () -> Unit, toggle: () -> Unit) = Card(Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(onClick = open), colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(12.dp).height(84.dp), verticalAlignment = Alignment.CenterVertically) { CafeImage(84.dp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) { Text(cafe.name, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("★ ${cafe.ratingText}", color = Ink, fontSize = 13.sp); Text("Cafe • ${cafe.distanceText(userLocation)}", color = Muted, fontSize = 12.sp); Text("Buka • sampai 22:00", color = Muted, fontSize = 11.sp) }; Text(if (fav) "♥" else "♡", color = Accent, fontSize = 28.sp, modifier = Modifier.align(Alignment.Top).clickable(onClick = toggle)) } }
@Composable private fun CafeImage(size: androidx.compose.ui.unit.Dp) = Box(Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(Sand), contentAlignment = Alignment.Center) { Text("CAFE\nIMAGE", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }

@Composable private fun Map(cafes: List<Cafe>, favs: Set<String>, selected: Cafe, select: (Cafe) -> Unit, details: () -> Unit, toggle: (String) -> Unit, location: () -> Unit, hasLocationPermission: Boolean, go: (Screen) -> Unit) {
    var centerRequest by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        GoogleCafeMap(cafes, hasLocationPermission, centerRequest, select, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(34.dp))
            Search("Cari kafe atau lokasi", "") {}
            Spacer(Modifier.weight(1f))
            if (selected.id.isNotBlank()) MapSheet(selected, selected.id in favs, details) { toggle(selected.id) }
            Bottom(Screen.MAP, go)
        }
        Button(onClick = { centerRequest++; location() }, Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 148.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = Ink)) { Text("◎", fontSize = 22.sp) }
    }
}

@Composable private fun GoogleCafeMap(cafes: List<Cafe>, hasLocationPermission: Boolean, centerRequest: Int, onCafeSelected: (Cafe) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    var map by remember { mutableStateOf<com.google.android.gms.maps.GoogleMap?>(null) }
    var initialCameraSet by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(map, hasLocationPermission, centerRequest) {
        if (map != null && hasLocationPermission && centerRequest > 0) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f)) }
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier) {
        if (map == null) {
            mapView.getMapAsync { loadedMap -> map = loadedMap }
        } else {
            val loadedMap = map ?: return@AndroidView
            loadedMap.clear()
            loadedMap.uiSettings.isMyLocationButtonEnabled = false
            if (hasLocationPermission) {
                loadedMap.isMyLocationEnabled = true
            }
            cafes.filter { it.latitude != 0.0 || it.longitude != 0.0 }.forEach { cafe ->
                loadedMap.addMarker(MarkerOptions().position(LatLng(cafe.latitude, cafe.longitude)).title(cafe.name))?.tag = cafe.id
            }
            loadedMap.setOnMarkerClickListener { marker ->
                cafes.firstOrNull { it.id == marker.tag as? String }?.let(onCafeSelected)
                true
            }
            if (!initialCameraSet && cafes.isNotEmpty()) {
                val firstCafe = cafes.firstOrNull { it.latitude != 0.0 || it.longitude != 0.0 }
                if (firstCafe != null) {
                    loadedMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(firstCafe.latitude, firstCafe.longitude), 13f))
                    initialCameraSet = true
                }
            }
        }
    }
}
@Composable private fun MapSheet(cafe: Cafe, fav: Boolean, details: () -> Unit, toggle: () -> Unit) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), colors = CardDefaults.cardColors(containerColor = Surface), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Column(Modifier.padding(24.dp)) { Box(Modifier.align(Alignment.CenterHorizontally).size(32.dp, 4.dp).background(Sand, CircleShape)); Spacer(Modifier.height(16.dp)); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(cafe.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f)); Text(if (fav) "♥" else "♡", color = Accent, fontSize = 26.sp, modifier = Modifier.clickable(onClick = toggle)) }; Text("★ ${cafe.ratingText} • ${cafe.distance}", color = Ink, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)); Text("Cafe • Coffee", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)); Spacer(Modifier.height(16.dp)); PrimaryButton("View Details", true, details) } }

@Composable private fun Detail(cafe: Cafe, fav: Boolean, back: () -> Unit, toggle: () -> Unit, directions: () -> Unit) = Column(Modifier.fillMaxSize()) { Row(Modifier.fillMaxWidth().height(64.dp).background(Surface).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Text("‹", color = Ink, fontSize = 40.sp, modifier = Modifier.clickable(onClick = back)); Spacer(Modifier.weight(1f)); Text(if (fav) "♥" else "♡", color = Accent, fontSize = 28.sp, modifier = Modifier.clickable(onClick = toggle)); Spacer(Modifier.width(16.dp)); Text("⋮", color = Ink, fontSize = 26.sp) }; Box(Modifier.fillMaxWidth().height(200.dp).background(Sand), contentAlignment = Alignment.Center) { Text("CAFE IMAGE", color = Muted, fontWeight = FontWeight.Bold) }; Column(Modifier.padding(16.dp)) { Text(cafe.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 24.sp); Text("★ ${cafe.ratingText} (328 reviews)", color = Ink, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)); Text("Cafe • Coffee • $$$", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)); Spacer(Modifier.height(24.dp)); Text("⌖ ${cafe.address}", color = Ink, fontSize = 13.sp); Text("Koordinat: ${cafe.latitude}, ${cafe.longitude}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)); Spacer(Modifier.height(24.dp)); Text("Jam Buka", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Senin–Jumat   08:00–22:00\nSabtu–Minggu  08:00–23:00", color = Muted, fontSize = 12.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 7.dp)); Text("● Open now", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)); Spacer(Modifier.height(18.dp)); Text("Tentang", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(cafe.description, color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp)); Spacer(Modifier.height(20.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) { Button(directions, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = CircleShape) { Text("Directions", color = Surface) }; Button({}, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Sand, contentColor = Ink), shape = CircleShape) { Text("Call") } } } }
@Composable private fun Profile(user: FirebaseUser?, favorites: () -> Unit, map: () -> Unit, logout: () -> Unit, go: (Screen) -> Unit) = Column(Modifier.fillMaxSize()) { Top("Profile"); Column(Modifier.fillMaxWidth().height(166.dp).background(Sand), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("●", color = Accent, fontSize = 54.sp); Text(user?.displayName?.ifBlank { null } ?: "Pengguna", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(user?.email ?: "", color = Muted, fontSize = 12.sp) }; Column(Modifier.weight(1f).padding(16.dp)) { ProfileRow("♡", "Favorites", favorites); ProfileRow("⚙", "Settings") {}; ProfileRow("⌖", "Location & Map", map); ProfileRow("?", "Help & Support") {}; Spacer(Modifier.height(12.dp)); SecondaryButton("Logout", logout) }; Bottom(Screen.PROFILE, go) }
@Composable private fun ProfileRow(icon: String, label: String, click: () -> Unit) = Card(Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(onClick = click), colors = CardDefaults.cardColors(containerColor = Surface), border = androidx.compose.foundation.BorderStroke(1.dp, Line), shape = RoundedCornerShape(10.dp)) { Row(Modifier.height(52.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, color = Ink, fontSize = 20.sp); Text(label, color = Ink, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp)); Spacer(Modifier.weight(1f)); Text("›", color = Muted, fontSize = 26.sp) } }
@Composable private fun Top(title: String, action: String? = null) = Row(Modifier.fillMaxWidth().height(56.dp).background(Surface).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp); Spacer(Modifier.weight(1f)); if (action != null) Text(action, color = Ink, fontSize = 26.sp) }
@Composable private fun Search(hint: String, value: String, input: (String) -> Unit) = OutlinedTextField(value, input, Modifier.fillMaxWidth(), leadingIcon = { Text("⌕", color = Muted, fontSize = 24.sp) }, placeholder = { Text(hint, color = Muted, fontSize = 13.sp) }, singleLine = true, shape = CircleShape, colors = fieldColors())
@Composable private fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Line, focusedLabelColor = Muted, cursorColor = Accent, focusedContainerColor = Surface, unfocusedContainerColor = Surface)
@Composable private fun PrimaryButton(label: String, enabled: Boolean, click: () -> Unit) = Button(click, Modifier.fillMaxWidth().height(50.dp), enabled = enabled, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Accent, disabledContainerColor = Accent.copy(alpha = .45f))) { Text(label, fontWeight = FontWeight.Bold, color = Surface) }
@Composable private fun SecondaryButton(label: String, click: () -> Unit) = Button(click, Modifier.fillMaxWidth().height(48.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = Ink), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Text(label, fontWeight = FontWeight.Bold) }
@Composable private fun Bottom(current: Screen, go: (Screen) -> Unit) { HorizontalDivider(color = Line); Row(Modifier.fillMaxWidth().height(64.dp).background(Surface), horizontalArrangement = Arrangement.SpaceAround) { listOf(Triple(Screen.HOME, "⌂", "Home"), Triple(Screen.MAP, "⌖", "Map"), Triple(Screen.FAVORITES, "♡", "Favorites"), Triple(Screen.PROFILE, "●", "Profile")).forEach { (screen, icon, label) -> val active = screen == current; Column(Modifier.width(76.dp).fillMaxHeight().clickable { go(screen) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(icon, color = if (active) Ink else Muted, fontSize = 20.sp); Text(label, color = if (active) Ink else Muted, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) } } } }
