package com.tinsic.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tinsic.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getUserById(userId: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }
        
        awaitClose { listener.remove() }
    }

    suspend fun likeSong(userId: String, songId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update(
                    mapOf(
                        "likedSongs" to FieldValue.arrayUnion(songId),
                        "dislikedSongs" to FieldValue.arrayRemove(songId)
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dislikeSong(userId: String, songId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update(
                    mapOf(
                        "dislikedSongs" to FieldValue.arrayUnion(songId),
                        "likedSongs" to FieldValue.arrayRemove(songId)
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAchievement(userId: String, achievementKey: String, value: Boolean): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update("achievements.$achievementKey", value).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFavoriteGenres(userId: String, genres: List<String>): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update("favoriteGenres", genres).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markOnboardingComplete(userId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update("hasCompletedOnboarding", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(userId: String): User? {
        return try {
            firestore.collection("users").document(userId)
                .get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRecentlyPlayed(userId: String, limit: Int = 10): List<String> {
        return try {
            firestore.collection("users").document(userId)
                .collection("history")
                .orderBy("playedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("songId") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addToHistory(userId: String, songId: String): Result<Unit> {
        return try {
            val historyItem = hashMapOf(
                "songId" to songId,
                "playedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(userId)
                .collection("history")
                .add(historyItem)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
