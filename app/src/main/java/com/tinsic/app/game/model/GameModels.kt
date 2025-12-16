package com.tinsic.app.game.model

// 3 Loại game
enum class GameType(val title: String, val description: String) {
    GUESS_THE_SONG("We The Best Music", "Nghe đoạn nhạc, đoán tên bài hát hoặc ca sĩ."),
    LYRICS_FLIP("Lyrics Flip", "Xem lời dịch tiếng Việt, đoán tên bài gốc tiếng Anh."),
    FINISH_THE_LYRICS("Finish The Lyrics", "Nghe nhạc và điền từ còn thiếu vào chỗ trống."),
    MUSIC_CODE("The Music Code", "Nhìn emoji đoán tên bài hát.")
}

// Các màn hình trong luồng game
enum class GameScreenState {
    MENU,           // Chọn game
    COUNTDOWN,      // Đếm ngược 5 giây trước khi bắt đầu
    PLAYING,        // Đang chơi
    MUSIC_PREVIEW,  // Phát nhạc preview sau khi trả lời (dành cho LYRICS_FLIP)
    QUESTION_RESULT,// Hiển thị bảng xếp hạng sau mỗi câu hỏi
    RESULT          // Kết thúc - bảng xếp hạng cuối cùng
}

// Cấu trúc câu hỏi
data class Question(
    val id: String,
    val type: GameType,
    val content: String, // URL nhạc (cho GUESS_THE_SONG, FINISH_THE_LYRICS) hoặc Lời bài hát (lyrics text cho LYRICS_FLIP)
    val options: List<String>,
    val correctAnswerIndex: Int,
    val durationSeconds: Int = 10,
    val musicUrl: String? = null,  // URL nhạc riêng cho LYRICS_FLIP (phát sau khi trả lời)
    val lyrics: String? = null,    // Lời bài hát cho FINISH_THE_LYRICS (hiển thị với từ cuối bị che)
    val songTitle: String? = null  // Tên bài hát thật để hiển thị trong MusicPreviewScreen
)

// Thông tin người chơi cho bảng xếp hạng (mock AI players)
data class PlayerScore(
    val playerId: String,
    val playerName: String,
    val score: Int,
    val answeredCorrectly: Boolean = false,
    val isCurrentPlayer: Boolean = false
)