package com.tinsic.app.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.tinsic.app.R

/**
 * Sound manager for game sound effects
 * Uses SoundPool for low-latency playback of correct/wrong answer sounds
 * 
 * Usage:
 * 1. Call initialize(context) when app starts (e.g., in MainActivity.onCreate)
 * 2. Add correct.mp3 and wrong.mp3 to res/raw folder
 * 3. Call playCorrectSound() or playWrongSound() when needed
 * 4. Call release() when app is destroyed (e.g., in MainActivity.onDestroy)
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
            correctSoundId = soundPool?.load(context, R.raw.correct, 1) ?: 0
            wrongSoundId = soundPool?.load(context, R.raw.wrong, 1) ?: 0
            
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Play correct answer sound
     * Make sure to call initialize() first
     */
    fun playCorrectSound() {
        if (!isInitialized || soundPool == null) return
        
        try {
            soundPool?.play(
                correctSoundId,
                0.5f,  // Left volume (50%)
                0.5f,  // Right volume (50%)
                1,     // Priority
                0,     // Loop (0 = no loop)
                1.0f   // Playback rate (1.0 = normal speed)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Play wrong answer sound
     * Make sure to call initialize() first
     */
    fun playWrongSound() {
        if (!isInitialized || soundPool == null) return
        
        try {
            soundPool?.play(
                wrongSoundId,
                0.5f,  // Left volume (50%)
                0.5f,  // Right volume (50%)
                1,     // Priority
                0,     // Loop (0 = no loop)
                1.0f   // Playback rate (1.0 = normal speed)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Release SoundPool resources
     * Call this in MainActivity.onDestroy()
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        isInitialized = false
    }
}
