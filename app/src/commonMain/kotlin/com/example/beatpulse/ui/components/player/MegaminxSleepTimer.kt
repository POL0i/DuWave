package com.example.beatpulse.ui.components.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MegaminxSleepTimerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    sleepTimerSeconds: Int,
    onSetSleepTimer: (Int) -> Unit,
    colorVibrant: Color,
    colorDominant: Color,
    bgStyle: Int = 0
) {
    if (!showDialog) return

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .size(360.dp)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            MegaminxInteractiveFace(
                timerSeconds = sleepTimerSeconds,
                onAddMinutes = { 
                    val next = sleepTimerSeconds + it * 60
                    onSetSleepTimer(if (next < 60) 0 else next) 
                },
                onAddHours = { 
                    val next = sleepTimerSeconds + it * 3600
                    onSetSleepTimer(if (next < 60) 0 else next) 
                },
                onCenterTap = {
                    if (sleepTimerSeconds > 0) {
                        onSetSleepTimer(0) // Cancel
                    } else {
                        onSetSleepTimer(300) // Add 5 mins
                    }
                },
                colorVibrant = colorVibrant,
                colorDominant = colorDominant,
                bgStyle = bgStyle
            )
        }
    }
}

@Composable
fun MegaminxInteractiveFace(
    timerSeconds: Int,
    onAddMinutes: (Int) -> Unit,
    onAddHours: (Int) -> Unit,
    onCenterTap: () -> Unit,
    colorVibrant: Color,
    colorDominant: Color,
    bgStyle: Int
) {
    val coroutineScope = rememberCoroutineScope()
    val currentOnAddMinutes by rememberUpdatedState(onAddMinutes)
    val currentOnAddHours by rememberUpdatedState(onAddHours)
    val currentOnCenterTap by rememberUpdatedState(onCenterTap)
    
    // Rotations for the inner (hours) and outer (minutes) rings
    var innerRotation by remember { mutableFloatStateOf(0f) }
    var outerRotation by remember { mutableFloatStateOf(0f) }
    
    val animatedInnerRotation = remember { Animatable(0f) }
    val animatedOuterRotation = remember { Animatable(0f) }
    
    val formatTime = {
        val h = timerSeconds / 3600
        val m = (timerSeconds % 3600) / 60
        val s = timerSeconds % 60
        if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dist = (offset - center).getDistance()
                        if (dist < size.width * 0.22f) { // Tap in center pentagon
                            currentOnCenterTap()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                var initialAngle = 0f
                var currentRing = 0 // 0 = none, 1 = inner (hours), 2 = outer (minutes)
                
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dist = (offset - center).getDistance()
                        val maxR = size.width / 2f
                        
                        initialAngle = (atan2(offset.y - center.y, offset.x - center.x) * 180f / PI.toFloat())
                        
                        if (dist > maxR * 0.25f && dist < maxR * 0.55f) {
                            currentRing = 1 // Inner ring
                        } else if (dist >= maxR * 0.55f && dist < maxR * 0.95f) {
                            currentRing = 2 // Outer ring
                        } else {
                            currentRing = 0
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (currentRing == 0) return@detectDragGestures
                        change.consume()
                        
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val pos = change.position
                        val currentAngle = (atan2(pos.y - center.y, pos.x - center.x) * 180f / PI.toFloat())
                        
                        var delta = currentAngle - initialAngle
                        // Handle wrap around -180 to 180
                        if (delta > 180f) delta -= 360f
                        if (delta < -180f) delta += 360f
                        
                        initialAngle = currentAngle
                        
                        coroutineScope.launch {
                            if (currentRing == 1) {
                                innerRotation += delta
                                animatedInnerRotation.snapTo(innerRotation)
                                // Threshold 72 degrees (360/5)
                                if (innerRotation > 72f) {
                                    innerRotation -= 72f
                                    currentOnAddMinutes(1)
                                } else if (innerRotation < -72f) {
                                    innerRotation += 72f
                                    currentOnAddMinutes(-1)
                                }
                            } else if (currentRing == 2) {
                                outerRotation += delta
                                animatedOuterRotation.snapTo(outerRotation)
                                if (outerRotation > 72f) {
                                    outerRotation -= 72f
                                    currentOnAddHours(1)
                                } else if (outerRotation < -72f) {
                                    outerRotation += 72f
                                    currentOnAddHours(-1)
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (currentRing == 1) {
                                animatedInnerRotation.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                                innerRotation = 0f
                            } else if (currentRing == 2) {
                                animatedOuterRotation.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                                outerRotation = 0f
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            if (currentRing == 1) {
                                animatedInnerRotation.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                innerRotation = 0f
                            } else if (currentRing == 2) {
                                animatedOuterRotation.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                outerRotation = 0f
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeStyle = Stroke(width = 6f, join = StrokeJoin.Round)
            val center = Offset(size.width / 2f, size.height / 2f)
            
            val rOutOuter = size.width * 0.45f
            val rOutInner = size.width * 0.35f
            val rMidOuter = size.width * 0.32f
            val rMidInner = size.width * 0.22f
            val rCenter = size.width * 0.18f

            fun Path.drawHeart(cx: Float, cy: Float, s: Float) {
                moveTo(cx, cy - s/4)
                cubicTo(cx - s, cy - s, cx - s*1.2f, cy + s/4, cx, cy + s*0.8f)
                cubicTo(cx + s*1.2f, cy + s/4, cx + s, cy - s, cx, cy - s/4)
                close()
            }

            fun Path.drawStar(cx: Float, cy: Float, outer: Float, inner: Float, points: Int) {
                for (i in 0 until points * 2) {
                    val a = (i * 180f / points - 90f) * kotlin.math.PI.toFloat() / 180f
                    val r = if (i % 2 == 0) outer else inner
                    val px = cx + r * kotlin.math.cos(a)
                    val py = cy + r * kotlin.math.sin(a)
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            
            // Background glow
            drawCircle(Brush.radialGradient(listOf(colorDominant.copy(alpha=0.3f), Color.Transparent), center, rOutOuter), radius = rOutOuter, center = center)

            // Draw outer triangles (Minutes Ring)
            rotate(animatedOuterRotation.value, center) {
                for (i in 0 until 5) {
                    val angle = (i * 72f - 90f) * kotlin.math.PI.toFloat() / 180f
                    val nextAngle = ((i + 1) * 72f - 90f) * kotlin.math.PI.toFloat() / 180f
                    val midAngle = (angle + nextAngle) / 2f
                    
                    val path = Path().apply {
                        when (bgStyle) {
                            8 -> { // Hearts
                                val cx = center.x + (rOutInner + rOutOuter)/2 * kotlin.math.cos(midAngle)
                                val cy = center.y + (rOutInner + rOutOuter)/2 * kotlin.math.sin(midAngle)
                                drawHeart(cx, cy, (rOutOuter - rOutInner) * 0.7f)
                            }
                            2 -> { // Anime (stars)
                                val cx = center.x + (rOutInner + rOutOuter)/2 * kotlin.math.cos(midAngle)
                                val cy = center.y + (rOutInner + rOutOuter)/2 * kotlin.math.sin(midAngle)
                                drawStar(cx, cy, (rOutOuter - rOutInner) * 0.5f, (rOutOuter - rOutInner) * 0.25f, 5)
                            }
                            else -> { // Default Megaminx Triangles / Cyberpunk polygons
                                val innerP1 = Offset(center.x + rOutInner * kotlin.math.cos(angle + 0.1f), center.y + rOutInner * kotlin.math.sin(angle + 0.1f))
                                val innerP2 = Offset(center.x + rOutInner * kotlin.math.cos(nextAngle - 0.1f), center.y + rOutInner * kotlin.math.sin(nextAngle - 0.1f))
                                val outerP = Offset(center.x + rOutOuter * kotlin.math.cos(midAngle), center.y + rOutOuter * kotlin.math.sin(midAngle))
                                moveTo(innerP1.x, innerP1.y)
                                lineTo(outerP.x, outerP.y)
                                lineTo(innerP2.x, innerP2.y)
                                close()
                            }
                        }
                    }
                    drawPath(path = path, color = colorDominant)
                    drawPath(path = path, color = colorVibrant, style = strokeStyle)
                }
            }

            // Draw middle quads (Hours Ring)
            rotate(animatedInnerRotation.value, center) {
                for (i in 0 until 5) {
                    val angle = (i * 72f - 90f) * kotlin.math.PI.toFloat() / 180f
                    val nextAngle = ((i + 1) * 72f - 90f) * kotlin.math.PI.toFloat() / 180f
                    val midAngle = (angle + nextAngle) / 2f
                    
                    val path = Path().apply {
                        val gap = 0.15f
                        when (bgStyle) {
                            8 -> { // Hearts
                                val cx = center.x + (rMidInner + rMidOuter)/2 * kotlin.math.cos(midAngle)
                                val cy = center.y + (rMidInner + rMidOuter)/2 * kotlin.math.sin(midAngle)
                                drawHeart(cx, cy, (rMidOuter - rMidInner) * 0.8f)
                            }
                            2 -> { // Anime (stars)
                                val cx = center.x + (rMidInner + rMidOuter)/2 * kotlin.math.cos(midAngle)
                                val cy = center.y + (rMidInner + rMidOuter)/2 * kotlin.math.sin(midAngle)
                                drawStar(cx, cy, (rMidOuter - rMidInner) * 0.6f, (rMidOuter - rMidInner) * 0.3f, 4)
                            }
                            else -> {
                                val inP1 = Offset(center.x + rMidInner * kotlin.math.cos(angle + gap), center.y + rMidInner * kotlin.math.sin(angle + gap))
                                val inP2 = Offset(center.x + rMidInner * kotlin.math.cos(nextAngle - gap), center.y + rMidInner * kotlin.math.sin(nextAngle - gap))
                                val outP2 = Offset(center.x + rMidOuter * kotlin.math.cos(nextAngle - gap/2), center.y + rMidOuter * kotlin.math.sin(nextAngle - gap/2))
                                val outP1 = Offset(center.x + rMidOuter * kotlin.math.cos(angle + gap/2), center.y + rMidOuter * kotlin.math.sin(angle + gap/2))
                                moveTo(inP1.x, inP1.y)
                                lineTo(outP1.x, outP1.y)
                                lineTo(outP2.x, outP2.y)
                                lineTo(inP2.x, inP2.y)
                                close()
                            }
                        }
                    }
                    drawPath(path = path, color = colorDominant)
                    drawPath(path = path, color = colorVibrant, style = strokeStyle)
                }
            }

            // Draw center shape
            val centerPath = Path().apply {
                when (bgStyle) {
                    8 -> { // Hearts
                        drawHeart(center.x, center.y, rCenter * 0.8f)
                    }
                    2 -> { // Anime
                        drawStar(center.x, center.y, rCenter, rCenter * 0.4f, 5)
                    }
                    1 -> { // Cyberpunk (Hexagon)
                        for (i in 0 until 6) {
                            val angle = (i * 60f - 90f) * kotlin.math.PI.toFloat() / 180f
                            val px = center.x + rCenter * kotlin.math.cos(angle)
                            val py = center.y + rCenter * kotlin.math.sin(angle)
                            if (i == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }
                    else -> { // Default Pentagon
                        for (i in 0 until 5) {
                            val angle = (i * 72f - 90f) * kotlin.math.PI.toFloat() / 180f
                            val px = center.x + rCenter * kotlin.math.cos(angle)
                            val py = center.y + rCenter * kotlin.math.sin(angle)
                            if (i == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }
                }
            }
            drawPath(path = centerPath, color = colorVibrant.copy(alpha = 0.8f))
            drawPath(path = centerPath, color = Color.White, style = strokeStyle)
        }
        
        // Timer Text in the center
        Text(
            text = if (timerSeconds > 0) formatTime() else "OFF",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
