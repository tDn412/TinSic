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
            // Header (Down Arrow + Title + Options)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.KeyboardArrowDown, "Dismiss", tint = Color.White)
                }
                
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                // Empty box to balance layout if needed, or just nothing.
                Box(modifier = Modifier.size(48.dp)) // Placeholder to keep title centered via SpaceBetween
            }

            if (!showLyrics) {
                Spacer(modifier = Modifier.weight(0.5f))
            }

            // Artwork (Large Rounded Square)
            if (!showLyrics) {
                AsyncImage(
                    model = currentSong?.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(320.dp) // Large Size
                        .clip(RoundedCornerShape(12.dp))
                        .shadow(16.dp, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Mini Lyrics View (if wanted in place of art) or Full Lyrics
                // User asked for Lyrics like screenshot (Full screen text). 
                // We handle full screen lyrics below as an overlay or here.
                // Let's keep Artwork here, but if lyrics are ON, we show Lyric View instead of Artwork + Controls
            }

            // If Lyrics Mode is ON, we replace the middle/bottom section, or overlay it.
            // Based on screenshot, Lyrics mode is a separate Full View.
            
            if (showLyrics) {
                 LyricsView(
                    lyrics = lyrics,
                    currentIndex = currentLyricIndex,
                    onDismiss = { showLyrics = false },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.height(48.dp))

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
                    IconButton(onClick = { viewModel.playNext() }) {
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

                Spacer(modifier = Modifier.weight(0.5f))

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

                    // Placeholder for future features or just remove
                    Spacer(modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
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
        PlaylistSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showPlaylistDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSelectionDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val playlists by viewModel.userPlaylists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        viewModel.createPlaylist(newPlaylistName, addCurrentSong = true)
                        showCreateDialog = false
                        onDismiss()
                    }
                }) {
                    Text("Create & Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Add to Playlist", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(16.dp))
            
            // Create New Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreateDialog = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, "New", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text("New Playlist", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            
            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
            
            // Existing Playlists
            LazyColumn {
                itemsIndexed(playlists) { _, playlist ->
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                viewModel.addToPlaylist(playlist.id)
                                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cover placeholder
                         Box(
                            modifier = Modifier.size(48.dp).background(Color.DarkGray, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QueueMusic, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(playlist.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text("${playlist.songIds.size} songs", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun LyricsView(
    lyrics: List<com.tinsic.app.data.model.LyricLine>,
    currentIndex: Int,
    onDismiss: () -> Unit,
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
         LazyColumn(
             state = listState,
             modifier = Modifier.weight(1f).fillMaxWidth(),
             contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
             horizontalAlignment = Alignment.CenterHorizontally
         ) {
             itemsIndexed(lyrics) { index, lyric ->
                 val isCurrent = index == currentIndex
                 Text(
                     text = lyric.text,
                     style = MaterialTheme.typography.headlineMedium,
                     fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                     color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.3f),
                     textAlign = TextAlign.Center,
                     modifier = Modifier
                         .padding(vertical = 12.dp, horizontal = 24.dp)
                         .fillMaxWidth()
                         .clickable { /* Seek to lyric time? */ }
                 )
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
