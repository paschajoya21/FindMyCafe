package com.example.findmycafe.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    val currentUser: FirebaseUser?
    suspend fun signIn(email: String, password: String): FirebaseUser
    suspend fun register(name: String, email: String, password: String): FirebaseUser
    suspend fun signInWithGoogle(idToken: String): FirebaseUser
    fun signOut()
}

class FirebaseAuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : AuthRepository {
    override val currentUser: FirebaseUser? get() = auth.currentUser
    override suspend fun signIn(email: String, password: String): FirebaseUser =
        auth.signInWithEmailAndPassword(email.trim(), password).await().user ?: error("Login gagal.")
    override suspend fun register(name: String, email: String, password: String): FirebaseUser {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user ?: error("Pendaftaran gagal.")
        user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        return user
    }
    override suspend fun signInWithGoogle(idToken: String): FirebaseUser =
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user ?: error("Login Google gagal.")
    override fun signOut() = auth.signOut()
}
