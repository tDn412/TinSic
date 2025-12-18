package com.tinsic.app.data.model.profile

import com.tinsic.app.R

// =================================================================================
// DỮ LIỆU GIẢ LẬP CHO BÀI HÁT
// =================================================================================

val dummyMusicList: List<Music> = listOf(
    // --- J97 (VIỆT NAM) ---
    Music("J97_001", "Sóng Gió", "J97", "POP", "", "", "", "VN", 250),
    Music("J97_002", "Bạc Phận", "J97", "POP", "", "", "", "VN", 245),
    Music("J97_003", "Hồng Nhan", "J97", "POP", "", "", "", "VN", 230),
    Music("J97_004", "Em Gì Ơi", "J97", "POP", "", "", "", "VN", 220),
    Music("J97_005", "Là 1 Thằng Con Trai", "J97", "POP", "", "", "", "VN", 260),
    Music("J97_006", "Hoa Vô Sắc", "J97", "POP", "", "", "", "VN", 275),
    Music("J97_007", "Thiên Hạ Hữu Tình Nhân", "J97", "POP", "", "", "", "VN", 210),
    Music("J97_008", "Đom Đóm", "J97", "POP", "", "", "", "VN", 300),
    Music("J97_009", "Cuộc Vui Cô Đơn", "J97", "POP", "", "", "", "VN", 215),
    Music("J97_010", "Ngủ Một Mình", "J97", "RNB", "", "", "", "VN", 205),

    // --- NHẠC VIỆT NAM KHÁC ---
    Music("VN_001", "See Tình", "Hoàng Thùy Linh", "POP", "", "", "", "VN", 185),
    Music("VN_002", "Bên Trên Tầng Lầu", "Tăng Duy Tân", "EDM", "", "", "", "VN", 210),
    Music("VN_003", "Nấu Ăn Cho Em", "Đen Vâu", "HIP HOP", "", "", "", "VN", 240),
    Music("VN_004", "Ngày Đầu Tiên", "Đức Phúc", "POP", "", "", "", "VN", 220),
    Music("VN_005", "Chạy Về Khóc Với Anh", "ERIK", "POP", "", "", "", "VN", 235),
    Music("VN_006", "Lối Nhỏ", "Đen Vâu", "HIP HOP", "", "", "", "VN", 250),
    Music("VN_007", "Cắt Đôi Nỗi Sầu", "Tăng Duy Tân", "EDM", "", "", "", "VN", 200),


    // --- NHẠC US-UK ---
    Music("USUK_001", "Blinding Lights", "The Weeknd", "POP", "", "", "", "US_UK", 202),
    Music("USUK_002", "As It Was", "Harry Styles", "POP", "", "", "", "US_UK", 167),
    Music("USUK_003", "Shape of You", "Ed Sheeran", "POP", "", "", "", "US_UK", 233),
    Music("USUK_004", "Bohemian Rhapsody", "Queen", "ROCK", "", "", "", "US_UK", 355),
    Music("USUK_005", "Rolling in the Deep", "Adele", "POP", "", "", "", "US_UK", 228),
    Music("USUK_006", "Smells Like Teen Spirit", "Nirvana", "ROCK", "", "", "", "US_UK", 301),
    Music("USUK_007", "God's Plan", "Drake", "HIP HOP", "", "", "", "US_UK", 198),
    Music("USUK_008", "Stairway to Heaven", "Led Zeppelin", "ROCK", "", "", "", "US_UK", 482),


    // --- NHẠC NHẬT BẢN (J-POP) ---
    Music("JP_001", "Idol", "YOASOBI", "POP", "", "", "", "JP", 213),
    Music("JP_002", "Lemon", "Kenshi Yonezu", "POP", "", "", "", "JP", 255),
    Music("JP_003", "Gurenge", "LiSA", "ROCK", "", "", "", "JP", 237),

    // --- NHẠC HÀN QUỐC (K-POP) ---
    Music("KR_001", "Dynamite", "BTS", "POP", "", "", "", "KR", 199),
    Music("KR_002", "Gangnam Style", "PSY", "POP", "", "", "", "KR", 219),
    Music("KR_003", "DDU-DU DDU-DU", "BLACKPINK", "HIP HOP", "", "", "", "KR", 209),
    Music("KR_004", "Super Shy", "NewJeans", "POP", "", "", "", "KR", 155),


    // --- CLASSICAL & OTHER ---
    Music("CL_001", "Für Elise", "Beethoven", "CLASSICAL", "", "", "", "OTHER", 150),
    Music("CL_002", "Canon in D", "Pachelbel", "CLASSICAL", "", "", "", "OTHER", 305),
    Music("JAZZ_001", "Take Five", "Dave Brubeck Quartet", "JAZZ", "", "", "", "OTHER", 324)
)

