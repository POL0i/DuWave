package com.example.beatpulse.ui.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.MutableTransitionState
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings

@Composable
fun StreamConfigDrawer(
    isMicModeActive: Boolean,
    isSilent: Boolean,
    uiVisible: Boolean,
    effectsVisible: Boolean,
    miniPlayerVisible: Boolean,
    aspectRatio: String,
    onUiVisibleChange: (Boolean) -> Unit,
    onEffectsVisibleChange: (Boolean) -> Unit,
    onMiniPlayerVisibleChange: (Boolean) -> Unit,
    onAspectRatioChange: (String) -> Unit
) {
    if (!isMicModeActive) return

    var isDrawerOpen by remember { mutableStateOf(false) }
    var showInitialHint by remember { mutableStateOf(false) }

    LaunchedEffect(isMicModeActive) {
        if (isMicModeActive) {
            showInitialHint = true
            delay(5000)
            showInitialHint = false
        }
    }

    // When the drawer is closed, we show a tiny handle (hitbox). 
    // It is fully visible with sparkles for the first 5 seconds, then 100% invisible.
    val handleAlpha by animateFloatAsState(
        targetValue = if (isDrawerOpen) 0f else if (showInitialHint) 1f else 0f,
        animationSpec = tween(1000)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp, end = 16.dp), // Position on the right side
        contentAlignment = Alignment.TopEnd
    ) {
        // The Invisible Hitbox / Handle
        if (!isDrawerOpen) {
            Box(contentAlignment = Alignment.Center) {
                if (showInitialHint) {
                    SparkleEffect(trigger = showInitialHint, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = handleAlpha * 0.3f))
                        .clickable { isDrawerOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Stream Config",
                        tint = Color.White.copy(alpha = handleAlpha) 
                    )
                }
            }
        }

        // The Configuration Drawer
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier.width(260.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E1E).copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Streamer Config",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Divider(color = Color.White.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mostrar Botones Principales", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = uiVisible,
                            onCheckedChange = onUiVisibleChange
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mostrar Efectos Extra", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = effectsVisible,
                            onCheckedChange = onEffectsVisibleChange
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mostrar Mini-Reproductor", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = miniPlayerVisible,
                            onCheckedChange = onMiniPlayerVisibleChange
                        )
                    }
                    
                    Text("Proyección / Márgenes", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { onAspectRatioChange("default") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (aspectRatio == "default") Color.White.copy(alpha = 0.2f) else Color.Transparent
                            )
                        ) {
                            Text("Full", color = Color.White)
                        }
                        
                        OutlinedButton(
                            onClick = { onAspectRatioChange("16:9") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (aspectRatio == "16:9") Color.White.copy(alpha = 0.2f) else Color.Transparent
                            )
                        ) {
                            Text("16:9", color = Color.White)
                        }
                        
                        OutlinedButton(
                            onClick = { onAspectRatioChange("9:16") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (aspectRatio == "9:16") Color.White.copy(alpha = 0.2f) else Color.Transparent
                            )
                        ) {
                            Text("9:16", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { isDrawerOpen = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                    ) {
                        Text("Hecho")
                    }
                }
            }
        }
    }
}


@androidx.compose.runtime.Composable
fun SparkleEffect(trigger: Boolean, color: androidx.compose.ui.graphics.Color) {
    val progress = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    
    androidx.compose.foundation.Canvas(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2f
        val maxDist = radius * 2.5f
        val currentDist = radius + (maxDist - radius) * progress.value
        val lineLength = radius * (1f - progress.value)
        val strokeWidth = 3.dp.toPx() * (1f - progress.value)
        
        if (progress.value in 0.01f..0.99f) {
            for (i in 0 until 8) {
                val angle = (i * 45f) * (kotlin.math.PI / 180f)
                val startX = center.x + kotlin.math.cos(angle).toFloat() * currentDist
                val startY = center.y + kotlin.math.sin(angle).toFloat() * currentDist
                val endX = center.x + kotlin.math.cos(angle).toFloat() * (currentDist + lineLength)
                val endY = center.y + kotlin.math.sin(angle).toFloat() * (currentDist + lineLength)
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(startX, startY),
                    end = androidx.compose.ui.geometry.Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}
