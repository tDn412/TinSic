package com.example.musicdna.ui.profile

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
import com.example.musicdna.model.ListeningHistory
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StatsSection(
    // 1. Nhận vào danh sách lịch sử nghe nhạc
    listeningHistory: List<ListeningHistory>
) {
    // 2. Tính toán số lượng bài hát yêu thích từ dữ liệu được truyền vào
    val songsLikedCount = remember(listeningHistory) {
        listeningHistory.count { it.isFavourite }
    }

    // 3. Định dạng số để có dấu phẩy (ví dụ: 1,247), giúp UI đẹp hơn
    val formattedSongsLiked = remember(songsLikedCount) {
        NumberFormat.getNumberInstance(Locale.US).format(songsLikedCount)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        // Dùng SpaceAround để khoảng cách giữa các card đều nhau và đẹp hơn
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // 4. Sử dụng giá trị đã tính toán thay vì hardcode
        StatCard(
            value = formattedSongsLiked,
            label = "Songs Liked",
            color = Color(0xFF8B1FA0),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = "89", // Giữ nguyên giá trị giả lập cho "Parties Joined"
            label = "Parties Joined",
            color = Color(0xFF0D47A1),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RowScope.StatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    // Cấu trúc của StatCard không thay đổi
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
