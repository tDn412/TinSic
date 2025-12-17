package com.example.musicdna.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.musicdna.data.dummyListeningHistory
import com.example.musicdna.data.dummyMusicList
import com.example.musicdna.model.HistoryItem
import com.example.musicdna.model.Music
import kotlin.math.cos
import kotlin.math.sin

// ===============================================
// 1. LOGIC TÍNH TOÁN (ĐÃ SỬA VỀ STRING)
// ===============================================
fun calculateMusicDNA(
    history: List<HistoryItem>,
    musicList: List<Music>
): Map<String, Float> {
    val favoriteMusicIds = history.map { it.id }.toSet()
    if (favoriteMusicIds.isEmpty()) return emptyMap()

    // Group theo String (it.genre)
    val genreCounts = musicList
        .filter { it.id in favoriteMusicIds }
        .groupingBy { it.genre }
        .eachCount()

    val maxCount = genreCounts.maxOfOrNull { it.value }?.toFloat() ?: 1f

    return genreCounts.mapValues { (_, count) ->
        (count / maxCount) * 100f
    }
}

// ===============================================
// 2. BIỂU ĐỒ RADAR CHART (KHÔNG DÙNG ENUM)
// ===============================================
@OptIn(ExperimentalTextApi::class)
@Composable
fun RadarChart(
    dnaData: Map<String, Float>, // <--- Tham số nhận String
    modifier: Modifier = Modifier
) {
    // KHAI BÁO CÁC TRỤC CỦA BIỂU ĐỒ BẰNG STRING
    // Bạn có thể thêm bớt tuỳ ý tại đây
    val chartGenres = listOf("POP", "ROCK", "EDM", "HIP HOP", "JAZZ", "CLASSICAL")

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 * 0.8f
        val angleBetweenAxes = 360f / chartGenres.size

        // Vẽ lưới và trục
        drawGridAndAxes(chartGenres.size, radius, angleBetweenAxes, center)

        // Vẽ vùng dữ liệu DNA
        val dnaPath = Path()
        chartGenres.forEachIndexed { index, genreName ->
            // Tìm giá trị trong Map.
            // Cố gắng tìm chính xác, hoặc tìm phiên bản thay thế dấu gạch dưới nếu cần
            val value = dnaData[genreName]
                ?: dnaData[genreName.replace(" ", "_")] // Thử tìm kiểu "HIP_HOP" nếu "HIP HOP" ko có
                ?: dnaData[genreName.uppercase()]       // Thử tìm kiểu viết hoa toàn bộ
                ?: 0f

            val angle = (angleBetweenAxes * index - 90).toDouble()
            val pointRadius = radius * (value / 100f)
            val x = centerX + (pointRadius * cos(Math.toRadians(angle))).toFloat()
            val y = centerY + (pointRadius * sin(Math.toRadians(angle))).toFloat()

            if (index == 0) {
                dnaPath.moveTo(x, y)
            } else {
                dnaPath.lineTo(x, y)
            }
        }
        dnaPath.close()

        drawPath(dnaPath, color = Color(0xFF8B1FA0).copy(alpha = 0.7f), style = Fill)
        drawPath(dnaPath, color = Color(0xFFB26CFF), style = Stroke(width = 3.dp.toPx()))

        // Vẽ nhãn (labels)
        drawLabels(chartGenres, radius, angleBetweenAxes, center, textMeasurer)
    }
}

// ===============================================
// 3. CÁC HÀM VẼ PHỤ TRỢ
// ===============================================
private fun DrawScope.drawGridAndAxes(
    sides: Int,
    radius: Float,
    angleBetween: Float,
    center: Offset
) {
    val gridLevels = 4
    val gridColor = Color.Gray.copy(alpha = 0.5f)

    (1..gridLevels).forEach { level ->
        val levelRadius = radius * level / gridLevels
        val path = Path()
        (0 until sides).forEach { i ->
            val angle = (angleBetween * i - 90).toDouble()
            val x = center.x + (levelRadius * cos(Math.toRadians(angle))).toFloat()
            val y = center.y + (levelRadius * sin(Math.toRadians(angle))).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = gridColor, style = Stroke(width = 1.dp.toPx()))
    }

    (0 until sides).forEach { i ->
        val angle = (angleBetween * i - 90).toDouble()
        val x = center.x + (radius * cos(Math.toRadians(angle))).toFloat()
        val y = center.y + (radius * sin(Math.toRadians(angle))).toFloat()
        drawLine(
            color = gridColor,
            start = center,
            end = Offset(x, y),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawLabels(
    genres: List<String>, // <--- Nhận List<String>
    radius: Float,
    angleBetween: Float,
    center: Offset,
    textMeasurer: TextMeasurer
) {
    val labelOffset = 1.2f
    genres.forEachIndexed { i, genreName ->
        val angle = (angleBetween * i - 90).toDouble()
        val textRadius = radius * labelOffset
        val x = center.x + (textRadius * cos(Math.toRadians(angle))).toFloat()
        val y = center.y + (textRadius * sin(Math.toRadians(angle))).toFloat()

        val textLayoutResult = textMeasurer.measure(
            text = genreName, // Hiển thị trực tiếp String
            style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        )

        val textX = x - textLayoutResult.size.width / 2
        val textY = y - textLayoutResult.size.height / 2

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(textX, textY)
        )
    }
}

// ===============================================
// 4. PREVIEW & SCREEN
// ===============================================

@Composable
fun MusicDNASection() {
    val dnaData = remember { calculateMusicDNA(dummyListeningHistory, dummyMusicList) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Music DNA",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), RoundedCornerShape(20.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarChart(
                dnaData = dnaData,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
fun MusicDNASectionPreview() {
    MusicDNASection()
}