package com.tinsic.app.presentation.party

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.tinsic.app.presentation.party.PartyUser
import com.tinsic.app.presentation.party.PartySong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivePartyRoom(
    roomId: String,
    users: List<PartyUser>,
    stageUsers: List<PartyUser>,
    currentUser: PartyUser,
    queue: List<PartySong>,
    searchQuery: String,
    searchResults: List<com.tinsic.app.data.model.KaraokeSong>, // New Param
    playbackState: String, // NEW: For sync overlay
    startTime: Long, // NEW: For countdown
    partyViewModel: PartyViewModel, // Core party logic
    karaokeController: com.tinsic.app.presentation.party.karaoke.KaraokePartyController, // Karaoke-specific logic
    onSearchQueryChange: (String) -> Unit,
    onRemoveSong: (Int) -> Unit,
    onAddSong: (com.tinsic.app.data.model.KaraokeSong) -> Unit, // New Param
    onLeaveRoom: () -> Unit,
    onToggleStage: () -> Unit,
    onStartSong: (String) -> Unit // Changed: Pass song ID
) {
    var showMembersSheet by remember { mutableStateOf(false) }
    var showPermissionMessage by remember { mutableStateOf(false) }

    // Permission handling for microphone access
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed to join stage
            onToggleStage()
        } else {
            // Permission denied, show message
            showPermissionMessage = true
        }
    }

    // Check if current user is on stage
    val isOnStage = stageUsers.any { it.id == currentUser.id }

    Scaffold(
        containerColor = Color.Transparent, // Using parent gradient
        topBar = {
            // Custom Header with Search
            Row(
                modifier = Modifier
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Bar Container
                Box(
                    modifier = Modifier.weight(1f).zIndex(1f) // Ensure dropdown is on top
                ) {
                    // Actual Input Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search songs...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x0DFFFFFF))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF00FFFF)
                        ),
                        singleLine = true
                    )

                    // Search Results Dropdown
                    if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .padding(top = 60.dp) // Offset below text field
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            LazyColumn {
                                itemsIndexed(searchResults) { _, song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onAddSong(song) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                    if (song.coverUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = song.coverUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.DarkGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(song.artist, color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00FFFF))
                                    }
                                    Divider(color = Color.Gray.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // User Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF00FF), Color(0xFF00FFFF))))
                        .clickable { showMembersSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("${users.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // THE STAGE
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0x1AFF00FF), Color(0x1A00FFFF)) // 10% opacity colors
                        )
                    )
                    .border(
                        2.dp,
                        Brush.linearGradient(listOf(Color(0x4DFF00FF), Color(0x4D00FFFF))),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable { 
                        // When joining stage, check permission first
                        if (isOnStage) {
                            // Already on stage, just leave
                            onToggleStage()
                        } else {
                            // Trying to join stage - check permission
                            val hasPermission = androidx.core.app.ActivityCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.RECORD_AUDIO
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
                                // Has permission, join stage directly
                                onToggleStage()
                            } else {
                                // No permission, request it
                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    } // Click to Join/Leave
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🎤 The Stage", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    
                    val stageText = if (isOnStage) "Tap to leave stage" else "Tap to join the performance"
                    Text(stageText, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 24.dp))
                    
                    // Render Stage Slots (Max 2)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Render confirmed singers
                        stageUsers.forEach { singer ->
                            SingerAvatar(singer)
                        }

                        // Render Empty Slots if any
                        repeat(2 - stageUsers.size) {
                             Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1AFFFFFF)) // White/10
                                    .border(2.dp, Color(0x33FFFFFF), CircleShape), // White/20
                                contentAlignment = Alignment.Center
                             ) {
                                 Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                             }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // QUEUE
            Text(
                text = "Up Next",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(queue) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x0DFFFFFF)) // White/5
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Index Badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFFFF00FF), Color(0xFF00FFFF)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(song.artist, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
                        }

                        // Logic: Chỉ hiện nút Mic nếu TÔI đang ở trên STAGE
                        if (isOnStage) {
                            IconButton(
                                onClick = { onStartSong(song.firebaseId ?: "") },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFFFF00FF), Color(0xFF00FFFF))),
                                        CircleShape
                                    )
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        // Nút Xóa bài (Ai cũng thấy được)
                        IconButton(
                            onClick = { onRemoveSong(song.id) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x33EF4444), CircleShape) // Red/20
                                .border(1.dp, Color(0x4DEF4444), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // --- FULLSCREEN OVERLAY: LOADING & COUNTDOWN ---
        if (playbackState == "LOADING" || playbackState == "COUNTDOWN") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .zIndex(10f), // Ensure it's on top
                contentAlignment = Alignment.Center
            ) {
                when (playbackState) {
                    "LOADING" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(80.dp),
                                color = Color(0xFF00FFFF),
                                strokeWidth = 6.dp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Đang tải dữ liệu...",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Vui lòng chờ...",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }
                    "COUNTDOWN" -> {
                        // SERVER TIME-BASED COUNTDOWN: Maximum accuracy
                        var timeLeft by remember { mutableStateOf(5) }
                        var serverTimeOffset by remember { mutableStateOf(0L) }
                        
                        // Get server time offset once
                        LaunchedEffect(Unit) {
                            // Calculate offset: how much to add to local time to get server time
                            // This is done via Firebase .info/serverTimeOffset
                            serverTimeOffset = 0L // Will be updated by Firebase
                        }
                        
                        // Trigger countdown when playbackState becomes COUNTDOWN
                        LaunchedEffect(playbackState) {  // Changed from startTime to playbackState
                            if (playbackState != "COUNTDOWN") return@LaunchedEffect
                            if (startTime == 0L) return@LaunchedEffect
                            
                            val receivedAt = System.currentTimeMillis()
                            android.util.Log.d("KaraokeUI", "========== COUNTDOWN START ==========")
                            android.util.Log.d("KaraokeUI", "[Countdown] startTime: $startTime")
                            android.util.Log.d("KaraokeUI", "[Countdown] Received at: $receivedAt")
                            android.util.Log.d("KaraokeUI", "[Countdown] Initial diff: ${startTime - receivedAt}ms")
                            
                            while (true) {
                                // Use local time for calculation (offset already in startTime from server)
                                val now = System.currentTimeMillis()
                                val remainingMs = startTime - now
                                val remaining = kotlin.math.ceil(remainingMs / 1000.0).toInt()
                                
                                timeLeft = remaining.coerceAtLeast(0)
                                
                                if (remaining in 1..10) {  // Log from 10 to 1
                                    android.util.Log.d("KaraokeUI", "[Countdown] $remaining (${remainingMs}ms remaining)")
                                }
                                
                                if (timeLeft <= 0) break
                                kotlinx.coroutines.delay(100)
                            }
                            
                            android.util.Log.d("KaraokeUI", "========== COUNTDOWN END ==========")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Only show countdown when ≤ 5 seconds (skip the 3s buffer)
                            val displayTime = if (timeLeft > 5) 5 else timeLeft
                            
                            Text(
                                text = if (displayTime > 0) "$displayTime" else "GO!",
                                color = Color(0xFF00FFFF),
                                fontSize = 120.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Chuẩn bị hát...",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        
        // --- FULLSCREEN OVERLAY: KARAOKE SCREEN ---
        if (playbackState == "PLAYING") {
            // Get both ViewModels
            val karaokeViewModel: com.tinsic.app.presentation.karaoke.KaraokeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val songNotes by karaokeController.currentSongNotes.collectAsState()  // From KaraokeController
            val songLyrics by karaokeController.currentSongLyrics.collectAsState()  // From KaraokeController
            
            android.util.Log.d("KaraokeRoom", "[PLAYING] State entered. Notes: ${songNotes.size}, Lyrics: ${songLyrics.size}")
            
            // Wire data to KaraokeViewModel when data is ready
            LaunchedEffect(songNotes, songLyrics) {
                if (songNotes.isNotEmpty() && songLyrics.isNotEmpty()) {
                    android.util.Log.d("KaraokeRoom", "[DataWiring] ✅ Starting karaoke with ${songNotes.size} notes, ${songLyrics.size} lyrics")
                    karaokeViewModel.startSinging(songNotes, songLyrics)
                } else {
                    android.util.Log.w("KaraokeRoom", "[DataWiring] ⚠️ Waiting for data... Notes: ${songNotes.size}, Lyrics: ${songLyrics.size}")
                }
            }
            
            // Observe karaoke score and sync to Firebase
            val karaokeUiState by karaokeViewModel.uiState.collectAsState()
            
            // Save score when leaving PLAYING state
            androidx.compose.runtime.DisposableEffect(Unit) {
                onDispose {
                    // User left PLAYING state (stopped or finished)
                    val finalScore = karaokeUiState.currentScore
                    if (finalScore > 0) {
                        android.util.Log.d("KaraokeRoom", "[ScoreSync] Saving final score: $finalScore")
                        // Use KaraokeController instead of PartyViewModel
                        karaokeController.updateScore(roomId, currentUser.id, finalScore)
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f) // Above everything else
            ) {
                com.tinsic.app.presentation.karaoke.KaraokeScreen(
                    viewModel = karaokeViewModel,
                    onStopRequested = {
                        // User confirmed stop → Reset state for everyone
                        // Use KaraokeController instead of PartyViewModel
                        karaokeController.endSongForAll(roomId)
                    }
                )
            }
        }
        
        // --- PERMISSION DENIED MESSAGE ---
        if (showPermissionMessage) {
            androidx.compose.material3.Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    androidx.compose.material3.TextButton(onClick = { showPermissionMessage = false }) {
                        androidx.compose.material3.Text("OK")
                    }
                },
                dismissAction = {
                    androidx.compose.material3.IconButton(onClick = { showPermissionMessage = false }) {
                        androidx.compose.material3.Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            ) {
                androidx.compose.material3.Text("Cần quyền mic để chấm điểm khi hát 🎤")
            }
        }
    }

    // Members Bottom Sheet
    if (showMembersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMembersSheet = false },
            containerColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        ) {
             Column(modifier = Modifier.padding(24.dp)) {
                Text("Members List", fontSize = 24.sp, color = Color.White)
                
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    itemsIndexed(users) { _, user ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                           Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(user.color), contentAlignment = Alignment.Center) {
                                Text(user.avatar)
                           }
                           Spacer(modifier = Modifier.width(12.dp))
                           Text(user.name, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        showMembersSheet = false
                        onLeaveRoom() 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("Leave Room") }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SingerAvatar(singer: PartyUser) {
    Box(contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .border(4.dp, Color(0x33FFFFFF), CircleShape)
                .background(Brush.linearGradient(listOf(singer.color, singer.color.copy(alpha = 0.9f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(singer.avatar, fontSize = 40.sp)
        }
        Surface(
            modifier = Modifier.offset(y = 12.dp),
            color = Color(0xCC000000), // Black/80
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
        ) {
            Text(
                text = singer.name,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}
