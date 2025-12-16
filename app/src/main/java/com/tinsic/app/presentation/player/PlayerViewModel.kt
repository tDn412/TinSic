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
    @ApplicationContext private val context: Context,
    private val playbackDataStore: com.tinsic.app.data.local.PlaybackDataStore
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

    private val _user = MutableStateFlow<com.tinsic.app.data.model.User?>(null)
    val user: StateFlow<com.tinsic.app.data.model.User?> = _user.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _userPlaylists = MutableStateFlow<List<com.tinsic.app.data.model.Playlist>>(emptyList())
    val userPlaylists: StateFlow<List<com.tinsic.app.data.model.Playlist>> = _userPlaylists.asStateFlow()

    private val playerListener = object : androidx.media3.common.Player.Listener {
        // ... (existing listener methods) ...
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                // Determine duration if unknown
                if (_duration.value == 0L && exoPlayer.duration > 0) {
                    _duration.value = exoPlayer.duration
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == androidx.media3.common.Player.STATE_READY && exoPlayer.isPlaying) {
                 _duration.value = exoPlayer.duration.takeIf { it > 0 } ?: 0L
            }
             if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                 playNext() // Auto play next
            }
        }
        
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            error.printStackTrace()
             println("DEBUG_PLAYER: Error code=${error.errorCode}, message=${error.message}")
            _isPlaying.value = false
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        
        // Observe Current User & Playlists
        viewModelScope.launch {
            auth.currentUser?.uid?.let { uid ->
                launch {
                    userRepository.getUserById(uid).collect { user ->
                        _user.value = user
                        checkIsLiked()
                    }
                }
                launch {
                    userRepository.getUserPlaylists(uid).collect { playlists ->
                        _userPlaylists.value = playlists
                    }
                }
            }
        }


        // Update playback position and lyrics periodically
        viewModelScope.launch {
            while (isActive) {
                if (_isPlaying.value) {
                    _currentPosition.value = exoPlayer.currentPosition
                    if (_duration.value <= 0 && exoPlayer.duration > 0) {
                         _duration.value = exoPlayer.duration
                    }
                    updateCurrentLyricLine()
                }
                delay(100)
            }
            // Restore last playback state
        viewModelScope.launch {
            launch {
                playbackDataStore.lastPlaylist.collect { restoredPlaylist ->
                    if (_playlist.value.isEmpty() && restoredPlaylist.isNotEmpty()) {
                        _playlist.value = restoredPlaylist
                    }
                }
            }
            launch {
                playbackDataStore.lastSong.collect { lastSong ->
                    if (_currentSong.value == null && lastSong != null) {
                         _currentSong.value = lastSong
                         // Prepare player but don't play
                         val mediaItem = MediaItem.Builder()
                            .setUri(lastSong.audioUrl)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(lastSong.title)
                                    .setArtist(lastSong.artist)
                                    .setArtworkUri(Uri.parse(lastSong.coverUrl))
                                    .build()
                            )
                            .build()
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        exoPlayer.pause() // Default to paused
                        
                        // Find index
                        currentIndex = _playlist.value.indexOfFirst { it.id == lastSong.id }.takeIf { it != -1 } ?: 0
                    }
                }
            }
        }
    }
    }
    
    private fun checkIsLiked() {
        val songId = _currentSong.value?.id ?: return
        val likedSongs = _user.value?.likedSongs ?: emptyList()
        _isLiked.value = likedSongs.contains(songId)
    }

    fun toggleLike() {
        val songId = _currentSong.value?.id ?: return
        val uid = auth.currentUser?.uid ?: return
        val currentLiked = _isLiked.value
        
        // Optimistic Update
        _isLiked.value = !currentLiked
        
        viewModelScope.launch {
            val result = if (currentLiked) {
                userRepository.dislikeSong(uid, songId)
            } else {
                userRepository.likeSong(uid, songId)
            }
            // Revert if failed (optional, usually firestore listener will correct it)
             if (result.isFailure) {
                 _isLiked.value = currentLiked
             }
        }
    }

    fun playSong(song: Song) {
        isPreviewMode = false
        _currentSong.value = song
        checkIsLiked() // Update like status
        
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
        // _isPlaying.value = true -> REMOVED, relying on Listener
        
        // Start Media Service for Action
        val intent = Intent(context, TinSicMediaService::class.java).apply {
            action = "ACTION_START_PLAYBACK"
        }
        try {
            // Use startService to avoid ForegroundServiceDidNotStartInTimeException
            // MediaSessionService will promote itself to foreground when playback starts
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Load lyrics
        loadLyrics(song)
        
        // Start history tracking (30 second threshold)
        startHistoryTracking(song)
        
        // Save state
        viewModelScope.launch {
            playbackDataStore.saveLastSong(song)
            if (_playlist.value.isNotEmpty()) {
                 playbackDataStore.savePlaylist(_playlist.value)
            }
        }
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
        // _isPlaying.value = true -> REMOVED
        
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
        // _isPlaying.value = false -> REMOVED
        historyTrackingJob?.cancel()
    }

    fun playPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        _currentPosition.value = position
    }
    
    /**
     * Stop playback and clear current song (hides MiniPlayer)
     * Use this when entering Game Room or other contexts where player should be hidden
     */
    fun stopAndClear() {
        android.util.Log.d("PlayerViewModel", "stopAndClear() called")
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _currentSong.value = null
        _isPlaying.value = false
        historyTrackingJob?.cancel()
        android.util.Log.d("PlayerViewModel", "currentSong cleared: ${_currentSong.value}")
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

    fun createPlaylist(name: String, addCurrentSong: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
             val result = userRepository.createPlaylist(uid, name)
             if (result.isSuccess && addCurrentSong) {
                 val playlistId = result.getOrNull()
                 if (playlistId != null) {
                     addToPlaylist(playlistId)
                 }
             }
        }
    }

    fun addToPlaylist(playlistId: String) {
         val songId = _currentSong.value?.id ?: return
         viewModelScope.launch {
             userRepository.addToPlaylist(playlistId, songId)
         }
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
