package com.tinsic.app.game.data

import com.tinsic.app.game.model.GameType
import com.tinsic.app.game.model.Question
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing game questions
 * Loads data from Firebase Firestore
 */
class GameRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val questionsCollection = firestore.collection("minigames")
    
    /**
     * Fetch all active questions from Firestore
     * @return List of Question objects
     * @throws Exception if network error or parsing fails
     */
    suspend fun getQuestions(): List<Question> {
        return try {
            val snapshot = questionsCollection
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(QuestionDto::class.java)?.toQuestion()
                } catch (e: Exception) {
                    // Log error but continue with other questions
                    e.printStackTrace()
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Fetch questions filtered by game type
     * @param type GameType to filter by
     * @return List of Question objects matching the type
     */
    suspend fun getQuestionsByType(type: GameType): List<Question> {
        return try {
            val snapshot = questionsCollection
                .whereEqualTo("type", type.name)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(QuestionDto::class.java)?.toQuestion()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}

/**
 * Extension function to convert DTO to domain model
 */
private fun QuestionDto.toQuestion(): Question {
    return Question(
        id = id.toIntOrNull() ?: id.hashCode(), // Fallback to hashCode if not numeric
        type = try {
            GameType.valueOf(type)
        } catch (e: Exception) {
            GameType.GUESS_THE_SONG // Default fallback
        },
        content = content,
        options = options,
        correctAnswerIndex = correctAnswerIndex,
        musicUrl = musicUrl,
        songTitle = songTitle,
        lyrics = lyrics
    )
}