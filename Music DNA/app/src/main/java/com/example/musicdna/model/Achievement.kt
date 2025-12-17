package com.example.musicdna.model

import androidx.annotation.StringRes

/**
 * Enum định nghĩa các loại điều kiện để đạt được thành tích.
 * Giúp code an toàn và dễ quản lý hơn.
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
 * Data class Achievement được thiết kế lại để an toàn và linh hoạt hơn.
 */
data class Achievement(
    // --- Thông tin định danh & Hiển thị ---
    val id: String,                         // ID độc nhất, VD: "pop_fan_1"

    // Sử dụng String Resource để hỗ trợ đa ngôn ngữ
    @StringRes val titleRes: Int,           // VD: R.string.achievement_pop_fan_1_title
    @StringRes val descriptionRes: Int,     // VD: R.string.achievement_pop_fan_1_description

    val iconUrl: String,                    // URL của ảnh huy hiệu

    // --- Logic & Điều kiện ---
    val type: AchievementConditionType,     // Loại điều kiện để đạt được (an toàn hơn với Enum)
    val targetCount: Int,                   // Số lượng mục tiêu cần đạt (VD: 100 bài)
    val criteriaValue: String? = null,      // Giá trị của điều kiện (VD: "Pop", "Taylor Swift", "VN")

    // --- Phần thưởng & Cấp độ ---
    val experienceReward: Int,              // Điểm kinh nghiệm nhận được
    val nextTierId: String? = null          // ID của thành tích cấp cao hơn (VD: "pop_fan_2")
)

/**
 * Data class thể hiện trạng thái của người dùng đối với một thành tích.
 * Tách biệt giữa 'Định nghĩa' và 'Tiến trình'.
 */
data class UserAchievementProgress(
    val achievementId: String,              // Liên kết với Achievement.id
    val currentProgress: Int = 0,           // Tiến trình hiện tại của người dùng
    val isUnlocked: Boolean = false,        // Thành tích đã được mở khóa chưa?
    val unlockedAt: Long? = null            // Thời gian mở khóa (timestamp)
)
