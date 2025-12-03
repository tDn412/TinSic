package com.example.musicdna.model

data class MusicDnaProfile(
    val genreDistribution: Map<MusicGenre, Float>,
    val topArtists: List<Pair<String, Int>>,
    val topCountries: List<Pair<String, Int>>
)