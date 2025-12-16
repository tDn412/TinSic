package com.tinsic.app.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tinsic.app.game.model.Question

@Composable
fun MusicPreviewScreen(
    currentQuestion: Question?,
    songTitle: String,
    onMusicFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentPosition by remember { mutableStateOf(0f) }

    // SIMPLE ExoPlayer - Same config as AppModule
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                volume = 0.8f  // Prevent clipping
                
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("MusicPreview", "Error: ${error.message}")
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            onMusicFinished()
                        }
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

    LaunchedEffect(currentQuestion) {
        if (currentQuestion != null) {
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                val audioUrl = currentQuestion.musicUrl ?: currentQuestion.content
                val mediaItem = MediaItem.fromUri(audioUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                currentPosition = 0f
                
                Log.d("MusicPreview", "Playing: $audioUrl")
            } catch (e: Exception) {
                Log.e("MusicPreview", "Error loading: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // UI
    val brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF2E003E), Color(0xFF000000))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Music Icon
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1DB954),
                                Color(0xFF191414)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎵",
                    fontSize = 80.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Song Title
            Text(
                text = songTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "🎵 Đang phát nhạc...",
                fontSize = 16.sp,
                color = Color(0xFF1DB954),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                val duration = if (exoPlayer.duration > 0) exoPlayer.duration.toFloat() else 1f
                val progress = if (duration > 0) currentPosition / duration else 0f

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
                        text = formatTime(currentPosition.toLong()),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(if (exoPlayer.duration > 0) exoPlayer.duration else 0),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
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
