package com.example.musicdna.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicdna.model.ListeningHistory
import com.example.musicdna.model.Music
import com.example.musicdna.model.MusicGenre
import com.example.musicdna.data.dummyListeningHistory
import com.example.musicdna.data.dummyMusicList
import kotlin.math.cos
import kotlin.math.sin


// ===============================================
// 2. HÀM TÍNH TOÁN DNA
// ===============================================

/**
 * Tính toán tỷ lệ phần trăm các thể loại nhạc dựa trên các bài hát yêu thích.
 */
fun calculateMusicDNA(
    history: List<ListeningHistory>,
    musicList: List<Music>
): Map<MusicGenre, Float> {
    // Lấy ra danh sách các bài hát yêu thích
    val favoriteMusicIds = history.filter { it.isFavourite }.map { it.musicId }.toSet()

    if (favoriteMusicIds.isEmpty()) return emptyMap()

    // Đếm số lượng bài hát yêu thích cho mỗi thể loại
    val genreCounts = musicList
        .filter { it.musicId in favoriteMusicIds }
        .groupingBy { it.genre }
        .eachCount()

    val totalFavorites = favoriteMusicIds.size.toFloat()

    // Chuyển đổi số lượng thành phần trăm và chuẩn hóa về thang 100 để vẽ
    val maxCount = genreCounts.maxOfOrNull { it.value }?.toFloat() ?: 1f

    return genreCounts.mapValues { (_, count) ->
        (count / maxCount) * 100f
    }
}


// ===============================================
// 3. COMPOSABLE CHÍNH
// ===============================================
@Composable
fun MusicDNASection() {
    // Tính toán dữ liệu DNA
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
            // Hiển thị biểu đồ thật
            RadarChart(
                dnaData = dnaData,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Giữ cho biểu đồ luôn là hình vuông
            )
        }
    }
}


// ===============================================
// 4. BIỂU ĐỒ RADAR CHART
// ===============================================
@OptIn(ExperimentalTextApi::class)
@Composable
fun RadarChart(
    dnaData: Map<MusicGenre, Float>,
    modifier: Modifier = Modifier
) {
    // Các thể loại sẽ hiển thị trên biểu đồ, theo thứ tự mong muốn
    val genres = listOf(MusicGenre.POP, MusicGenre.ROCK, MusicGenre.EDM, MusicGenre.HIP_HOP, MusicGenre.JAZZ, MusicGenre.CLASSICAL)
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 * 0.8f // Bán kính của vòng ngoài cùng
        val angleBetweenAxes = 360f / genres.size

        // Vẽ các đường lưới (grid lines) và trục (axes)
        drawGridAndAxes(genres.size, radius, angleBetweenAxes, center)

        // Vẽ vùng dữ liệu DNA
        val dnaPath = Path()
        genres.forEachIndexed { index, genre ->
            val value = dnaData[genre] ?: 0f // Lấy giá trị, mặc định là 0 nếu không có
            val angle = (angleBetweenAxes * index - 90).toDouble() // -90 độ để Pop ở trên cùng
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

        // Tô màu vùng dữ liệu
        drawPath(dnaPath, color = Color(0xFF8B1FA0).copy(alpha = 0.7f), style = Fill)
        // Vẽ đường viền cho vùng dữ liệu
        drawPath(dnaPath, color = Color(0xFFB26CFF), style = Stroke(width = 3.dp.toPx()))


        // Vẽ nhãn (labels) cho các thể loại
        drawLabels(genres, radius, angleBetweenAxes, center, textMeasurer)
    }
}

private fun DrawScope.drawGridAndAxes(
    sides: Int,
    radius: Float,
    angleBetween: Float,
    center: Offset
) {
    val gridLevels = 4
    val gridColor = Color.Gray.copy(alpha = 0.5f)

    // Vẽ các đường lưới đa giác đồng tâm
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

    // Vẽ các đường trục
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
    genres: List<MusicGenre>,
    radius: Float,
    angleBetween: Float,
    center: Offset,
    textMeasurer: TextMeasurer
) {
    val labelOffset = 1.2f // Khoảng cách từ vòng ngoài cùng đến text
    genres.forEachIndexed { i, genre ->
        val angle = (angleBetween * i - 90).toDouble()
        val textRadius = radius * labelOffset
        val x = center.x + (textRadius * cos(Math.toRadians(angle))).toFloat()
        val y = center.y + (textRadius * sin(Math.toRadians(angle))).toFloat()

        val textLayoutResult = textMeasurer.measure(
            text = genre.name,
            style = TextStyle(color = Color.White, fontSize = 12.sp)
        )

        // Căn chỉnh text để nó không bị lệch
        val textX = x - textLayoutResult.size.width / 2
        val textY = y - textLayoutResult.size.height / 2

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(textX, textY)
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
fun MusicDNASectionPreview() {
    MusicDNASection()
}
