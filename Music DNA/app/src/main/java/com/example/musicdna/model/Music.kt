package com.example.musicdna.model

data class Music(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val genre: String = "",
    val audioUrl: String = "",
    val coverUrl: String = "",
    val lyricUrl: String = "",
    val country: String = "",
    val duration: Long = 0L
)
