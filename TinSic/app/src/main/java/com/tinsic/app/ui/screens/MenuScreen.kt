package com.tinsic.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinsic.app.game.model.GameType

@Composable
fun MenuScreen(
    onGameSelected: (GameType) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎮 MINI GAMES", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(32.dp))
        
        // Show error message if any
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text(
                    text = error,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Show loading indicator
        if (isLoading) {
            CircularProgressIndicator(color = Color.Cyan)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Đang tải câu hỏi từ Firebase...", fontSize = 14.sp, color = Color.Gray)
        }

        GameType.values().forEach { type ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable(enabled = !isLoading) { onGameSelected(type) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isLoading) Color(0xFF1C1C1C) else Color(0xFF2C2C2C)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        type.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLoading) Color.DarkGray else Color.Cyan
                    )
                    Text(
                        type.description,
                        fontSize = 14.sp,
                        color = if (isLoading) Color.DarkGray else Color.Gray
                    )
                }
            }
        }
    }
}