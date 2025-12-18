package com.tinsic.app.ui.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor // [FIX] Cần import cái này
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tinsic.app.R
import com.tinsic.app.data.model.profile.DisplayAchievement
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinsic.app.presentation.profile.ProfileViewModel
import kotlinx.coroutines.launch

// --- Bảng màu mới, tinh tế hơn ---
// [FIX] Tách màu chủ đạo ra để dễ dùng lại, tránh lỗi ép kiểu Brush
private val PrimaryGoldColor = Color(0xFFFBC02D)
private val GoldGradient = Brush.linearGradient(listOf(PrimaryGoldColor, Color(0xFFFFD54F), Color(0xFFF9A825)))
private val UnlockedLineColor = Color(0xAAE0E0E0)
private val LockedLineColor = Color(0xFF424242)
private val UnlockedNodeColor = Color(0xFF2C2C2C)
private val LockedNodeColor = Color(0xFF212121)
private val BackgroundColor = Color(0xFF1A1A1A)
private val ProgressBarColor = Color(0xFFFBC02D)
private val ProgressBarBackgroundColor = Color(0xFF555555)

@Composable
fun AchievementSection(
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    var achievementToShowDialog by remember { mutableStateOf<DisplayAchievement?>(null) }

    // Collect display data from ViewModel
    val displayData by profileViewModel.displayAchievements.collectAsState()

    if (achievementToShowDialog != null) {
        AchievementDetailsDialog(
            achievement = achievementToShowDialog!!,
            onDismiss = { achievementToShowDialog = null }
        )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Thành Tích",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundColor, RoundedCornerShape(20.dp))
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (displayData.isEmpty()) {
                Text(
                    "Bắt đầu nghe nhạc để mở khóa thành tích!",
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                displayData.forEach { chain ->
                    AchievementChainRow(chain, onNodeClick = { achievement ->
                        achievementToShowDialog = achievement
                    })
                }
            }
        }
    }
}

@Composable
private fun AchievementChainRow(
    chain: List<DisplayAchievement>,
    onNodeClick: (DisplayAchievement) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chain.forEachIndexed { index, achievement ->
            if (index > 0) {
                ChainConnector(isUnlocked = chain[index - 1].isUnlocked)
            }
            AchievementNode(
                achievement = achievement,
                onClick = { onNodeClick(achievement) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AchievementNode(
    achievement: DisplayAchievement,
    onClick: () -> Unit
) {
    val progress = (achievement.currentProgress.toFloat() / achievement.targetCount.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(durationMillis = 1000), label = "progress")

    // TooltipBox removed due to API compatibility issues
    Box(
        modifier = Modifier
            .shadow(
                elevation = if (achievement.isUnlocked) 8.dp else 0.dp,
                shape = CircleShape,
                spotColor = ProgressBarColor
            )
            .size(72.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { /* Tooltip removed */ }
            )
            .drawBehind {
                if (!achievement.isUnlocked && animatedProgress > 0f) {
                    drawArc(
                        color = ProgressBarBackgroundColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawArc(
                        color = ProgressBarColor,
                        startAngle = -90f,
                        sweepAngle = 360 * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            .padding(3.dp)
            .background(
                if (achievement.isUnlocked) UnlockedNodeColor else LockedNodeColor,
                CircleShape
            )
            .border(
                width = if (achievement.isUnlocked) 2.dp else 1.dp,
                brush = if (achievement.isUnlocked) GoldGradient else SolidColor(Color(0xFF424242)),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = achievement.iconUrl,
            contentDescription = stringResource(achievement.titleRes),
            modifier = Modifier
                .fillMaxSize(0.6f)
                .clip(CircleShape),
            error = painterResource(id = R.drawable.ic_launcher_foreground)
        )
    }
}


@Composable
private fun ChainConnector(isUnlocked: Boolean) {
    Canvas(modifier = Modifier.width(32.dp)) {
        val y = size.height / 2
        drawLine(
            color = if (isUnlocked) UnlockedLineColor else LockedLineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = if (!isUnlocked) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
        )
    }
}

@Composable
fun AchievementDetailsDialog(achievement: DisplayAchievement, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF2C2C2C),
        title = {
            Text(
                text = stringResource(id = achievement.titleRes),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(id = achievement.descriptionRes),
                    color = Color.LightGray,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(16.dp))
                if (!achievement.isUnlocked) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            progress = (achievement.currentProgress.toFloat() / achievement.targetCount.toFloat()).coerceIn(0f, 1f),
                            modifier = Modifier.size(24.dp),
                            color = ProgressBarColor,
                            strokeWidth = 3.dp,
                            trackColor = ProgressBarBackgroundColor,
                            strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Tiến trình: ${achievement.currentProgress} / ${achievement.targetCount}",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                // [FIX] Dùng trực tiếp PrimaryGoldColor đã khai báo ở trên
                Text("Đóng", color = PrimaryGoldColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
fun AchievementSectionPreview() {
    AchievementSection()
}