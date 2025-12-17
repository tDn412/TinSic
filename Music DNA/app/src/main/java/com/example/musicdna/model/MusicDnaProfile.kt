package com.example.musicdna.model

data class MusicDnaProfile(
    val genreDistribution: Map<String, Float>,
    val topArtists: List<Pair<String, Int>>,
    val topCountries: List<Pair<String, Int>>
)