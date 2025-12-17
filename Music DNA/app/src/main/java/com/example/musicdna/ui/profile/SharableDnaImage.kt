package com.example.musicdna.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicdna.model.MusicDnaProfile
import com.example.musicdna.model.User

/**
 * Composable này chỉ dùng để làm "khuôn" cho ảnh chia sẻ.
 * Nó sẽ không bao giờ được gọi trực tiếp trong cây giao diện chính.
 */
@Composable
fun SharableDnaImage(dnaProfile: MusicDnaProfile, user: User) {
    // Box ngoài cùng để đảm bảo Composable lấp đầy kích thước được cung cấp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Nền tối
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header
            Text("Music DNA của ${user.name}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // 2. Biểu đồ Radar
            // Lưu ý: RadarChart và TopItemsList cần được public (không có private)
            RadarChart(
                dnaData = dnaProfile.genreDistribution,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 3. Top Lists
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    TopItemsList(title = "Top Artists", items = dnaProfile.topArtists)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    TopItemsList(title = "Top Markets", items = dnaProfile.topCountries)
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Đẩy footer xuống dưới

            // 4. Footer
            Text("Tạo bởi Music DNA App", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
