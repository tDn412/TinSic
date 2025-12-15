package com.tinsic.app.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tinsic.app.data.model.Song
import com.tinsic.app.data.repository.SongRepository
import com.tinsic.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _discoverQueue = MutableStateFlow<List<Song>>(emptyList())
    val discoverQueue: StateFlow<List<Song>> = _discoverQueue.asStateFlow()
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var heardSongIds = setOf<String>()
    private var allSongs = listOf<Song>()
    private var currentIndex = 0

    init {
        loadInitialBatch()
    }

    private fun loadInitialBatch() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = auth.currentUser?.uid
            
            if (userId != null) {
                // Get user's liked and disliked songs
                val user = userRepository.getCurrentUser(userId)
                val likedIds = user?.likedSongs?.toSet() ?: emptySet()
                val dislikedIds = user?.dislikedSongs?.toSet() ?: emptySet()
                
                // Get user's play history
                val historyIds = userRepository.getRecentlyPlayed(userId, limit = 1000)
                
                // Combine all heard song IDs
                heardSongIds = historyIds.toSet() + likedIds + dislikedIds
            }
            
            // Get all songs once and cache them
            songRepository.getAllSongs().collect { songs ->
                allSongs = songs
                loadNextBatch()
                _isLoading.value = false
            }
        }
    }

    private fun loadNextBatch() {
        // Filter unheard songs and take next 10
        val unheardSongs = allSongs.filter { it.id !in heardSongIds }
        val nextBatch = unheardSongs.take(10)
        
        _discoverQueue.value = nextBatch
        currentIndex = 0
        _currentSong.value = nextBatch.getOrNull(0)
    }

    fun likeSong(song: Song) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            userRepository.likeSong(userId, song.id)
            
            // Add to heard songs
            heardSongIds = heardSongIds + song.id
            
            // Move to next song
            moveToNextSong()
        }
    }

    fun dislikeSong(song: Song) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            userRepository.dislikeSong(userId, song.id)
            
            // Add to heard songs
            heardSongIds = heardSongIds + song.id
            
            // Move to next song
            moveToNextSong()
        }
    }

    private fun moveToNextSong() {
        currentIndex += 1
        
        if (currentIndex < _discoverQueue.value.size) {
            // Move to next song in current batch
            _currentSong.value = _discoverQueue.value.getOrNull(currentIndex)
        } else {
            // Load next batch when queue is exhausted
            loadNextBatch()
        }
    }
}
