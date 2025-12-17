package com.tinsic.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tinsic.app.data.model.Playlist
import com.tinsic.app.data.repository.SongRepository
import com.tinsic.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val songRepository: SongRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // Helper to get current UID safely
    private val _uid: String?
        get() = auth.currentUser?.uid

    val playlists: StateFlow<List<Playlist>> = flow {
        val uid = _uid
        if (uid != null) {
            combine(
                userRepository.getUserById(uid),
                userRepository.getUserPlaylists(uid)
            ) { user, customPlaylists ->
                val likedSongsPlaylist = Playlist(
                    id = "liked_songs",
                    name = "Liked Songs",
                    userId = uid,
                    songIds = user?.likedSongs ?: emptyList(),
                    isDefault = true
                )
                listOf(likedSongsPlaylist) + customPlaylists
            }.collect { emit(it) }
        } else {
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listeningHistory: StateFlow<List<com.tinsic.app.data.model.HistoryItem>> = flow {
         val uid = _uid
         if (uid != null) {
             userRepository.getHistoryFlow(uid).collect { emit(it) }
         } else {
             emit(emptyList())
         }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val musicDna: StateFlow<com.tinsic.app.data.model.profile.MusicDnaProfile> = flow {
        val uid = _uid
        if (uid != null) {
            combine(
                userRepository.getHistoryFlow(uid),
                songRepository.getAllSongs()
            ) { history, songs ->
                com.tinsic.app.analytics.AnalyticsEngine.calculateDnaProfile(history, songs)
            }.collect { emit(it) }
        } else {
            emit(com.tinsic.app.data.model.profile.MusicDnaProfile(emptyMap(), emptyList(), emptyList()))
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.tinsic.app.data.model.profile.MusicDnaProfile(emptyMap(), emptyList(), emptyList())
    )
}
