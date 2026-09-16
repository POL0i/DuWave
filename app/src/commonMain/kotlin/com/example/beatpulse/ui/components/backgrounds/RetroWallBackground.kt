package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.theme.VT323
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager

@Composable
fun RetroWallBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit = {}
) {
    val bassAmplitudes by visualizerManager.bassAmplitudes.collectAsState()
    val trebleAmplitudes by visualizerManager.highAmplitudes.collectAsState()

    val avgBass = if (bassAmplitudes.isNotEmpty()) {
        val avg = bassAmplitudes.average().toFloat()
        if (avg.isNaN()) 0f else avg
    } else 0f

    val avgTreble = if (trebleAmplitudes.isNotEmpty()) {
        val avg = trebleAmplitudes.average().toFloat()
        if (avg.isNaN()) 0f else avg
    } else 0f
    
    // Zoom effect
    val targetScale = 1f + (avgBass * 0.6f) + (avgTreble * 0.2f)
    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (targetScale.isNaN()) 1f else targetScale.coerceIn(1f, 1.4f),
        animationSpec = androidx.compose.animation.core.tween(150)
    )

    val currentIsPlayerScreen by rememberUpdatedState(isPlayerScreen)

    var timeMillis by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (isActive) {
            withFrameNanos { currentTime ->
                if (lastTime == 0L) lastTime = currentTime
                val dt = ((currentTime - lastTime) / 1_000_000f)
                timeMillis += dt.toLong()
                lastTime = currentTime
            }
            if (!currentIsPlayerScreen) kotlinx.coroutines.delay(24L)
        }
    }

    val cornerRadius = if (isPlayerScreen) 64.dp else 24.dp
    
    // Pre-allocated paths for 4 color variants
    val paths = remember { Array(4) { Path() } }
    val cachedScanlinePath = remember { Path() }
        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(cornerRadius))) {
            Spacer(modifier = Modifier.fillMaxSize().drawWithCache {
                val baseColor = if (paletteColors.dominant != Color.Unspecified && paletteColors.dominant != Color.Transparent) {
                    paletteColors.dominant
                } else if (paletteColors.vibrant != Color.Unspecified && paletteColors.vibrant != Color.Transparent) {
                    paletteColors.vibrant
                } else {
                    Color.DarkGray
                }
            
                val variants = listOf(
                    baseColor,
                    Color(baseColor.red * 0.8f, baseColor.green * 0.8f, baseColor.blue * 0.8f, baseColor.alpha),
                    Color(baseColor.red * 0.6f, baseColor.green * 0.6f, baseColor.blue * 0.6f, baseColor.alpha),
                    Color(baseColor.red * 0.4f, baseColor.green * 0.4f, baseColor.blue * 0.4f, baseColor.alpha)
                )

                val brickWidth = size.width / (if (isPlayerScreen) 8 else 12)
                val brickHeight = brickWidth / 2.2f
                
                paths.forEach { it.reset() }
                cachedScanlinePath.reset()
                
                val rowsVisible = (size.height / brickHeight).toInt() + 4
                val cols = (size.width / brickWidth).toInt() + 4

                for (row in -2..rowsVisible) {
                    val isOffsetRow = (row % 2 == 0)
                    val startX = if (isOffsetRow) -brickWidth / 2f else 0f
                    for (col in -2..cols) {
                        val x = startX + (col * brickWidth)
                        val y = (row * brickHeight)
                        
                        val randomHash = ((row * 31) + (col * 17) + (row xor col)) % variants.size
                        val colorIdx = kotlin.math.abs(randomHash)
                        
                        paths[colorIdx].addRect(androidx.compose.ui.geometry.Rect(x + 2f, y + 2f, x + brickWidth - 2f, y + brickHeight - 2f))
                    }
                }
                
                val scanlineHeight = 18f
                val numScanlines = (size.height / scanlineHeight).toInt()
                for (i in 0..numScanlines step 2) {
                    cachedScanlinePath.addRect(androidx.compose.ui.geometry.Rect(0f, i * scanlineHeight, size.width, i * scanlineHeight + scanlineHeight))
                }
                
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val radialBrush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    center = centerOffset,
                    radius = size.width * 0.75f
                )
                
                onDrawBehind {
                    scale(scale = animatedScale, pivot = centerOffset) {
                        val pixelsPerSecond = size.height * 0.1f 
                        val wrapHeight = brickHeight * 2f
                        val globalYOffset = ((timeMillis / 1000f) * pixelsPerSecond) % wrapHeight
                        
                        translate(top = -globalYOffset) {
                            for (i in 0 until 4) {
                                drawPath(paths[i], variants[i])
                            }
                        }
                    }
                    
                    drawPath(cachedScanlinePath, Color.Black.copy(alpha = 0.15f))
                    
                    drawRect(
                        brush = radialBrush,
                        size = size
                    )
                    
                    val borderThickness = size.width * 0.05f
                    val crtCorner = cornerRadius.toPx() * 1.5f
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(crtCorner, crtCorner),
                        style = Stroke(width = borderThickness)
                    )
                }
            })
            content()
        }
}
