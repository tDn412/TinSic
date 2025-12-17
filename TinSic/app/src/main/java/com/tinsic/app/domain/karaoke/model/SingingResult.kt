package com.tinsic.app.domain.karaoke.model

data class SingingResult(
    val midiUser: Float,
    val targetMidi: Int,
    val noteName: String,
    val isHit: Boolean,
    val scoreAdded: Int,
    val feedbackText: String,
    val feedbackColor: Long,
    val currentTime: Double
)
