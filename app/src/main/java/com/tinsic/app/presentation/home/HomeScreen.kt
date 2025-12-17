package com.tinsic.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tinsic.app.data.model.Song
import com.tinsic.app.ui.theme.CardBackground
import com.tinsic.app.ui.theme.NeonPurple
import androidx.compose.ui.graphics.Color

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onSongClick: (Song, List<Song>) -> Unit,
    onProfileClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val songs by viewModel.songs.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        HomeHeader(onProfileClick = onProfileClick, onHistoryClick = onHistoryClick)

        // Filter (bo phan nay)
        LazyRow(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(viewModel.genres) { genre ->
                FilterChip(
                    selected = genre == selectedGenre,
                    onClick = { viewModel.filterByGenre(genre) },
                    label = { Text(genre) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPurple,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonPurple)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Quick Picks Section
                item {
                    SectionTitle("Quick Picks")
                }
                
                val quickPicks = viewModel.getQuickPicks()
                items(quickPicks) { song ->
                    SongListItem(song = song, onClick = { onSongClick(song, quickPicks) })
                }

                // Keep Listening Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionTitle("Keep Listening")
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val keepListening = viewModel.getKeepListening()
                        items(keepListening) { song ->
                            KeepListeningCard(song = song, onClick = { onSongClick(song, keepListening) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(onProfileClick: () -> Unit, onHistoryClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onHistoryClick) {
                Icon(
                    Icons.Default.History,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onProfileClick),
                shape = CircleShape,
                color = NeonPurple
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun SongListItem(song: Song, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = CardBackground,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(song.coverUrl)
                    .crossfade(true)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build(),
                contentDescription = song.title,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray), // Placeholder background
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Song Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // More Options
            IconButton(onClick = { /* Show options */ }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun KeepListeningCard(song: Song, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        tonalElevation = 4.dp
    ) {
        Column {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(song.coverUrl)
                    .crossfade(true)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build(),
                contentDescription = song.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color.DarkGray), // Placeholder background
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}
