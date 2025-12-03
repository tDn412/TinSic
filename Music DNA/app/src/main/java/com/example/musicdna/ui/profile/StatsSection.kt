package com.example.musicdna.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope // Import RowScope
import androidx.compose.foundation.layout.fillMaxWidth
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
fun StatsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Apply the weight modifier here
        StatCard("1,247", "Songs Liked", Color(0xFF8B1FA0), modifier = Modifier.weight(1f))
        StatCard("89", "Parties Joined", Color(0xFF0D47A1), modifier = Modifier.weight(1f))
    }
}

@Composable
fun RowScope.StatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    // Use the passed-in modifier
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
