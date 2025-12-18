package com.tinsic.app.data.model.profile

import androidx.annotation.StringRes

/**
 * Data class này gộp thông tin từ 'Achievement' (định nghĩa)
 * và 'UserAchievementProgress' (tiến trình) để dễ dàng hiển thị trên UI.
 */
data class DisplayAchievement(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val iconUrl: String,
    val currentProgress: Int,
    val targetCount: Int,
    val isUnlocked: Boolean,
    val nextTierId: String?
)