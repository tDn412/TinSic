package com.tinsic.app.game.data

/**
 * Data Transfer Object for Question from Firestore
 * Maps directly to Firestore document structure
 */
data class QuestionDto(
    val id: String = "",
    val type: String = "",
    val content: String = "",
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val musicUrl: String? = null,
    val songTitle: String? = null,
    val lyrics: String? = null,
    @get:com.google.firebase.firestore.PropertyName("isActive") val isActive: Boolean = true
)
