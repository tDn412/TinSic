package com.example.musicdna.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicdna.analytics.AnalyticsEngine
import com.example.musicdna.data.dummyListeningHistory
import com.example.musicdna.data.dummyMusicList
import com.example.musicdna.model.MusicGenre
import com.example.musicdna.model.User

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

    // =======================================================================
    // BƯỚC 4.1: TÍNH TOÁN DỮ LIỆU DNA MỘT LẦN DUY NHẤT Ở ĐÂY
    // =======================================================================
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
        // BƯỚC 4.2: CẬP NHẬT CÁC SECTION ĐỂ NHẬN DỮ LIỆU TỪ `dnaProfile`
        // =======================================================================
        MusicDNASection(genreData = dnaProfile.genreDistribution)
        Spacer(modifier = Modifier.height(24.dp))

        TopArtistsAndCountriesSection(
            topArtists = dnaProfile.topArtists,
            topCountries = dnaProfile.topCountries
        )
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


// =======================================================================
// BƯỚC 4.3: ĐỊNH NGHĨA COMPOSABLE MỚI VÀ CẬP NHẬT COMPOSABLE CŨ
// =======================================================================

/**
 * Composable hiển thị biểu đồ Radar.
 * QUAN TRỌNG: Chữ ký hàm đã thay đổi để nhận `genreData` từ bên ngoài.
 */
@Composable
fun MusicDNASection(genreData: Map<MusicGenre, Float>) {
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
                .background(Color(0xFF1A1A1A), RoundedCornerShape(20.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // RadarChart giờ sử dụng dữ liệu được truyền vào
            RadarChart(
                dnaData = genreData,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }
    }
}

/**
 * Composable mới để hiển thị Top 5 nghệ sĩ và Top 5 thị trường.
 */
@Composable
fun TopArtistsAndCountriesSection(
    topArtists: List<Pair<String, Int>>,
    topCountries: List<Pair<String, Int>>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cột Top Nghệ sĩ
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Top Artists",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            topArtists.forEachIndexed { index, artist ->
                Text(
                    text = "${index + 1}. ${artist.first} (${artist.second})",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        // Cột Top Quốc gia
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Top Markets",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            topCountries.forEachIndexed { index, country ->
                Text(
                    text = "${index + 1}. ${country.first} (${country.second})",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}


@Preview(showBackground = true, name = "Profile Screen Preview", backgroundColor = 0xFF0A0A0A)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen()
}
