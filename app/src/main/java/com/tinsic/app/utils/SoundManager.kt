package com.tinsic.app.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.tinsic.app.R

/**
 * Sound manager for game sound effects
 * Uses SoundPool for low-latency playback of correct/wrong answer sounds
 */
object SoundManager {
    private var soundPool: SoundPool? = null
    private var correctSoundId: Int = 0
    private var wrongSoundId: Int = 0
    private var isInitialized = false
    
    /**
     * Initialize SoundPool and load sound files
     * Call this in MainActivity.onCreate() or Application.onCreate()
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        try {
            // Create SoundPool with audio attributes for game sounds
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            soundPool = SoundPool.Builder()
                .setMaxStreams(2)  // Max 2 sounds playing simultaneously
                .setAudioAttributes(audioAttributes)
                .build()
            
            // Load sound files from res/raw
            try {
                correctSoundId = soundPool?.load(context, R.raw.correct, 1) ?: 0
                android.util.Log.d("SoundManager", "Loaded correct.mp3 with ID: $correctSoundId")
            } catch (e: Exception) {
                android.util.Log.e("SoundManager", "Failed to load correct.mp3: ${e.message}")
                correctSoundId = 0
            }
            
            try {
                wrongSoundId = soundPool?.load(context, R.raw.wrong, 1) ?: 0
                android.util.Log.d("SoundManager", "Loaded wrong.mp3 with ID: $wrongSoundId")
            } catch (e: Exception) {
                android.util.Log.e("SoundManager", "Failed to load wrong.mp3: ${e.message}")
                wrongSoundId = 0
            }
            
            isInitialized = true
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "Failed to initialize SoundPool: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Play correct answer sound
     */
    fun playCorrectSound() {
        if (!isInitialized || soundPool == null || correctSoundId == 0) {
            android.util.Log.w("SoundManager", "Cannot play correct sound: initialized=$isInitialized, soundId=$correctSoundId")
            return
        }
        
        try {
            soundPool?.play(
                correctSoundId,
                0.7f,  // Left volume
                0.7f,  // Right volume
                1,     // Priority
                0,     // Loop (0 = no loop)
                1.0f   // Playback rate
            )
            android.util.Log.d("SoundManager", "Playing correct sound")
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "Error playing correct sound: ${e.message}")
        }
    }
    
    /**
     * Play wrong answer sound
     */
    fun playWrongSound() {
        if (!isInitialized || soundPool == null || wrongSoundId == 0) {
            android.util.Log.w("SoundManager", "Cannot play wrong sound: initialized=$isInitialized, soundId=$wrongSoundId")
            return
        }
        
        try {
            soundPool?.play(
                wrongSoundId,
                0.7f,  // Left volume
                0.7f,  // Right volume
                1,     // Priority
                0,     // Loop (0 = no loop)
                1.0f   // Playback rate
            )
            android.util.Log.d("SoundManager", "Playing wrong sound")
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "Error playing wrong sound: ${e.message}")
        }
    }
    
    /**
     * Release SoundPool resources
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        isInitialized = false
        android.util.Log.d("SoundManager", "Released SoundPool")
    }
}
