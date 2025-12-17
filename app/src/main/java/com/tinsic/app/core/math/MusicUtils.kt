package com.tinsic.app.core.math

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

object MusicUtils {
    // A4 = 440Hz -> MIDI 69
    fun hzToMidi(hz: Float): Float {
        if (hz <= 0) return 0f
        // MIDI = 69 + 12 * log2(Hz / 440)
        return (69 + 12 * (ln(hz / 440.0) / ln(2.0))).toFloat()
    }

    fun midiToHz(midi: Float): Float {
        // Hz = 440 * 2^((MIDI-69)/12)
        return (440.0 * 2.0.pow((midi - 69.0) / 12.0)).toFloat()
    }
}
