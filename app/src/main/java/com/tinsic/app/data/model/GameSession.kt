package com.tinsic.app.data.model

import com.tinsic.app.game.model.GameType

/**
 * Represents a synchronized multiplayer game session
 * Stored in Firebase: parties/{roomId}/gameSession
 */
data class GameSession(
    val gameType: String = "",  // GameType enum name
    @get:com.google.firebase.database.PropertyName("isActive") val isActive: Boolean = false,
    val hostId: String = "",
    val currentQuestionIndex: Int = 0,
    val timeLeft: Int = 10,
    val phase: String = "MENU",  // COUNTDOWN, PLAYING, ANSWER_REVEAL, QUESTION_RESULT
    val questionIds: List<String> = emptyList(),  // Shuffled question IDs (same order for all)
    val startedAt: Long = 0,
    val countdownStartedAt: Long = 0,  // Server timestamp for sync
    val questionStartedAt: Long = 0   // Server timestamp for timer sync
) {
    // Empty constructor for Firebase
    constructor() : this("", false, "", 0, 10, "MENU", emptyList(), 0, 0, 0)
}

/**
 * Game phases
 */
enum class GamePhase {
    MENU,
    COUNTDOWN,
    PLAYING,
    ANSWER_REVEAL,
    QUESTION_RESULT,
    GAME_OVER
}
