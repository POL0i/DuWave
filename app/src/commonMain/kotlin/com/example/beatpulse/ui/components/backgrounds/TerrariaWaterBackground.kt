package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager
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
    
    var timeMs by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlayerScreen) {
        if (isPlayerScreen) {
            val startTime = withFrameNanos { it / 1_000_000L } - timeMs.toLong()
            while (true) {
                withFrameNanos { frameTime ->
                    timeMs = (frameTime / 1_000_000L - startTime).toFloat()
                }
            }
        }
    }

    val skyColor = paletteColors.dominant.copy(alpha = 0.8f)
    val cloudColor = paletteColors.vibrant.copy(alpha = 0.4f)
    val cloudLightColor = paletteColors.lightVibrant.copy(alpha = 0.6f)
    
    val baseWaterColor = paletteColors.vibrant
    val waterTop = baseWaterColor.copy(alpha = 0.85f)
    val waterDeep = paletteColors.muted.copy(alpha = 0.9f)
    
    val violentColor = paletteColors.vibrant.copy(alpha = 0.7f)
    val peakColor = paletteColors.muted.copy(alpha = 0.7f)
    val foamColor = Color.White.copy(alpha = 0.5f)

    // Pre-allocate paths to avoid allocations in Canvas
    val peakPath = remember { Path() }
    val violentPath = remember { Path() }
    val waterTopPath = remember { Path() }
    val waterDeepPath = remember { Path() }
    val foamPath = remember { Path() }
    val foamTopPath = remember { Path() }

    Box(modifier = Modifier.fillMaxSize().background(skyColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val baseWaterLevel = h * 0.65f
            
            // 1. Nubes gigantes pixel art (densas y asimétricas)
            val numCloudClusters = 4
            val cloudPixelSize = 14f 
            
            for (i in 0 until numCloudClusters) {
                val cloudX = (i * 350f + timeMs * 0.02f) % (w + 600f) - 300f
                val cloudY = (i * 57f) % (baseWaterLevel * 0.4f) + 30f
                val cloudAlpha = 0.4f + highAvg * 0.4f
                val cColor = cloudColor.copy(alpha = cloudAlpha.coerceIn(0f, 1f))
                val cLight = cloudLightColor.copy(alpha = (cloudAlpha + 0.2f).coerceIn(0f, 1f))
                
                val gridX = (cloudX / cloudPixelSize).toInt() * cloudPixelSize
                val gridY = (cloudY / cloudPixelSize).toInt() * cloudPixelSize
                
                drawRect(color = cColor, topLeft = Offset(gridX, gridY + cloudPixelSize * 2), size = Size(cloudPixelSize * 12, cloudPixelSize * 3))
                drawRect(color = cColor, topLeft = Offset(gridX + cloudPixelSize, gridY + cloudPixelSize * 5), size = Size(cloudPixelSize * 10, cloudPixelSize))
                drawRect(color = cLight, topLeft = Offset(gridX + cloudPixelSize * 3, gridY), size = Size(cloudPixelSize * 6, cloudPixelSize * 2))
                drawRect(color = cLight, topLeft = Offset(gridX + cloudPixelSize * 4, gridY - cloudPixelSize), size = Size(cloudPixelSize * 4, cloudPixelSize))
                drawRect(color = cColor, topLeft = Offset(gridX - cloudPixelSize * 2, gridY + cloudPixelSize * 3), size = Size(cloudPixelSize * 3, cloudPixelSize * 2))
                drawRect(color = cColor, topLeft = Offset(gridX + cloudPixelSize * 11, gridY + cloudPixelSize * 3), size = Size(cloudPixelSize * 3, cloudPixelSize * 2))
                
                if (i % 2 == 0) {
                    val subX = gridX + cloudPixelSize * 16
                    val subY = gridY + cloudPixelSize * 2
                    drawRect(color = cColor, topLeft = Offset(subX, subY + cloudPixelSize), size = Size(cloudPixelSize * 6, cloudPixelSize * 2))
                    drawRect(color = cLight, topLeft = Offset(subX + cloudPixelSize, subY), size = Size(cloudPixelSize * 3, cloudPixelSize))
                }
            }

            // 2. MAREAS Optimizadas usando Paths
            val blockSize = 12f // Aumentamos tamaño para menos nodos
            val timeOffset = timeMs * 0.04f
            
            peakPath.reset()
            violentPath.reset()
            waterTopPath.reset()
            waterDeepPath.reset()
            foamPath.reset()
            foamTopPath.reset()

            // Initialize paths
            peakPath.moveTo(0f, h)
            violentPath.moveTo(0f, h)
            waterTopPath.moveTo(0f, h)
            waterDeepPath.moveTo(0f, h)

            for (x in 0 until w.toInt() step blockSize.toInt()) {
                val fx = x.toFloat()
                
                val wave1 = sin(fx * 0.015f + timeOffset) * 10f + 
                            sin(fx * 0.03f - timeOffset * 0.7f) * 6f
                
                val violentAmplitude = 5f + bassAvg * 15f
                val phase2 = fx * 0.007f - timeOffset * 1.2f
                val wave2Base = sin(phase2 + 1.2f * cos(phase2)) + cos(fx * 0.02f + timeOffset * 0.8f) * 0.3f
                val wave2 = if (wave2Base > 0.7f) ((wave2Base - 0.7f) * (wave2Base - 0.7f)) * 30f * violentAmplitude else 0f
                
                val peakAmplitude = 10f + midAvg * 20f
                val phase3 = fx * 0.005f + timeOffset * 0.8f
                val wave3 = (sin(phase3 + 1.5f * cos(phase3)) * 0.7f + sin(fx * 0.012f - timeOffset * 0.5f) * 0.3f) * peakAmplitude

                val peakY = baseWaterLevel - 15f - wave3
                peakPath.lineTo(fx, peakY)
                if (wave3 > 12f) foamPath.addRect(androidx.compose.ui.geometry.Rect(fx, peakY, fx + blockSize, peakY + blockSize))

                val violentY = baseWaterLevel + 10f - wave2
                violentPath.lineTo(fx, violentY)
                if (wave2 > 3f) foamPath.addRect(androidx.compose.ui.geometry.Rect(fx, violentY, fx + blockSize, violentY + blockSize))

                val frontY = baseWaterLevel + 25f + wave1
                waterTopPath.lineTo(fx, frontY)
                waterTopPath.lineTo(fx, frontY + blockSize)
                waterTopPath.lineTo(fx - blockSize, frontY + blockSize)
                
                waterDeepPath.lineTo(fx, frontY + blockSize)
                
                foamTopPath.addRect(androidx.compose.ui.geometry.Rect(fx, frontY, fx + blockSize, frontY + blockSize))
            }

            peakPath.lineTo(w, h)
            violentPath.lineTo(w, h)
            waterDeepPath.lineTo(w, h)

            drawPath(peakPath, peakColor)
            drawPath(violentPath, violentColor)
            drawPath(waterDeepPath, waterDeep)
            // Para el top water usamos trazo de path no hace falta ya que es sólo un line block. Lo reescribimos:
            drawPath(waterDeepPath, waterDeep)
            
            drawPath(foamPath, foamColor)
            drawPath(foamTopPath, foamColor.copy(alpha = 0.2f))
            
            // Burbujas
            for (i in 0..30) {
                val bubbleX = (i * 97 + timeMs * 0.1f) % w
                val bubbleY = h - ((i * 151 + timeMs * 0.25f) % (h - baseWaterLevel - 30f))
                drawRect(color = foamColor.copy(alpha = 0.3f), topLeft = Offset(x = bubbleX.toFloat(), y = bubbleY.toFloat()), size = Size(width = blockSize/2f, height = blockSize/2f))
            }
        }
        content()
    }
}
