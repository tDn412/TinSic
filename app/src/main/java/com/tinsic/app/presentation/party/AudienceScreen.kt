package com.tinsic.app.presentation.party

import androidx.compose.animation.core.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun AudienceScreen(
    currentSong: PartySong?,
    singer: PartyUser?,
    onSendReaction: (String) -> Unit
) {
    // Pulse Animation
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        )
    )

    // Use Surface for background and touch handling
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { /* Consume touches */ },
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .background(Color.Transparent), // Surface handles color
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // 1. "Now Singing" Pill
        Box(
            modifier = Modifier
                .padding(top = 32.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF2D2D2D))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(singer?.color ?: Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(singer?.avatar ?: "🎤")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Now Singing", fontSize = 10.sp, color = Color.Gray)
                    Text(singer?.name ?: "Unknown", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 2. Song Info
        Text(
            text = currentSong?.title ?: "No Song Playing",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = currentSong?.artist ?: "Unknown Artist",
            color = Color(0xFF00FFFF),
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // 3. Central Visualizer (Pulse)
        Box(contentAlignment = Alignment.Center) {
            // Ripple 1
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0x4D00FFFF), Color.Transparent)))
            )
            // Ripple 2 (Delayed/Offset - simplified to just alpha pulse)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
                    .border(2.dp, Color(0xFF00FFFF).copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF00FFFF),
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Waiting for your turn...",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
        Text(
            "Send reactions to cheer!",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // 4. Reaction Grid (4x4)
        Text("Tap to send", color = Color(0xFFFF00FF), fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))
        
        val emojis = listOf(
            "❤️", "⭐", "🔥", "✨",
            "🏆", "👍", "💎", "🎵",
            "🎤", "👏", "🌟", "💯",
            "🎉", "😍", "🤘", "💫"
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            emojis.chunked(4).forEach { rowEmojis ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowEmojis.forEach { emoji ->
                        ReactionButton(emoji = emoji, onClick = { onSendReaction(emoji) })
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
}

@Composable
fun ReactionButton(emoji: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1AFFFFFF)) // Glass effect
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 28.sp)
    }
}
