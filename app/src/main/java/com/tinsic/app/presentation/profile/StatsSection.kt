package com.tinsic.app.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinsic.app.data.model.HistoryItem
import com.tinsic.app.data.model.User
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StatsSection(
    // 1. SỬA LỖI: Nhận vào đối tượng User hiện tại
    user: User,
    listeningHistory: List<HistoryItem>
) {
    // Tính toán tổng số bài hát đã nghe (đã đúng)
    val totalSongsPlayed = remember(listeningHistory) {
        listeningHistory.size
    }

    // 2. SỬA LỖI: Sử dụng đối tượng 'user' để lấy số lượng bài hát đã thích
    val formattedSongsLiked = remember(user.likedSongs) {
        NumberFormat.getNumberInstance(Locale.US).format(user.likedSongs.size)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatCard(
            // Sử dụng giá trị đã tính toán
            value = formattedSongsLiked,
            label = "Songs Liked",
            color = Color(0xFF8B1FA0),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            // Sử dụng totalSongsPlayed cho "Total Played"
            value = NumberFormat.getNumberInstance(Locale.US).format(totalSongsPlayed),
            label = "Total Played",
            color = Color(0xFF0D47A1),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RowScope.StatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .background(color.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(label, color = Color.Gray, fontSize = 14.sp)
    }
}
