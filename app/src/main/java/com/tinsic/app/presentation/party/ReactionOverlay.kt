package com.tinsic.app.presentation.party

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlin.random.Random

data class FloatingReaction(
    val id: Long,
    val text: String,
    var x: Float,
    var y: Float,
    val speed: Float,
    val wobbleFrequency: Float,
    val wobbleAmplitude: Float,
    var alpha: Float = 1f,
    val scale: Float = 1f,
    val createdAt: Long = System.currentTimeMillis()
)

@Composable
fun ReactionOverlay(
    reactionFlow: SharedFlow<com.tinsic.app.data.model.PartyReaction>,
    modifier: Modifier = Modifier
) {
    var reactions by remember { mutableStateOf(listOf<FloatingReaction>()) }
    val density = LocalDensity.current.density
    
    // Listen for new reactions
    LaunchedEffect(Unit) {
        reactionFlow.collect { reaction ->
            val startX = Random.nextFloat() * 0.8f + 0.1f // 10% to 90% width
            val size = Random.nextFloat() * 0.5f + 1.0f // Scale 1.0 to 1.5
            
            val reactionObj = FloatingReaction(
                id = System.nanoTime(),
                text = reaction.emoji,
                x = startX, // Normalized 0..1
                y = 1.0f,   // Start at bottom (normalized)
                speed = (Random.nextFloat() * 0.3f + 0.2f), // Speed per second (normalized height)
                wobbleFrequency = Random.nextFloat() * 4f + 2f,
                wobbleAmplitude = Random.nextFloat() * 0.05f + 0.02f,
                scale = size
            )
            
            reactions = reactions + reactionObj
        }
    }

    // Animation Loop
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dt = (now - lastTime) / 1_000_000_000f // Delta time in seconds
            lastTime = now

            if (dt > 0) {
                // Update positions
                reactions = reactions.mapNotNull { r ->
                    // Move up
                    val newY = r.y - (r.speed * dt)
                    
                    // Wobble X
                    val timeAlive = (System.currentTimeMillis() - r.createdAt) / 1000f
                    val wobble = Math.sin((timeAlive * r.wobbleFrequency).toDouble()).toFloat() * r.wobbleAmplitude
                    val newX = r.x + (wobble * dt) // Apply wobble delta? No, x is base + wobble.
                    // simpler: x is base, draw with offset. But let's just mutate x slightly or keep baseX.
                    // Let's keep it simple: linear up movement.
                    
                    // Fade out near top
                    val newAlpha = if (newY < 0.2f) r.alpha - (dt * 2f) else r.alpha
                    
                    if (newY < -0.1f || newAlpha <= 0f) {
                        null // Remove
                    } else {
                        r.copy(y = newY, alpha = newAlpha) // X wobble calculated at draw time
                    }
                }
            }
            
            // Cap at 60 FPS
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val paint = Paint().asFrameworkPaint().apply {
            textSize = 32.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
        }

        reactions.forEach { r ->
            val timeAlive = (System.currentTimeMillis() - r.createdAt) / 1000f
            val wobbleX = Math.sin((timeAlive * r.wobbleFrequency).toDouble()) * r.wobbleAmplitude * width
            
            val drawX = (r.x * width) + wobbleX
            val drawY = r.y * height
            
            paint.alpha = (r.alpha * 255).toInt().coerceIn(0, 255)
            // Scale text size
            paint.textSize = 32.sp.toPx() * r.scale

            drawIntoCanvas {
                it.nativeCanvas.drawText(r.text, drawX.toFloat(), drawY, paint)
            }
        }
    }
}
