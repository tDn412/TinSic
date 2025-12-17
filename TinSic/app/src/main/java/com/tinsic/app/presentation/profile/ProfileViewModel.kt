package com.tinsic.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tinsic.app.data.model.Playlist
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
    private val auth: FirebaseAuth
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            // Combine User stream (for Liked Songs) and Playlist stream (for Custom Playlists)
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
                
                // Return Liked Songs + Custom Playlists
                listOf(likedSongsPlaylist) + customPlaylists
            }.collect { emit(it) }
        } else {
            emit(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}
