package com.tinsic.app.presentation.karaoke

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tinsic.app.presentation.karaoke.components.PitchVisualizer

@Composable
fun KaraokeScreen(
    viewModel: KaraokeViewModel = hiltViewModel(),
    onStopRequested: (() -> Unit)? = null  // Callback when user wants to stop (for Party Mode)
) {
    // Collect State from ViewModel
    val state by viewModel.uiState.collectAsState()
    val latencyOffset by viewModel.latencyOffset.collectAsState()
    val context = LocalContext.current
    
    // State for stop confirmation dialog
    var showStopDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (state.isRecording) {
                    viewModel.stopSinging()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission granted, but toggleRecording is removed for Party Mode
        // In Party Mode, karaoke starts automatically via startSinging()
    }

    // --- BACKGROUND GRADIENT (GRAVITY THEME) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF000000), Color(0xFF1A0033), Color(0xFF4A148C)) // Deep Space
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP: SCORE BAR
            ScoreProgressBar(currentScore = state.currentScore)

            Spacer(modifier = Modifier.height(10.dp))

            // 2. MIDDLE-TOP: VISUALIZER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                border = BorderStroke(2.dp, Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFD500F9))))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.songNotes.isNotEmpty()) {
                        PitchVisualizer(
                            currentTime = state.currentTime,
                            songNotes = state.songNotes,
                            userPitchHistory = state.userPitchHistory
                        )
                    } else {
                        Text("Loading...", color = Color.White)
                    }
                }
            }

            // 3. MIDDLE-BOTTOM: LYRIC BOX
            val lyricList = state.lyrics
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()

            LaunchedEffect(state.currentTime) {
                val currentIndex = lyricList.indexOfLast { 
                    it.startTime <= state.currentTime + 0.5 
                }
                if (currentIndex >= 0) {
                    listState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0))
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (lyricList.isEmpty()) {
                         Text(
                            text = "Chưa có lời bài hát\n(Thử nhấn nút Play để load nhạc mẫu)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(lyricList.size) { index ->
                                val line = lyricList[index]
                                val isCurrent = if (index < lyricList.size - 1) {
                                    state.currentTime >= line.startTime && state.currentTime < lyricList[index + 1].startTime
                                } else {
                                    state.currentTime >= line.startTime
                                }
                                
                                Text(
                                    text = line.content,
                                    style = if (isCurrent) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                                    color = if (isCurrent) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.3f),
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Only show stop button when recording (Party Mode auto-starts)
                if (state.isRecording) {
                    IconButton(
                        onClick = { showStopDialog = true },  // Show dialog instead of immediate stop
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFFB7185))),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    // Placeholder to maintain layout
                    Spacer(modifier = Modifier.size(64.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(30.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(30.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    IconButton(onClick = { viewModel.decreaseLatency() }) {
                        Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        "${latencyOffset}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { viewModel.increaseLatency() }) {
                        Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        
        // --- STOP CONFIRMATION DIALOG ---
        if (showStopDialog) {
            AlertDialog(
                onDismissRequest = { showStopDialog = false },
                title = {
                    Text(
                        "Dừng hát?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Text("Bạn muốn dừng hát? Tất cả mọi người trong phòng sẽ trở về room.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showStopDialog = false
                            viewModel.stopSinging()  // Stop engine locally
                            onStopRequested?.invoke() // Notify Party Mode to reset state
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444)
                        )
                    ) {
                        Text("Dừng hát", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStopDialog = false }) {
                        Text("Huỷ")
                    }
                }
            )
        }
    }
}

@Composable
fun ScoreProgressBar(currentScore: Int) {
    val maxScore = 5000f 
    val progress = (currentScore / maxScore).coerceIn(0f, 1.0f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(15.dp))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(15.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(listOf(Color(0xFFD500F9), Color(0xFF00E5FF))),
                        shape = RoundedCornerShape(15.dp)
                    )
            )
            
            Text(
                text = "$currentScore",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 4f))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(0.2f))
            Text("A", color = if (progress > 0.25f) Color(0xFF00E5FF) else Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(0.25f))
            Text("S", color = if (progress > 0.5f) Color(0xFF00E5FF) else Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(0.25f))
            Text("SS", color = if (progress > 0.75f) Color(0xFFD500F9) else Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(0.25f))
            Text("SSS", color = if (progress > 0.95f) Color(0xFFFFD700) else Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}
