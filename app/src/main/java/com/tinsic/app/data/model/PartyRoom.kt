package com.tinsic.app.data.model

data class PartyRoom(
    val roomId: String = "",
    val hostId: String = "",
    val currentSongId: String = "",
    val isPlaying: Boolean = false,
    val timestamp: Long = 0L,
    val members: Map<String, UserMember> = emptyMap()
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "", false, 0L, emptyMap())
}

data class UserMember(
    val uid: String = "",
    val displayName: String = "",
    val joinedAt: Long = 0L
) {
    constructor() : this("", "", 0L)
}
