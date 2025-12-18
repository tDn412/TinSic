package com.tinsic.app.data.model

data class PartyReaction(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val emoji: String = "", // e.g., "❤️", "🔥"
    val timestamp: Long = 0L
)
