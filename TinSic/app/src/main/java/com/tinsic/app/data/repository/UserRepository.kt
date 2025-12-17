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
            val docRef = firestore.collection("users").document(userId)
                .collection("history").document(songId)
            
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val historyItem = hashMapOf(
                    "songId" to songId,
                    "playedAt" to System.currentTimeMillis()
                )
                docRef.set(historyItem).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromHistory(userId: String, songId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("history").document(songId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFullHistory(userId: String): List<com.tinsic.app.data.model.HistoryItem> {
        return try {
            firestore.collection("users").document(userId)
                .collection("history")
                .orderBy("playedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(com.tinsic.app.data.model.HistoryItem::class.java)?.copy(id = doc.id)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun createPlaylist(userId: String, name: String): Result<String> {
        return try {
            val playlist = hashMapOf(
                "name" to name,
                "userId" to userId,
                "createdAt" to System.currentTimeMillis(),
                "songIds" to emptyList<String>()
            )
            val docRef = firestore.collection("playlists").add(playlist).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToPlaylist(playlistId: String, songId: String): Result<Unit> {
        return try {
            firestore.collection("playlists").document(playlistId)
                .update("songIds", FieldValue.arrayUnion(songId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPlaylists(userId: String): Flow<List<com.tinsic.app.data.model.Playlist>> = callbackFlow {
        val listener = firestore.collection("playlists")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val playlists = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(com.tinsic.app.data.model.Playlist::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(playlists)
            }
        awaitClose { listener.remove() }
    }
    
    // Helper to get "Liked Songs" as a virtual playlist
    suspend fun getLikedSongsPlaylist(userId: String): com.tinsic.app.data.model.Playlist {
         val user = getCurrentUser(userId)
         return com.tinsic.app.data.model.Playlist(
             id = "liked_songs",
             name = "Liked Songs",
             userId = userId,
             songIds = user?.likedSongs ?: emptyList(),
             isDefault = true
         )
    }
    suspend fun getPlaylistById(playlistId: String): com.tinsic.app.data.model.Playlist? {
        return try {
            val doc = firestore.collection("playlists").document(playlistId).get().await()
            doc.toObject(com.tinsic.app.data.model.Playlist::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
             null
        }
    }
}
