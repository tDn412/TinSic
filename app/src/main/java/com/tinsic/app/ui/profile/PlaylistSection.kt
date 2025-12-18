package com.tinsic.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinsic.app.data.model.Playlist

@Composable
fun PlaylistSection(
    playlists: List<Playlist>,
    onPlaylistClick: (String, String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Playlists",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistItem(playlist = playlist, onClick = { onPlaylistClick(playlist.id, playlist.name) })
            }
        }
    }
}

@Composable
fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(shape) // Clip content to shape
                .background(Color(0xFF1A1A1A), shape),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.coverUrl.isNotEmpty()) {
                 coil.compose.AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = playlist.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (playlist.id == "liked_songs") {
                // Special styling for Liked Songs
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF8B1FA0), Color(0xFF4A00E0)) // Purple-ish gradient like Header
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                         androidx.compose.material.icons.Icons.Default.Favorite,
                        contentDescription = "Liked Songs",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                // Default placeholder
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp)
        )
        Text(
            text = "${playlist.songIds.size} songs",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
