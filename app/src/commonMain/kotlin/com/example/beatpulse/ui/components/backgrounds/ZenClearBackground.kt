package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager

@Composable
fun ZenClearBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    val dominant = paletteColors.dominant
    val isDark = dominant.luminance() < 0.5f
    // Pastel/Clear wash
    val bgColor = if (isDark) dominant.copy(alpha = 0.8f) else Color(0xFFF0F4F8)
    val glowColor = paletteColors.vibrant.copy(alpha = 0.15f)
    
    val timeMs = androidx.compose.animation.core.rememberInfiniteTransition(label = "time").animateFloat(
        initialValue = 0f, targetValue = 100000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000000, easing = androidx.compose.animation.core.LinearEasing)
        ), label = "time"
    )

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            
            // Draw very soft, slow moving light orbs
            val orb1X = cx + Math.sin(timeMs.value * 0.001) * w * 0.3f
            val orb1Y = cy + Math.cos(timeMs.value * 0.0012) * h * 0.3f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = Offset(orb1X.toFloat(), orb1Y.toFloat()),
                    radius = w * 0.8f
                ),
                radius = w * 0.8f,
                center = Offset(orb1X.toFloat(), orb1Y.toFloat())
            )
            
            val orb2X = cx + Math.sin(timeMs.value * 0.0008 + 2f) * w * 0.4f
            val orb2Y = cy + Math.cos(timeMs.value * 0.0009 + 1f) * h * 0.4f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(paletteColors.muted.copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(orb2X.toFloat(), orb2Y.toFloat()),
                    radius = w * 0.9f
                ),
                radius = w * 0.9f,
                center = Offset(orb2X.toFloat(), orb2Y.toFloat())
            )
        }
        content()
    }
}
