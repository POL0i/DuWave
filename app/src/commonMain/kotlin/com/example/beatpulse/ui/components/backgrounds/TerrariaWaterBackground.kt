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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TerrariaWaterBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    val bassAmplitudes by visualizerManager.bassAmplitudes.collectAsState()
    val midAmplitudes by visualizerManager.midAmplitudes.collectAsState()
    val highAmplitudes by visualizerManager.highAmplitudes.collectAsState()
    
    val bassAvg = remember(bassAmplitudes) { if (bassAmplitudes.isNotEmpty()) bassAmplitudes.average().toFloat().let { if (it.isNaN()) 0f else it } else 0f }
    val midAvg = remember(midAmplitudes) { if (midAmplitudes.isNotEmpty()) midAmplitudes.average().toFloat().let { if (it.isNaN()) 0f else it } else 0f }
    val highAvg = remember(highAmplitudes) { if (highAmplitudes.isNotEmpty()) highAmplitudes.average().toFloat().let { if (it.isNaN()) 0f else it } else 0f }
    
    val timeMs = androidx.compose.animation.core.rememberInfiniteTransition(label = "time").animateFloat(
        initialValue = 0f, targetValue = 100000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000000, easing = androidx.compose.animation.core.LinearEasing)
        ), label = "time"
    )

    // Colores basados en la paleta del álbum
    val skyColor = paletteColors.dominant.copy(alpha = 0.8f)
    val cloudColor = paletteColors.vibrant.copy(alpha = 0.3f)
    
    val baseWaterColor = paletteColors.vibrant
    val waterTop = baseWaterColor.copy(alpha = 0.85f)
    val waterDeep = paletteColors.muted.copy(alpha = 0.9f)
    
    val violentColor = paletteColors.vibrant.copy(alpha = 0.6f)
    val peakColor = paletteColors.muted.copy(alpha = 0.7f)

    Box(modifier = Modifier.fillMaxSize().background(skyColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Marea más baja (65% desde arriba en lugar del 40%)
            val baseWaterLevel = h * 0.65f
            
            // 1. CIELO: Nubes/estrellas que reaccionan a los altos
            val skyBlockSize = 12f
            for (i in 0..15) {
                val cloudX = (i * 150 + timeMs.value * 0.02f) % w
                val cloudY = (i * 37) % (baseWaterLevel * 0.7f)
                val cloudAlpha = 0.2f + highAvg * 0.5f
                drawRect(color = cloudColor.copy(alpha = cloudAlpha.coerceIn(0f, 1f)), topLeft = Offset(x = cloudX, y = cloudY.toFloat()), size = Size(width = skyBlockSize * 4, height = skyBlockSize))
                drawRect(color = cloudColor.copy(alpha = cloudAlpha.coerceIn(0f, 1f)), topLeft = Offset(x = cloudX + skyBlockSize, y = cloudY.toFloat() - skyBlockSize), size = Size(width = skyBlockSize * 2, height = skyBlockSize))
            }

            // 2. MAREAS: Tres tipos distintos
            val blockSize = 6f
            val timeOffset = timeMs.value * 0.05f
            
            for (x in 0 until w.toInt() step blockSize.toInt()) {
                val fx = x.toFloat()
                
                // Marea A: Pulida (Suma de senos para patrón menos predecible, reacciona a todo)
                val wave1 = sin(fx * 0.02f + timeOffset) * 10f + 
                            sin(fx * 0.04f - timeOffset * 0.8f) * 5f +
                            cos(fx * 0.01f + timeOffset * 0.5f) * (5f + bassAvg * 10f)
                
                // Marea B: Violenta (Picos agresivos de bajos que desaparecen por debajo)
                val violentAmplitude = bassAvg * 50f
                val wave2Raw = sin(fx * 0.015f - timeOffset * 2f)
                val wave2 = if (wave2Raw > 0.8f) {
                    (wave2Raw - 0.8f) * 5f * violentAmplitude
                } else {
                    0f // Se pierde debajo
                }
                
                // Marea C: Picos redondeados (Reacciona a los medios)
                val peakAmplitude = 15f + midAvg * 30f
                // Valor absoluto de seno da picos redondeados arriba y puntas abajo
                val wave3Raw = abs(sin(fx * 0.03f + timeOffset * 1.5f)) 
                val wave3 = wave3Raw * peakAmplitude

                // Dibujar Capa Trasera: Picos
                val peakY = baseWaterLevel - 15f - wave3
                drawRect(color = peakColor, topLeft = Offset(x = fx, y = peakY), size = Size(width = blockSize, height = h - peakY))

                // Dibujar Capa Media: Violenta
                if (wave2 > 0f) {
                    val violentY = baseWaterLevel + 10f - wave2
                    drawRect(color = violentColor, topLeft = Offset(x = fx, y = violentY), size = Size(width = blockSize, height = h - violentY))
                }

                // Dibujar Capa Frontal: Pulida
                val frontY = baseWaterLevel + wave1
                drawRect(color = waterTop, topLeft = Offset(x = fx, y = frontY), size = Size(width = blockSize, height = blockSize))
                drawRect(color = waterDeep, topLeft = Offset(x = fx, y = frontY + blockSize), size = Size(width = blockSize, height = h - frontY - blockSize))
            }
            
            // Burbujas
            for (i in 0..40) {
                val bubbleX = (i * 73 + timeMs.value * 0.1f) % w
                val bubbleY = h - ((i * 123 + timeMs.value * 0.3f) % (h - baseWaterLevel))
                drawRect(color = peakColor.copy(alpha = 0.5f), topLeft = Offset(x = bubbleX.toFloat(), y = bubbleY.toFloat()), size = Size(width = blockSize/2f, height = blockSize/2f))
            }
        }
        content()
    }
}
