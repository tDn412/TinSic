package com.tinsic.app.presentation.party

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@Composable
fun PartyScreen(
    viewModel: PartyViewModel = hiltViewModel(),
    isRoomMode: Boolean = false, // If true, show Room immediately
    onStartSession: () -> Unit = {},
    onLeaveSession: () -> Unit = {}
) {
    val mode by viewModel.mode.collectAsState()
    val users by viewModel.connectedUsers.collectAsState()
    val stageUsers by viewModel.stageUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val queue by viewModel.queue.collectAsState() // Fixed: use queue
    val searchQuery by viewModel.searchQuery.collectAsState()
    val roomId by viewModel.roomId.collectAsState()
    val roomType by viewModel.roomType.collectAsState()
    
    // Sync Engine States
    val playbackState by viewModel.playbackState.collectAsState()
    val startTime by viewModel.startTime.collectAsState()

    // Force mode if entered directly (Run ONCE on entry Only)
    LaunchedEffect(Unit) {
        if (isRoomMode) {
            viewModel.setMode(PartyModeState.ROOM)
        } else {
            // Only set to LOBBY if not already in a specific state (preserve VM state if needed)
            if (viewModel.mode.value != PartyModeState.ROOM) {
                viewModel.setMode(PartyModeState.LOBBY)
            }
        }
    }

    // --- NAVIGATION LOGIC ---
    // REMOVED: Conflicting navigation logic.
    // PartyScreen will handle the transition locally via 'when(mode)'
    // This prevents the 'State Loop' bug where Navigation Stack holds a stale ROOM state.
    /*
    LaunchedEffect(mode) {
        if (mode == PartyModeState.ROOM && !isRoomMode) {
             onStartSession() // Navigate to Immersive Screen (PartyRoom route)
        }
    }
    */

    // Neon Gradient Background
    val mainGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF121212), // Dark bg
            Color(0xFF1A1A1A)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(mainGradient)
    ) {
        when (mode) {
            PartyModeState.LOBBY -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                LobbyScreen(
                    roomId = roomId,
                    users = users,
                    onStartParty = { type ->
                        android.widget.Toast.makeText(context, "Starting $type Mode...", android.widget.Toast.LENGTH_SHORT).show()
                        viewModel.startPartySession(type) 
                    },
                    onJoinParty = { inputId ->
                        viewModel.joinRoom(inputId) { success ->
                             if (!success) {
                                 android.widget.Toast.makeText(context, "Room not found!", android.widget.Toast.LENGTH_SHORT).show()
                             }
                        }
                    }
                )
            }
            PartyModeState.ROOM -> {
                // SWITCH UI BASED ON ROOM TYPE
                if (roomType == "GAME") {
                    // Placeholder for Game Team
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎮 Game Room", color = Color.Green, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text("ID: $roomId", color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.leaveRoom() }) {
                                Text("Leave Game")
                            }
                        }
                    }
                } else {
                    val searchResults by viewModel.searchResults.collectAsState()
                    
                    ActivePartyRoom(
                        roomId = roomId,
                        users = users,
                        stageUsers = stageUsers,
                        currentUser = currentUser,
                        queue = queue,
                        searchQuery = searchQuery,
                        searchResults = searchResults, // Pass results
                        playbackState = playbackState, // NEW
                        startTime = startTime, // NEW
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onRemoveSong = { viewModel.removeSong(it) },
                        onAddSong = { viewModel.addSongToQueue(it) }, // Pass Add action
                        onLeaveRoom = {
                            viewModel.leaveRoom()
                            onLeaveSession() 
                        },
                        onToggleStage = { viewModel.toggleStageJoin() },
                        onStartSong = { songId -> viewModel.startSong(songId) } // Wire to ViewModel
                    )
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Mode ${mode.name} coming soon!", color = Color.White)
                    Button(onClick = { viewModel.setMode(PartyModeState.ROOM) }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Back to Room")
                    }
                }
            }
        }
    }
}

// --- SUB SCREENS ---

@Composable
fun LobbyScreen(
    roomId: String,
    users: List<PartyUser>,
    onStartParty: (String) -> Unit,
    onJoinParty: (String) -> Unit
) {
    var showTypeDialog by remember { mutableStateOf(false) }
    var inputRoomId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title Gradient
        Text(
            text = "Party Mode",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Transparent, // Will be masked if possible, but basic color for now
            modifier = Modifier // Simplified gradient text effect
        )
        Text(
            text = "Scan to join the karaoke room",
            color = Color.Gray,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // QR Code Placeholder
        Box(
            modifier = Modifier
                .size(256.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.QrCode, // Need generic QR icon
                    contentDescription = "QR Code",
                    modifier = Modifier.size(160.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Room: $roomId",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Participants Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF60A5FA)) // Blue-400
            Spacer(modifier = Modifier.width(8.dp))
            Text("${users.size} participants", color = Color.Gray)
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(users) { user ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0x1AFFFFFF), CircleShape) // White/10
                            .background(Brush.linearGradient(listOf(user.color, user.color.copy(alpha = 0.9f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(user.avatar, fontSize = 28.sp)
                    }
                    Text(user.name, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // JOIN ROOM SECTION
        Text("Join Existing Room", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputRoomId,
                onValueChange = { if (it.length <= 4) inputRoomId = it },
                placeholder = { Text("Enter ID (4 digits)", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FFFF),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color(0xFF00FFFF), // Updated from inputTextColor
                    unfocusedTextColor = Color.White      // Updated from inputTextColor
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { onJoinParty(inputRoomId) },
                enabled = inputRoomId.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("JOIN", color = Color.White)
            }
        }

        // Divide
        Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(bottom = 24.dp))

        // Start Button
        Button(
            onClick = { showTypeDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues() // Reset padding for gradient
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFFFF00FF), Color(0xFF00FFFF)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Start Party Session", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showTypeDialog) {
        AlertDialog(
            onDismissRequest = { showTypeDialog = false },
            containerColor = Color(0xFF222222),
            title = {
                Text("Select Party Type", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // KARAOKE OPTION
                    PartyTypeOption(
                        title = "🎤 Karaoke",
                        description = "Sing along with friends",
                        color = Color(0xFFFF00FF),
                        onClick = {
                            showTypeDialog = false
                            onStartParty("KARAOKE")
                        }
                    )
                    
                    // GAME OPTION (Disabled)
                    PartyTypeOption(
                        title = "🎮 Game Room",
                        description = "Play mini-games (Coming Soon)",
                        color = Color.Gray,
                        isEnabled = false,
                        onClick = {}
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTypeDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun PartyTypeOption(
    title: String,
    description: String,
    color: Color,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF333333))
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(title.first().toString(), fontSize = 24.sp) // Emoji or Initial
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (isEnabled) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
            Text(description, color = Color.Gray, fontSize = 12.sp)
        }
        if (!isEnabled) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

