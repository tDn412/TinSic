package com.tinsic.app.presentation.party

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ResultOverlay(
    score: Int,
    singerName: String,
    singerAvatar: String,
    onClose: () -> Unit
) {
    // Animation States
    var showContent by remember { mutableStateOf(false) }
    val scaleAnim = remember { Animatable(0f) }
    
    // Rank Calculation
    val rank = when {
        score >= 4500 -> "SSS"
        score >= 4000 -> "SS"
        score >= 3000 -> "S"
        score >= 2000 -> "A"
        else -> "B"
    }
    
    val rankColor = when(rank) {
        "SSS" -> Color(0xFFFFD700) // Gold
        "SS" -> Color(0xFFD500F9) // Purple
        "S" -> Color(0xFF00E5FF) // Cyan
        else -> Color(0xFFE0E0E0) // Gray
    }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showContent) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .scale(scaleAnim.value)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF2D2D2D), Color(0xFF1A1A1A))
                        )
                    )
                    .border(2.dp, Brush.linearGradient(listOf(rankColor, Color.White)), RoundedCornerShape(32.dp))
                    .padding(32.dp)
            ) {
                // Title
                Text(
                    "KẾT QUẢ",
                    color = Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(4.dp, rankColor, CircleShape)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(singerAvatar, fontSize = 48.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Name
                Text(
                    singerName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Rank
                Text(
                    rank,
                    color = rankColor,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black
                )
                
                // Score
                Text(
                    "$score điểm",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Close Button
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = rankColor.copy(alpha = 0.2f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, rankColor),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Hoàn thành", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
