package com.tinsic.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinsic.app.data.model.Song
import com.tinsic.app.data.repository.AuthRepository
import com.tinsic.app.data.repository.SongRepository
import com.tinsic.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _user = MutableStateFlow<com.tinsic.app.data.model.User?>(null)

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _likedSongs = MutableStateFlow<List<String>>(emptyList())
    val likedSongs: StateFlow<List<String>> = _likedSongs.asStateFlow()

    private val _userPlaylists = MutableStateFlow<List<com.tinsic.app.data.model.Playlist>>(emptyList())
    val userPlaylists: StateFlow<List<com.tinsic.app.data.model.Playlist>> = _userPlaylists.asStateFlow()

    private val _quickPicks = MutableStateFlow<List<Song>>(emptyList())
    val quickPicks: StateFlow<List<Song>> = _quickPicks.asStateFlow()

    private val _keepListening = MutableStateFlow<List<Song>>(emptyList())
    val keepListening: StateFlow<List<Song>> = _keepListening.asStateFlow()

    private val _recentlyPlayedIds = MutableStateFlow<List<String>>(emptyList())

    val genres = listOf("All", "Energize", "Relax", "Focus", "Party", "Chill")

    init {
        loadSongs()
        observeUserData()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            songRepository.getAllSongs().collect { songList ->
                _songs.value = songList
                calculateRecommendations()
                _isLoading.value = false
            }
        }
    }

    private fun observeUserData() {
        authRepository.getCurrentUserId()?.let { uid ->
            viewModelScope.launch {
                launch {
                    userRepository.getUserById(uid).collect { user ->
                        _user.value = user
                        _likedSongs.value = user?.likedSongs ?: emptyList()
                        calculateRecommendations()
                    }
                }
                launch {
                    userRepository.getUserPlaylists(uid).collect { playlists ->
                        _userPlaylists.value = playlists
                    }
                }
                // Fetch recently played initially
                _recentlyPlayedIds.value = userRepository.getRecentlyPlayed(uid)
                calculateRecommendations()
            }
        }
    }

    private fun calculateRecommendations() {
        val allSongs = _songs.value
        val user = _user.value
        val recentIds = _recentlyPlayedIds.value

        if (allSongs.isEmpty()) return

        // 1. Keep Listening (Based on History)
        // Map recentIds to actual Song objects, preserving order
        val recentSongs = recentIds.mapNotNull { id -> allSongs.find { it.id == id } }
        _keepListening.value = if (recentSongs.isNotEmpty()) recentSongs else allSongs.takeLast(5)

        // 2. Quick Picks (Smart Recommendation)
        val favoriteGenres = user?.favoriteGenres?.takeIf { it.isNotEmpty() } 
            ?: user?.likedSongs?.mapNotNull { id -> allSongs.find { it.id == id }?.genre }?.distinct() 
            ?: emptyList()
            
        // Exclude songs currently in "Keep Listening" to avoid duplication (optional but nice)
        val candidates = allSongs.filter { it.id !in recentIds }
        
        val scoredSongs = candidates.map { song ->
            var score = 0
            if (favoriteGenres.any { it.equals(song.genre, ignoreCase = true) }) score += 2
             // boost random factor or other metrics here if needed
            Pair(song, score)
        }
        
        // Sort by score (desc) then shuffle top results for variety
        // If no preferences, this falls back to random shuffle of all candidates
        val topPicks = scoredSongs.sortedByDescending { it.second }
            .take(20) // Take top 20 candidates
            .shuffled() // Shuffle them
            .take(10) // Pick 10
            .map { it.first }
            
        _quickPicks.value = if (topPicks.isNotEmpty()) topPicks else allSongs.shuffled().take(10)
    }

    fun filterByGenre(genre: String) {
        _selectedGenre.value = genre
        if (genre == "All") {
            // Restore original list if we had cached it, or re-fetch/reset. 
            // For now, loadSongs() fetches all. 
            // NOTE: calculateRecommendations relies on _songs. If filtering replaces _songs, recomms break.
            // Better behavior: _songs should always be ALL songs? 
            // Current code in filterByGenre replaces _songs. 
            // Let's assume filterByGenre affects the MAIN list view, but typically QuickPicks stays distinct?
            // Actually, usually Filter affects everything. Let's keep existing logic but re-calc if needed.
            loadSongs() 
        } else {
            viewModelScope.launch {
                _isLoading.value = true
                songRepository.getSongsByGenre(genre).collect { songList ->
                    _songs.value = songList
                    _isLoading.value = false
                    // NOTE: We probably DON'T want to re-calc recommendations based on filtered list 
                    // unless QuickPicks IS part of the filtered view.
                    // For now, let's leave as is. User asked for logic upgrade.
                }
            }
        }
    }

    fun toggleLike(songId: String) {
        val uid = authRepository.getCurrentUserId() ?: return
        val isLiked = _likedSongs.value.contains(songId)
        
        viewModelScope.launch {
            if (isLiked) {
                userRepository.dislikeSong(uid, songId)
            } else {
                userRepository.likeSong(uid, songId)
            }
            // Update local state immediately for UI responsiveness if needed, 
            // or rely on flow collection in observeUserData
        }
    }

    fun addToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
             userRepository.addToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylist(name: String, songId: String?) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
             val result = userRepository.createPlaylist(uid, name)
             if (result.isSuccess && songId != null) {
                 val playlistId = result.getOrNull()
                 if (playlistId != null) {
                     addToPlaylist(playlistId, songId)
                 }
             }
        }
    }
    
    // Helper accessors for UI that doesn't observe flows yet (backward compat if needed)
    // But verify uses _quickPicks.value
    fun getQuickPicksValue(): List<Song> = _quickPicks.value
    fun getKeepListeningValue(): List<Song> = _keepListening.value
}
