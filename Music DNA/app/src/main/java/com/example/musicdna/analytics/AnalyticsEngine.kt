package com.example.musicdna.analytics

import com.example.musicdna.model.HistoryItem
import com.example.musicdna.model.Music
import com.example.musicdna.model.MusicDnaProfile

object AnalyticsEngine {

    fun calculateDnaProfile(
        history: List<HistoryItem>,
        musicList: List<Music>
    ): MusicDnaProfile {

        // Tạo Map để tra cứu nhanh bài hát theo ID (Music.id)
        val musicMap = musicList.associateBy { it.id }

        // Lấy danh sách các đối tượng Music tương ứng với lịch sử nghe
        // Lưu ý: Vì HistoryItem mới không có 'isFavourite', ta tính toán dựa trên toàn bộ lịch sử nghe.
        val listenedMusic = history
            .mapNotNull { historyItem ->
                musicMap[historyItem.songId]
            }

        if (listenedMusic.isEmpty()) {
            return MusicDnaProfile(emptyMap(), emptyList(), emptyList())
        }

        // a. Tính Top 5 Nghệ sĩ
        val topArtists = listenedMusic
            .groupingBy { it.artist }
            .eachCount() // Đếm số lần xuất hiện của nghệ sĩ trong lịch sử
            .toList()
            .sortedByDescending { it.second } // Sắp xếp giảm dần
            .take(5) // Lấy 5 người đầu tiên

        // b. Tính Top 5 Quốc gia (Thị trường âm nhạc)
        val topCountries = listenedMusic
            .groupingBy { it.country }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // c. Tính toán phân bổ thể loại (Dựa trên String)
        val genreCounts = listenedMusic
            .groupingBy { it.genre } // it.genre bây giờ là String (ví dụ: "POP")
            .eachCount()

        // Tìm giá trị lớn nhất để chuẩn hóa về thang 100 (cho biểu đồ Radar)
        val maxCount = genreCounts.maxOfOrNull { it.value }?.toFloat() ?: 1f

        val genreDistribution = genreCounts.mapValues { (_, count) ->
            (count / maxCount) * 100f
        }

        // Trả về kết quả
        return MusicDnaProfile(
            genreDistribution = genreDistribution,
            topArtists = topArtists,
            topCountries = topCountries
        )
    }
}