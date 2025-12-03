package com.example.musicdna.model

data class Music(
    val musicId: String,
    val title: String,
    val artist: String,
    val genre: MusicGenre,
    val duration: Int,
    val coverArtUrl: String,
    val country: String
)
