package com.tinsic.app.presentation.party

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinsic.app.game.model.GameScreenState
import com.tinsic.app.game.model.GameType
import com.tinsic.app.game.viewmodel.GameViewModel
import com.tinsic.app.ui.screens.*

/**
 * GameRoomScreen - Multiplayer Game Integration
 * 
 * Connects Party Room infrastructure with Game mechanics
 * - Uses PartyViewModel for player management (connectedUsers)
 * - Uses GameViewModel for game logic
 * - Syncs state via Firebase Realtime Database
 */
@Composable
fun GameRoomScreen(
    partyViewModel: PartyViewModel = hiltViewModel(),
    gameViewModel: GameViewModel = hiltViewModel(),
    onLeaveRoom: () -> Unit
) {
    val partyUsers by partyViewModel.connectedUsers.collectAsState()
    val currentUser by partyViewModel.currentUser.collectAsState()
    val roomId by partyViewModel.roomId.collectAsState()
    val gameUiState by gameViewModel.uiState.collectAsState()
    
    // Get PlayerViewModel to pause music
    val playerViewModel: com.tinsic.app.presentation.player.PlayerViewModel = hiltViewModel()

    // Inject real players into GameViewModel whenever players change
    LaunchedEffect(partyUsers, currentUser) {
        gameViewModel.setPlayers(partyUsers, currentUser.id)
    }
    
    // Set roomId for Firebase sync
    LaunchedEffect(roomId) {
        if (roomId.isNotEmpty()) {
            android.util.Log.d("GameRoomScreen", "Connecting to Room: $roomId")
            gameViewModel.setRoomId(roomId)
        }
    }
    
    // Determine and set host status from PartyViewModel dynamically
    // This ensures if you become host (or host leaves), permissions update
    LaunchedEffect(partyUsers) {
        val hostId = partyUsers.firstOrNull()?.id ?: ""
        if (hostId.isNotEmpty()) {
            android.util.Log.d("GameRoomScreen", "Updating Host Status: CurrentUser=${currentUser.id}, Host=$hostId")
            gameViewModel.setIsHost(hostId)
        }
    }
    
    // Stop music and hide MiniPlayer when entering Game Room
    DisposableEffect(Unit) {
        android.util.Log.d("GameRoomScreen", "Stopping music and clearing MiniPlayer")
        playerViewModel.stopAndClear()
        
        onDispose {
            android.util.Log.d("GameRoomScreen", "GameRoomScreen disposed")
        }
    }

    // Gradient Background matching Party theme
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
        when (gameUiState.currentScreen) {
            GameScreenState.MENU -> {
                GameMenuRoomScreen(
                    roomId = roomId,
                    players = partyUsers,
                    currentPlayer = currentUser,
                    onGameSelected = { type -> gameViewModel.selectGame(type) },
                    isLoading = gameUiState.isLoading,
                    errorMessage = gameUiState.error,
                    onLeaveRoom = onLeaveRoom
                )
            }
            GameScreenState.COUNTDOWN -> {
                CountdownScreen(
                    gameType = gameUiState.selectedGameType,
                    countdownTime = gameUiState.countdownTime
                )
            }
            GameScreenState.PLAYING -> {
                PlayScreen(
                    uiState = gameUiState,
                    currentQuestion = gameViewModel.currentQuestion,
                    onSubmitAnswer = { index -> gameViewModel.submitAnswer(index) }
                )
            }
            GameScreenState.MUSIC_PREVIEW -> {
                MusicPreviewScreen(
                    currentQuestion = gameViewModel.currentQuestion,
                    songTitle = gameViewModel.currentQuestion?.songTitle 
                        ?: gameViewModel.currentQuestion?.options?.get(
                            gameViewModel.currentQuestion?.correctAnswerIndex ?: 0
                        ) ?: "",
                    onMusicFinished = { gameViewModel.onMusicPreviewFinished() }
                )
            }
            GameScreenState.QUESTION_RESULT -> {
                QuestionResultScreen(
                    playerScores = gameUiState.playerScores
                )
            }
            GameScreenState.RESULT -> {
                GameOverView(
                    score = gameUiState.score,
                    playerScores = gameUiState.playerScores,
                    onReplay = { gameViewModel.backToMenu() }
                )
            }
        }


    }
}

/**
 * Custom Menu Screen for Multiplayer Game Room
 * Shows connected players + game selection
 */
@Composable
private fun GameMenuRoomScreen(
    roomId: String,
    players: List<PartyUser>,
    currentPlayer: PartyUser,
    onGameSelected: (GameType) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onLeaveRoom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header với Room Info
        Text(
            "🎮 GAME ROOM",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF) // Cyan accent
        )
        
        Text(
            "Room: $roomId",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Players Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0x0DFFFFFF) // White/5
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Players (${players.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                players.forEach { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            player.avatar,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            player.name,
                            fontSize = 16.sp,
                            color = if (player.id == currentPlayer.id) Color(0xFF00E5FF) else Color.White,
                            fontWeight = if (player.id == currentPlayer.id) FontWeight.Bold else FontWeight.Normal
                        )
                        if (player.id == currentPlayer.id) {
                            Text(
                                " (Bạn)",
                                fontSize = 14.sp,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error Message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text(
                    text = error,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Loading Indicator
        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFF00E5FF))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Đang tải câu hỏi...", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Game Selection
        Text(
            "Chọn Mini Game:",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        GameType.values().forEach { type ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLoading) Color(0xFF1C1C1C) else Color(0xFF2C2C2C)
                ),
                shape = RoundedCornerShape(12.dp),
                onClick = { if (!isLoading) onGameSelected(type) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        type.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLoading) Color.DarkGray else Color(0xFF00E5FF)
                    )
                    Text(
                        type.description,
                        fontSize = 14.sp,
                        color = if (isLoading) Color.DarkGray else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Leave Room Button
        Button(
            onClick = onLeaveRoom,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x1AEF4444) // Red/10
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Rời Phòng",
                color = Color(0xFFF87171), // Red-400
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
