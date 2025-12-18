package com.tinsic.app.domain.karaoke.model

data class SongNote(
    val midi: Int,
    val name: String,
    val startSec: Double,
    val durationSec: Double,
    val singerId: Int = 1 // 1=Singer1, 2=Singer2, 3=Both
)
