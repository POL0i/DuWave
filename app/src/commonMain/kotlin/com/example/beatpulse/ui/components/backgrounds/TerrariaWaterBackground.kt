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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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

    val skyColor = paletteColors.dominant.copy(alpha = 0.8f)
    val cloudColor = paletteColors.vibrant.copy(alpha = 0.4f)
    val cloudLightColor = paletteColors.lightVibrant.copy(alpha = 0.6f)
    
    val baseWaterColor = paletteColors.vibrant
    val waterTop = baseWaterColor.copy(alpha = 0.85f)
    val waterDeep = paletteColors.muted.copy(alpha = 0.9f)
    
    val violentColor = paletteColors.vibrant.copy(alpha = 0.7f)
    val peakColor = paletteColors.muted.copy(alpha = 0.7f)
    val foamColor = Color.White.copy(alpha = 0.5f)

    Box(modifier = Modifier.fillMaxSize().background(skyColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val baseWaterLevel = h * 0.65f
            
            // 1. CIELO: Nubes densas y con forma de pixel art (avanzan de izquierda a derecha)
            val numCloudClusters = 4
            val cloudPixelSize = 12f
            
            for (i in 0 until numCloudClusters) {
                // Posicionamiento de las nubes (avanzan lento)
                val cloudX = (i * 350f + timeMs.value * 0.02f) % (w + 400f) - 200f
                val cloudY = (i * 57f) % (baseWaterLevel * 0.4f) + 30f
                val cloudAlpha = 0.3f + highAvg * 0.5f
                val cColor = cloudColor.copy(alpha = cloudAlpha.coerceIn(0f, 1f))
                val cLight = cloudColor.copy(alpha = (cloudAlpha + 0.2f).coerceIn(0f, 1f))
                
                // Dibujamos una nube densa con un "molde" fijo para darle forma característica
                val cloudShape = listOf(
                    // Fila 0 (top)
                    3 to 0, 4 to 0, 5 to 0, 6 to 0,
                    // Fila 1
                    2 to 1, 3 to 1, 4 to 1, 5 to 1, 6 to 1, 7 to 1, 8 to 1,
                    // Fila 2
                    1 to 2, 2 to 2, 3 to 2, 4 to 2, 5 to 2, 6 to 2, 7 to 2, 8 to 2, 9 to 2, 10 to 2,
                    // Fila 3
                    0 to 3, 1 to 3, 2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 3, 7 to 3, 8 to 3, 9 to 3, 10 to 3, 11 to 3,
                    // Fila 4 (bottom)
                    1 to 4, 2 to 4, 3 to 4, 4 to 4, 5 to 4, 6 to 4, 7 to 4, 8 to 4, 9 to 4, 10 to 4
                )
                
                for ((cx, cy) in cloudShape) {
                    // Darle un sombreado leve (la parte de arriba es más clara)
                    val color = if (cy < 2) cLight else cColor
                    drawRect(
                        color = color, 
                        topLeft = Offset(cloudX + cx * cloudPixelSize, cloudY + cy * cloudPixelSize), 
                        size = Size(cloudPixelSize, cloudPixelSize)
                    )
                }
            }

            // 2. MAREAS
            val blockSize = 6f
            val timeOffset = timeMs.value * 0.04f // Ligeramente más lento
            
            for (x in 0 until w.toInt() step blockSize.toInt()) {
                val fx = x.toFloat()
                
                // Marea A: Pulida (Capa frontal) - Olas muy suaves
                val wave1 = sin(fx * 0.015f + timeOffset) * 10f + 
                            sin(fx * 0.03f - timeOffset * 0.7f) * 6f
                
                // Marea B: Violenta - Transición más lógica y suave
                val violentAmplitude = 5f + bassAvg * 20f
                val wave2Base = sin(fx * 0.008f - timeOffset * 1.5f) + 
                                cos(fx * 0.02f + timeOffset * 0.8f) * 0.5f
                val wave2 = if (wave2Base > 0.8f) {
                    // Transición cuadrática para que no haya salto abrupto
                    val excess = wave2Base - 0.8f
                    (excess * excess) * 30f * violentAmplitude
                } else {
                    0f
                }
                
                // Marea C: Picos redondeados - Mucho más suaves y menos frecuentes
                val peakAmplitude = 10f + midAvg * 20f
                // Suma de senos de muy baja frecuencia genera paisajes suaves en lugar del abs()
                val wave3Raw = sin(fx * 0.006f + timeOffset * 0.9f) + 
                               sin(fx * 0.011f - timeOffset * 0.6f) * 0.6f +
                               cos(fx * 0.018f + timeOffset * 1.1f) * 0.3f
                val wave3 = wave3Raw * peakAmplitude

                // -- Dibujar Capa Trasera: Picos (Suaves) --
                val peakY = baseWaterLevel - 15f - wave3
                drawRect(color = peakColor, topLeft = Offset(x = fx, y = peakY), size = Size(width = blockSize, height = h - peakY))
                // Pequeña espuma en la cresta si es alta
                if (wave3 > 15f) {
                    drawRect(color = foamColor, topLeft = Offset(x = fx, y = peakY), size = Size(width = blockSize, height = blockSize))
                }

                // -- Dibujar Capa Media: Violenta (Suave y con margen) --
                val violentY = baseWaterLevel + 10f - wave2
                val baseViolentY = baseWaterLevel + 40f 
                drawRect(color = violentColor, topLeft = Offset(x = fx, y = violentY), size = Size(width = blockSize, height = baseViolentY - violentY))
                // Espuma agresiva
                if (wave2 > 5f) {
                    drawRect(color = foamColor, topLeft = Offset(x = fx, y = violentY), size = Size(width = blockSize, height = blockSize))
                }

                // -- Dibujar Capa Frontal: Pulida --
                val frontY = baseWaterLevel + 25f + wave1
                drawRect(color = waterTop, topLeft = Offset(x = fx, y = frontY), size = Size(width = blockSize, height = blockSize))
                drawRect(color = waterDeep, topLeft = Offset(x = fx, y = frontY + blockSize), size = Size(width = blockSize, height = h - frontY - blockSize))
                // Espuma frontal
                drawRect(color = foamColor.copy(alpha = 0.2f), topLeft = Offset(x = fx, y = frontY), size = Size(width = blockSize, height = blockSize))
            }
            
            // Burbujas
            for (i in 0..30) {
                val bubbleX = (i * 97 + timeMs.value * 0.1f) % w
                val bubbleY = h - ((i * 151 + timeMs.value * 0.25f) % (h - baseWaterLevel - 30f))
                drawRect(color = foamColor.copy(alpha = 0.3f), topLeft = Offset(x = bubbleX.toFloat(), y = bubbleY.toFloat()), size = Size(width = blockSize/2f, height = blockSize/2f))
            }
        }
        content()
    }
}
