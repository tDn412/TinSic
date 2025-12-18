package com.tinsic.app.data.model.profile

data class User(
    val uid: String = "",
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val photoUrl: String ?= null,
    val favoriteGenres: List<String> = emptyList(),
    val hasCompletedOnboarding: Boolean = false,
    val likedSongs: List<String> = emptyList(), // List of Song IDs
    val dislikedSongs: List<String> = emptyList(), // List of Song IDs
    val achievements: Map<Achievement, UserAchievementProgress> = emptyMap() // e.g., "dom_con": true
)
