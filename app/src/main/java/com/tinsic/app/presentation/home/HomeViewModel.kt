package com.tinsic.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinsic.app.data.model.Song
import com.tinsic.app.data.repository.AuthRepository
import com.tinsic.app.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val genres = listOf("All", "Energize", "Relax", "Focus", "Party", "Chill")

    init {
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            songRepository.getAllSongs().collect { songList ->
                _songs.value = songList
                _isLoading.value = false
            }
        }
    }

    fun filterByGenre(genre: String) {
        _selectedGenre.value = genre
        if (genre == "All") {
            loadSongs()
        } else {
            viewModelScope.launch {
                _isLoading.value = true
                songRepository.getSongsByGenre(genre).collect { songList ->
                    _songs.value = songList
                    _isLoading.value = false
                }
            }
        }
    }

    fun getQuickPicks(): List<Song> {
        return _songs.value.take(10)
    }

    fun getKeepListening(): List<Song> {
        return _songs.value.takeLast(5)
    }
}
