package com.example.findmycafe.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface CafeRepository {
    fun observeCafes(): Flow<List<Cafe>>
    fun observeFavoriteIds(uid: String): Flow<Set<String>>
    suspend fun setFavorite(uid: String, cafeId: String, isFavorite: Boolean)
}

class FirebaseCafeRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CafeRepository {
    override fun observeCafes(): Flow<List<Cafe>> = callbackFlow {
        val listener = firestore.collection("cafeList").addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snapshot?.documents.orEmpty().map(::toCafe))
        }
        awaitClose { listener.remove() }
    }

    override fun observeFavoriteIds(uid: String): Flow<Set<String>> = callbackFlow {
        val listener = firestore.collection("users").document(uid).collection("favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { it.id }?.toSet().orEmpty())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun setFavorite(uid: String, cafeId: String, isFavorite: Boolean) {
        val reference = firestore.collection("users").document(uid).collection("favorites").document(cafeId)
        if (isFavorite) reference.set(mapOf("cafeId" to cafeId, "savedAt" to FieldValue.serverTimestamp()))
        else reference.delete()
    }

    private fun toCafe(document: DocumentSnapshot): Cafe {
        val point = document.getGeoPoint("location")
        return Cafe(
            id = document.getString("id") ?: document.id,
            name = document.getString("name") ?: "Kafe tanpa nama",
            rating = document.getDouble("rating") ?: 0.0,
            address = document.getString("address") ?: "",
            description = document.getString("description") ?: "",
            location = point?.let { Coordinate(it.latitude, it.longitude) }
        )
    }
}
