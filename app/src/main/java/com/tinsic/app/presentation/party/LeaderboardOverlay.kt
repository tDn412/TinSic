package com.tinsic.app.presentation.party

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
fun LeaderboardOverlay(
    results: List<PartyUser>,
    onClose: () -> Unit
) {
    // Animation States
    var showContent by remember { mutableStateOf(false) }
    val scaleAnim = remember { Animatable(0f) }
    
    // Sort results just in case
    val sortedResults = remember(results) { results.sortedByDescending { it.lastScore } }

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
            .background(Color.Black.copy(alpha = 0.85f))
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
                        Brush.linearGradient(
                            listOf(Color(0xFF2D2D2D), Color(0xFF1A1A1A))
                        )
                    )
                    .border(2.dp, Brush.linearGradient(listOf(Color(0xFF00FFFF), Color(0xFFFF00FF))), RoundedCornerShape(32.dp))
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    "BẢNG XẾP HẠNG",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    itemsIndexed(sortedResults) { index, user ->
                        LeaderboardItem(index + 1, user)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Close Button
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FFFF).copy(alpha = 0.2f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FFFF)),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Hoàn thành", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(rank: Int, user: PartyUser) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.Gray
    }
    
    val scoreRank = when {
        user.lastScore >= 4500 -> "SSS"
        user.lastScore >= 4000 -> "SS"
        user.lastScore >= 3000 -> "S"
        user.lastScore >= 2000 -> "A"
        else -> "B"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF333333))
            .padding(12.dp)
    ) {
        // Rank
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(rankColor.copy(alpha = 0.2f))
                .border(2.dp, rankColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$rank", color = rankColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Avatar
        Text(user.avatar, fontSize = 32.sp)
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Name & Grade
        Column(modifier = Modifier.weight(1f)) {
            Text(user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Rank: $scoreRank", color = rankColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        
        // Score
        Text(
            "${user.lastScore}",
            color = Color(0xFF00FFFF),
            fontWeight = FontWeight.Black,
            fontSize = 24.sp
        )
    }
}
