package com.tinsic.app.presentation.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinsic.app.data.model.Playlist
import com.tinsic.app.data.model.Song
import com.tinsic.app.data.repository.SongRepository
import com.tinsic.app.data.repository.UserRepository
import com.tinsic.app.presentation.player.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val songRepository: SongRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])
    private val userId: String = checkNotNull(savedStateHandle["userId"]) // Might need to pass userId if generic

    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPlaylist()
    }

    private fun loadPlaylist() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Handle "liked_songs" special case or generic playlist
            val fetchedPlaylist = if (playlistId == "liked_songs") {
                // We need userId here. If argument is missing, try to get current user?
                // For "liked_songs", we assume passed userId is valid or we fetch current user id from Repo?
                // Let's assume passed userId is correct.
                userRepository.getLikedSongsPlaylist(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "")
            } else {
                userRepository.getPlaylistById(playlistId)
            }

            if (fetchedPlaylist != null) {
                _playlist.value = fetchedPlaylist
                _songs.value = songRepository.getSongsByIds(fetchedPlaylist.songIds)
            }
            
            _isLoading.value = false
        }
    }
}
