package com.example.beatpulse.core.focus

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

fun Modifier.animatedFocusBorder(
    section: FocusSection,
    activeColor: Color = Color(0xFF00FFCC),
    strokeWidth: Dp = 3.dp
): Modifier = composed {
    val isFocused = AppFocusManager.isTabNavigationActive && AppFocusManager.currentFocusedSection == section
    
    if (!isFocused) {
        return@composed this
    }

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    this.then(
        Modifier.drawWithCache {
            onDrawWithContent {
                drawContent()
                val sweepGradient = Brush.sweepGradient(
                    colors = listOf(activeColor.copy(alpha = 0f), activeColor, activeColor.copy(alpha = 0f)),
                    center = Offset(size.width / 2f, size.height / 2f)
                )
                drawRect(
                    brush = sweepGradient,
                    style = Stroke(width = strokeWidth.toPx())
                )
                // A true rotating border requires a custom shader or rotating the canvas
                // For simplicity and performance, we'll draw a dashed/moving line or just a simple border that highlights
            }
        }
    )
}
