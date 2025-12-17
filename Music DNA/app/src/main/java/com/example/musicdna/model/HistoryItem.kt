package com.example.musicdna.model

/**
 * Data class ghi lại một sự kiện nghe nhạc của người dùng.
 * Đây là dữ liệu thô để tính toán ra "Music DNA".
 *
 * @param historyId Mã định danh duy nhất cho sự kiện nghe này.
 * @param userId Mã định danh của người dùng đã nghe.
 * @param musicId Mã định danh của bài hát đã được nghe.
 * @param isFavourite Người dùng có đánh dấu bài hát này là "yêu thích" hay không.
 * @param listenedAt Thời điểm (ngày và giờ) bài hát được nghe.
 * @param isSkipped Bài hát có bị bỏ qua nhanh (ví dụ: nghe dưới 15 giây) hay không.
 */
data class HistoryItem(
    val id: String = "",
    val songId: String = "",
    val playedAt: Long = System.currentTimeMillis()
)