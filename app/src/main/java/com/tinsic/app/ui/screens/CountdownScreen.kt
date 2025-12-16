package com.tinsic.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinsic.app.game.model.GameType

@Composable
fun CountdownScreen(gameType: GameType?, countdownTime: Int) {
    // DEBUG: Check if UI receives updates
    SideEffect {
        android.util.Log.d("CountdownScreen", "Recomposed with time: $countdownTime")
    }

    // Pulsing animation for countdown
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Gradient background
    val brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E3A8A), Color(0xFF000000))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Game title
            Text(
                text = gameType?.title ?: "Mini Game",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Cyan
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = gameType?.description ?: "",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Countdown circle
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(if (countdownTime > 0) scale else 1f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4CAF50).copy(alpha = 0.3f),
                                Color(0xFF2E7D32).copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (countdownTime > 0) "$countdownTime" else "GO!",
                    fontSize = if (countdownTime > 0) 80.sp else 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (countdownTime > 0) Color.White else Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Hint text
            if (countdownTime > 0) {
                Text(
                    text = "Chuẩn bị...",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
