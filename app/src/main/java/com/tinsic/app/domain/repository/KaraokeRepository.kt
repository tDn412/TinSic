package com.tinsic.app.domain.repository

import com.tinsic.app.domain.karaoke.model.LyricLine
import com.tinsic.app.domain.karaoke.model.SongNote

interface KaraokeRepository {
    suspend fun getSongNotes(fileName: String): List<SongNote>
    suspend fun getLyrics(fileName: String): List<LyricLine>
}
