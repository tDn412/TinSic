package com.tinsic.app.presentation.karaoke.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.tinsic.app.domain.karaoke.model.SongNote
import com.tinsic.app.presentation.karaoke.UserPitchPoint
import kotlin.math.abs
import kotlin.math.round
import kotlin.random.Random

// Helper class for particle system
private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    val color: Color
)

@Composable
fun PitchVisualizer(
    currentTime: Double,
    songNotes: List<SongNote>,
    userPitchHistory: List<UserPitchPoint>,
    modifier: Modifier = Modifier
) {
    // 1. Auto calculate range
    val (minMidi, maxMidi) = remember(songNotes) {
        if (songNotes.isEmpty()) 48f to 72f
        else {
            val min = songNotes.minOf { it.midi }.toFloat()
            val max = songNotes.maxOf { it.midi }.toFloat()
            (min - 6f) to (max + 6f)
        }
    }
    val midiRange = maxMidi - minMidi
    val timeWindow = 3.0

    val particles = remember { mutableListOf<Particle>() }

    // --- BRUSHES (GRAVITY THEME) ---
    val targetNoteBrush = remember {
        Brush.verticalGradient(colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.6f), Color(0xFF2979FF).copy(alpha = 0.8f)))
    }
    val targetNoteHitBrush = remember {
        Brush.verticalGradient(colors = listOf(Color(0xFFD500F9), Color(0xFFAA00FF))) // Neon Purple for Hit
    }
    val playheadBrush = remember {
        Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xFF00E5FF), Color.Transparent)) // Cyan Playhead
    }

    // --- SMART OCTAVE CALCULATION ---
    val lastPoint = userPitchHistory.lastOrNull()
    
    val currentTargetMidi = remember(currentTime, songNotes) {
        songNotes.firstOrNull { 
            currentTime >= it.startSec && currentTime <= (it.startSec + it.durationSec) 
        }?.midi?.toFloat()
    }

    val targetVisualMidi = remember(lastPoint, currentTargetMidi) {
        if (lastPoint != null) {
            if (currentTargetMidi != null) {
                val diff = lastPoint.midi - currentTargetMidi
                val octaveDiff = round(diff / 12f) * 12f
                lastPoint.midi - octaveDiff
            } else {
                lastPoint.midi.coerceIn(minMidi, maxMidi)
            }
        } else {
            minMidi 
        }
    }

    val smoothedVisualMidi by animateFloatAsState(
        targetValue = targetVisualMidi,
        animationSpec = tween(durationMillis = 100),
        label = "PitchSmoothing"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFF121212))
    ) {
        val width = size.width
        val height = size.height
        val pxPerSec = (width / timeWindow).toFloat()
        val pxPerMidi = height / midiRange

        fun midiToY(midi: Float): Float = height - ((midi - minMidi) * pxPerMidi)

        val playheadX = width / 3
        val scrollOffset = -(currentTime * pxPerSec) + playheadX

        withTransform({
            translate(left = scrollOffset.toFloat(), top = 0f)
        }) {
            val visibleStart = currentTime - (playheadX / pxPerSec) - 1.0
            val visibleEnd = currentTime + ((width - playheadX) / pxPerSec) + 1.0

            // LAYER 1: DRAW NOTES
            songNotes.forEach { note ->
                if (note.startSec + note.durationSec >= visibleStart && note.startSec <= visibleEnd) {
                    val noteX = (note.startSec * pxPerSec).toFloat()
                    val noteY = midiToY(note.midi.toFloat())
                    val noteW = (note.durationSec * pxPerSec).toFloat()
                    val noteH = pxPerMidi * 2.0f

                    val isBeingHit = userPitchHistory.lastOrNull()?.let { 
                        it.time >= note.startSec && it.time <= (note.startSec + note.durationSec) &&
                        abs((it.midi % 12) - (note.midi % 12)) < 1.5 || 
                        abs((it.midi % 12) - (note.midi % 12)) > 10.5
                    } ?: false

                    drawRoundRect(
                        brush = if (isBeingHit) targetNoteHitBrush else targetNoteBrush,
                        topLeft = Offset(noteX, noteY - (noteH / 2)),
                        size = Size(noteW, noteH),
                        cornerRadius = CornerRadius(8f, 8f),
                        alpha = if (isBeingHit) 1.0f else 0.7f
                    )
                    
                    if (isBeingHit) {
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(noteX, noteY - (noteH / 2)),
                            size = Size(noteW, noteH),
                            cornerRadius = CornerRadius(8f, 8f),
                            style = Stroke(width = 4f),
                            alpha = 0.9f
                        )
                    }
                }
            }
        }

        // LAYER 3: PLAYHEAD & CURSOR
        drawLine(
            brush = playheadBrush,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, height),
            strokeWidth = 4f
        )

        if (lastPoint != null && abs(lastPoint.time - currentTime) < 0.5) {
            val cursorY = midiToY(smoothedVisualMidi)
            
            if (lastPoint.color == 0xFF00E676.toLong() || lastPoint.color == 0xFFCDDC39.toLong()) {
                 repeat(2) {
                     particles.add(
                         Particle(
                             x = playheadX,
                             y = cursorY,
                             vx = Random.nextFloat() * 10 - 5,
                             vy = Random.nextFloat() * 10 - 5,
                             alpha = 1.0f,
                             color = Color(lastPoint.color)
                         )
                     )
                 }
            }

            val iterator = particles.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.x += p.vx
                p.y += p.vy
                p.alpha -= 0.05f
                if (p.alpha <= 0) {
                    iterator.remove()
                } else {
                    drawCircle(color = p.color.copy(alpha = p.alpha), radius = 4f, center = Offset(p.x, p.y))
                }
            }

            drawCircle(color = Color.White, radius = 10f, center = Offset(playheadX, cursorY))
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = 22f,
                center = Offset(playheadX, cursorY),
                style = Stroke(width = 2f)
            )
        }
    }
}
