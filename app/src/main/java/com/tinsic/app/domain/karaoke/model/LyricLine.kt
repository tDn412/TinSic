package com.tinsic.app.domain.karaoke.model

data class LyricLine(
    val startTime: Double,
    val content: String,
    val singerId: Int = 1, // 1 or 2
    val isDuet: Boolean = false // Only true if singerId == 3 (Both)
)
