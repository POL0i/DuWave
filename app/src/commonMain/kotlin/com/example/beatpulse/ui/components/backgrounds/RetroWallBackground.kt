package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
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

    // Continuous scroll time
    var timeMillis by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        val startTime = withFrameNanos { it / 1_000_000L }
        while (true) {
            withFrameNanos { frameTime ->
                timeMillis = (frameTime / 1_000_000L) - startTime
            }
        }
    }

    val cornerRadius = if (isPlayerScreen) 64.dp else 24.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(cornerRadius))) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseColor = if (paletteColors.dominant != Color.Unspecified && paletteColors.dominant != Color.Transparent) {
                    paletteColors.dominant
                } else if (paletteColors.vibrant != Color.Unspecified && paletteColors.vibrant != Color.Transparent) {
                    paletteColors.vibrant
                } else {
                    Color.DarkGray
                }
            
            // Helper to generate variants
            val variants = listOf(
                baseColor,
                Color(baseColor.red * 0.8f, baseColor.green * 0.8f, baseColor.blue * 0.8f, baseColor.alpha),
                Color(baseColor.red * 0.6f, baseColor.green * 0.6f, baseColor.blue * 0.6f, baseColor.alpha),
                Color(baseColor.red * 0.4f, baseColor.green * 0.4f, baseColor.blue * 0.4f, baseColor.alpha)
            )

            scale(scale = animatedScale, pivot = center) {
                // Smaller bricks
                val brickWidth = size.width / (if (isPlayerScreen) 35 else 50)
                val brickHeight = brickWidth / 2.2f
                val rowsVisible = (size.height / brickHeight).toInt() + 15
                val cols = (size.width / brickWidth).toInt() + 10

                // Scroll offset (upwards)
                val pixelsPerSecond = size.height * 0.1f // scroll 10% of screen per second
                val globalYOffset = (timeMillis / 1000f) * pixelsPerSecond
                
                val firstVisibleRow = (globalYOffset / brickHeight).toInt() - 5
                val lastVisibleRow = firstVisibleRow + rowsVisible
                
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                for (row in firstVisibleRow..lastVisibleRow) {
                    val isOffsetRow = (row % 2 == 0) // adjusted for negative rows
                    val startX = if (isOffsetRow) -brickWidth / 2f else 0f
                    for (col in -5..cols) {
                        val x = startX + (col * brickWidth)
                        val y = (row * brickHeight) - globalYOffset
                        
                        // Select color based on a pseudo-random pattern
                        val randomHash = ((row * 31) + (col * 17) + (row xor col)) % variants.size
                        val brickColor = variants[kotlin.math.abs(randomHash)]
                        
                        val bx = x + brickWidth / 2f
                        val by = y + brickHeight / 2f
                        
                        // Barrel distortion (Fisheye)
                        val dx = bx - centerX
                        val dy = by - centerY
                        
                        val nx = (dx / (size.width / 2f)).coerceIn(-1.5f, 1.5f)
                        val ny = (dy / (size.height / 2f)).coerceIn(-1.5f, 1.5f)
                        val distSq = nx * nx + ny * ny
                        
                        // k controls distortion strength (negative = bulge/globe effect)
                        val k = -0.05f
                        val distortion = (1f + k * distSq).coerceAtLeast(0.5f)
                        
                        val newBx = centerX + dx * distortion
                        val newBy = centerY + dy * distortion
                        val newWidth = (brickWidth - 4f) * distortion
                        val newHeight = (brickHeight - 4f) * distortion
                        
                        drawRect(
                            color = brickColor,
                            topLeft = Offset(newBx - newWidth / 2f, newBy - newHeight / 2f),
                            size = Size(newWidth, newHeight)
                        )
                    }
                }
            }
            
            // CRT Effects overlay
            // 1. Scanlines
            val scanlineHeight = 4f
            val numScanlines = (size.height / scanlineHeight).toInt()
            for (i in 0..numScanlines) {
                if (i % 2 == 0) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.15f),
                        topLeft = Offset(0f, i * scanlineHeight),
                        size = Size(size.width, scanlineHeight)
                    )
                }
            }
            
            // 2. Vignette (Darkening towards edges)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    center = center,
                    radius = size.width * 0.75f
                ),
                size = size
            )
            
            // 3. Thick inner TV border
            val borderThickness = size.width * 0.05f
            val crtCorner = cornerRadius.toPx() * 1.5f
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.6f),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(crtCorner, crtCorner),
                style = Stroke(width = borderThickness)
            )
        }
        
        content()
        }
    }
}
