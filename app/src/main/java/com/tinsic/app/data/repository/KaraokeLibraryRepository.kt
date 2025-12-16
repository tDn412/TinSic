package com.tinsic.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tinsic.app.data.model.Song
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KaraokeLibraryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Search songs from the independent 'karaoke_songs' collection
    suspend fun searchKaraokeSongs(query: String): List<com.tinsic.app.data.model.KaraokeSong> {
        return try {
            if (query.isBlank()) return emptyList()

            // Using simple prefix search
            val capitalizedQuery = query.replaceFirstChar { it.uppercase() }

            // Strictly query the dedicated karaoke assets collection
            val snapshot = firestore.collection("karaoke_assets")
                .orderBy("title")
                .startAt(capitalizedQuery)
                .endAt(capitalizedQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.tinsic.app.data.model.KaraokeSong::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("KaraokeLibraryRepo", "Search error on karaoke_songs: $e")
            emptyList()
        }
    }
}
