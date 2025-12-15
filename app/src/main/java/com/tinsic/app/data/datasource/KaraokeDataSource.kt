package com.tinsic.app.data.datasource

import android.content.Context
import com.tinsic.app.core.utils.LrcParser
import com.tinsic.app.domain.karaoke.model.LyricLine
import com.tinsic.app.domain.karaoke.model.SongNote
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

class KaraokeDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getSongNotes(fileName: String): List<SongNote> = withContext(Dispatchers.IO) {
        val rawNotes = mutableListOf<SongNote>()
        try {
            // Read file from Assets
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val tracks = jsonObject.getJSONArray("tracks")

            if (tracks.length() > 0) {
                val melodyTrack = tracks.getJSONObject(0)
                val notesArray = melodyTrack.getJSONArray("notes")

                for (i in 0 until notesArray.length()) {
                    val noteJson = notesArray.getJSONObject(i)
                    val originalMidi = noteJson.getInt("midi")
                    val transposedMidi = if (originalMidi > 65) originalMidi - 12 else originalMidi
                    rawNotes.add(
                        SongNote(
                            midi = transposedMidi,
                            name = noteJson.optString("name", ""),
                            startSec = noteJson.getDouble("time"),
                            durationSec = noteJson.getDouble("duration")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // --- MONOPHONIC FILTERING logic ---
        rawNotes.sortBy { it.startSec }

        val filteredNotes = mutableListOf<SongNote>()
        val groupedByTime = rawNotes.groupBy { it.startSec }
        val sortedUniqueNotes = groupedByTime.map { (_, notesAtSameTime) ->
            notesAtSameTime.maxByOrNull { it.midi }!!
        }.sortedBy { it.startSec }

        for (i in sortedUniqueNotes.indices) {
            val currentNote = sortedUniqueNotes[i]
            if (i < sortedUniqueNotes.size - 1) {
                val nextNote = sortedUniqueNotes[i + 1]
                val currentEnd = currentNote.startSec + currentNote.durationSec
                if (currentEnd > nextNote.startSec) {
                    val newDuration = nextNote.startSec - currentNote.startSec
                    if (newDuration > 0.01) {
                        filteredNotes.add(currentNote.copy(durationSec = newDuration))
                    }
                } else {
                    filteredNotes.add(currentNote)
                }
            } else {
                filteredNotes.add(currentNote)
            }
        }
        return@withContext filteredNotes
    }

    suspend fun getLyrics(fileName: String): List<LyricLine> = withContext(Dispatchers.IO) {
        try {
            val lrcFileName = if (fileName.endsWith(".json")) fileName.replace(".json", ".lrc") else fileName
            val lrcContent = context.assets.open(lrcFileName).bufferedReader().use { it.readText() }
            return@withContext LrcParser.parse(lrcContent)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
}
