package com.tinsic.app.data.model

data class PartyStatus(
    val playbackState: String = "IDLE", // IDLE, LOADING, COUNTDOWN, PLAYING, ENDED
    val startTime: Long = 0L,           // Timestamp for countdown start
    val readyState: Map<String, Boolean> = emptyMap() // UserId -> isReady
) {
    constructor() : this("IDLE", 0L, emptyMap())
}

data class PartyRoom(
    val roomId: String = "",
    val hostId: String = "",
    val type: String = "KARAOKE", // KARAOKE or GAME
    val currentSongId: String = "",
    val isPlaying: Boolean = false,
    val timestamp: Long = 0L,
    val members: Map<String, UserMember> = emptyMap(),
    val stage: Map<String, UserMember> = emptyMap(), // Users currently on stage
    val queue: Map<String, QueueSong> = emptyMap(),   // Shared Song Queue
    val status: PartyStatus = PartyStatus()           // Sync state for karaoke playback
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "KARAOKE", "", false, 0L, emptyMap(), emptyMap(), emptyMap(), PartyStatus())
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

data class QueueSong(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val coverUrl: String = "",
    val audioUrl: String = "", // Used for playback
    val lyricUrl: String = "", // LRC or JSON lyrics
    val pitchDataUrl: String = "", // Asset for scoring (Pitch contour)
    val addedByUserId: String = "",
    val addedByUserName: String = "",
    val timestamp: Long = 0L
) {
    constructor() : this("", "", "", "", "", "", "", "", "", 0L)
}
