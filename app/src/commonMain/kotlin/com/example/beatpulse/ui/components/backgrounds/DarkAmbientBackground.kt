package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager

expect @Composable
fun DarkAmbientBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
