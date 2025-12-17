package com.tinsic.app.presentation.party.karaoke

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinsic.app.core.utils.LrcParser
import com.tinsic.app.data.model.QueueSong
import com.tinsic.app.data.repository.PartyRepository
import com.tinsic.app.domain.karaoke.model.LyricLine
import com.tinsic.app.domain.karaoke.model.SongNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject

/**
 * KaraokePartyController
 * 
 * Handles all karaoke-specific logic for Party Mode
 * Separates karaoke concerns from generic party room management
 * 
 * Responsibilities:
 * - Load song resources (notes + lyrics) from URLs
 * - Manage karaoke state (current song data)
 * - Update scores to Firebase
 * - End song sessions for all users
 */
@HiltViewModel
class KaraokePartyController @Inject constructor(
    private val partyRepository: PartyRepository
) : ViewModel() {

    // === KARAOKE STATE ===
    
    private val _currentSongNotes = MutableStateFlow<List<SongNote>>(emptyList())
    val currentSongNotes: StateFlow<List<SongNote>> = _currentSongNotes.asStateFlow()

    private val _currentSongLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val currentSongLyrics: StateFlow<List<LyricLine>> = _currentSongLyrics.asStateFlow()

    // === KARAOKE FUNCTIONS ===

    /**
     * Load song resources from URLs (pitch data + lyrics)
     * Downloads JSON and LRC files, parses them, applies monophonic filtering
     */
    suspend fun loadSongResources(song: QueueSong, roomId: String, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("KaraokeCtrl", "[LoadSong] Starting resource load for: ${song.title}")

                // 1. Download & Parse Pitch Data (JSON)
                val pitchJson = URL(song.pitchDataUrl).readText()
                val jsonObject = JSONObject(pitchJson)
                
                // JSON structure: { "tracks": [ { "notes": [...] } ] }
                val tracks = jsonObject.getJSONArray("tracks")
                if (tracks.length() == 0) {
                    throw Exception("No tracks found in JSON")
                }
                
                val melodyTrack = tracks.getJSONObject(0)
                val notesArray = melodyTrack.getJSONArray("notes")

                // Parse raw notes
                val rawNotes = mutableListOf<SongNote>()
                for (i in 0 until notesArray.length()) {
                    val noteObj = notesArray.getJSONObject(i)
                    val originalMidi = noteObj.getInt("midi")
                    // Transpose high notes down one octave  
                    val transposedMidi = if (originalMidi > 65) originalMidi - 12 else originalMidi

                    rawNotes.add(
                        SongNote(
                            midi = transposedMidi,
                            name = noteObj.optString("name", ""),
                            startSec = noteObj.getDouble("time"),
                            durationSec = noteObj.getDouble("duration")
                        )
                    )
                }

                // 2. Apply Monophonic Filtering
                rawNotes.sortBy { it.startSec }  // Sort by time first
                
                val groupedByTime = rawNotes.groupBy { it.startSec }
                val sortedUniqueNotes = groupedByTime.map { (_, notesAtSameTime) ->
                    notesAtSameTime.maxByOrNull { it.midi }!!
                }.sortedBy { it.startSec }

                val filteredNotes = mutableListOf<SongNote>()
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

                // 3. Download & Parse Lyrics (LRC)
                val lrcContent = URL(song.lyricUrl).readText()
                val lyrics = LrcParser.parse(lrcContent)

                // 4. Update State
                _currentSongNotes.value = filteredNotes
                _currentSongLyrics.value = lyrics

                Log.d("KaraokeCtrl", "[LoadSong] ✅ Loaded ${filteredNotes.size} notes, ${lyrics.size} lyrics")

                // 5. Set Ready State
                partyRepository.setMemberReady(roomId, userId, true)

            } catch (e: Exception) {
                Log.e("KaraokeCtrl", "[LoadSong] ❌ Error loading resources: ${e.message}", e)
                // On error, set ready to false
                partyRepository.setMemberReady(roomId, userId, false)
            }
        }
    }

    /**
     * Update user's karaoke score in Firebase
     * Updates both members and stage lists
     */
    fun updateScore(roomId: String, userId: String, score: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = partyRepository.updateMemberScore(roomId, userId, score)
            if (result.isSuccess) {
                Log.d("KaraokeCtrl", "[ScoreSync] ✅ Score updated successfully: $score")
            } else {
                Log.e("KaraokeCtrl", "[ScoreSync] ❌ Failed to update score: ${result.exceptionOrNull()}")
            }
        }
    }

    /**
     * End song for all users (reset to IDLE state)
     * Called when any user stops singing from KaraokeScreen
     */
    fun endSongForAll(roomId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("KaraokeCtrl", "[EndSong] Resetting playback state to IDLE for all users")
            partyRepository.updatePlaybackState(roomId, "IDLE", 0L)
        }
    }

    /**
     * Clear loaded song data
     * Called when exiting karaoke session
     */
    fun clearSongData() {
        _currentSongNotes.value = emptyList()
        _currentSongLyrics.value = emptyList()
    }
}
