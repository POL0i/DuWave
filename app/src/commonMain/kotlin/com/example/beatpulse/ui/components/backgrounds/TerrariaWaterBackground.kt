package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager
import kotlin.math.sin

@Composable
fun TerrariaWaterBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    val bassAmplitudes by visualizerManager.bassAmplitudes.collectAsState()
    val bassAvg = remember(bassAmplitudes) {
        if (bassAmplitudes.isNotEmpty()) bassAmplitudes.average().toFloat().let { if (it.isNaN()) 0f else it } else 0f
    }
    
    val timeMs = androidx.compose.animation.core.rememberInfiniteTransition(label = "time").animateFloat(
        initialValue = 0f, targetValue = 100000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000000, easing = androidx.compose.animation.core.LinearEasing)
        ), label = "time"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F1B2E))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val waterLevel = h * 0.4f
            
            // Draw pixelated sky
            drawRect(color = Color(0xFF1B2E4A), topLeft = Offset(x = 0f, y = 0f), size = Size(width = w, height = waterLevel))
            
            // Draw Terraria water with slight bass reaction (olas diminutas)
            val blockSize = 8f
            val waveAmplitude = 5f + (bassAvg * 10f) // Sutil
            val waveFrequency = 0.05f
            val timeOffset = timeMs.value * 0.05f
            
            for (x in 0 until w.toInt() step blockSize.toInt()) {
                val yOffset = sin(x * waveFrequency + timeOffset) * waveAmplitude
                val columnWaterLevel = waterLevel + yOffset
                
                // Top water block (brighter)
                drawRect(color = Color(0xFF285CC4).copy(alpha = 0.8f), topLeft = Offset(x = x.toFloat(), y = columnWaterLevel), size = Size(width = blockSize, height = blockSize))
                
                // Deep water
                drawRect(color = Color(0xFF123478).copy(alpha = 0.9f), topLeft = Offset(x = x.toFloat(), y = columnWaterLevel + blockSize), size = Size(width = blockSize, height = h - columnWaterLevel - blockSize))
            }
            
            // Small pixel bubbles
            for (i in 0..40) {
                val bubbleX = (i * 73 + timeMs.value * 0.1f) % w
                val bubbleY = h - ((i * 123 + timeMs.value * 0.3f) % (h - waterLevel))
                drawRect(color = Color(0xFF6B9CFF).copy(alpha = 0.6f), topLeft = Offset(x = bubbleX.toFloat(), y = bubbleY.toFloat()), size = Size(width = blockSize/2f, height = blockSize/2f))
            }
        }
        content()
    }
}
