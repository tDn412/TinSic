package com.tinsic.app.data.model

data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val genre: String = "",
    val audioUrl: String = "",
    val coverUrl: String = "",
    val lyricUrl: String = "",
    val duration: Long = 0L, // Duration in milliseconds
    val country: String = "Unknown"
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "", "", "", "", "", 0L, "Unknown")
}
