package com.tinsic.app.presentation.karaoke.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import com.tinsic.app.R
import com.tinsic.app.core.math.MusicUtils
import com.tinsic.app.data.ai.SpiceDetector
import com.tinsic.app.domain.karaoke.logic.ScoringEngine
import com.tinsic.app.domain.karaoke.model.SingingResult
import com.tinsic.app.domain.karaoke.model.SongNote
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Arrays
import javax.inject.Inject

data class KaraokeConfig(
    val audioUrl: String = "",                    // Fallback: URL for streaming audio
    val mp3FilePath: String? = null,              // Preferred: Local file path (instant load!)
    val isPlaybackEnabled: Boolean = true,        // Host-only flag
    val isRecordingEnabled: Boolean = true,       // Always true for scoring
    val startTimeMs: Long = 0L,                   // Server start time for guest sync
    val initialLatencyOffsetMs: Int = 0
)

class KaraokeEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spiceDetector: SpiceDetector,
    private val scoringEngine: ScoringEngine
) {

    private val SILENCE_THRESHOLD = 0.005f
    private val CONFIDENCE_THRESHOLD = 0.6f

    // --- OUTPUT STREAM ---
    private val _singingFlow = MutableSharedFlow<SingingResult>(replay = 0)
    val singingFlow = _singingFlow.asSharedFlow()

    // --- AUDIO CONFIG ---
    private val SAMPLE_RATE = 16000
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 4

    // --- STATE ---
    private var audioRecord: AudioRecord? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRunning = false
    private var processingJob: Job? = null

    private var currentSongNotes: List<SongNote> = emptyList()
    internal var config: KaraokeConfig = KaraokeConfig()

    // Latency Control
    private var latencyOffsetMs: Int = 0
    private val HARD_LATENCY_CORRECTION = 0.2 // 200ms hardware latency assumption
    private val TIME_WINDOW = 0.5

    fun setLatencyOffset(offsetMs: Int) {
        this.latencyOffsetMs = offsetMs
    }

    fun getLatencyOffset(): Int = latencyOffsetMs

    @SuppressLint("MissingPermission")
    fun startRecording(songNotes: List<SongNote>, config: KaraokeConfig = KaraokeConfig()) {
        if (isRunning) return

        this.currentSongNotes = songNotes
        this.config = config
        this.latencyOffsetMs = config.initialLatencyOffsetMs
        isRunning = true

        // Start processing job that will initialize everything
        processingJob = CoroutineScope(Dispatchers.IO).launch {
            // 1. Init Media Player (HOST ONLY)
            if (config.isPlaybackEnabled) {
                // Prefer local file (instant!), fallback to streaming
                val useLocalFile = !config.mp3FilePath.isNullOrEmpty()
                
                if (useLocalFile) {
                    android.util.Log.d("KaraokeEngine", "[HOST] Loading from PREFETCHED file: ${config.mp3FilePath}")
                } else {
                    android.util.Log.d("KaraokeEngine", "[HOST] Streaming from URL (no cache): ${config.audioUrl}")
                }
                
                try {
                    mediaPlayer = MediaPlayer().apply {
                        if (useLocalFile) {
                            // INSTANT LOAD from local file!
                            setDataSource(config.mp3FilePath!!)
                        } else {
                            // FALLBACK: Stream from URL
                            setDataSource(config.audioUrl)
                        }
                        
                        setVolume(1.0f, 1.0f)
                        
                        setOnErrorListener { _, what, extra ->
                            android.util.Log.e("KaraokeEngine", "[HOST] MediaPlayer error: what=$what, extra=$extra")
                            true
                        }
                        
                        // Synchronous prepare (blocking in IO thread)
                        val prepareStartTime = System.currentTimeMillis()
                        prepare()
                        val prepareDelayMs = System.currentTimeMillis() - prepareStartTime
                        
                        android.util.Log.d("KaraokeEngine", "[HOST] ✅ PREPARED in ${prepareDelayMs}ms! ${if (useLocalFile) "INSTANT" else "Streaming"} - Ready to start!")
                        // DO NOT start() - will be called separately in startPlayback()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("KaraokeEngine", "[HOST] Failed to init audio: ${e.message}", e)
                    mediaPlayer = null
                }
            } else if (!config.isPlaybackEnabled) {
                android.util.Log.d("KaraokeEngine", "[GUEST] Playback disabled, no audio will be played")
            }

            // 2. Init AudioRecord (ALWAYS for pitch detection & scoring)
            if (config.isRecordingEnabled) {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    BUFFER_SIZE
                )
                audioRecord?.startRecording()
                android.util.Log.d("KaraokeEngine", "AudioRecord started for pitch detection")
            }

            // 3. Start Processing Loop (MediaPlayer is ready now!)
            android.util.Log.d("KaraokeEngine", "Starting processing loop...")
            processingLoop()
        }
    }

    /**
     * PHASE 2: Start playback (call when PLAYING state begins)
     * MediaPlayer must already be prepared by startRecording()!
     */
    fun startPlayback() {
        if (!isRunning) {
            android.util.Log.w("KaraokeEngine", "[HOST] Cannot start - engine not prepared!")
            return
        }
        
        try {
            mediaPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                    android.util.Log.d("KaraokeEngine", "[HOST] 🎵 PLAYBACK STARTED!")
                } else {
                    android.util.Log.w("KaraokeEngine", "[HOST] Already playing!")
                }
            } ?: android.util.Log.w("KaraokeEngine", "[HOST] MediaPlayer not initialized!")
        } catch (e: Exception) {
            android.util.Log.e("KaraokeEngine", "[HOST] Failed to start playback: ${e.message}", e)
        }
    }

    fun stopRecording() {
        isRunning = false
        processingJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) { e.printStackTrace() }
        audioRecord = null

        try {
            if (mediaPlayer?.isPlaying == true) mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) { e.printStackTrace() }
        mediaPlayer = null
    }

    @SuppressLint("MissingPermission")
    private fun restartAudioRecord() {
        if (!config.isRecordingEnabled) return
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) { e.printStackTrace() }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE
            )
            audioRecord?.startRecording()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun processingLoop() {
        val audioBuffer = ShortArray(512)
        val floatBuffer = FloatArray(512)
        var errorCount = 0

        while (isRunning) {
            try {
            
                val player = mediaPlayer
                // If playback enabled but player failed to init/finish, stop
                if (config.isPlaybackEnabled && player == null && isRunning) {
                     // Keep running if it's just missing resource, but time will be 0
                }
                
                if (config.isPlaybackEnabled && player != null && !player.isPlaying && isRunning) {
                     break
                }

                // Calculate current time in song
                val currentSec = if (config.isPlaybackEnabled) {
                    // Host: Use MediaPlayer position (sync with actual audio!)
                    (player?.currentPosition ?: 0) / 1000.0
                } else {
                    // Guest: Use server time sync
                    if (config.startTimeMs > 0) {
                        val now = System.currentTimeMillis()
                        val elapsedMs = now - config.startTimeMs
                        val elapsed = elapsedMs / 1000.0
                        
                        // Debug log every 2 seconds
                        if (elapsedMs % 2000 < 100) {
                            android.util.Log.d("KaraokeEngine", "[GUEST] Now: $now, StartTime: ${config.startTimeMs}, Elapsed: ${elapsed}s")
                        }
                        
                        elapsed
                    } else {
                        android.util.Log.w("KaraokeEngine", "[GUEST] WARNING: startTimeMs = 0! Timing broken!")
                        0.0
                    }
                }

                val manualOffsetSec = latencyOffsetMs / 1000.0
                val adjustedTime = currentSec - HARD_LATENCY_CORRECTION + manualOffsetSec

                // --- AUDIO INPUT ---
                var readCount = 0
                if (config.isRecordingEnabled) {
                    readCount = audioRecord?.read(audioBuffer, 0, 512) ?: -1
                } else {
                    delay(20) 
                }

                if (readCount < 0) {
                    errorCount++
                    if (errorCount > 5) {
                        restartAudioRecord()
                        errorCount = 0
                        delay(200)
                    }
                    delay(20)
                    continue
                }
                errorCount = 0

                if (readCount > 0) {
                    // 1. RMS & Normalization
                    Arrays.fill(floatBuffer, 0f)
                    var sumSquares = 0.0
                    for (i in 0 until readCount) {
                        val sampleVal = audioBuffer[i] / 32768.0f
                        floatBuffer[i] = sampleVal
                        sumSquares += sampleVal * sampleVal
                    }
                    val rms = Math.sqrt(sumSquares / readCount).toFloat()

                    // 2. Pitch Detection
                    val (pitchHz, confidence) = spiceDetector.getPitch(floatBuffer)
                    val cleanPitch = if (rms > SILENCE_THRESHOLD && confidence > CONFIDENCE_THRESHOLD) pitchHz else 0f
                    val userMidi = if (cleanPitch > 0) MusicUtils.hzToMidi(cleanPitch) else 0f

                    // 3. Find Target Note
                    val candidates = currentSongNotes.filter { note ->
                        val noteEnd = note.startSec + note.durationSec
                        adjustedTime >= (note.startSec - TIME_WINDOW) && adjustedTime <= (noteEnd + TIME_WINDOW)
                    }

                    var activeNote = candidates.find { note ->
                        val noteEnd = note.startSec + note.durationSec
                        adjustedTime >= note.startSec && adjustedTime <= noteEnd
                    }

                    if (activeNote == null) {
                        activeNote = candidates.minByOrNull { note ->
                            val noteCenter = note.startSec + (note.durationSec / 2)
                            Math.abs(adjustedTime - noteCenter)
                        }
                    }

                    // 4. Scoring
                    if (activeNote != null) {
                        val result = scoringEngine.evaluate(
                            userMidi = userMidi,
                            targetMidi = activeNote.midi,
                            targetNoteName = activeNote.name,
                            confidence = confidence,
                            rms = rms,
                            currentTime = adjustedTime
                        )
                        _singingFlow.emit(result.copy(currentTime = adjustedTime))
                    } else {
                        _singingFlow.emit(
                            SingingResult(
                                currentTime = adjustedTime,
                                midiUser = userMidi,
                                targetMidi = 0,
                                noteName = "...",
                                isHit = false,
                                scoreAdded = 0,
                                feedbackText = "(Nhạc dạo)",
                                feedbackColor = 0xFF808080
                            )
                        )
                    }
                    yield()
                } else {
                    delay(5)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                delay(100)
            }
        }
    }
}
