package com.tinsic.app.domain.karaoke.model

data class SongNote(
    val midi: Int,
    val name: String,
    val startSec: Double,
    val durationSec: Double
)
