package com.example.musicdna.ui.profile

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicdna.analytics.AnalyticsEngine
import com.example.musicdna.data.dummyListeningHistory
import com.example.musicdna.data.dummyMusicList
import com.example.musicdna.model.MusicDnaProfile
import com.example.musicdna.model.User
import com.example.musicdna.model.MusicGenre

// Enum để quản lý các view một cách an toàn và rõ ràng
private enum class DnaView {
    RADAR, ARTISTS, COUNTRIES
}

@Composable
fun ProfileScreen() {
    val scrollState = rememberScrollState()

    var user by remember {
        mutableStateOf(
            User(
                userId = "uid123",
                name = "Alex Johnson",
                email = "alex.j@example.com",
                password = "password123",
                avatarUrl = null
            )
        )
    }
    var showEditDialog by remember { mutableStateOf(false) }

    val dnaProfile = remember {
        AnalyticsEngine.calculateDnaProfile(dummyListeningHistory, dummyMusicList)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFF0A0A0A))
            .padding(bottom = 80.dp)
    ) {
        HeaderSection(user = user, onEditProfileClick = { showEditDialog = true })
        Spacer(modifier = Modifier.height(16.dp))

        StatsSection(listeningHistory = dummyListeningHistory)
        Spacer(modifier = Modifier.height(24.dp))

        // =======================================================================
        // THAY ĐỔI LỚN: Gọi MusicDNASection mới và truyền toàn bộ dnaProfile.
        // Xóa bỏ hoàn toàn lời gọi TopArtistsAndCountriesSection cũ.
        // =======================================================================
        MusicDNASection(dnaProfile = dnaProfile)
        Spacer(modifier = Modifier.height(24.dp))

        AchievementSection()
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showEditDialog) {
        EditProfileDialog(
            user = user,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail, newPassword ->
                val updatedUser = user.copy(name = newName, email = newEmail)
                if (newPassword != null) {
                    user = updatedUser.copy(password = newPassword)
                } else {
                    user = updatedUser
                }
                showEditDialog = false
            }
        )
    }
}

/**
 * Composable MusicDNASection đã được tái cấu trúc hoàn toàn.
 * Giờ đây nó nhận toàn bộ dnaProfile và tự quản lý việc hiển thị các view khác nhau.
 */
@Composable
fun MusicDNASection(dnaProfile: MusicDnaProfile) {
    // 1. State để quản lý view hiện tại, mặc định là biểu đồ RADAR
    var currentView by remember { mutableStateOf(DnaView.RADAR) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Hàng chứa tiêu đề và các nút chuyển đổi
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
            // 2. Giao diện chuyển đổi (Switcher)
            DnaViewSwitcher(
                selectedView = currentView,
                onViewSelected = { newView -> currentView = newView }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Box nền chính
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), RoundedCornerShape(20.dp))
                .padding(16.dp)
                .animateContentSize(), // Thêm hiệu ứng chuyển động mượt mà khi đổi view
            contentAlignment = Alignment.Center
        ) {
            // 3. Hiển thị nội dung động dựa trên state
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

/**
 * Composable tái sử dụng để hiển thị danh sách Top 5 (Nghệ sĩ hoặc Quốc gia).
 */
@Composable
private fun TopItemsList(title: String, items: List<Pair<String, Int>>) {
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
        // Nếu không có dữ liệu, hiển thị thông báo
        if (items.isEmpty()){
            Text(
                "Not enough data yet.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
            )
        } else {
            items.forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}. ${item.first} (${item.second} songs)",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Profile Screen Preview", backgroundColor = 0xFF0A0A0A)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen()
}