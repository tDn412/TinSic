package com.tinsic.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinsic.app.game.model.GameType

@Composable
fun ReadyScreen(gameType: GameType?, onStartClick: () -> Unit, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Bạn đã sẵn sàng?", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Chế độ chơi:", color = Color.Gray)
        Text(text = gameType?.title ?: "", fontSize = 24.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = onStartClick, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("BẮT ĐẦU NGAY", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
            Text("Quay lại")
        }
    }
}