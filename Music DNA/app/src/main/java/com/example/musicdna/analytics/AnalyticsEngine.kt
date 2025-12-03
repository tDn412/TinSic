package com.example.musicdna.analytics

import com.example.musicdna.model.ListeningHistory
import com.example.musicdna.model.Music
import com.example.musicdna.model.MusicDnaProfile
import com.example.musicdna.model.MusicGenre // <-- ĐÃ SỬA LỖI

// object AnalyticsEngine: Singleton object để chứa logic phân tích
object AnalyticsEngine { // <-- ĐÃ SỬA LỖI

    fun calculateDnaProfile(
        history: List<ListeningHistory>,
        musicList: List<Music>
    ): MusicDnaProfile {

        val musicMap = musicList.associateBy { it.musicId }

        // Lọc ra các bài hát yêu thích để phân tích
        val favoriteMusic = history
            .filter { it.isFavourite }
            .mapNotNull { musicMap[it.musicId] }

        // a. Tính Top 5 Nghệ sĩ
        val topArtists = favoriteMusic
            .groupingBy { it.artist }
            .eachCount() // Đếm số bài hát của mỗi nghệ sĩ
            .toList()
            .sortedByDescending { it.second } // Sắp xếp giảm dần theo số lượng
            .take(5) // Lấy 5 người đứng đầu

        // b. Tính Top 5 Quốc gia
        val topCountries = favoriteMusic
            .groupingBy { it.country }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // Tính toán phân bổ thể loại (giữ nguyên logic cũ)
        val genreCounts = favoriteMusic
            .groupingBy { it.genre }
            .eachCount()
        val maxCount = genreCounts.maxOfOrNull { it.value }?.toFloat() ?: 1f
        val genreDistribution = genreCounts.mapValues { (_, count) ->
            (count / maxCount) * 100f
        }

        // Trả về đối tượng tổng hợp đã được rút gọn
        return MusicDnaProfile(
            genreDistribution = genreDistribution,
            topArtists = topArtists,
            topCountries = topCountries
        )
    }
}