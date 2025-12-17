package com.tinsic.app.presentation.party

import androidx.compose.ui.graphics.Color

data class PartyUser(
    val id: String,
    val name: String,
    val avatar: String, // Emoji
    val color: Color,
    val score: Int
)

data class PartySong(
    val id: Int,
    val title: String,
    val artist: String,
    val coverUrl: String, // Renamed from albumArt
    val duration: Int = 0,
    val firebaseId: String = "" // Added for removal logic
)

enum class PartyModeState {
    LOBBY, ROOM, KARAOKE, SINGING, MINIGAME, DUAL_SCREEN
}
