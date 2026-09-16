package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
expect fun ProceduralCRTCdc3rxBackground(
    dominantColor: Color,
    vibrantColor: Color,
    mutedColor: Color,
    dynamicEnergy: Float,
    dynamicOffsetY: Float,
    dynamicOffsetX: Float,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
)
