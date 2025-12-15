package com.tinsic.app.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.google.firebase.auth.FirebaseAuth
import com.tinsic.app.data.model.LyricLine
import com.tinsic.app.data.model.Song
import com.tinsic.app.data.parser.LyricParser
import com.tinsic.app.data.repository.SongRepository
import com.tinsic.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.content.Context
import android.content.Intent
import com.tinsic.app.service.TinSicMediaService
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.media3.common.MediaMetadata
import android.net.Uri
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val songRepository: SongRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _currentLyricIndex = MutableStateFlow(0)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()

    private var currentIndex = 0
    private var historyTrackingJob: Job? = null
    private var isPreviewMode = false

    init {
        // Update playback position and lyrics periodically
        viewModelScope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _currentPosition.value = exoPlayer.currentPosition
                    _duration.value = exoPlayer.duration.takeIf { it > 0 } ?: 0L
                    updateCurrentLyricLine()
                }
                delay(100)
            }
        }
    }

    fun playSong(song: Song) {
        isPreviewMode = false
        _currentSong.value = song
        
        // Create MediaItem with Metadata for Notification
        val mediaItem = MediaItem.Builder()
            .setUri(song.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(Uri.parse(song.coverUrl))
                    .build()
            )
            .build()
            
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        _isPlaying.value = true
        
        // Start Media Service for Foreground Playback & Notification
        val intent = Intent(context, TinSicMediaService::class.java)
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            context.startService(intent)
        }
        
        // Load lyrics
        loadLyrics(song)
        
        // Start history tracking (30 second threshold)
        startHistoryTracking(song)
    }

    fun playPreview(song: Song, durationSeconds: Int = 15) {
        isPreviewMode = true  // Do NOT track history for previews
        _currentSong.value = song
        val mediaItem = MediaItem.fromUri(song.audioUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        
        // Always start from beginning for preview
        exoPlayer.seekTo(0)
        exoPlayer.play()
        _isPlaying.value = true
        
        // Auto-stop after preview duration
        viewModelScope.launch {
            delay((durationSeconds * 1000).toLong())
            if (_currentSong.value?.id == song.id) {
                pause()
            }
        }
    }

    fun pause() {
        exoPlayer.pause()
        _isPlaying.value = false
        historyTrackingJob?.cancel()
    }

    fun playPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            exoPlayer.play()
            _isPlaying.value = true
        }
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        _currentPosition.value = position
    }

    fun playNext() {
        if (_playlist.value.isEmpty()) return
        
        currentIndex = if (_isShuffleEnabled.value) {
            (0 until _playlist.value.size).random()
        } else {
            (currentIndex + 1) % _playlist.value.size
        }
        
        playSong(_playlist.value[currentIndex])
    }

    fun playPrevious() {
        if (_playlist.value.isEmpty()) return
        
        currentIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            _playlist.value.size - 1
        }
        
        playSong(_playlist.value[currentIndex])
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        _playlist.value = songs
        currentIndex = startIndex
        if (songs.isNotEmpty()) {
            playSong(songs[startIndex])
        }
    }
    
    // Helper to queue a single song effectively (e.g. "Play Next")
    fun playNextInQueue(song: Song) {
        val currentList = _playlist.value.toMutableList()
        if (currentIndex + 1 < currentList.size) {
            currentList.add(currentIndex + 1, song)
        } else {
            currentList.add(song)
        }
        _playlist.value = currentList
    }

    private fun startHistoryTracking(song: Song) {
        historyTrackingJob?.cancel()
        historyTrackingJob = viewModelScope.launch {
            delay(30000) // 30 seconds
            if (!isPreviewMode && _currentSong.value?.id == song.id) {
                // Add to history after 30 seconds of playback
                auth.currentUser?.uid?.let { userId ->
                    userRepository.addToHistory(userId, song.id)
                }
            }
        }
    }
    
    private fun loadLyrics(song: Song) {
        println("DEBUG_LYRICS: Loading lyrics for ${song.title}, URL: ${song.lyricUrl}")
        if (song.lyricUrl.isBlank()) {
            println("DEBUG_LYRICS: URL is blank")
            _lyrics.value = emptyList()
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                println("DEBUG_LYRICS: Fetching content on ${Thread.currentThread().name}")
                val lrcContent = java.net.URL(song.lyricUrl).readText()
                println("DEBUG_LYRICS: Content fetched, length: ${lrcContent.length}")
                _lyrics.value = LyricParser.parseLrc(lrcContent)
                println("DEBUG_LYRICS: Parsed ${_lyrics.value.size} lines")
                _currentLyricIndex.value = 0
            } catch (e: Exception) {
                e.printStackTrace()
                println("DEBUG_LYRICS: Error loading lyrics: ${e.message}")
                _lyrics.value = emptyList()
            }
        }
    }
    
    private fun updateCurrentLyricLine() {
        if (_lyrics.value.isEmpty()) return
        
        val currentPos = _currentPosition.value
        val index = _lyrics.value.indexOfLast { it.timeMs <= currentPos }
        if (index >= 0 && index != _currentLyricIndex.value) {
            _currentLyricIndex.value = index
        }
    }

    private var sleepTimerJob: Job? = null
    
    // Sleep Timer
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                pause()
                // Optionally stop service or release player here
            }
        }
    }
    
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
    }
    
    // Queue Management
    fun addToQueue(song: Song) {
        val currentList = _playlist.value.toMutableList()
        currentList.add(song)
        _playlist.value = currentList
    }
    
    fun removeFromQueue(index: Int) {
        if (index in _playlist.value.indices && index != currentIndex) {
            val currentList = _playlist.value.toMutableList()
            currentList.removeAt(index)
            _playlist.value = currentList
            // Update currentIndex if needed (if removed item was before current)
            if (index < currentIndex) {
                currentIndex--
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        historyTrackingJob?.cancel()
        sleepTimerJob?.cancel()
        exoPlayer.release()
    }

    enum class RepeatMode {
        OFF, ALL, ONE
    }
}
