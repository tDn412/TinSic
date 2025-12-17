package com.tinsic.app.data.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.math.pow

class SpiceDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private val MODEL_INPUT_SIZE = 512

    // Optimization: Reuse buffer to avoid memory leak
    private val inputBuffer: ByteBuffer
    private val outputPitch = FloatArray(2)
    private val outputUncertainty = FloatArray(2)
    private val outputs: Map<Int, Any>

    init {
        // Pre-allocate buffers once
        inputBuffer = ByteBuffer.allocateDirect(MODEL_INPUT_SIZE * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        outputs = mapOf(
            0 to outputPitch,
            1 to outputUncertainty
        )

        try {
            val modelFile = FileUtil.loadMappedFile(context, "spice.tflite")
            val options = Interpreter.Options()
            interpreter = Interpreter(modelFile, options)

            // QUAN TRỌNG: Resize Input
            interpreter?.resizeInput(0, intArrayOf(MODEL_INPUT_SIZE))
            interpreter?.allocateTensors()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPitch(audioFloatArray: FloatArray): Pair<Float, Float> {
        if (interpreter == null) return Pair(0f, 0f)

        // Reuse buffer: Reset position to 0
        inputBuffer.rewind()
        for (sample in audioFloatArray) {
            inputBuffer.putFloat(sample)
        }

        try {
            interpreter?.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

            // Lấy phần tử đầu tiên [0] như cũ
            val normalizedPitch = outputPitch[0]
            val uncertainty = outputUncertainty[0]

            val pitchInHz = 10.0f * 2.0f.pow(6.0f * normalizedPitch)
            val confidence = 1.0f - uncertainty

            return Pair(pitchInHz, confidence)

        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(0f, 0f)
        }
    }

    fun close() { interpreter?.close() }
}
