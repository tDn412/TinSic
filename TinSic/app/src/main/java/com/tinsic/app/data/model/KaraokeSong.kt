package com.tinsic.app.data.model

data class KaraokeSong(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val genre: String = "",
    val audioUrl: String = "", // Beat/Instrumental URL
    val coverUrl: String = "",
    val lyricUrl: String = "", // LRC or JSON lyrics
    val pitchDataUrl: String = "", // Asset for scoring (Pitch contour)
    val duration: Long = 0L
) {
    // Empty constructor for Firebase serialization
    constructor() : this("", "", "", "", "", "", "", "", 0L)
}
