package com.example.beatpulse.ui.components.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun CircularVolumeSlider(
    volume: Float, // 0.0f to 2.0f
    onVolumeChange: (Float) -> Unit,
    colorNormal: Color,
    colorBoost: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    // Determine the target size based on volume > 1.0f
    val isBoosted = volume > 1.0f
    val baseScale by animateFloatAsState(
        targetValue = if (isBoosted) 0.8f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "slider_scale"
    )

    Box(
        modifier = modifier
            .size(100.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Compute angle based on position
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val pos = change.position
                    val angle = atan2(pos.y - center.y, pos.x - center.x)
                    // Convert to degrees and map so that bottom-center is 0, sweeping clockwise to 360
                    var degrees = Math.toDegrees(angle.toDouble()).toFloat() + 90f
                    if (degrees < 0) degrees += 360f

                    // Limit range: we use 40 to 320 degrees (280 degree sweep)
                    val minDeg = 40f
                    val maxDeg = 320f
                    val sweep = maxDeg - minDeg

                    var clamped = degrees
                    if (clamped < minDeg) {
                        clamped = if (clamped < 20f) minDeg else maxDeg
                    } else if (clamped > maxDeg) {
                        clamped = if (clamped > 340f) minDeg else maxDeg
                    }
                    
                    val fraction = (clamped - minDeg) / sweep
                    // Maps to 0.0 .. 2.0
                    onVolumeChange((fraction * 2.0f).coerceIn(0f, 2f))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius1 = (size.width / 2f - strokeWidth) * baseScale
            val radius2 = (size.width / 2f - strokeWidth) // The outer boosted ring
            
            val startAngle = 130f // From bottom left
            val totalSweep = 280f

            // Inner track (Normal volume 0.0 - 1.0)
            drawArc(
                color = colorNormal.copy(alpha = 0.2f),
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius1, center.y - radius1),
                size = androidx.compose.ui.geometry.Size(radius1 * 2, radius1 * 2)
            )

            // Inner fill
            val normalFraction = volume.coerceAtMost(1f)
            if (normalFraction > 0) {
                drawArc(
                    color = colorNormal,
                    startAngle = startAngle,
                    sweepAngle = totalSweep * normalFraction,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius1, center.y - radius1),
                    size = androidx.compose.ui.geometry.Size(radius1 * 2, radius1 * 2)
                )
            }

            // Outer track (Boost volume 1.0 - 2.0)
            if (isBoosted) {
                drawArc(
                    color = colorBoost.copy(alpha = 0.2f),
                    startAngle = startAngle,
                    sweepAngle = totalSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius2, center.y - radius2),
                    size = androidx.compose.ui.geometry.Size(radius2 * 2, radius2 * 2)
                )

                val boostFraction = (volume - 1f).coerceAtMost(1f)
                if (boostFraction > 0) {
                    drawArc(
                        color = colorBoost,
                        startAngle = startAngle,
                        sweepAngle = totalSweep * boostFraction,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - radius2, center.y - radius2),
                        size = androidx.compose.ui.geometry.Size(radius2 * 2, radius2 * 2)
                    )
                }
            }
        }

        // Center Icon & Percentage
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val icon = when {
                volume == 0f -> Icons.Default.VolumeMute
                volume < 1f -> Icons.Default.VolumeDown
                else -> Icons.Default.VolumeUp
            }
            Icon(
                imageVector = icon,
                contentDescription = "Volume",
                tint = if (isBoosted) colorBoost else colorNormal,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "${(volume * 100).toInt()}%",
                color = textColor,
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}
