package com.tinsic.app.presentation.karaoke

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinsic.app.domain.karaoke.model.LyricLine
import com.tinsic.app.domain.karaoke.model.SongNote
import com.tinsic.app.presentation.karaoke.engine.KaraokeEngine
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

    fun startSinging(notes: List<SongNote>, lyrics: List<LyricLine>) {
        _uiState.update { it.copy(isLoading = true, feedbackText = "Đang tải...") }

        viewModelScope.launch {
            pitchBuffer.clear()
            
            _uiState.update {
                it.copy(
                    isRecording = true,
                    isLoading = false,
                    currentScore = 0, 
                    feedbackText = "Đang phát nhạc...",
                    songNotes = notes, 
                    lyrics = lyrics,
                    userPitchHistory = emptyList()
                )
            }

            karaokeEngine.startRecording(notes)
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
        val newOffset = _latencyOffset.value + 50
        setLatencyOffset(newOffset)
    }

    fun decreaseLatency() {
        val newOffset = _latencyOffset.value - 50
        setLatencyOffset(newOffset)
    }
}
