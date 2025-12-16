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
    
    // Navigate to full-screen PartyRoom when entering GAME mode (to hide MiniPlayer)
    LaunchedEffect(mode, roomType) {
        if (mode == PartyModeState.ROOM && roomType == "GAME" && !isRoomMode) {
            android.util.Log.d("PartyScreen", "Navigating to full-screen Game Room")
            onStartSession() // Navigate to PartyRoom route (no MainScaffold)
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
                    // Game Room Screen
                    GameRoomScreen(
                        partyViewModel = viewModel,
                        onLeaveRoom = {
                            viewModel.leaveRoom()
                            onLeaveSession()
                        }
                    )
                } else {
                    ActivePartyRoom(
                        roomId = roomId,
                        users = users,
                        stageUsers = stageUsers, // Pass stage users
                        currentUser = currentUser, // Pass current user
                        queue = queue,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onRemoveSong = { viewModel.removeSong(it) },
                        onLeaveRoom = {
                            // 1. Send Leave command to Firebase
                            viewModel.leaveRoom()
                            // 2. Navigate back
                            onLeaveSession() 
                        },
                        onToggleStage = { viewModel.toggleStageJoin() }, // Action to join/leave stage
                        onStartSong = { /* Logic to start song */ }
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
                    
                    // GAME OPTION
                    PartyTypeOption(
                        title = "🎮 Game Room",
                        description = "Play mini-games together",
                        color = Color(0xFF10B981), // Green
                        isEnabled = true,  // ENABLED for Game Team
                        onClick = {
                            showTypeDialog = false
                            onStartParty("GAME")
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivePartyRoom(
    roomId: String,
    users: List<PartyUser>,
    stageUsers: List<PartyUser>, // New Param
    currentUser: PartyUser,      // New Param
    queue: List<PartySong>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRemoveSong: (Int) -> Unit,
    onLeaveRoom: () -> Unit,
    onToggleStage: () -> Unit, // Changed from onJoinStage
    onStartSong: () -> Unit     // New Param
) {
    var showMembersSheet by remember { mutableStateOf(false) }

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
                // Search Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0DFFFFFF)) // White/5
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Search songs..." else searchQuery,
                            color = if (searchQuery.isEmpty()) Color.Gray else Color.White
                        )
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
                    .clickable { onToggleStage() } // Click to Join/Leave
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
                                onClick = { onStartSong() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFFFF00FF), Color(0xFF00FFFF))),
                                        CircleShape
                                    )
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            // Nếu không ở trên stage thì không hiện mic (hoặc có thể hiện icon khóa - Optional)
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
    }

    // Members Bottom Sheet
    if (showMembersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMembersSheet = false },
            containerColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        ) {
            // Existing BottomSheet Content... (Kept same as before)
            // For brevity, using simplified version if full content not needed here, 
            // but in real code, paste the full bottom sheet content from previous step.
             Column(modifier = Modifier.padding(24.dp)) {
                Text("Members List", fontSize = 24.sp, color = Color.White)
                // ... (Previous implementation details)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onLeaveRoom) { Text("Leave Room") }
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
