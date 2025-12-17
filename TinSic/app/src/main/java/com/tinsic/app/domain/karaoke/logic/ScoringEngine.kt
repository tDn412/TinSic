package com.tinsic.app.domain.karaoke.logic

import com.tinsic.app.domain.karaoke.model.SingingResult
import javax.inject.Inject
import kotlin.math.abs

class ScoringEngine @Inject constructor() {
    // Giảm ngưỡng RMS một chút để mic yếu vẫn bắt được
    private val RMS_THRESHOLD = 0.003f
    // Giữ confidence để lọc tạp âm, nhưng logic so sánh nốt sẽ nới lỏng
    private val CONFIDENCE_THRESHOLD = 0.4f

    private val COLOR_HIT = 0xFF4CAF50 // Xanh
    private val COLOR_NEAR = 0xFFCDDC39 // Vàng chanh (Mới: Gần đúng)
    private val COLOR_MISS = 0xFFF44336 // Đỏ
    private val COLOR_WEAK = 0xFFFFC107 // Vàng
    private val COLOR_GRAY = 0xFF808080 // Xám

    fun evaluate(userMidi: Float, targetMidi: Int, targetNoteName: String, confidence: Float, rms: Float, currentTime: Double): SingingResult {

        val isSinging = userMidi > 0 && confidence > CONFIDENCE_THRESHOLD && rms > RMS_THRESHOLD

        var isHit = false
        var scoreAdded = 0
        var text = ""
        var color = COLOR_GRAY

        if (isSinging) {
            val diff = abs(userMidi - targetMidi)
            // Lấy phần dư trong quãng 8 (Octave agnostic)
            // Ví dụ: Đô 4 (60) và Đô 5 (72) lệch 12 -> coi như đúng nốt
            val remainder = diff % 12.0f
            val distance = if (remainder > 6) (12 - remainder) else remainder

            // 1. Chuẩn (Perfect): Sai số < 1.5 bán cung (Cũ là 1.0)
            if (distance < 1.0f) {
                isHit = true
                scoreAdded = 5
                text = "Tuyệt vời!"
                color = COLOR_HIT
            }
            else if (distance < 2.0f) {
                isHit = true
                scoreAdded = 2
                text = "Được!"
                color = COLOR_NEAR
            }
            // 3. Sai (Miss)
            else {
                text = "Lệch tông"
                color = COLOR_MISS
            }
        } else {
            if (rms < RMS_THRESHOLD) {
                text = "Hát to lên..."
                color = COLOR_WEAK
            } else {
                text = "..."
                color = COLOR_GRAY
            }
        }

        return SingingResult(
            midiUser = userMidi,
            targetMidi = targetMidi,
            noteName = targetNoteName,
            isHit = isHit,
            scoreAdded = scoreAdded,
            feedbackText = text,
            feedbackColor = color,
            currentTime = currentTime
        )
    }
}
