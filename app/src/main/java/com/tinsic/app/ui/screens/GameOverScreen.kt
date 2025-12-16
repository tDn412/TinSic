package com.tinsic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinsic.app.game.model.PlayerScore

@Composable
fun GameOverView(score: Int, playerScores: List<PlayerScore>, onReplay: () -> Unit) {
    val brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E3A8A), Color(0xFF000000))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎉 KẾT THÚC!",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bảng xếp hạng cuối cùng",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }

            // Final Leaderboard
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(playerScores) { index, player ->
                    FinalRankingItem(rank = index + 1, player = player)
                }
            }

            // Button
            Button(
                onClick = onReplay,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Về Menu Chính", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FinalRankingItem(rank: Int, player: PlayerScore) {
    val backgroundColor = when (rank) {
        1 -> Color(0xFFFFD700).copy(alpha = 0.3f) // Gold
        2 -> Color(0xFFC0C0C0).copy(alpha = 0.3f) // Silver
        3 -> Color(0xFFCD7F32).copy(alpha = 0.3f) // Bronze
        else -> Color.White.copy(alpha = 0.1f)
    }

    val rankEmoji = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "$rank"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (player.isCurrentPlayer) Color(0xFF1976D2).copy(alpha = 0.4f) else backgroundColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (player.isCurrentPlayer) 12.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rank badge
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            color = when (rank) {
                                1 -> Color(0xFFFFD700)
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> Color.Gray
                            }.copy(alpha = 0.4f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rankEmoji,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Player name
                Column {
                    Text(
                        text = player.playerName,
                        fontSize = 20.sp,
                        fontWeight = if (player.isCurrentPlayer) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (player.isCurrentPlayer) Color.Yellow else Color.White
                    )
                    if (player.isCurrentPlayer) {
                        Text(
                            text = "⭐ Điểm của bạn",
                            fontSize = 12.sp,
                            color = Color.Yellow.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Final score
            Text(
                text = "${player.score}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (player.isCurrentPlayer) Color.Yellow else Color.White
            )
        }
    }
}