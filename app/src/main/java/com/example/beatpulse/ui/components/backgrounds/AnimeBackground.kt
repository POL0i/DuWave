package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.visualizer.AudioVisualizerManager
import kotlin.math.sin

@Composable
fun AnimeBackground(
    paletteColors: PaletteColors,
    visualizerManager: AudioVisualizerManager,
    isPlayerScreen: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val lightVibrantState = animateColorAsState(paletteColors.lightVibrant, tween(1500), label = "ab_lv")
    val mutedState = animateColorAsState(paletteColors.muted, tween(1500), label = "ab_m")
    val dominantState = animateColorAsState(paletteColors.dominant, tween(1500), label = "ab_d")
    val vibrantState = animateColorAsState(paletteColors.vibrant, tween(1500), label = "ab_v")

    val amplitudesState = visualizerManager.amplitudes.collectAsState()

    val reactFactor = if (isPlayerScreen) 0.8f else 0.2f
    var dynamicEnergy by remember { mutableFloatStateOf(0f) }
    
    // Custom loop to prevent recompositions
    LaunchedEffect(isPlayerScreen) {
        var smoothEnergy = 0f
        var lastTime = 0L
        while(true) {
            withFrameMillis { time ->
                if (lastTime == 0L) lastTime = time
                val dt = ((time - lastTime) / 1000f).coerceAtMost(0.1f)
                lastTime = time
                
                val currentAmps = amplitudesState.value
                var sumAmps = 0f
                val limit = if (currentAmps.size < 24) currentAmps.size else 24
                for (i in 0 until limit) {
                    sumAmps += currentAmps[i]
                }
                val rawEnergy = if (limit > 0) sumAmps / limit else 0f
                
                // Exponential decay interpolation for smoothness
                smoothEnergy += (rawEnergy - smoothEnergy) * (1f - kotlin.math.exp(-15f * dt))
                dynamicEnergy = smoothEnergy * reactFactor
                
                // --- LOOP_HOOK ---
            }
        }
    }

    
    var wavePhase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastTime = 0L
        while(true) {
            withFrameMillis { time ->
                if (lastTime == 0L) lastTime = time
                val dt = (time - lastTime) / 1000f
                lastTime = time
                
                // Speed increases with dynamicEnergy, integrated smoothly
                val speed = 1f + dynamicEnergy * 3f
                wavePhase += dt * speed
            }
        }
    }

    // Cached paths for performance
    val path1 = remember { Path() }
    val path2 = remember { Path() }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFFF5F8))) {
        // Soft blurry background simulated with giant gradients
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lightVibrant = lightVibrantState.value
            val muted = mutedState.value
            val dominant = dominantState.value
            val vibrant = vibrantState.value

            // Creamy base
            drawRect(color = Color(0xFFFFF0F5))
            
            // Giant blurry "album art" shadows
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        lightVibrant.copy(alpha = 0.6f + dynamicEnergy * 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.3f, size.height * 0.2f),
                    radius = size.maxDimension * (0.6f + dynamicEnergy * 0.2f)
                )
            )
            
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        muted.copy(alpha = 0.5f + dynamicEnergy * 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.5f),
                    radius = size.maxDimension * 0.8f
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        dominant.copy(alpha = 0.4f + dynamicEnergy * 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.2f, size.height * 0.9f),
                    radius = size.maxDimension * 0.6f
                )
            )

            // Bottom Wavy Shapes (Mountains)
            path1.reset()
            path2.reset()
            
            // Height increases with energy
            val waveHeight = (size.height * 0.08f) + (size.height * 0.15f * dynamicEnergy)
            val baseY1 = size.height * 0.85f
            val baseY2 = size.height * 0.92f

            path1.moveTo(0f, baseY1)
            path2.moveTo(0f, baseY2)

            val segments = 40
            val segmentWidth = size.width / segments

            for (i in 0..segments) {
                val x = i * segmentWidth
                val y1 = baseY1 + sin((i * 0.2f) + wavePhase) * waveHeight
                val y2 = baseY2 + sin((i * 0.15f) - wavePhase * 0.8f) * (waveHeight * 1.2f)
                
                path1.lineTo(x, y1.toFloat())
                path2.lineTo(x, y2.toFloat())
            }

            path1.lineTo(size.width, size.height)
            path1.lineTo(0f, size.height)
            path1.close()

            path2.lineTo(size.width, size.height)
            path2.lineTo(0f, size.height)
            path2.close()

            drawPath(
                path = path1,
                color = lightVibrant.copy(alpha = 0.35f + dynamicEnergy * 0.2f)
            )
            drawPath(
                path = path2,
                color = vibrant.copy(alpha = 0.25f + dynamicEnergy * 0.2f)
            )
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
