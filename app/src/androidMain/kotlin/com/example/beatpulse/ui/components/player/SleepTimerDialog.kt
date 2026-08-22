package com.example.beatpulse.ui.components.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.beatpulse.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2

@Composable
fun AdvancedSleepTimerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    sleepTimerSeconds: Int,
    onSetSleepTimer: (Int) -> Unit,
    colorVibrant: Color,
    colorDominant: Color
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exact_timer), color = colorVibrant) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Digital Clock
                val h = sleepTimerSeconds / 3600
                val m = (sleepTimerSeconds % 3600) / 60
                val s = sleepTimerSeconds % 60
                Text(
                    text = String.format("%02d:%02d:%02d", h, m, s),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(modifier = Modifier.size(240.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val x = change.position.x - size.width / 2
                                    val y = change.position.y - size.height / 2
                                    val distance = kotlin.math.hypot(x.toDouble(), y.toDouble()).toFloat()
                                    
                                    var newAngle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
                                    newAngle = (newAngle + 90f) % 360f
                                    if (newAngle < 0) newAngle += 360f

                                    val strokeWidth = 20f
                                    val gap = 35f
                                    
                                    val radiusH = size.width / 2f - strokeWidth / 2f
                                    val radiusM = radiusH - gap
                                    val radiusS = radiusM - gap

                                    val currentH = sleepTimerSeconds / 3600
                                    val currentM = (sleepTimerSeconds % 3600) / 60
                                    val currentS = sleepTimerSeconds % 60

                                    // Check which ring is dragged (with some tolerance)
                                    val tolerance = gap / 2f + 5f
                                    if (kotlin.math.abs(distance - radiusH) < tolerance) {
                                        // Hours (0-12)
                                        val newH = ((newAngle / 360f) * 12f).toInt()
                                        onSetSleepTimer(newH * 3600 + currentM * 60 + currentS)
                                    } else if (kotlin.math.abs(distance - radiusM) < tolerance) {
                                        // Minutes (0-59)
                                        val newM = ((newAngle / 360f) * 60f).toInt()
                                        onSetSleepTimer(currentH * 3600 + newM * 60 + currentS)
                                    } else if (kotlin.math.abs(distance - radiusS) < tolerance) {
                                        // Seconds (0-59)
                                        val newS = ((newAngle / 360f) * 60f).toInt()
                                        onSetSleepTimer(currentH * 3600 + currentM * 60 + newS)
                                    }
                                }
                            }
                    ) {
                        val strokeWidth = 20f
                        val gap = 35f
                        
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radiusH = size.width / 2f - strokeWidth / 2f
                        val radiusM = radiusH - gap
                        val radiusS = radiusM - gap

                        // Draw background arcs
                        drawArc(Color.DarkGray, -90f, 360f, false, topLeft = Offset(center.x - radiusH, center.y - radiusH), size = Size(radiusH * 2, radiusH * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))
                        drawArc(Color.DarkGray, -90f, 360f, false, topLeft = Offset(center.x - radiusM, center.y - radiusM), size = Size(radiusM * 2, radiusM * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))
                        drawArc(Color.DarkGray, -90f, 360f, false, topLeft = Offset(center.x - radiusS, center.y - radiusS), size = Size(radiusS * 2, radiusS * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))

                        // Draw foreground arcs
                        val currentH = sleepTimerSeconds / 3600
                        val currentM = (sleepTimerSeconds % 3600) / 60
                        val currentS = sleepTimerSeconds % 60

                        val angleH = (currentH.toFloat() / 12f) * 360f
                        val angleM = (currentM.toFloat() / 60f) * 360f
                        val angleS = (currentS.toFloat() / 60f) * 360f

                        drawArc(colorVibrant, -90f, angleH, false, topLeft = Offset(center.x - radiusH, center.y - radiusH), size = Size(radiusH * 2, radiusH * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))
                        drawArc(colorVibrant.copy(alpha = 0.7f), -90f, angleM, false, topLeft = Offset(center.x - radiusM, center.y - radiusM), size = Size(radiusM * 2, radiusM * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))
                        drawArc(colorVibrant.copy(alpha = 0.4f), -90f, angleS, false, topLeft = Offset(center.x - radiusS, center.y - radiusS), size = Size(radiusS * 2, radiusS * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { onSetSleepTimer(0) }) {
                        Text(stringResource(R.string.turn_off), color = colorVibrant)
                    }
                    TextButton(onClick = { onSetSleepTimer(sleepTimerSeconds + 300) }) {
                        Text("+5m", color = colorVibrant)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = colorVibrant) }
        },
        containerColor = colorDominant.copy(alpha = 0.95f)
    )
}
