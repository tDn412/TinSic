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
            
            // Temporarily disabled - no sound files yet
            // App will work silently until sound files are added
            correctSoundId = 0
            wrongSoundId = 0
            
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Play correct answer sound (currently disabled)
     */
    fun playCorrectSound() {
        // Disabled - add correct.mp3 to res/raw to enable
        // if (!isInitialized || soundPool == null || correctSoundId == 0) return
        // soundPool?.play(correctSoundId, 0.5f, 0.5f, 1, 0, 1.0f)
    }
    
    /**
     * Play wrong answer sound (currently disabled)
     */
    fun playWrongSound() {
        // Disabled - add wrong.mp3 to res/raw to enable
        // if (!isInitialized || soundPool == null || wrongSoundId == 0) return
        // soundPool?.play(wrongSoundId, 0.5f, 0.5f, 1, 0, 1.0f)
    }
    
    /**
     * Release SoundPool resources
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        isInitialized = false
    }
}
