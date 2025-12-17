package com.tinsic.app.presentation.discover

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.tinsic.app.data.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Create a separate MediaPlayer for previews (not using PlayerViewModel!)
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    
    // Auto-play 15-second preview when song changes
    LaunchedEffect(currentSong?.id) {
        currentSong?.let { song ->
            // Release old player
            mediaPlayer?.release()
            
            // Create new player for preview
            mediaPlayer = android.media.MediaPlayer().apply {
                try {
                    setDataSource(song.audioUrl)
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        mp.start()
                        // Auto-stop after 15 seconds
                        launch {
                            delay(15000)
                            mp.pause()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (currentSong != null) {
            SwipeableCard(
                song = currentSong!!,
                onLike = { 
                    mediaPlayer?.stop()
                    viewModel.likeSong(it) 
                },
                onDislike = { 
                    mediaPlayer?.stop()
                    viewModel.dislikeSong(it) 
                }
            )
        } else {
            Text(
                text = "No more songs to discover!",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
fun SwipeableCard(
    song: Song,
    onLike: (Song) -> Unit,
    onDislike: (Song) -> Unit
) {
    var offset by remember { mutableStateOf(IntOffset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val screenWidth = LocalConfiguration.current.screenWidthDp.toFloat()
    val swipeThreshold = 200f

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(0.75f)
                .offset { offset }
                .graphicsLayer {
                    rotationZ = (offset.x / 20f)
                    alpha = 1f - (abs(offset.x.toFloat()) / (screenWidth * 2))
                }
                .pointerInput(song.id) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            
                            when {
                                offset.x > swipeThreshold -> {
                                    // Swipe right - Like
                                    scope.launch {
                                        offset = IntOffset((screenWidth * 2).toInt(), 0)
                                        delay(200)
                                        onLike(song)
                                        offset = IntOffset.Zero
                                    }
                                }
                                offset.x < -swipeThreshold -> {
                                    // Swipe left - Dislike
                                    scope.launch {
                                        offset = IntOffset(-(screenWidth * 2).toInt(), 0)
                                        delay(200)
                                        onDislike(song)
                                        offset = IntOffset.Zero
                                    }
                                }
                                else -> {
                                    // Reset position with spring animation
                                    scope.launch {
                                        androidx.compose.animation
                                            .core
                                            .Animatable(offset.x.toFloat())
                                            .animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            ) {
                                                offset = IntOffset(value.toInt(), 0)
                                            }
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offset = IntOffset(
                                (offset.x + dragAmount.x.toInt()).coerceIn(
                                    -(screenWidth * 2).toInt(),
                                    (screenWidth * 2).toInt()
                                ),
                                0
                            )
                        }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Album Art
                Image(
                    painter = rememberAsyncImagePainter(song.coverUrl),
                    contentDescription = song.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentScale = ContentScale.Crop
                )
                
                // Song Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.genre,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Like/Dislike indicators during swipe
            if (abs(offset.x.toFloat()) > 50) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = if (offset.x > 0) Alignment.TopEnd else Alignment.TopStart
                ) {
                    Icon(
                        imageVector = if (offset.x > 0) Icons.Default.Favorite else Icons.Default.Close,
                        contentDescription = if (offset.x > 0) "Like" else "Dislike",
                        tint = if (offset.x > 0) Color(0xFFE91E63) else Color(0xFFF44336),
                        modifier = Modifier
                            .size(64.dp)
                            .alpha((abs(offset.x.toFloat()) / swipeThreshold).coerceIn(0f, 1f))
                    )
                }
            }
        }
        
        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Dislike Button
            FloatingActionButton(
                onClick = { onDislike(song) },
                containerColor = Color(0xFF2C2C2C)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dislike",
                    tint = Color(0xFFF44336)
                )
            }
            
            // Like Button
            FloatingActionButton(
                onClick = { onLike(song) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Like",
                    tint = Color.White
                )
            }
        }
    }
}
