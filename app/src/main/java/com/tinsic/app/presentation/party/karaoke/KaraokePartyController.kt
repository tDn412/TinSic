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
 * - Prefetch & cache song resources for instant loading
 * - Load song resources (notes + lyrics) from URLs
 * - Manage karaoke state (current song data)
 * - Update scores to Firebase
 * - End song sessions for all users
 */

// Data class for cached song resources
data class CachedSongData(
    val notes: List<SongNote>,
    val lyrics: List<LyricLine>,
    val mp3FilePath: String? = null,  // Path to locally downloaded MP3
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class KaraokePartyController @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val partyRepository: PartyRepository
) : ViewModel() {

    // === KARAOKE STATE ===
    
    private val _currentSongNotes = MutableStateFlow<List<SongNote>>(emptyList())
    val currentSongNotes: StateFlow<List<SongNote>> = _currentSongNotes.asStateFlow()

    private val _currentSongLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val currentSongLyrics: StateFlow<List<LyricLine>> = _currentSongLyrics.asStateFlow()
    
    private val _currentMp3Path = MutableStateFlow<String?>(null)
    val currentMp3Path: StateFlow<String?> = _currentMp3Path.asStateFlow()
    
    // === PREFETCH CACHE ===
    // Store prefetched song data by songId
    private val prefetchCache = mutableMapOf<String, CachedSongData>()

    // === KARAOKE FUNCTIONS ===

    /**
     * Prefetch song resources in background (called when song added to queue)
     * Downloads and caches notes + lyrics + MP3 (host only) for instant loading later
     */
    fun prefetchSong(song: QueueSong) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("KaraokeCtrl", "[Prefetch] Starting prefetch for: ${song.title}")
                
                // Check if already cached
                if (prefetchCache.containsKey(song.id)) {
                    Log.d("KaraokeCtrl", "[Prefetch] Already cached: ${song.title}")
                    return@launch
                }
                
                // Download and parse JSON + LRC (all devices)
                val (notes, lyrics) = downloadAndParseSong(song)
                
                // Download MP3 to disk (ALWAYS for everyone - needed if they jump on stage)
                val mp3FilePath = if (song.audioUrl.isNotEmpty()) {
                    try {
                        downloadMP3(song)
                    } catch (e: Exception) {
                        Log.e("KaraokeCtrl", "[Prefetch] MP3 download failed: ${e.message}")
                        null
                    }
                } else {
                    null
                }
                
                // Store in cache
                prefetchCache[song.id] = CachedSongData(notes, lyrics, mp3FilePath)
                
                val mp3Status = if (mp3FilePath != null) "MP3 ✅" else "no MP3"
                Log.d("KaraokeCtrl", "[Prefetch] ✅ Cached ${notes.size} notes, ${lyrics.size} lyrics, $mp3Status for: ${song.title}")
            } catch (e: Exception) {
                Log.e("KaraokeCtrl", "[Prefetch] ❌ Failed to prefetch ${song.title}: ${e.message}")
            }
        }
    }

    /**
     * Load song resources - checks cache first, downloads if needed
     * Downloads JSON and LRC files, parses them, applies monophonic filtering
     */
    suspend fun loadSongResources(song: QueueSong, roomId: String, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("KaraokeCtrl", "[LoadSong] Starting resource load for: ${song.title}")
                
                // Check cache first!
                val cached = prefetchCache[song.id]
                val (filteredNotes, lyrics) = if (cached != null) {
                    Log.d("KaraokeCtrl", "[LoadSong] ✅ CACHE HIT! Instant load for: ${song.title}")
                    Pair(cached.notes, cached.lyrics)
                } else {
                    Log.d("KaraokeCtrl", "[LoadSong] ⚠️ Cache miss, downloading now...")
                    downloadAndParseSong(song)
                }

                // 4. Update State
                _currentSongNotes.value = filteredNotes
                _currentSongLyrics.value = lyrics
                _currentMp3Path.value = cached?.mp3FilePath  // Set MP3 path

                val mp3Status = if (cached?.mp3FilePath != null) "MP3 ready ✅" else "no MP3"
                Log.d("KaraokeCtrl", "[LoadSong] ✅ Loaded ${filteredNotes.size} notes, ${lyrics.size} lyrics, $mp3Status")

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
     * Helper: Download and parse song data (used by both prefetch and load)
     * Returns Pair<notes, lyrics>
     */
    private suspend fun downloadAndParseSong(song: QueueSong): Pair<List<SongNote>, List<LyricLine>> {
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
        
        return Pair(filteredNotes, lyrics)
    }
    
    /**
     * Helper: Download MP3 file to internal storage
     * Returns path to downloaded file for instant playback
     */
    private suspend fun downloadMP3(song: QueueSong): String? {
        try {
            Log.d("KaraokeCtrl", "[DownloadMP3] Downloading: ${song.audioUrl}")
            
            // Create cache directory
            val cacheDir = java.io.File(context.cacheDir, "karaoke_mp3")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            // Generate filename from song ID
            val fileName = "song_${song.id}.mp3"
            val outputFile = java.io.File(cacheDir, fileName)
            
            // Download MP3
            val connection = URL(song.audioUrl).openConnection()
            connection.connect()
            
            connection.getInputStream().use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.d("KaraokeCtrl", "[DownloadMP3] ✅ Downloaded to: ${outputFile.absolutePath} (${outputFile.length() / 1024}KB)")
            return outputFile.absolutePath
        } catch (e: Exception) {
            Log.e("KaraokeCtrl", "[DownloadMP3] ❌ Failed: ${e.message}", e)
            return null
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
        _currentMp3Path.value = null
    }
    
    /**
     * Check if song is already prefetched and ready
     * Used to skip LOADING state if cache hit
     */
    fun isSongCached(songId: String): Boolean {
        return prefetchCache.containsKey(songId)
    }
    
    /**
     * Remove song from prefetch cache
     * Called when song is removed from queue
     */
    fun removeSongFromCache(songId: String) {
        // Delete MP3 file if exists
        prefetchCache[songId]?.mp3FilePath?.let { path ->
            try {
                java.io.File(path).delete()
                Log.d("KaraokeCtrl", "[Cache] Deleted MP3 file: $path")
            } catch (e: Exception) {
                Log.e("KaraokeCtrl", "[Cache] Failed to delete MP3: ${e.message}")
            }
        }
        
        prefetchCache.remove(songId)
        Log.d("KaraokeCtrl", "[Cache] Removed song from cache: $songId")
    }
    
    /**
     * Clear all cached songs
     * Called when leaving room or clearing queue
     */
    fun clearAllCache() {
        // Delete all MP3 files
        prefetchCache.values.forEach { cached ->
            cached.mp3FilePath?.let { path ->
                try {
                    java.io.File(path).delete()
                } catch (e: Exception) {
                    Log.e("KaraokeCtrl", "[Cache] Failed to delete MP3: ${e.message}")
                }
            }
        }
        
        prefetchCache.clear()
        Log.d("KaraokeCtrl", "[Cache] Cleared all cached songs and MP3 files")
    }
}
