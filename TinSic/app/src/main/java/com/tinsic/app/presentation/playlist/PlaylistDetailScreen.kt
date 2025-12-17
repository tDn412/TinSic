package com.tinsic.app.presentation.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tinsic.app.presentation.home.SongListItem
import com.tinsic.app.ui.theme.NeonPurple
import com.tinsic.app.data.model.Song

@Composable
fun PlaylistDetailScreen(
    onBackClick: () -> Unit,
    onPlayPlaylist: (List<Song>, Boolean) -> Unit, // Boolean is shuffle
    onSongClick: (Song, List<Song>) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    playerViewModel: com.tinsic.app.presentation.player.PlayerViewModel,
    onPlayerExpand: () -> Unit
) {
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            com.tinsic.app.presentation.components.MiniPlayer(
                viewModel = playerViewModel,
                onExpand = onPlayerExpand,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = NeonPurple
                )
            } else if (playlist == null) {
                Text(
                    text = "Playlist not found",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp) // Space for MiniPlayer
                ) {
                    // Header
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Cover
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(8.dp),
                                modifier = Modifier.size(200.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.DarkGray)
                                ) {
                                    // Use first song cover as playlist cover if available, or generic
                                    val coverUrl = songs.firstOrNull()?.coverUrl
                                    if (coverUrl != null) {
                                        AsyncImage(
                                            model = coverUrl,
                                            contentDescription = playlist?.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .align(Alignment.Center),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Title
                            Text(
                                text = playlist?.name ?: "",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Stats
                            Text(
                                text = "${songs.size} songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { onPlayPlaylist(songs, false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Play")
                                }
                                
                                OutlinedButton(
                                    onClick = { onPlayPlaylist(songs, true) },
                                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Shuffle")
                                }
                            }
                        }
                    }
                    
                    // Song List
                    items(songs) { song ->
                        SongListItem(
                            song = song, 
                            onClick = { onSongClick(song, songs) }
                        )
                    }
                }
            }
        }
    }
}
