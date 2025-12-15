package com.tinsic.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tinsic.app.data.model.Song
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getAllSongs(): Flow<List<Song>> = callbackFlow {
        val listener = firestore.collection("songs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val songs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Song::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(songs)
            }
        
        awaitClose { listener.remove() }
    }

    suspend fun getSongById(songId: String): Result<Song> {
        return try {
            val doc = firestore.collection("songs").document(songId).get().await()
            val song = doc.toObject(Song::class.java)?.copy(id = doc.id)
                ?: throw Exception("Song not found")
            Result.success(song)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSongsByGenre(genre: String): Flow<List<Song>> = callbackFlow {
        val listener = firestore.collection("songs")
            .whereEqualTo("genre", genre)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val songs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Song::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(songs)
            }
        
        awaitClose { listener.remove() }
    }

    suspend fun getSongsByGenres(genres: List<String>, limit: Int = 10): List<Song> {
        return try {
            if (genres.isEmpty()) {
                // Return first 10 songs if no genres selected
                firestore.collection("songs")
                    .limit(limit.toLong())
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(Song::class.java)?.copy(id = it.id) }
            } else {
                // Filter by genres
                firestore.collection("songs")
                    .whereIn("genre", genres)
                    .limit(limit.toLong())
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(Song::class.java)?.copy(id = it.id) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSongsByIds(songIds: List<String>): List<Song> {
        return try {
            if (songIds.isEmpty()) return emptyList()
            
            val songs = mutableListOf<Song>()
            songIds.forEach { songId ->
                val result = getSongById(songId)
                result.getOrNull()?.let { songs.add(it) }
            }
            songs
        } catch (e: Exception) {
            emptyList()
        }
    }
}
