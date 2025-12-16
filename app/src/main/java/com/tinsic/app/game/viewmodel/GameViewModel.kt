package com.tinsic.app.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinsic.app.game.data.GameRepository
import com.tinsic.app.game.model.GameScreenState
import com.tinsic.app.game.model.GameType
import com.tinsic.app.game.model.PlayerScore
import com.tinsic.app.game.model.Question
import com.tinsic.app.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class GameUiState(
    val currentScreen: GameScreenState = GameScreenState.MENU,
    val selectedGameType: GameType? = null,
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val timeLeft: Int = 10,
    val countdownTime: Int = 5,
    val isGameOver: Boolean = false,
    val selectedAnswerIndex: Int? = null,
    val isAnswerRevealed: Boolean = false,
    val isAnswerLocked: Boolean = false,
    val streak: Int = 0,
    val highScore: Int = 0,
    val playerScores: List<PlayerScore> = emptyList(),
    val mockPlayers: List<PlayerScore> = emptyList(),
    val isMusicPreviewPhase: Boolean = false,
    val isLoading: Boolean = false,  // Loading state for Firebase
    val error: String? = null  // Error message if loading fails
)

@dagger.hilt.android.lifecycle.HiltViewModel
class GameViewModel @javax.inject.Inject constructor(
    private val gameRepository: GameRepository,
    private val partyRepository: com.tinsic.app.data.repository.PartyRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()
    
    // Track current player ID and room ID for Firebase sync
    private var currentPlayerId: String = ""
    private var currentRoomId: String = ""
    
    /**
     * Inject real players from PartyViewModel
     * Called initially and when Firebase updates player data
     */
    fun setPlayers(players: List<com.tinsic.app.presentation.party.PartyUser>, currentUserId: String) {
        currentPlayerId = currentUserId
        
        // Merge: Use scores from Firebase, but preserve local game state
        val updatedScores = players.map { firebasePlayer ->
            // Check if this player already exists in local state
            val existingPlayer = _uiState.value.playerScores.find { it.playerId == firebasePlayer.id }
            
            PlayerScore(
                playerId = firebasePlayer.id,
                playerName = firebasePlayer.name,
                score = firebasePlayer.score,  // Always use Firebase score (source of truth)
                answeredCorrectly = existingPlayer?.answeredCorrectly ?: false,  // Preserve local state
                isCurrentPlayer = firebasePlayer.id == currentUserId
            )
        }.sortedByDescending { it.score }
        
        _uiState.value = _uiState.value.copy(playerScores = updatedScores)
        android.util.Log.d("GameViewModel", "Updated players from Firebase: ${players.size} players, current scores: ${updatedScores.map { "${it.playerName}:${it.score}" }}")
    }
    
    /**
     * Set room ID for Firebase sync
     * Called from GameRoomScreen after PartyViewModel provides roomId
     */
    fun setRoomId(roomId: String) {
        currentRoomId = roomId
        android.util.Log.d("GameViewModel", "RoomId set: $roomId")
    }

    fun selectGame(type: GameType) {
        viewModelScope.launch {
            // Set loading state
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                // Fetch questions from Firebase
                val allQuestions = gameRepository.getQuestionsByType(type)
                val filteredQuestions = allQuestions.shuffled().take(5)
                
                if (filteredQuestions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Không tìm thấy câu hỏi cho game này. Vui lòng thử lại sau."
                    )
                    return@launch
                }


                // Initialize player scores from already-set players (via setPlayers)
                val initialPlayerScores = _uiState.value.playerScores.ifEmpty {
                    // Fallback if setPlayers wasn't called
                    listOf(
                        PlayerScore(
                            playerId = currentPlayerId,
                            playerName = "You",
                            score = 0,
                            answeredCorrectly = false,
                            isCurrentPlayer = true
                        )
                    )
                }

                _uiState.value = _uiState.value.copy(
                    selectedGameType = type,
                    questions = filteredQuestions,
                    currentScreen = GameScreenState.COUNTDOWN,
                    score = 0,
                    streak = 0,
                    countdownTime = 5,
                    isGameOver = false,
                    mockPlayers = emptyList(), // No more AI players
                    playerScores = initialPlayerScores,
                    isLoading = false,
                    error = null
                )

                startCountdown()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi khi tải câu hỏi: ${e.message}. Vui lòng kiểm tra kết nối internet."
                )
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (_uiState.value.countdownTime > 0 && _uiState.value.currentScreen == GameScreenState.COUNTDOWN) {
                delay(1000)
                _uiState.value = _uiState.value.copy(countdownTime = _uiState.value.countdownTime - 1)
            }
            if (_uiState.value.countdownTime == 0 && _uiState.value.currentScreen == GameScreenState.COUNTDOWN) {
                startGame()
            }
        }
    }

    fun startGame() {
        val gameType = _uiState.value.selectedGameType
        
        // === FIREBASE SYNC: Set playing = true ===
        if (currentRoomId.isNotEmpty() && currentPlayerId.isNotEmpty()) {
            viewModelScope.launch {
                partyRepository.updatePlayerPlayingStatus(currentRoomId, currentPlayerId, true)
                android.util.Log.d("GameViewModel", "Set playing = true in Firebase")
            }
        }
        
        // Only GUESS_THE_SONG starts with music preview phase
        if (gameType == GameType.GUESS_THE_SONG) {
            _uiState.value = _uiState.value.copy(
                currentScreen = GameScreenState.PLAYING,
                currentQuestionIndex = 0,
                timeLeft = 10,
                selectedAnswerIndex = null,
                isAnswerRevealed = false,
                isAnswerLocked = false,
                streak = 0,
                isMusicPreviewPhase = true  // Set music preview phase
            )
            startMusicPreviewThenTimer()
        } else {
            // For LYRICS_FLIP, FINISH_THE_LYRICS, and MUSIC_CODE, go straight to playing without music preview
            _uiState.value = _uiState.value.copy(
                currentScreen = GameScreenState.PLAYING,
                currentQuestionIndex = 0,
                timeLeft = 10,
                selectedAnswerIndex = null,
                isAnswerRevealed = false,
                isAnswerLocked = false,
                streak = 0,
                isMusicPreviewPhase = false
            )
            startTimer()
        }
    }
    
    private fun startMusicPreviewThenTimer() {
        viewModelScope.launch {
            // Play music for 15 seconds (music preview phase)
            delay(10000)
            
            // After music preview, start the answer timer
            if (_uiState.value.currentScreen == GameScreenState.PLAYING && _uiState.value.isMusicPreviewPhase) {
                _uiState.value = _uiState.value.copy(
                    isMusicPreviewPhase = false  // Exit music preview phase
                )
                startTimer()
            }
        }
    }

    fun backToMenu() {
        // === FIREBASE SYNC: Set playing = false ===
        if (currentRoomId.isNotEmpty() && currentPlayerId.isNotEmpty()) {
            viewModelScope.launch {
                partyRepository.updatePlayerPlayingStatus(currentRoomId, currentPlayerId, false)
                android.util.Log.d("GameViewModel", "Set playing = false in Firebase")
            }
        }
        
        _uiState.value = GameUiState(currentScreen = GameScreenState.MENU, highScore = _uiState.value.highScore)
    }
    
    fun onMusicPreviewFinished() {
        // Chuyển sang màn hình kết quả câu hỏi
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentScreen = GameScreenState.QUESTION_RESULT
            )
            
            delay(5000)
            nextQuestion()
        }
    }

    val currentQuestion: Question?
        get() = if (_uiState.value.questions.isNotEmpty() && _uiState.value.questions.indices.contains(_uiState.value.currentQuestionIndex))
            _uiState.value.questions[_uiState.value.currentQuestionIndex] else null

    private fun startTimer() {
        viewModelScope.launch {
            while (_uiState.value.timeLeft > 0 && !_uiState.value.isAnswerRevealed && _uiState.value.currentScreen == GameScreenState.PLAYING) {
                delay(1000)
                _uiState.value = _uiState.value.copy(timeLeft = _uiState.value.timeLeft - 1)
            }
            if (_uiState.value.timeLeft == 0 && !_uiState.value.isAnswerRevealed && _uiState.value.currentScreen == GameScreenState.PLAYING) {
                revealAnswer()
            }
        }
    }

    fun submitAnswer(index: Int) {
        if (_uiState.value.isAnswerRevealed || _uiState.value.isAnswerLocked) return
        _uiState.value = _uiState.value.copy(
            selectedAnswerIndex = index,
            isAnswerLocked = true
        )
    }

    private fun revealAnswer() {
        val question = currentQuestion ?: return
        val isCorrect = _uiState.value.selectedAnswerIndex == question.correctAnswerIndex
        
        // Play sound effect based on answer correctness
        if (isCorrect) {
            SoundManager.playCorrectSound()
        } else {
            SoundManager.playWrongSound()
        }
        
        var newScore = _uiState.value.score
        var newStreak = _uiState.value.streak

        if (isCorrect) {
            newScore += 10
            newStreak += 1
            if (newStreak >= 3) {
                newScore += 5
            }
        } else {
            newStreak = 0
        }

        val currentHighScore = _uiState.value.highScore
        val finalHighScore = if (newScore > currentHighScore) newScore else currentHighScore

        // Update only current player's score (other players will sync via Firebase)
        val updatedPlayerScores = _uiState.value.playerScores.map { player ->
            if (player.playerId == currentPlayerId) {
                player.copy(
                    score = newScore,
                    answeredCorrectly = isCorrect
                )
            } else {
                player // Keep other players' scores unchanged for now
            }
        }.sortedByDescending { it.score }

        _uiState.value = _uiState.value.copy(
            isAnswerRevealed = true, 
            score = newScore, 
            streak = newStreak,
            highScore = finalHighScore,
            mockPlayers = emptyList(), // No more AI players
            playerScores = updatedPlayerScores
        )
        
        // === FIREBASE SYNC: Update score in Realtime Database ===
        if (currentRoomId.isNotEmpty() && currentPlayerId.isNotEmpty()) {
            viewModelScope.launch {
                partyRepository.updatePlayerScore(currentRoomId, currentPlayerId, newScore)
                android.util.Log.d("GameViewModel", "Synced score to Firebase: $newScore")
            }
        } else {
            android.util.Log.w("GameViewModel", "Cannot sync score - roomId or playerId empty")
        }

        viewModelScope.launch {
            delay(2000)
            
            // For LYRICS_FLIP, FINISH_THE_LYRICS, and MUSIC_CODE, show music preview after answer
            if (question.type == GameType.LYRICS_FLIP || question.type == GameType.FINISH_THE_LYRICS || question.type == GameType.MUSIC_CODE) {
                _uiState.value = _uiState.value.copy(
                    currentScreen = GameScreenState.MUSIC_PREVIEW
                )
                // Music will finish on its own, no fixed delay needed
                // The onMusicFinished callback will handle the transition
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(
                currentScreen = GameScreenState.QUESTION_RESULT
            )
            
            delay(5000)
            nextQuestion()
        }
    }

    private fun nextQuestion() {
        val nextIndex = _uiState.value.currentQuestionIndex + 1
        if (nextIndex < _uiState.value.questions.size) {
            val gameType = _uiState.value.selectedGameType
            
            // Only GUESS_THE_SONG starts with music preview phase again
            if (gameType == GameType.GUESS_THE_SONG) {
                _uiState.value = _uiState.value.copy(
                    currentScreen = GameScreenState.PLAYING,
                    currentQuestionIndex = nextIndex,
                    timeLeft = 10,
                    selectedAnswerIndex = null,
                    isAnswerRevealed = false,
                    isAnswerLocked = false,
                    isMusicPreviewPhase = true  // Set music preview phase
                )
                startMusicPreviewThenTimer()
            } else {
                // For LYRICS_FLIP, FINISH_THE_LYRICS, and MUSIC_CODE, go straight to playing
                _uiState.value = _uiState.value.copy(
                    currentScreen = GameScreenState.PLAYING,
                    currentQuestionIndex = nextIndex,
                    timeLeft = 10,
                    selectedAnswerIndex = null,
                    isAnswerRevealed = false,
                    isAnswerLocked = false,
                    isMusicPreviewPhase = false
                )
                startTimer()
            }
        } else {
            _uiState.value = _uiState.value.copy(currentScreen = GameScreenState.RESULT)
        }
    }
}


