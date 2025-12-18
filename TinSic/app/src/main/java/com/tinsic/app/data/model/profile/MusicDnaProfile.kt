package com.tinsic.app.data.model.profile

data class MusicDnaProfile(
    val genreDistribution: Map<String, Float>,
    val topArtists: List<Pair<String, Int>>,
    val topCountries: List<Pair<String, Int>>
)