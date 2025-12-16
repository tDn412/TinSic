package com.tinsic.app.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tinsic.app.game.model.GameType
import com.tinsic.app.game.model.Question
import com.tinsic.app.game.viewmodel.GameUiState

// Custom Pause Icon composable
@Composable
fun PauseIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(24.dp)
                .background(tint, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(24.dp)
                .background(tint, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun PlayScreen(
    uiState: GameUiState,
    currentQuestion: Question?,
    onSubmitAnswer: (Int) -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0f) }

    // SIMPLE ExoPlayer - Same as AppModule (no complex config to avoid distortion)
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                volume = 0.8f  // Prevent clipping
                
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("GamePlayScreen", "Error: ${error.message}")
                    }
                    
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                })
            }
    }

    // Update progress
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition.toFloat()
            }
            kotlinx.coroutines.delay(100)
        }
    }

    // Music playback logic
    LaunchedEffect(currentQuestion, uiState.isMusicPreviewPhase, uiState.isAnswerRevealed) {
        if (currentQuestion != null && currentQuestion.type == GameType.GUESS_THE_SONG) {
            // Only GUESS_THE_SONG plays music during music preview phase
            if (uiState.isMusicPreviewPhase && !uiState.isAnswerRevealed) {
                try {
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    val clippingConfiguration = MediaItem.ClippingConfiguration.Builder().setEndPositionMs(10000).build(); val mediaItem = MediaItem.Builder().setUri(currentQuestion.content).setClippingConfiguration(clippingConfiguration).build()
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                    currentPosition = 0f
                } catch (e: Exception) { e.printStackTrace() }
            } else if (!uiState.isMusicPreviewPhase && !uiState.isAnswerRevealed) {
                // Stop music when entering answer phase
                exoPlayer.stop()
            } else if (uiState.isAnswerRevealed) {
                // Stop music when answer is revealed
                exoPlayer.stop()
            }
        } else {
            exoPlayer.stop()
        }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    // Background Gradient
    val brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF2E003E), Color(0xFF000000))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Score: ${uiState.score}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (uiState.streak > 1) {
                        Text("🔥 Streak: ${uiState.streak}", color = Color(0xFFFF9800), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Circular Timer
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = uiState.timeLeft / 10f,
                        modifier = Modifier.size(50.dp),
                        color = if (uiState.timeLeft < 4) Color.Red else Color.Green,
                        trackColor = Color.Gray.copy(alpha = 0.3f),
                    )
                    Text(
                        text = "${uiState.timeLeft}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show "Listening to music..." message during music preview phase
            if (uiState.isMusicPreviewPhase) {
                Text(
                    text = "🎧 Đang nghe nhạc...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1DB954),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Question Content - Music Player UI for GUESS_THE_SONG only
            if (currentQuestion?.type == GameType.GUESS_THE_SONG) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Question Title
                    Text(
                        text = "Đây là bài hát nào?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Music Player Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Play/Pause Button (only show during music preview phase)
                                if (uiState.isMusicPreviewPhase) {
                                    IconButton(
                                        onClick = {
                                            if (exoPlayer.isPlaying) {
                                                exoPlayer.pause()
                                            } else {
                                                exoPlayer.play()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF1DB954),
                                                        Color(0xFF1AA34A)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    ) {
                                        if (isPlaying) {
                                            PauseIcon(
                                                modifier = Modifier.size(20.dp),
                                                tint = Color.White
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    
                                    // Progress Bar + Time
                                    Column(modifier = Modifier.weight(1f)) {
                                        val duration = 10000f
                                        val cappedPosition = currentPosition.coerceAtMost(10000f); val progress = if (duration > 0) cappedPosition / duration else 0f
                                        
                                        LinearProgressIndicator(
                                            progress = progress.coerceIn(0f, 1f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = Color(0xFF1DB954),
                                            trackColor = Color.White.copy(alpha = 0.2f)
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                         Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = formatTime(currentPosition.toLong().coerceAtMost(10000)),
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "00:10",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else if (currentQuestion?.type == GameType.FINISH_THE_LYRICS) {
                // FINISH_THE_LYRICS - Only show title and lyrics (no music player)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Question Title
                    Text(
                        text = "Điền từ còn thiếu",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Lyrics Card (without music player)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Display lyrics directly (user has already added ___ in the text)
                            val lyrics = currentQuestion.lyrics ?: ""
                            
                            Text(
                                text = lyrics,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else if (currentQuestion?.type == GameType.MUSIC_CODE) {
                // MUSIC_CODE - Display emoji clues
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Question Title
                    Text(
                        text = "🎵 Emoji này thuộc bài hát nào?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Emoji Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(24.dp)
                        ) {
                            Text(
                                text = currentQuestion.content,
                                color = Color.White,
                                fontSize = 48.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                // Original UI for LYRICS_FLIP
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📝 Lời bài hát này của bài nào?", color = Color.Cyan, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text(
                                text = currentQuestion?.content ?: "",
                                color = Color.White,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Answer Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                currentQuestion?.options?.forEachIndexed { index, option ->
                    val isSelected = uiState.selectedAnswerIndex == index
                    val isCorrect = currentQuestion.correctAnswerIndex == index

                    val backgroundColor = when {
                        uiState.isAnswerRevealed && isCorrect -> Color(0xFF4CAF50)
                        uiState.isAnswerRevealed && isSelected && !isCorrect -> Color(0xFFE53935)
                        uiState.isAnswerLocked && isSelected -> Color(0xFF1976D2)
                        else -> Color.White.copy(alpha = 0.1f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable(enabled = !uiState.isAnswerLocked && !uiState.isMusicPreviewPhase) {
                                onSubmitAnswer(index)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                option, 
                                fontSize = 18.sp, 
                                color = Color.White, 
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            
                            if (uiState.isAnswerRevealed) {
                                if (isCorrect) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Correct",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Wrong",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / 1000) / 60
    return String.format("%02d:%02d", minutes, seconds)
}

