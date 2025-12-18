package com.tinsic.app.presentation.profile

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinsic.app.data.model.profile.MusicDnaProfile
import com.tinsic.app.data.model.User
import com.tinsic.app.utils.profile.SharableDnaImage
import com.tinsic.app.utils.profile.captureComposableToBitmap
import com.tinsic.app.utils.profile.shareBitmap
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// Enum để quản lý các view một cách an toàn và rõ ràng
// Make it internal or public to be accessible if needed, or private if only used here
private enum class DnaView { RADAR, ARTISTS, COUNTRIES }

/**
 * •Composable MusicDNASection đã được tái cấu trúc hoàn toàn.
 * •Giờ đây nó nhận toàn bộ dnaProfile và tự quản lý việc hiển thị các view khác nhau.
 */
@Composable
fun MusicDNASection(dnaProfile: MusicDnaProfile, user: User) {
    var currentView by remember { mutableStateOf(DnaView.RADAR) }

    val context = LocalContext.current
    val density = LocalDensity.current
    val compositionContext = rememberCompositionContext()
    val coroutineScope = rememberCoroutineScope()

    // Hàm xử lý việc chụp và chia sẻ
    fun handleShare() {
        coroutineScope.launch {
            try {
                // Chuyển đổi dp sang pixel
                val widthPx = with(density) { 400.dp.toPx().toInt() }
                val heightPx = with(density) { 700.dp.toPx().toInt() }

                // Chụp Composable thành Bitmap (nay là suspend function)
                val bitmap = captureComposableToBitmap(context, widthPx, heightPx, compositionContext) {
                    SharableDnaImage(dnaProfile = dnaProfile, user = user)
                }

                // Chia sẻ bitmap vừa tạo
                shareBitmap(context, bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Lỗi khi chia sẻ: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Music DNA",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { handleShare() }) {
                Icon(Icons.Default.Share, "Share DNA", tint = Color.White)
            }

            DnaViewSwitcher(
                selectedView = currentView,
                onViewSelected = { newView -> currentView = newView }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), RoundedCornerShape(20.dp))
                .padding(16.dp)
                .animateContentSize(),
            contentAlignment = Alignment.Center
        ) {
            when (currentView) {
                DnaView.RADAR -> RadarChart(
                    dnaData = dnaProfile.genreDistribution,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                DnaView.ARTISTS -> TopItemsList(
                    title = "Top Artists",
                    items = dnaProfile.topArtists
                )
                DnaView.COUNTRIES -> TopItemsList(
                    title = "Top Markets",
                    items = dnaProfile.topCountries
                )
            }
        }
    }
}

@Composable
private fun DnaViewSwitcher(selectedView: DnaView, onViewSelected: (DnaView) -> Unit) {
    Row(
        modifier = Modifier
            .background(Color(0xFF2C2C2C), RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        DnaViewButton("DNA", selectedView == DnaView.RADAR) { onViewSelected(DnaView.RADAR) }
        DnaViewButton("Artists", selectedView == DnaView.ARTISTS) { onViewSelected(DnaView.ARTISTS) }
        DnaViewButton("Markets", selectedView == DnaView.COUNTRIES) { onViewSelected(DnaView.COUNTRIES) }
    }
}

@Composable
private fun DnaViewButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) Color.DarkGray else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Hàm helper để lấy emoji cờ từ mã quốc gia
private fun getFlagEmoji(countryCode: String): String {
    return when (countryCode.uppercase()) {
        "VN" -> "🇻🇳"
        "US_UK" -> "🇬🇧/🇺🇸" // Giữ nguyên cách xử lý US_UK như trong code cũ
        "JP" -> "🇯🇵"
        "KR" -> "🇰🇷"
        "CN" -> "🇨🇳"
        "US" -> "🇺🇸" // Thêm US riêng
        "GB" -> "🇬🇧" // Thêm GB riêng
        else -> "🌍"
    }
}

/**
 * •Composable tái sử dụng để hiển thị danh sách Top 5 (Nghệ sĩ hoặc Quốc gia).
 * •PHIÊN BẢN HOÀN CHỈNH VÀ ĐÃ SỬA LỖI.
 */
@Composable
fun TopItemsList(title: String, items: List<Pair<String, Int>>) {
    // Xác định xem đây là danh sách nghệ sĩ hay quốc gia
    val isArtistList = title.contains("Artists")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (items.isEmpty()) {
            Text(
                "Not enough data yet.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thêm Icon hoặc Emoji ở đầu
                        if (isArtistList) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Artist",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(text = getFlagEmoji(item.first), fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Tên chính (không cần số thứ tự nữa)
                        Text(
                            text = item.first,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            // Thêm weight(1f) để Text này chiếm hết không gian còn lại (tránh tràn)
                            modifier = Modifier.weight(1f)
                        )

                        // Spacer(modifier = Modifier.weight(1f)) // Đã di chuyển weight(1f) vào Text trên để đảm bảo nó không đẩy Text sang phải

                        // Số lượng
                        Text(
                            text = "${item.second} songs",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.End // Căn phải số lượng
                        )
                    }
                }
            }
        }
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
    val chartGenres = listOf("Pop", "Rock", "R&B", "Dance", "Rap", "Ballad")

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