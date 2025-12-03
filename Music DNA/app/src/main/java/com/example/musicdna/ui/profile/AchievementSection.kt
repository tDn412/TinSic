package com.example.musicdna.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun AchievementSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {

        Text(
            "Achievements",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        val achievements = listOf(
            Achievement("Đóm Con", "Listened to Jack 100 times", Color(0xFFFFA726), true),
            Achievement("Night Owl", "Active after midnight 30 days", Color(0xFF212121), true),
            Achievement("Party Animal", "Joined 50+ party sessions", Color(0xFF1A1A1A), true),
            Achievement("Music Explorer", "Discovered 500+ songs", Color(0xFF1A1A1A), true),
            Achievement("Karaoke Star", "Complete 100 karaoke sessions", Color(0xFF1A1A1A), false),
            Achievement("Trendsetter", "Share 50 viral songs", Color(0xFF1A1A1A), false)
        )

        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(800.dp)) {
            items(achievements) { AchieveCard(it) }
        }
    }
}

data class Achievement(
    val title: String,
    val description: String,
    val bgColor: Color,
    val earned: Boolean
)

@Composable
fun AchieveCard(item: Achievement) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .height(140.dp)
            .background(item.bgColor.copy(alpha = if (item.earned) 1f else 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {

        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text(item.description, color = Color.LightGray, fontSize = 13.sp)

        Spacer(modifier = Modifier.weight(1f))

        if (item.earned) {
            Text("✓ Earned", color = Color.Green, fontSize = 12.sp)
        } else {
            Text("Locked", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
