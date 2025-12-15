package com.tinsic.app.data.repository

import com.tinsic.app.data.datasource.KaraokeDataSource
import com.tinsic.app.domain.karaoke.model.LyricLine
import com.tinsic.app.domain.karaoke.model.SongNote
import com.tinsic.app.domain.repository.KaraokeRepository
import javax.inject.Inject

class KaraokeRepositoryImpl @Inject constructor(
    private val dataSource: KaraokeDataSource
) : KaraokeRepository {

    override suspend fun getSongNotes(fileName: String): List<SongNote> {
        return dataSource.getSongNotes(fileName)
    }

    override suspend fun getLyrics(fileName: String): List<LyricLine> {
        return dataSource.getLyrics(fileName)
    }
}
