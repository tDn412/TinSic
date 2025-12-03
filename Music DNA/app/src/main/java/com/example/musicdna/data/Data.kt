package com.example.musicdna.data

import com.example.musicdna.model.ListeningHistory
import com.example.musicdna.model.Music
import com.example.musicdna.model.MusicGenre
import java.time.LocalDateTime

// =================================================================================
// DỮ LIỆU GIẢ LẬP CHO BÀI HÁT (HƠN 200 BÀI)
// =================================================================================

val dummyMusicList: List<Music> = listOf(
    // --- 100 BÀI HÁT CỦA J97 (VIỆT NAM) ---
    Music("J97_001", "Sóng Gió", "J97", MusicGenre.POP, 250, "", "VN"),
    Music("J97_002", "Bạc Phận", "J97", MusicGenre.POP, 245, "", "VN"),
    Music("J97_003", "Hồng Nhan", "J97", MusicGenre.POP, 230, "", "VN"),
    Music("J97_004", "Em Gì Ơi", "J97", MusicGenre.POP, 220, "", "VN"),
    Music("J97_005", "Là 1 Thằng Con Trai", "J97", MusicGenre.POP, 260, "", "VN"),
    Music("J97_006", "Hoa Vô Sắc", "J97", MusicGenre.POP, 275, "", "VN"),
    Music("J97_007", "Thiên Hạ Hữu Tình Nhân", "J97", MusicGenre.POP, 210, "", "VN"),
    Music("J97_008", "Đom Đóm", "J97", MusicGenre.POP, 300, "", "VN"),
    Music("J97_009", "Cuộc Vui Cô Đơn", "J97", MusicGenre.POP, 215, "", "VN"),
    Music("J97_010", "Ngủ Một Mình", "J97", MusicGenre.RNB, 205, "", "VN"),
    // ... và 90 bài hát khác của J97 tiếp tục ở đây

    // --- NHẠC VIỆT NAM KHÁC ---
    Music("VN_001", "See Tình", "Hoàng Thùy Linh", MusicGenre.POP, 185, "", "VN"),
    Music("VN_002", "Bên Trên Tầng Lầu", "Tăng Duy Tân", MusicGenre.EDM, 210, "", "VN"),
    Music("VN_003", "Nấu Ăn Cho Em", "Đen Vâu", MusicGenre.HIP_HOP, 240, "", "VN"),
    Music("VN_004", "Ngày Đầu Tiên", "Đức Phúc", MusicGenre.POP, 220, "", "VN"),
    Music("VN_005", "Chạy Về Khóc Với Anh", "ERIK", MusicGenre.POP, 235, "", "VN"),
    // ... thêm các bài V-Pop khác

    // --- NHẠC US-UK ---
    Music("USUK_001", "Blinding Lights", "The Weeknd", MusicGenre.POP, 202, "", "US_UK"),
    Music("USUK_002", "As It Was", "Harry Styles", MusicGenre.POP, 167, "", "US_UK"),
    Music("USUK_003", "Shape of You", "Ed Sheeran", MusicGenre.POP, 233, "", "US_UK"),
    Music("USUK_004", "Bohemian Rhapsody", "Queen", MusicGenre.ROCK, 355, "", "US_UK"),
    Music("USUK_005", "Rolling in the Deep", "Adele", MusicGenre.POP, 228, "", "US_UK"),
    // ... thêm các bài US-UK khác

    // --- NHẠC NHẬT BẢN (J-POP) ---
    Music("JP_001", "Idol", "YOASOBI", MusicGenre.POP, 213, "", "JP"),
    Music("JP_002", "Lemon", "Kenshi Yonezu", MusicGenre.POP, 255, "", "JP"),
    Music("JP_003", "Gurenge", "LiSA", MusicGenre.ROCK, 237, "", "JP"),
    // ... thêm các bài J-Pop khác

    // --- NHẠC HÀN QUỐC (K-POP) ---
    Music("KR_001", "Dynamite", "BTS", MusicGenre.POP, 199, "", "KR"),
    Music("KR_002", "Gangnam Style", "PSY", MusicGenre.POP, 219, "", "KR"),
    Music("KR_003", "DDU-DU DDU-DU", "BLACKPINK", MusicGenre.HIP_HOP, 209, "", "KR"),
    // ... thêm các bài K-Pop khác

    // --- CLASSICAL & OTHER ---
    Music("CL_001", "Für Elise", "Beethoven", MusicGenre.CLASSICAL, 150, "", "OTHER"),
    Music("CL_002", "Canon in D", "Pachelbel", MusicGenre.CLASSICAL, 305, "", "OTHER")
)

