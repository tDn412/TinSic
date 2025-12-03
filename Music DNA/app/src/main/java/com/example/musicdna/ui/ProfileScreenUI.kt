package com.example.musicdna.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFF0A0A0A))
            .padding(bottom = 80.dp)
    ) {
        HeaderSection()
        Spacer(modifier = Modifier.height(16.dp))

        StatsSection()
        Spacer(modifier = Modifier.height(24.dp))

        MusicDNASection()
        Spacer(modifier = Modifier.height(24.dp))

        AchievementSection()
        Spacer(modifier = Modifier.height(16.dp))
    }
}
