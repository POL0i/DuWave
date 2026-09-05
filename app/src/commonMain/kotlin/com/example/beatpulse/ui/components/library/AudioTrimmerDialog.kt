package com.example.beatpulse.ui.components.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beatpulse.data.TrackEntity
import kotlinx.coroutines.launch

@Composable
fun AudioTrimmerDialog(
    track: TrackEntity,
    onDismiss: () -> Unit,
    onTrimSuccess: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimError by remember { mutableStateOf<String?>(null) }
    
    val maxDurationMs = remember { com.example.beatpulse.utils.getAudioDuration(track.dataPath) }
    val safeMax = if (maxDurationMs > 0) maxDurationMs.toFloat() else 60000f
    
    var startMs by remember { mutableStateOf(0f) }
    var endMs by remember { mutableStateOf(safeMax) }
    var anchorMs by remember { mutableStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    
    val colorVibrant = MaterialTheme.colorScheme.primary

    com.example.beatpulse.utils.AudioPreviewPlayer(
        path = track.dataPath,
        isPlaying = isPlaying,
        startMs = anchorMs.toLong(),
        endMs = endMs.toLong(),
        onPlaybackCompleted = { isPlaying = false }
    )

    AlertDialog(
        onDismissRequest = { if (!isTrimming) onDismiss() },
        title = { Text("Recortar Audio") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (maxDurationMs <= 0) {
                    Text("Nota: No se pudo obtener la duración exacta.", color = Color.Yellow)
                }
                
                Text(
                    text = "Ancla actual: ${formatTime(anchorMs.toLong())}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Inicio: ${formatTime(startMs.toLong())}", color = colorVibrant)
                    Text("Fin: ${formatTime(endMs.toLong())}", color = colorVibrant)
                }

                TrimmerTimeline(
                    maxDurationMs = safeMax,
                    startMs = startMs,
                    endMs = endMs,
                    anchorMs = anchorMs,
                    onStartChange = { startMs = it },
                    onEndChange = { endMs = it },
                    onAnchorChange = { 
                        anchorMs = it
                        if (isPlaying) { isPlaying = false }
                    },
                    colorVibrant = colorVibrant,
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                )

                Button(
                    onClick = { isPlaying = !isPlaying },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color.Red else colorVibrant)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPlaying) "Detener" else "Reproducir desde ancla")
                }

                if (trimError != null) {
                    Text(trimError!!, color = Color.Red)
                }
                if (isTrimming) {
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isTrimming,
                onClick = {
                    if (com.example.beatpulse.utils.SystemUtils.isMobilePlatform) {
                        isTrimming = true
                        isPlaying = false // Stop preview before trimming
                        coroutineScope.launch {
                            val outputDir = track.dataPath.substringBeforeLast("/")
                            val outputPath = com.example.beatpulse.utils.trimAudioFile(
                                inputPath = track.dataPath,
                                outputDir = outputDir,
                                outputFileNameBase = "trimmed_${track.title}_${startMs.toLong()}",
                                startMs = startMs.toLong(),
                                endMs = endMs.toLong()
                            )
                            isTrimming = false
                            if (outputPath != null) {
                                onTrimSuccess(outputPath)
                                onDismiss()
                            } else {
                                trimError = "Error al recortar el audio."
                            }
                        }
                    } else {
                        trimError = "Recorte de audio no soportado en esta plataforma."
                    }
                }
            ) {
                Text("Recortar", color = colorVibrant)
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isTrimming,
                onClick = { 
                    isPlaying = false
                    onDismiss()
                }
            ) {
                Text("Cancelar")
            }
        }
    )
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "${m}:${s.toString().padStart(2, '0')}"
}

@Composable
fun TrimmerTimeline(
    maxDurationMs: Float,
    startMs: Float,
    endMs: Float,
    anchorMs: Float,
    onStartChange: (Float) -> Unit,
    onEndChange: (Float) -> Unit,
    onAnchorChange: (Float) -> Unit,
    colorVibrant: Color,
    modifier: Modifier = Modifier
) {
    var width by remember { mutableStateOf(0f) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                var draggingHandle: String? = null
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        width = size.width.toFloat()
                        if (width == 0f) return@detectHorizontalDragGestures
                        
                        val startX = (startMs / maxDurationMs) * width
                        val endX = (endMs / maxDurationMs) * width
                        val anchorX = (anchorMs / maxDurationMs) * width
                        
                        val touchX = offset.x
                        
                        val distStart = kotlin.math.abs(touchX - startX)
                        val distEnd = kotlin.math.abs(touchX - endX)
                        val distAnchor = kotlin.math.abs(touchX - anchorX)
                        
                        // Hitbox logic. Anchor gets priority if very close.
                        draggingHandle = when {
                            distAnchor < 60f && distAnchor <= distStart && distAnchor <= distEnd -> "anchor"
                            distStart < 60f && distStart <= distEnd -> "start"
                            distEnd < 60f -> "end"
                            else -> null
                        }
                    },
                    onDragEnd = { draggingHandle = null },
                    onDragCancel = { draggingHandle = null },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (width == 0f) return@detectHorizontalDragGestures
                        val msPerPixel = maxDurationMs / width
                        val msChange = dragAmount * msPerPixel
                        
                        when (draggingHandle) {
                            "start" -> {
                                val newStart = (startMs + msChange).coerceIn(0f, anchorMs)
                                onStartChange(newStart)
                            }
                            "end" -> {
                                val newEnd = (endMs + msChange).coerceIn(anchorMs, maxDurationMs)
                                onEndChange(newEnd)
                            }
                            "anchor" -> {
                                val newAnchor = (anchorMs + msChange).coerceIn(startMs, endMs)
                                onAnchorChange(newAnchor)
                            }
                        }
                    }
                )
            }
    ) {
        width = size.width
        val startX = (startMs / maxDurationMs) * width
        val endX = (endMs / maxDurationMs) * width
        val anchorX = (anchorMs / maxDurationMs) * width
        val trackHeight = 12.dp.toPx()
        val centerY = size.height / 2f
        
        // Draw background track
        drawRoundRect(
            color = Color.DarkGray,
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2)
        )
        
        // Draw active track
        drawRoundRect(
            color = colorVibrant.copy(alpha = 0.5f),
            topLeft = Offset(startX, centerY - trackHeight / 2),
            size = Size(endX - startX, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2)
        )
        
        // Draw Start Handle
        drawRect(
            color = colorVibrant,
            topLeft = Offset(startX - 6.dp.toPx(), centerY - 24.dp.toPx()),
            size = Size(12.dp.toPx(), 48.dp.toPx())
        )
        
        // Draw End Handle
        drawRect(
            color = colorVibrant,
            topLeft = Offset(endX - 6.dp.toPx(), centerY - 24.dp.toPx()),
            size = Size(12.dp.toPx(), 48.dp.toPx())
        )
        
        // Draw Anchor Handle (Location Pin)
        val path = Path().apply {
            val px = anchorX
            val py = centerY - 15.dp.toPx()
            val rad = 12.dp.toPx()
            moveTo(px, py + rad * 1.5f)
            lineTo(px - rad, py)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(px - rad, py - rad, px + rad, py + rad),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(px, py + rad * 1.5f)
            close()
        }
        drawPath(
            path = path,
            color = Color.White
        )
    }
}
