package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager

@Composable
fun FountainBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit = {}
) {
    val bassAmplitudes by visualizerManager.bassAmplitudes.collectAsState()
    val trebleAmplitudes by visualizerManager.highAmplitudes.collectAsState()

    val avgBass = if (bassAmplitudes.isNotEmpty()) {
        val avg = bassAmplitudes.average().toFloat()
        if (avg.isNaN()) 0f else avg
    } else 0f

    val avgTreble = if (trebleAmplitudes.isNotEmpty()) {
        val avg = trebleAmplitudes.average().toFloat()
        if (avg.isNaN()) 0f else avg
    } else 0f

    val targetScale = 1f + (avgBass * 0.4f) + (avgTreble * 0.1f)
    val animatedScale by animateFloatAsState(
        targetValue = if (targetScale.isNaN()) 1f else targetScale.coerceIn(1f, 1.3f),
        animationSpec = tween(150)
    )

    var timeMillis by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        val startTime = withFrameNanos { it / 1_000_000L }
        while (true) {
            withFrameNanos { frameTime ->
                timeMillis = (frameTime / 1_000_000L) - startTime
            }
        }
    }

    val cornerRadius = if (isPlayerScreen) 64.dp else 24.dp
    val dominant = if (paletteColors.dominant != Color.Unspecified && paletteColors.dominant != Color.Transparent) paletteColors.dominant else Color.Cyan
    val vibrant = if (paletteColors.vibrant != Color.Unspecified && paletteColors.vibrant != Color.Transparent) paletteColors.vibrant else Color.Blue

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Background is ALWAYS dark
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .scale(animatedScale)
        ) {
            val timeSecs = timeMillis / 1000f
            val timeMod = timeSecs // Fixed teleportation by not scaling absolute time with treble
            val numParticles = 30 + (avgTreble * 30).toInt()

            // Back Particles
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fountainBaseWidth = size.width * 0.5f + (avgBass * size.width * 0.1f)
                for (p in 0 until numParticles) {
                    val pPhase = p * 137.5f
                    val pTime = timeMod * (0.5f + (p % 3) * 0.2f)
                    val pY = size.height - ((pTime * 200f + p * 50f) % (size.height * 1.2f))
                    val pAngle = pTime * 1.5f + pPhase
                    val pZ = kotlin.math.sin(pAngle.toDouble()).toFloat()
                    if (pY < size.height && pZ < 0f) {
                        val pX = center.x + kotlin.math.cos(pAngle.toDouble()).toFloat() * (fountainBaseWidth * 0.7f)
                        drawCircle(
                            color = vibrant.copy(alpha = 0.3f),
                            radius = 3f + (p % 4),
                            center = Offset(pX, pY)
                        )
                    }
                }
            }

            // Procedural Dark Fountain Shader
            DarkFountainShader(
                modifier = Modifier.fillMaxSize(),
                time = timeMod,
                bass = avgBass,
                treble = avgTreble,
                dominantColor = dominant,
                vibrantColor = vibrant
            )

            // Front Particles
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fountainBaseWidth = size.width * 0.5f + (avgBass * size.width * 0.1f)
                for (p in 0 until numParticles) {
                    val pPhase = p * 137.5f
                    val pTime = timeMod * (0.5f + (p % 3) * 0.2f)
                    val pY = size.height - ((pTime * 200f + p * 50f) % (size.height * 1.2f))
                    val pAngle = pTime * 1.5f + pPhase
                    val pZ = kotlin.math.sin(pAngle.toDouble()).toFloat()
                    if (pY < size.height && pZ >= 0f) {
                        val pX = center.x + kotlin.math.cos(pAngle.toDouble()).toFloat() * (fountainBaseWidth * 0.7f)
                        drawCircle(
                            color = vibrant.copy(alpha = 0.8f),
                            radius = 4f + (p % 4),
                            center = Offset(pX, pY)
                        )
                    }
                }
            }
        }
        content()
    }
}
