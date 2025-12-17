package com.tinsic.app.data.model

data class LyricLine(
    val timeMs: Long,      // Timestamp in milliseconds
    val text: String,      // Lyric text for this line
    var translation: String = "" // Translated text
)
