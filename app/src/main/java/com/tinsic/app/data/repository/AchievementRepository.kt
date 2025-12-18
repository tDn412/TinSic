package com.tinsic.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tinsic.app.data.model.profile.Achievement
import com.tinsic.app.data.model.profile.UserAchievementProgress
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Stream all achievement definitions from Firestore.
     * Real-time updates when achievements are added/modified.
     */
    fun getAllAchievements(): Flow<List<Achievement>> = callbackFlow {
        val listener = firestore.collection("achievements")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val achievements = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        // Get string resource keys from Firebase
                        val titleResName = doc.getString("titleRes") ?: return@mapNotNull null
                        val descResName = doc.getString("descriptionRes") ?: return@mapNotNull null
                        
                        // Convert string keys to Int resource IDs
                        val context = firestore.app.applicationContext
                        val titleResId = context.resources.getIdentifier(
                            titleResName, "string", context.packageName
                        )
                        val descResId = context.resources.getIdentifier(
                            descResName, "string", context.packageName
                        )
                        
                        // Skip if resource not found
                        if (titleResId == 0 || descResId == 0) {
                            return@mapNotNull null
                        }
                        
                        Achievement(
                            id = doc.id,
                            titleRes = titleResId,
                            descriptionRes = descResId,
                            iconUrl = doc.getString("iconUrl") ?: "",
                            type = com.tinsic.app.data.model.profile.AchievementConditionType.valueOf(
                                doc.getString("type") ?: "CUSTOM"
                            ),
                            targetCount = doc.getLong("targetCount")?.toInt() ?: 0,
                            criteriaValue = doc.getString("criteriaValue"),
                            experienceReward = doc.getLong("experienceReward")?.toInt() ?: 0,
                            nextTierId = doc.getString("nextTierId")
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(achievements)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get user's achievement progress from User document.
     * Supports both old (Boolean) and new (Object) schema.
     */
    fun getUserProgress(userId: String): Flow<Map<String, UserAchievementProgress>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyMap())
                    return@addSnapshotListener
                }

                val achievementsField = snapshot?.get("achievements") as? Map<*, *>
                val progressMap = mutableMapOf<String, UserAchievementProgress>()

                achievementsField?.forEach { (key, value) ->
                    val achievementId = key as? String ?: return@forEach

                    when (value) {
                        // Old schema: Boolean
                        is Boolean -> {
                            progressMap[achievementId] = UserAchievementProgress(
                                achievementId = achievementId,
                                currentProgress = if (value) 1 else 0,
                                isUnlocked = value,
                                unlockedAt = null
                            )
                        }
                        // New schema: Map with progress
                        is Map<*, *> -> {
                            progressMap[achievementId] = UserAchievementProgress(
                                achievementId = achievementId,
                                currentProgress = (value["currentProgress"] as? Long)?.toInt() ?: 0,
                                isUnlocked = value["isUnlocked"] as? Boolean ?: false,
                                unlockedAt = value["unlockedAt"] as? Long
                            )
                        }
                    }
                }

                trySend(progressMap)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Update achievement progress for a user.
     * Uses atomic field update to prevent race conditions.
     */
    suspend fun updateProgress(
        userId: String,
        achievementId: String,
        newProgress: Int,
        isUnlocked: Boolean
    ): Result<Unit> {
        return try {
            val progressData = mapOf(
                "achievements.$achievementId" to mapOf(
                    "currentProgress" to newProgress,
                    "isUnlocked" to isUnlocked,
                    "unlockedAt" to if (isUnlocked) System.currentTimeMillis() else null
                )
            )

            firestore.collection("users").document(userId)
                .update(progressData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Batch update multiple achievement progress at once.
     * More efficient for tracking multiple achievements simultaneously.
     */
    suspend fun batchUpdateProgress(
        userId: String,
        updates: Map<String, Pair<Int, Boolean>> // achievementId -> (progress, isUnlocked)
    ): Result<Unit> {
        return try {
            val updateMap = mutableMapOf<String, Any>()

            updates.forEach { (achievementId, progressPair) ->
                val (progress, isUnlocked) = progressPair
                updateMap["achievements.$achievementId"] = mapOf(
                    "currentProgress" to progress,
                    "isUnlocked" to isUnlocked,
                    "unlockedAt" to if (isUnlocked) System.currentTimeMillis() else null
                )
            }

            firestore.collection("users").document(userId)
                .update(updateMap)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    /**
     * Check if playing a song triggers any achievements.
     * Handles logic for GENRE, ARTIST, COUNTRY, and TOTAL_SONGS.
     */
    suspend fun checkAndUnlockAchievements(
        userId: String,
        song: com.tinsic.app.data.model.Song,
        allAchievements: List<Achievement>,
        currentProgress: Map<String, UserAchievementProgress>
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Pair<Int, Boolean>>()
            var hasUpdates = false

            for (achievement in allAchievements) {
                // Skip if already completed (optional: unless we want to track overflow or levels)
                // For now, let's allow updating progress even if unlocked, but won't re-unlock
                
                val progressData = currentProgress[achievement.id]
                var currentCount = progressData?.currentProgress ?: 0
                val isUnlocked = progressData?.isUnlocked == true
                
                // If already unlocked and we don't care about further progress, skip
                // But maybe we want to show 100/50? Let's check logic.
                // For "Streak" or "Total", we might keep counting. 
                // Let's assume we want to update progress until it hits target, or maybe beyond.
                // Simple version: Update if not unlocked OR if we want to track accurate counts.
                
                var match = false
                
                when (achievement.type) {
                    com.tinsic.app.data.model.profile.AchievementConditionType.LISTEN_GENRE -> {
                        // Normalize strings: remove spaces, lowercase, distinct separators
                        val songGenre = song.genre.trim().lowercase()
                        val targetGenre = achievement.criteriaValue?.trim()?.lowercase() ?: ""
                        if (songGenre.contains(targetGenre)) {
                             match = true
                        }
                    }
                    com.tinsic.app.data.model.profile.AchievementConditionType.LISTEN_COUNTRY -> {
                        // Handle US_UK / US-UK mismatch
                        val songCountry = normalizeString(song.country)
                        val targetCountry = normalizeString(achievement.criteriaValue)
                        if (songCountry == targetCountry) {
                             match = true
                        }
                    }
                    com.tinsic.app.data.model.profile.AchievementConditionType.LISTEN_ARTIST -> {
                        if (song.artist.equals(achievement.criteriaValue, ignoreCase = true)) {
                            match = true
                        }
                    }
                    com.tinsic.app.data.model.profile.AchievementConditionType.TOTAL_SONGS -> {
                        match = true // Every song counts
                    }
                    else -> {
                        // Other types handled elsewhere (e.g. LIKED_SONGS)
                    }
                }
                
                if (match) {
                    currentCount++
                    val newIsUnlocked = currentCount >= achievement.targetCount
                    
                    // Only update if changed
                    if (currentCount != (progressData?.currentProgress ?: 0) || (newIsUnlocked && !isUnlocked)) {
                         updates[achievement.id] = Pair(currentCount, newIsUnlocked || isUnlocked)
                         hasUpdates = true
                    }
                }
            }

            if (hasUpdates) {
                batchUpdateProgress(userId, updates)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeString(input: String?): String {
        return input?.replace("-", "_")?.replace(" ", "")?.lowercase() ?: ""
    }
}
