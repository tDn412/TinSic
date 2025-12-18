package com.tinsic.app.data.model.profile

import androidx.annotation.StringRes

/**
 * Enum định nghĩa các loại điều kiện để đạt được thành tích.
 */
enum class AchievementConditionType {
    LISTEN_GENRE,       // Nghe nhạc theo thể loại
    LISTEN_ARTIST,      // Nghe nhạc của một nghệ sĩ
    LISTEN_COUNTRY,     // Nghe nhạc từ một quốc gia
    TOTAL_SONGS,        // Nghe tổng số bài hát
    LIKED_SONGS,        // Số bài hát đã thích
    SESSION_STREAK,     // Chuỗi ngày đăng nhập/nghe nhạc
    CUSTOM              // Loại tùy chỉnh khác
}

/**
 * Achievement definition - stored in Firestore collection "achievements"
 * Note: titleRes and descriptionRes are stored as string keys in Firestore
 * (e.g., "achievement_pop_fan_1_title") and resolved to Int resource IDs in the app
 */
data class Achievement(
    val id: String = "",
    @StringRes val titleRes: Int = 0,
    @StringRes val descriptionRes: Int = 0,
    val iconUrl: String = "",
    val type: AchievementConditionType = AchievementConditionType.CUSTOM,
    val targetCount: Int = 0,
    val criteriaValue: String? = null,
    val experienceReward: Int = 0,
    val nextTierId: String? = null
) {
    // Empty constructor for Firebase
    constructor() : this("", 0, 0, "", AchievementConditionType.CUSTOM, 0, null, 0, null)
}

/**
 * User's progress for a specific achievement - stored in User document
 * Schema: users/{userId}/achievements/{achievementId}
 */
data class UserAchievementProgress(
    val achievementId: String = "",
    val currentProgress: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
