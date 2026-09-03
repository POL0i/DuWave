package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.Modifier
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager
import kotlinx.coroutines.isActive

@Composable
fun SandsFlowBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    val bassAmplitudes by visualizerManager.bassAmplitudes.collectAsState()
    val trebleAmplitudes by visualizerManager.highAmplitudes.collectAsState()
    
    val bassAvgRaw = remember(bassAmplitudes) { if (bassAmplitudes.isNotEmpty()) bassAmplitudes.average().toFloat().let { if (it.isNaN()) 0f else it } else 0f }
    val trebleAvgRaw = remember(trebleAmplitudes) { if (trebleAmplitudes.isNotEmpty()) trebleAmplitudes.average().toFloat().let { if (it.isNaN()) 0f else it } else 0f }
    
    val bassAvg by animateFloatAsState(targetValue = bassAvgRaw, animationSpec = tween(durationMillis = 150, easing = LinearEasing), label = "bassSmooth")
    val trebleAvg by animateFloatAsState(targetValue = trebleAvgRaw, animationSpec = tween(durationMillis = 150, easing = LinearEasing), label = "trebleSmooth")

    var accumulatedTime by remember { mutableStateOf(0f) }
    
    // We need to read the latest bass inside the frame loop without capturing the old value
    val latestBass by rememberUpdatedState(bassAvg)

    LaunchedEffect(isPlayerScreen) {
        if (isPlayerScreen) {
            var lastTime = withFrameNanos { it }
            while (isActive) {
                val currentTime = withFrameNanos { it }
                val dt = (currentTime - lastTime) / 1_000_000_000f
                // Base speed + bass acceleration
                accumulatedTime += dt * (1f + latestBass * 1.0f)
                lastTime = currentTime
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SandsFlowShader(
            modifier = Modifier.fillMaxSize(),
            time = accumulatedTime,
            bass = bassAvg,
            treble = trebleAvg,
            dominantColor = paletteColors.dominant,
            vibrantColor = paletteColors.vibrant,
            mutedColor = paletteColors.muted
        )
        content()
    }
}
