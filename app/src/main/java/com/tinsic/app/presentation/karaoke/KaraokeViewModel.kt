package com.tinsic.app.presentation.karaoke

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinsic.app.domain.karaoke.model.LyricLine
import com.tinsic.app.domain.karaoke.model.SongNote
import com.tinsic.app.presentation.karaoke.engine.KaraokeEngine
import com.tinsic.app.presentation.karaoke.engine.KaraokeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KaraokeViewModel @Inject constructor(
    private val karaokeEngine: KaraokeEngine
) : ViewModel() {

    // UI State (Single Source of Truth)
    private val _uiState = MutableStateFlow(KaraokeUiState())
    val uiState = _uiState.asStateFlow()

    // Latency State
    private val _latencyOffset = MutableStateFlow(0)
    val latencyOffset = _latencyOffset.asStateFlow()

    private val pitchBuffer = ArrayDeque<UserPitchPoint>(200)
    private val MAX_HISTORY_SIZE = 200
    
    private var feedbackJob: Job? = null

    init {
        observeEngine()
    }

    private fun observeEngine() {
        viewModelScope.launch {
            var currentPhraseScore = 0
            
            karaokeEngine.singingFlow.collect { result ->
                if (result.midiUser > 0) {
                    if (pitchBuffer.size >= MAX_HISTORY_SIZE) {
                        pitchBuffer.removeFirst()
                    }
                    pitchBuffer.addLast(
                        UserPitchPoint(
                            result.currentTime,
                            result.midiUser,
                            result.feedbackColor
                        )
                    )
                }

                val isHit = result.isHit
                var currentCombo = _uiState.value.combo
                var newFeedbackText = _uiState.value.feedbackText
                var newFeedbackColor = _uiState.value.feedbackColor

                if (isHit) {
                    currentCombo++
                    currentPhraseScore++
                } else {
                    if (result.midiUser > 0) {
                        currentCombo = 0
                        currentPhraseScore = 0
                        newFeedbackText = "" 
                    } else {
                        // Silence (phrase end check)
                        if (currentPhraseScore > 10) {
                            val scaledCombo = currentCombo / 10
                            val baseText = "Perfect"
                            val color = 0xFF00E676 // Green

                            newFeedbackText = "$baseText x$scaledCombo"
                            newFeedbackColor = color.toLong()

                            feedbackJob?.cancel()
                            feedbackJob = viewModelScope.launch {
                                delay(1500)
                                _uiState.update { it.copy(feedbackText = "") }
                            }
                        }
                        currentPhraseScore = 0
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        currentTime = result.currentTime,
                        currentNoteDisplay = result.noteName,
                        currentScore = state.currentScore + result.scoreAdded,
                        combo = currentCombo,
                        feedbackText = newFeedbackText,
                        feedbackColor = newFeedbackColor,
                        userPitchHistory = pitchBuffer.toList()
                    )
                }
            }
        }
    }

    fun startSinging(
    notes: List<SongNote>, 
    lyrics: List<LyricLine>,
    audioUrl: String = "",           // Fallback: Streaming URL
        mp3FilePath: String? = null,     // Preferred: Local prefetched file
        
        // REFACTORED: Decoupled from Host. 
        // This is now determined by who is the "first" person on stage.
        shouldPlayAudio: Boolean = false, 
        
        startTimeMs: Long = 0L,          // Server start time for guest sync
        mySingerId: Int = 1,             // New: Singer ID (1 or 2)
        isSoloMode: Boolean = false      // New: Solo Override
    ) {
        _uiState.update { it.copy(isLoading = true, feedbackText = "Đang tải...") }

        viewModelScope.launch {
            pitchBuffer.clear()
            
            _uiState.update {
                it.copy(
                    isRecording = true,
                    isLoading = false,
                    currentScore = 0, 
                    feedbackText = if (shouldPlayAudio) "Đang phát nhạc..." else "Sẵn sàng hát...",
                    songNotes = notes, 
                    lyrics = lyrics,
                    userPitchHistory = emptyList()
                )
            }

            // Create config with prefetched file support
            val config = com.tinsic.app.presentation.karaoke.engine.KaraokeConfig(
                audioUrl = audioUrl,
                mp3FilePath = mp3FilePath,   // Local file (instant load if available!)
                isPlaybackEnabled = shouldPlayAudio,  // Determined by stage order
            isRecordingEnabled = true,   // Everyone records for scoring
            startTimeMs = startTimeMs,   // Server time for guest sync
            initialLatencyOffsetMs = _latencyOffset.value,
            mySingerId = mySingerId,
            isSoloMode = isSoloMode     // Pass to engine
        )

            karaokeEngine.startRecording(notes, config)
            android.util.Log.d("KaraokeVM", "Started singing - PlayAudio: $shouldPlayAudio, MP3: ${if (mp3FilePath != null) "LOCAL ✅" else "Stream"}, StartTime: $startTimeMs")
    }
}

    /**
     * PHASE 1: Prepare everything (call during LOADING)
     */
    fun prepareSinging(
        notes: List<SongNote>,
        lyrics: List<LyricLine>,
        audioUrl: String = "",
        mp3FilePath: String? = null,
        shouldPlayAudio: Boolean = false,
        mySingerId: Int = 1,
        isSoloMode: Boolean = false // New
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    songNotes = notes,
                    lyrics = lyrics
                )
            }
            
            val config = KaraokeConfig(
                audioUrl = audioUrl,
                mp3FilePath = mp3FilePath,
                isPlaybackEnabled = shouldPlayAudio,
                isRecordingEnabled = true,
                startTimeMs = 0L,
                initialLatencyOffsetMs = _latencyOffset.value,
                mySingerId = mySingerId,
                isSoloMode = isSoloMode // Pass to engine
            )
            
            karaokeEngine.startRecording(notes, config)
            android.util.Log.d("KaraokeVM", "Prepared - PlayAudio: $shouldPlayAudio, MP3: ${if (mp3FilePath != null) "LOCAL" else "Stream"}")
        }
    }

    /**
     * PHASE 2: Start playback (call when PLAYING)
     */
    fun startPlayback(startTimeMs: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRecording = true,
                    isLoading = false,
                    currentScore = 0,
                    feedbackText = "Đang hát...",
                    userPitchHistory = emptyList()
                )
            }
            
            karaokeEngine.config = karaokeEngine.config.copy(startTimeMs = startTimeMs)
            karaokeEngine.startPlayback()
            
            android.util.Log.d("KaraokeVM", "Playback started at $startTimeMs")
        }
    }
     fun stopSinging() {
        karaokeEngine.stopRecording()
        _uiState.update {
            it.copy(
                isRecording = false,
                feedbackText = "Đã dừng",
                feedbackColor = 0xFF808080
            )
        }
    }

    fun setLatencyOffset(offsetMs: Int) {
        _latencyOffset.value = offsetMs
        karaokeEngine.setLatencyOffset(offsetMs)
    }

    fun increaseLatency() {
        val newOffset = _latencyOffset.value + 200
        setLatencyOffset(newOffset)
    }

    fun decreaseLatency() {
        val newOffset = _latencyOffset.value - 200
        setLatencyOffset(newOffset)
    }
}
