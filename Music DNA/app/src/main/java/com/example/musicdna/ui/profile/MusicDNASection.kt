package com.example.musicdna.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MusicDNASection() {
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
                .height(300.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            MusicRadarChart() // bạn thay bằng biểu đồ thật sau
        }
    }
}

// Placeholder radar chart
@Composable
fun MusicRadarChart() {
    Text("Radar Chart Here", color = Color.Gray)
}
