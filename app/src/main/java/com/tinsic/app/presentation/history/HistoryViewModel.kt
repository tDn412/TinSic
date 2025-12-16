package com.tinsic.app.presentation.history

import android.text.format.DateUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tinsic.app.data.model.HistoryItem
import com.tinsic.app.data.model.Song
import com.tinsic.app.data.repository.SongRepository
import com.tinsic.app.data.repository.UserRepository
import com.tinsic.app.presentation.player.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val songRepository: SongRepository
) : ViewModel() {

    private val _historyItems = MutableStateFlow<Map<String, List<Pair<Song, Long>>>>(emptyMap())
    val historyItems: StateFlow<Map<String, List<Pair<Song, Long>>>> = _historyItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _likedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val likedSongIds: StateFlow<Set<String>> = _likedSongIds.asStateFlow()

    private val _userPlaylists = MutableStateFlow<List<com.tinsic.app.data.model.Playlist>>(emptyList())
    val userPlaylists: StateFlow<List<com.tinsic.app.data.model.Playlist>> = _userPlaylists.asStateFlow()

    init {
        loadHistory()
        observeUserData()
    }

    private fun observeUserData() {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            
            // Observe Liked Songs
            launch {
                userRepository.getUserById(userId).collect { user ->
                    _likedSongIds.value = user?.likedSongs?.toSet() ?: emptySet()
                }
            }
            
            // Observe Playlists
            launch {
                userRepository.getUserPlaylists(userId).collect { playlists ->
                    _userPlaylists.value = playlists
                }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            
            // Fetch history tracking items (SongID + Timestamp)
            val history = userRepository.getFullHistory(userId)
            if (history.isEmpty()) {
                _isLoading.value = false
                return@launch
            }

            // Fetch song details
            val songIds = history.map { it.songId }.distinct()
            val songs = songRepository.getSongsByIds(songIds).associateBy { it.id }

            // Group by Date
            val groupedHistory = history.mapNotNull { item ->
                songs[item.songId]?.let { song ->
                    song to item.playedAt
                }
            }.groupBy { (_, timestamp) ->
                formatDate(timestamp)
            }

            _historyItems.value = groupedHistory
            _isLoading.value = false
        }
    }

    fun removeFromHistory(songId: String) {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val result = userRepository.removeFromHistory(userId, songId)
            if (result.isSuccess) {
                // Refresh history locally
                loadHistory()
            }
        }
    }

    fun addToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            userRepository.addToPlaylist(playlistId, songId)
        }
    }

    private fun formatDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        return if (DateUtils.isToday(timestamp)) {
            "Today"
        } else if (DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS)) {
            "Yesterday"
        } else {
            SimpleDateFormat("yyyy/MM", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
