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
            
            // 1. CIELO: Nubes gigantes pixel art (densas y asimétricas)
            val numCloudClusters = 4
            val cloudPixelSize = 14f // Píxeles más grandes para mayor densidad
            
            for (i in 0 until numCloudClusters) {
                // Posicionamiento de las nubes
                val cloudX = (i * 350f + timeMs.value * 0.02f) % (w + 600f) - 300f
                val cloudY = (i * 57f) % (baseWaterLevel * 0.4f) + 30f
                val cloudAlpha = 0.4f + highAvg * 0.4f
                val cColor = cloudColor.copy(alpha = cloudAlpha.coerceIn(0f, 1f))
                val cLight = cloudLightColor.copy(alpha = (cloudAlpha + 0.2f).coerceIn(0f, 1f))
                
                // Alineamos al grid de píxeles
                val gridX = (cloudX / cloudPixelSize).toInt() * cloudPixelSize
                val gridY = (cloudY / cloudPixelSize).toInt() * cloudPixelSize
                
                // Generamos una nube enorme combinando rectángulos
                // Base ancha central
                drawRect(color = cColor, topLeft = Offset(gridX, gridY + cloudPixelSize * 2), size = Size(cloudPixelSize * 12, cloudPixelSize * 3))
                drawRect(color = cColor, topLeft = Offset(gridX + cloudPixelSize, gridY + cloudPixelSize * 5), size = Size(cloudPixelSize * 10, cloudPixelSize))
                
                // Bulto superior (Iluminado)
                drawRect(color = cLight, topLeft = Offset(gridX + cloudPixelSize * 3, gridY), size = Size(cloudPixelSize * 6, cloudPixelSize * 2))
                drawRect(color = cLight, topLeft = Offset(gridX + cloudPixelSize * 4, gridY - cloudPixelSize), size = Size(cloudPixelSize * 4, cloudPixelSize))
                
                // Bultos laterales
                drawRect(color = cColor, topLeft = Offset(gridX - cloudPixelSize * 2, gridY + cloudPixelSize * 3), size = Size(cloudPixelSize * 3, cloudPixelSize * 2))
                drawRect(color = cColor, topLeft = Offset(gridX + cloudPixelSize * 11, gridY + cloudPixelSize * 3), size = Size(cloudPixelSize * 3, cloudPixelSize * 2))
                
                // Nubes secundarias más pequeñas atadas a la principal
                if (i % 2 == 0) {
                    val subX = gridX + cloudPixelSize * 16
                    val subY = gridY + cloudPixelSize * 2
                    drawRect(color = cColor, topLeft = Offset(subX, subY + cloudPixelSize), size = Size(cloudPixelSize * 6, cloudPixelSize * 2))
                    drawRect(color = cLight, topLeft = Offset(subX + cloudPixelSize, subY), size = Size(cloudPixelSize * 3, cloudPixelSize))
                }
            }

            // 2. MAREAS
            val blockSize = 6f
            val timeOffset = timeMs.value * 0.04f
            
            for (x in 0 until w.toInt() step blockSize.toInt()) {
                val fx = x.toFloat()
                
                // Marea A: Pulida (Capa frontal) - Olas suaves
                val wave1 = sin(fx * 0.015f + timeOffset) * 10f + 
                            sin(fx * 0.03f - timeOffset * 0.7f) * 6f
                
                // Marea B: Violenta - Asimétrica y suave
                // Usamos sin(phase + 1.2 * cos(phase)) para generar olas con relieve progresivo y caída abrupta (forma de ola real)
                val violentAmplitude = 5f + bassAvg * 15f
                val phase2 = fx * 0.007f - timeOffset * 1.2f
                val wave2Base = sin(phase2 + 1.2f * cos(phase2)) + cos(fx * 0.02f + timeOffset * 0.8f) * 0.3f
                val wave2 = if (wave2Base > 0.7f) {
                    val excess = wave2Base - 0.7f
                    (excess * excess) * 30f * violentAmplitude
                } else {
                    0f
                }
                
                // Marea C: Picos redondeados - Forma de ola asimétrica natural (relieve en la izq, caída marcada en la der)
                val peakAmplitude = 10f + midAvg * 20f
                val phase3 = fx * 0.005f + timeOffset * 0.8f
                // La asimetría matemática crea el relieve suave a la izquierda y la caída marcada a la derecha
                val wave3Raw = sin(phase3 + 1.5f * cos(phase3)) * 0.7f + 
                               sin(fx * 0.012f - timeOffset * 0.5f) * 0.3f
                val wave3 = wave3Raw * peakAmplitude

                // -- Dibujar Capa Trasera: Picos (Asimétricos) --
                val peakY = baseWaterLevel - 15f - wave3
                drawRect(color = peakColor, topLeft = Offset(x = fx, y = peakY), size = Size(width = blockSize, height = h - peakY))
                // Espuma en la cresta de la ola asimétrica
                if (wave3 > 12f) {
                    drawRect(color = foamColor, topLeft = Offset(x = fx, y = peakY), size = Size(width = blockSize, height = blockSize))
                }

                // -- Dibujar Capa Media: Violenta (Suave, con margen, asimétrica) --
                val violentY = baseWaterLevel + 10f - wave2
                val baseViolentY = baseWaterLevel + 40f 
                drawRect(color = violentColor, topLeft = Offset(x = fx, y = violentY), size = Size(width = blockSize, height = baseViolentY - violentY))
                // Espuma agresiva
                if (wave2 > 3f) {
                    drawRect(color = foamColor, topLeft = Offset(x = fx, y = violentY), size = Size(width = blockSize, height = blockSize))
                }

                // -- Dibujar Capa Frontal: Pulida --
                val frontY = baseWaterLevel + 25f + wave1
                drawRect(color = waterTop, topLeft = Offset(x = fx, y = frontY), size = Size(width = blockSize, height = blockSize))
                drawRect(color = waterDeep, topLeft = Offset(x = fx, y = frontY + blockSize), size = Size(width = blockSize, height = h - frontY - blockSize))
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