// =================================================================================
// DỮ LIỆU GIẢ LẬP CHO LỊCH SỬ NGHE (ĐÃ ĐỒNG BỘ VỚI DUMMYMUSICLIST)
// =================================================================================
val dummyListeningHistory: List<HistoryItem> = listOf(
    // === SỞ THÍCH CỐT LÕI: "POP" ===
    HistoryItem("h001", "J97_001", System.currentTimeMillis()),   // Sóng Gió
    HistoryItem("h002", "J97_002", System.currentTimeMillis()),   // Bạc Phận
    HistoryItem("h003", "J97_008", System.currentTimeMillis()),   // Đom Đóm
    HistoryItem("h004", "VN_001", System.currentTimeMillis()),    // See Tình
    HistoryItem("h005", "USUK_001", System.currentTimeMillis()),  // Blinding Lights

    // === GU ÂM NHẠC PHỤ: ROCK ===
    HistoryItem("h006", "USUK_004", System.currentTimeMillis()), // Bohemian Rhapsody
    HistoryItem("h007", "USUK_006", System.currentTimeMillis()), // Smells Like Teen Spirit
    HistoryItem("h008", "USUK_008", System.currentTimeMillis()), // Stairway to Heaven
    HistoryItem("h009", "JP_003", System.currentTimeMillis()),   // Gurenge

    // === GU ÂM NHẠC PHỤ: HIP-HOP & EDM ===
    HistoryItem("h010", "VN_003", System.currentTimeMillis()),   // Nấu Ăn Cho Em
    HistoryItem("h011", "VN_006", System.currentTimeMillis()),   // Lối Nhỏ
    HistoryItem("h012", "USUK_007", System.currentTimeMillis()), // God's Plan
    HistoryItem("h013", "VN_002", System.currentTimeMillis()),   // Bên Trên Tầng Lầu
    HistoryItem("h014", "VN_007", System.currentTimeMillis()),   // Cắt Đôi Nỗi Sầu

    // === THÍNH THOẢNG NGHE: JAZZ & CLASSICAL ===
    HistoryItem("h015", "CL_002", System.currentTimeMillis()),   // Canon in D
    HistoryItem("h016", "JAZZ_001", System.currentTimeMillis()), // Take Five

    // === CÁC BÀI ĐÃ NGHE KHÁC ===
    HistoryItem("h017", "JP_001", System.currentTimeMillis()),   // Idol - YOASOBI
    HistoryItem("h018", "KR_001", System.currentTimeMillis()),   // Dynamite - BTS
    HistoryItem("h019", "KR_004", System.currentTimeMillis()),   // Super Shy - NewJeans
    HistoryItem("h020", "CL_001", System.currentTimeMillis())    // Für Elise
)

// =================================================================================
// DỮ LIỆU THÀNH TÍCH - ĐÃ CHUYỂN SANG FIREBASE
// =================================================================================
// Achievement definitions are now stored in Firestore collection "achievements"
// User progress is now stored in User document field "achievements"
// Use AchievementRepository to access this data

/*
// DEPRECATED: Old hardcoded achievements (kept for reference)
val allAchievements: List<Achievement> = listOf(...)
val dummyUserProgress: List<UserAchievementProgress> = listOf(...)
*/

