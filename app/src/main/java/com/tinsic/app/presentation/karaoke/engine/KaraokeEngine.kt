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
    val isPlaybackEnabled: Boolean = true,
    val isRecordingEnabled: Boolean = true,
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
    private var config: KaraokeConfig = KaraokeConfig()

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

        // 1. Init Media Player (if enabled)
        if (config.isPlaybackEnabled) {
            // Note: R.raw.beat_phia_sau_mot_co_gai needs to exist or handle error
            // Using try-catch to avoid crash if resource missing
            try {
               // Placeholder for resource ID, user must provide the file name or ID mapped
               // For now assume the user will copy the MP3 to res/raw
               val resId = context.resources.getIdentifier("beat_phia_sau_mot_co_gai", "raw", context.packageName)
               if(resId != 0) {
                    mediaPlayer = MediaPlayer.create(context, resId).apply {
                        setVolume(1.0f, 1.0f)
                        start()
                    }
               }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Init AudioRecord (if enabled)
        if (config.isRecordingEnabled) {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE
            )
            audioRecord?.startRecording()
        }

        // 3. Start Processing Loop
        processingJob = CoroutineScope(Dispatchers.IO).launch {
            processingLoop()
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

                val currentSec = if (config.isPlaybackEnabled) {
                    (player?.currentPosition ?: 0) / 1000.0
                } else {
                    0.0 
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
