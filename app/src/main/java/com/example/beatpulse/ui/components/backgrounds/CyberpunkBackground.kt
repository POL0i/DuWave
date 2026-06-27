package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.visualizer.AudioVisualizerManager
import kotlin.random.Random

@Composable
fun CyberpunkBackground(
    paletteColors: PaletteColors,
    visualizerManager: AudioVisualizerManager,
    isPlayerScreen: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val darkVibrantState = animateColorAsState(paletteColors.darkVibrant, tween(1000), label = "cv_dv")
    val lightVibrantState = animateColorAsState(paletteColors.lightVibrant, tween(1000), label = "cv_lv")
    val vibrantState = animateColorAsState(paletteColors.vibrant, tween(1000), label = "cv_v")

    val amplitudesState = visualizerManager.amplitudes.collectAsState()

    val reactFactor = if (isPlayerScreen) 0.8f else 0.2f
    var dynamicEnergy by remember { mutableFloatStateOf(0f) }
    
    // Custom loop to prevent recompositions
    LaunchedEffect(isPlayerScreen) {
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
                dynamicEnergy += (rawEnergy - dynamicEnergy) * (1f - kotlin.math.exp(-15f * dt))
                dynamicEnergy = dynamicEnergy * reactFactor
                
                // --- LOOP_HOOK ---
            }
        }
    }

    
    val infiniteTransition = rememberInfiniteTransition(label = "cyberpunk_anim")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_pulse"
    )
    
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "flow_phase"
    )

    // Particle system state
    val numLasers = if (isPlayerScreen) 45 else 12
    val lasers = remember(isPlayerScreen) { List(numLasers) { LaserLine() } }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF030005))) {
        // Volumetric glow and bloom
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = glowPulse }) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        darkVibrantState.value.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.2f),
                    radius = size.maxDimension * 0.85f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        lightVibrantState.value.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.1f, size.height * 0.9f),
                    radius = size.maxDimension * 0.7f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        vibrantState.value.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.9f, size.height * 0.7f),
                    radius = size.maxDimension * 0.6f
                )
            )
        }

        // Animated particles and lasers
        var frameTime by remember { mutableLongStateOf(0L) }
        LaunchedEffect(lasers) {
            var lastTime = 0L
            while(true) {
                withFrameMillis { time ->
                    if (lastTime == 0L) lastTime = time
                    val dt = (time - lastTime) / 1000f
                    lastTime = time
                    frameTime = time
                    val safeDt = dt.coerceAtMost(0.1f)
                    lasers.forEach { it.update(safeDt, dynamicEnergy) }
                }
            }
        }
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val darkVibrant = darkVibrantState.value
            val lightVibrant = lightVibrantState.value
            val vibrant = vibrantState.value

            val t = frameTime // read state to force recomposition every frame
            val w = size.width
            val h = size.height
            lasers.forEach { l ->
                val yOffset = l.y * h
                
                // X progresses across screen based on direction
                val currentX = if (l.direction > 0) {
                    (l.progress * l.progress) * w * 1.5f - (w * 0.2f)
                } else {
                    w * 1.2f - (l.progress * l.progress) * w * 1.5f
                }
                
                // Color shifts slightly based on progress and phase
                val mixRatio = (kotlin.math.sin(flowPhase + l.colorShift) * 0.5f + 0.5f).toFloat()
                val laserColor = if (mixRatio > 0.5f) lightVibrant else vibrant
                
                val endX = if (l.direction > 0) {
                    currentX + (l.progress * 300f) + 50f
                } else {
                    currentX - (l.progress * 300f) - 50f
                }

                drawLine(
                    color = laserColor.copy(alpha = (kotlin.math.sin(l.progress * kotlin.math.PI).toFloat()) * 0.8f),
                    start = Offset(currentX, yOffset),
                    end = Offset(endX, yOffset),
                    strokeWidth = 2f + (dynamicEnergy * 5f)
                )
            }
        }

        // Flowing glassmorphism panel border
        // Make it much slower: flowPhase is 0..1000 over 20s. 
        // We want a slow drift.
        val flowOffset = (flowPhase * 5f) % 2000f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(32.dp))
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            vibrantState.value,
                            darkVibrantState.value,
                            lightVibrantState.value,
                            vibrantState.value
                        ),
                        start = Offset(flowOffset, flowOffset),
                        end = Offset(flowOffset + 500f, flowOffset + 500f)
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .background(Color(0xFF070711).copy(alpha = 0.65f))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            vibrantState.value.copy(alpha = 0.12f),
                            Color.Transparent,
                            darkVibrantState.value.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            content()
        }
    }
}

class LaserLine {
    var y = Random.nextFloat()
    var progress = Random.nextFloat()
    var colorShift = Random.nextFloat() * 10f
    var direction = if (Random.nextBoolean()) 1f else -1f

    fun update(dt: Float, energy: Float) {
        val speed = 0.05f + (energy * 0.4f)
        progress += speed * direction * dt
        if (progress > 1.0f) {
            progress = 1.0f
            direction = -1f
            y = Random.nextFloat()
            colorShift = Random.nextFloat() * 10f
        } else if (progress < 0.0f) {
            progress = 0.0f
            direction = 1f
            y = Random.nextFloat()
            colorShift = Random.nextFloat() * 10f
        }
    }
}
