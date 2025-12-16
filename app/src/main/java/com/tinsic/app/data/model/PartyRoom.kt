package com.tinsic.app.data.model

data class PartyRoom(
    val roomId: String = "",
    val hostId: String = "",
    val type: String = "KARAOKE", // KARAOKE or GAME
    val currentSongId: String = "",
    val isPlaying: Boolean = false,
    val timestamp: Long = 0L,
    val members: Map<String, UserMember> = emptyMap(),
    val stage: Map<String, UserMember> = emptyMap() // Users currently on stage
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "KARAOKE", "", false, 0L, emptyMap(), emptyMap())
}

data class UserMember(
    val uid: String = "",
    val displayName: String = "",
    val avatar: String = "👤",
    val score: Int = 0,
    val color: Long = 0xFF000000, // ARGB Long
    val joinedAt: Long = 0L
) {
    constructor() : this("", "", "👤", 0, 0xFF000000, 0L)
}