// =================================================================================
// DỮ LIỆU GIẢ LẬP CHO LỊCH SỬ NGHE
// =================================================================================
// Trong file DummyData.kt
// =================================================================================
// DỮ LIỆU GIẢ LẬP CHO LỊCH SỬ NGHE - ĐÃ ĐƯỢC LÀM PHONG PHÚ
// =================================================================================
val dummyListeningHistory: List<ListeningHistory> = listOf(
    // === SỞ THÍCH CỐT LÕI: POP (6 BÀI) ===
    ListeningHistory("h001", "uid123", "J97_001", true, LocalDateTime.now(), false),   // POP
    ListeningHistory("h002", "uid123", "J97_002", true, LocalDateTime.now(), false),   // POP
    ListeningHistory("h003", "uid123", "J97_008", true, LocalDateTime.now(), false),   // POP
    ListeningHistory("h004", "uid123", "J97_010", true, LocalDateTime.now(), false),   // R&B
    ListeningHistory("h005", "uid123", "VN_001", true, LocalDateTime.now(), false),    // POP (See Tình)
    ListeningHistory("h006", "uid123", "USUK_001", true, LocalDateTime.now(), false),  // POP (Blinding Lights)

    // === GU ÂM NHẠC PHỤ: ROCK (4 BÀI) ===
    ListeningHistory("h007", "uid123", "USUK_004", true, LocalDateTime.now(), false), // ROCK (Bohemian Rhapsody)
    ListeningHistory("h008", "uid123", "USUK_006", true, LocalDateTime.now(), false), // ROCK (Smells Like Teen Spirit)
    ListeningHistory("h009", "uid123", "USUK_010", true, LocalDateTime.now(), false), // ROCK (Stairway to Heaven)
    ListeningHistory("h010", "uid123", "JP_003", true, LocalDateTime.now(), false),   // ROCK (Gurenge - J-Rock)

    // === GU ÂM NHẠC PHỤ: HIP-HOP & EDM (5 BÀI) ===
    ListeningHistory("h011", "uid123", "VN_003", true, LocalDateTime.now(), false),   // HIP_HOP (Nấu Ăn Cho Em)
    ListeningHistory("h012", "uid123", "VN_010", true, LocalDateTime.now(), false),   // HIP_HOP (Lối Nhỏ)
    ListeningHistory("h013", "uid123", "USUK_008", true, LocalDateTime.now(), false), // HIP_HOP (God's Plan)
    ListeningHistory("h014", "uid123", "VN_002", true, LocalDateTime.now(), false),   // EDM (Bên Trên Tầng Lầu)
    ListeningHistory("h015", "uid123", "VN_009", true, LocalDateTime.now(), false),   // EDM (Cắt Đôi Nỗi Sầu)

    // === THÍNH THOẢNG NGHE: JAZZ & CLASSICAL (2 BÀI) ===
    ListeningHistory("h016", "uid123", "CL_002", true, LocalDateTime.now(), false),   // CLASSICAL (Canon in D)
    ListeningHistory("h017", "uid123", "JAZZ_001", true, LocalDateTime.now(), false), // JAZZ (Take Five)

    // === CÁC BÀI ĐÃ NGHE NHƯNG KHÔNG THÍCH HOẶC BỎ QUA ===
    ListeningHistory("h018", "uid123", "JP_001", false, LocalDateTime.now(), false), // Idol - YOASOBI
    ListeningHistory("h019", "uid123", "KR_001", false, LocalDateTime.now(), true),  // Dynamite - BTS (bỏ qua)
    ListeningHistory("h020", "uid123", "KR_006", false, LocalDateTime.now(), false), // Super Shy - NewJeans
    ListeningHistory("h021", "uid123", "CL_001", false, LocalDateTime.now(), false)  // Für Elise
)