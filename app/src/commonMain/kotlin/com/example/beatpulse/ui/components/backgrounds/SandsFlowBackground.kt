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
    
    val bassAvgState = animateFloatAsState(targetValue = bassAvgRaw, animationSpec = tween(durationMillis = 150, easing = LinearEasing), label = "bassSmooth")
    val trebleAvgState = animateFloatAsState(targetValue = trebleAvgRaw, animationSpec = tween(durationMillis = 150, easing = LinearEasing), label = "trebleSmooth")

    val accumulatedTimeState = remember { mutableStateOf(0f) }

    val currentIsPlayerScreen by rememberUpdatedState(isPlayerScreen)

    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (isActive) {
            withFrameNanos { currentTime ->
                if (lastTime == 0L) lastTime = currentTime
                val dt = ((currentTime - lastTime) / 1_000_000_000f).coerceAtMost(0.1f)
                val currentBass = bassAvgState.value
                accumulatedTimeState.value += dt * (1f + currentBass * 1.0f)
                lastTime = currentTime
            }
            if (!currentIsPlayerScreen) kotlinx.coroutines.delay(24L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SandsFlowShader(
            modifier = Modifier.fillMaxSize(),
            time = accumulatedTimeState,
            bass = bassAvgState,
            treble = trebleAvgState,
            dominantColor = paletteColors.dominant,
            vibrantColor = paletteColors.vibrant,
            mutedColor = paletteColors.muted
        )
        content()
    }
}
