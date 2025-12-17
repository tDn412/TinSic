package com.tinsic.app.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val favoriteGenres: List<String> = emptyList(),
    val hasCompletedOnboarding: Boolean = false,
    val likedSongs: List<String> = emptyList(), // List of Song IDs
    val dislikedSongs: List<String> = emptyList(), // List of Song IDs
    val achievements: Map<String, Boolean> = emptyMap() // e.g., "dom_con": true
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "", "", emptyList(), false, emptyList(), emptyList(), emptyMap())
}
