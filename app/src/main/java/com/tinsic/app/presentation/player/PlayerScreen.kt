package com.tinsic.app.presentation.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tinsic.app.ui.theme.NeonPink
import com.tinsic.app.ui.theme.NeonPurple
import com.tinsic.app.data.model.LyricLine
import kotlinx.coroutines.launch

private typealias RepeatMode = PlayerViewModel.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()

    var showLyrics by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    
    // Smooth animation for drag
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "drag"
    )

    // Bottom Sheet State
    val sleepTimerSheetState = rememberModalBottomSheetState()
    val queueSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = animatedOffset.coerceAtLeast(0f)
                alpha = (1f - (animatedOffset / 1000f)).coerceIn(0.5f, 1f)
            }
            .background(Color.Black) // Premium Dark Background
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 200f) onDismiss() else dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f }
                ) { _, dragAmount ->
                    if (dragAmount > 0) dragOffset += dragAmount
                }
            }
    ) {
        // blurred background effect (simulated with gradient overlay)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C2C2C),
                            Color.Black
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header (Title + Options)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center, // Centered Title since arrow is gone
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Arrow removed as requested
                
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Crossfade between Artwork+Controls and LyricsView
            androidx.compose.animation.Crossfade(targetState = showLyrics, label = "LyricsTransition") { isLyricsVisible ->
                if (isLyricsVisible) {
                     val isTranslationEnabled by viewModel.isTranslationEnabled.collectAsState()
                     val isTranslating by viewModel.isTranslating.collectAsState()
                     val isEligible = viewModel.isTranslationEligible()
                     
                     LyricsView(
                        lyrics = lyrics,
                        currentIndex = currentLyricIndex,
                        onDismiss = { showLyrics = false },
                        isTranslationEnabled = isTranslationEnabled,
                        isTranslating = isTranslating,
                        isTranslationEligible = isEligible,
                        onToggleTranslation = { viewModel.toggleTranslation() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Artwork
                        AsyncImage(
                            model = currentSong?.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f) // Fill available space
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .shadow(16.dp, RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        Spacer(modifier = Modifier.height(24.dp))

                // Song Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong?.title ?: "No Song",
                            style = MaterialTheme.typography.headlineSmall, // Bigger Title
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentSong?.artist ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                    
                    // Share (Optional, can keep or remove)
                    /* 
                    IconButton(onClick = { /* Share */ }) {
                         Icon(Icons.Default.Share, "Share", tint = Color.White)
                    }
                    */
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Seekbar
                 Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Text(formatTime(duration), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                        Icon(
                            Icons.Default.Shuffle,
                            "Shuffle",
                            tint = if (isShuffleEnabled) NeonPurple else Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Prev
                    IconButton(onClick = { viewModel.playPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    
                    // PLAY/PAUSE (Circle)
                    Box(
                         modifier = Modifier
                             .size(72.dp)
                             .clip(CircleShape)
                             .background(Color.White)
                             .clickable { viewModel.playPause() },
                         contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
                    // Next
                    IconButton(onClick = { viewModel.playNext(auto = false) }) {
                        Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    
                    // Repeat
                    IconButton(onClick = { viewModel.toggleRepeat() }) {
                         Icon(
                            when(repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                             "Repeat",
                             tint = if (repeatMode != RepeatMode.OFF) NeonPurple else Color.White.copy(alpha = 0.7f)
                         )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bottom Tools (Lyrics, Sleep, Queue)
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                     IconButton(onClick = { 
                         if (lyrics.isNotEmpty()) showLyrics = true
                         else android.widget.Toast.makeText(context, "No lyrics", android.widget.Toast.LENGTH_SHORT).show()
                     }) {
                         Icon(Icons.Default.Lyrics, "Lyrics", tint = Color.White)
                     }
                    
                     IconButton(onClick = { showSleepTimer = true }) {
                         Icon(Icons.Outlined.Timer, "Sleep Timer", tint = Color.White)
                     }
                    
                    IconButton(onClick = { showQueue = true }) {
                        Icon(Icons.Default.QueueMusic, "Queue", tint = Color.White)
                    }
                     
                    IconButton(onClick = { showPlaylistDialog = true }) {
                        Icon(Icons.Default.PlaylistAdd, "Add to Playlist", tint = Color.White)
                    }

                    IconButton(onClick = { viewModel.toggleLikeCurrentSong() }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) NeonPurple else Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
        }
    }
    
    // Sleep Timer Sheet
    if (showSleepTimer) {
        ModalBottomSheet(
            onDismissRequest = { showSleepTimer = false },
            sheetState = sleepTimerSheetState,
            containerColor = Color(0xFF1E1E1E)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Sleep Timer", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(Modifier.height(16.dp))
                listOf(5, 10, 15, 30, 45, 60).forEach { mins ->
                    TextButton(
                        onClick = { 
                            viewModel.setSleepTimer(mins)
                            scope.launch { sleepTimerSheetState.hide() }.invokeOnCompletion { showSleepTimer = false }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stop audio in $mins minutes", color = Color.White)
                    }
                }
                TextButton(
                    onClick = { 
                        viewModel.cancelSleepTimer()
                        scope.launch { sleepTimerSheetState.hide() }.invokeOnCompletion { showSleepTimer = false }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Turn off Timer", color = NeonPink)
                }
            }
        }
    }
    
    // Queue Sheet
    if (showQueue) {
        ModalBottomSheet(
             onDismissRequest = { showQueue = false },
             sheetState = queueSheetState,
             containerColor = Color(0xFF1E1E1E)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Playing Queue", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                
                // Status Indicator
                val shuffleText = if (isShuffleEnabled) "Shuffle: ON" else "Shuffle: OFF"
                val repeatText = when(repeatMode) {
                    RepeatMode.OFF -> "Repeat: OFF"
                    RepeatMode.ONE -> "Repeat: ONE"
                    RepeatMode.ALL -> "Repeat: ALL"
                }
                Text(
                    text = "$shuffleText | $repeatText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonPurple,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(16.dp))
                LazyColumn {
                     itemsIndexed(playlist) { index, song ->
                         Row(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .padding(vertical = 8.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             AsyncImage(
                                 model = song.coverUrl,
                                 contentDescription = null,
                                 modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
                             )
                             Spacer(Modifier.width(12.dp))
                             Column(Modifier.weight(1f)) {
                                 Text(song.title, color = if (song == currentSong) NeonPurple else Color.White, maxLines = 1)
                                 Text(song.artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                             }
                             IconButton(onClick = { viewModel.removeFromQueue(index) }) {
                                 Icon(Icons.Default.Close, "Remove", tint = Color.Gray)
                             }
                         }
                     }
                }
            }
        }
    }

    if (showPlaylistDialog) {
        val userPlaylists by viewModel.userPlaylists.collectAsState()
        com.tinsic.app.presentation.components.PlaylistSelectionDialog(
            playlists = userPlaylists,
            onDismiss = { showPlaylistDialog = false },
            onPlaylistSelected = { playlistId -> viewModel.addToPlaylist(playlistId) },
            onCreatePlaylist = { name -> viewModel.createPlaylist(name, addCurrentSong = true) }
        )
    }
}


@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    isTranslationEnabled: Boolean,
    isTranslating: Boolean,
    isTranslationEligible: Boolean,
    onToggleTranslation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll logic
    LaunchedEffect(currentIndex) {
        if (currentIndex > 0) {
             listState.animateScrollToItem(currentIndex, scrollOffset = -200) // Centering hack
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
         // Header for Lyrics View
         Row(
             modifier = Modifier
                 .fillMaxWidth()
                 .padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
             horizontalArrangement = Arrangement.SpaceBetween,
             verticalAlignment = Alignment.CenterVertically
         ) {
             IconButton(onClick = onDismiss) {
                 Icon(Icons.Default.KeyboardArrowDown, "Dismiss", tint = Color.White)
             }
             
             if (isTranslationEligible) {
                 if (isTranslating) {
                     CircularProgressIndicator(
                         modifier = Modifier.size(24.dp).padding(4.dp),
                         color = NeonPurple,
                         strokeWidth = 2.dp
                     )
                 } else {
                     IconButton(onClick = onToggleTranslation) {
                        Icon(
                             Icons.Default.Translate, 
                             "Translate", 
                             tint = if (isTranslationEnabled) NeonPurple else Color.White
                        )
                     }
                 }
             } else {
                 Spacer(modifier = Modifier.size(48.dp))
             }
         }

         LazyColumn(
             state = listState,
             modifier = Modifier.weight(1f).fillMaxWidth(),
             contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
             horizontalAlignment = Alignment.CenterHorizontally
         ) {
             itemsIndexed(lyrics) { index, lyric ->
                 val isCurrent = index == currentIndex
                 Column(
                     modifier = Modifier
                         .padding(vertical = 12.dp, horizontal = 24.dp)
                         .fillMaxWidth()
                         .clickable { /* Seek to lyric time? */ },
                     horizontalAlignment = Alignment.CenterHorizontally
                 ) {
                     // Original Text
                     Text(
                         text = lyric.text,
                         style = MaterialTheme.typography.headlineMedium,
                         fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                         color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.3f),
                         textAlign = TextAlign.Center
                     )
                     
                     // Translated Text (if enabled and exists)
                     if (isTranslationEnabled && lyric.translation.isNotBlank()) {
                         Spacer(modifier = Modifier.height(4.dp))
                         Text(
                             text = lyric.translation,
                             style = MaterialTheme.typography.titleMedium,
                             fontWeight = FontWeight.Normal,
                             color = if (isCurrent) NeonPurple else NeonPurple.copy(alpha = 0.6f),
                             textAlign = TextAlign.Center,
                             fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                         )
                     }
                 }
             }
         }
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
