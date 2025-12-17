package com.tinsic.app.presentation.karaoke

import com.tinsic.app.domain.karaoke.model.LyricLine
import com.tinsic.app.domain.karaoke.model.SongNote

data class KaraokeUiState(
    val isRecording: Boolean = false,
    val currentScore: Int = 0,
    val currentNoteDisplay: String = "--",
    val feedbackText: String = "Nhấn HÁT để bắt đầu",
    val feedbackColor: Long = 0xFF808080, // Gray
    val isLoading: Boolean = false,
    val combo: Int = 0,
    val currentTime: Double = 0.0,
    val songNotes: List<SongNote> = emptyList(),
    val userPitchHistory: List<UserPitchPoint> = emptyList(),
    val lyrics: List<LyricLine> = emptyList()
)

data class UserPitchPoint(
    val time: Double,
    val midi: Float,
    val color: Long
)
