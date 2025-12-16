package com.tinsic.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tinsic.app.game.model.GameScreenState
import com.tinsic.app.game.viewmodel.GameViewModel
import com.tinsic.app.ui.screens.CountdownScreen
import com.tinsic.app.ui.screens.GameOverView
import com.tinsic.app.ui.screens.MenuScreen
import com.tinsic.app.ui.screens.MusicPreviewScreen
import com.tinsic.app.ui.screens.PlayScreen
import com.tinsic.app.ui.screens.QuestionResultScreen

@Composable
fun GameEntryScreen(viewModel: GameViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        when (uiState.currentScreen) {
            GameScreenState.MENU -> {
                MenuScreen(
                    onGameSelected = { type -> viewModel.selectGame(type) },
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.error
                )
            }
            GameScreenState.COUNTDOWN -> {
                CountdownScreen(
                    gameType = uiState.selectedGameType,
                    countdownTime = uiState.countdownTime
                )
            }
            GameScreenState.PLAYING -> {
                PlayScreen(
                    uiState = uiState,
                    currentQuestion = viewModel.currentQuestion,
                    onSubmitAnswer = { index -> viewModel.submitAnswer(index) }
                )
            }
            GameScreenState.MUSIC_PREVIEW -> {
                MusicPreviewScreen(
                    currentQuestion = viewModel.currentQuestion,
                    songTitle = viewModel.currentQuestion?.songTitle ?: viewModel.currentQuestion?.options?.get(
                        viewModel.currentQuestion?.correctAnswerIndex ?: 0
                    ) ?: "",
                    onMusicFinished = { viewModel.onMusicPreviewFinished() }
                )
            }
            GameScreenState.QUESTION_RESULT -> {
                QuestionResultScreen(
                    playerScores = uiState.playerScores
                )
            }
            GameScreenState.RESULT -> {
                GameOverView(
                    score = uiState.score,
                    playerScores = uiState.playerScores,
                    onReplay = { viewModel.backToMenu() }
                )
            }
        }
    }
}