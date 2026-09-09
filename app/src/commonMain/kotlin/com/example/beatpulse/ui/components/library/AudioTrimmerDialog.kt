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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AudioTrimmerDialog(
    track: TrackEntity,
    onDismiss: () -> Unit,
    onTrimSuccess: (String) -> Unit,
    colorVibrant: Color = MaterialTheme.colorScheme.primary,
    colorSurface: Color = MaterialTheme.colorScheme.surface,
    colorText: Color = MaterialTheme.colorScheme.onSurface
) {
    com.example.beatpulse.utils.SystemBackHandler { onDismiss() }
    val coroutineScope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimError by remember { mutableStateOf<String?>(null) }
    val trimErrorMsg = com.example.beatpulse.utils.getLocalizedString("trim_error_generic")
    
    val maxDurationMs = track.duration
    val safeMax = if (maxDurationMs > 0) maxDurationMs.toFloat() else {
        val fallback = remember { com.example.beatpulse.utils.getAudioDuration(track.dataPath) }
        if (fallback > 0) fallback.toFloat() else 60000f
    }
    
    var startMs by remember { mutableStateOf(0f) }
    var endMs by remember { mutableStateOf(safeMax) }
    var anchorMs by remember { mutableStateOf(0f) }
    
    var isReady by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(true) }
    var isDownloadingDeps by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var playSessionId by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        isReady = com.example.beatpulse.utils.isAudioTrimmerReady()
        isChecking = false
    }
    
    LaunchedEffect(isPlaying, playSessionId) {
        if (isPlaying) {
            while (true) {
                kotlinx.coroutines.delay(100)
                anchorMs += 100f
                if (anchorMs >= endMs) {
                    anchorMs = startMs
                    isPlaying = false
                }
            }
        }
    }

    val playerStartMs = androidx.compose.runtime.remember(playSessionId) { anchorMs.toLong() }
    val playerEndMs = androidx.compose.runtime.remember(playSessionId) { endMs.toLong() }
    
    com.example.beatpulse.utils.AudioPreviewPlayer(
        path = track.dataPath,
        isPlaying = isPlaying,
        startMs = playerStartMs,
        endMs = playerEndMs,
        onPlaybackCompleted = { isPlaying = false }
    )

    fun playFrom(timeMs: Float) {
        anchorMs = timeMs
        playSessionId++
        isPlaying = true
    }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight(),
        containerColor = colorSurface,
        titleContentColor = colorText,
        textContentColor = colorText,
        onDismissRequest = { if (!isTrimming) onDismiss() },
        title = { Text(com.example.beatpulse.utils.getLocalizedString("trim_audio")) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isChecking) {
                    CircularProgressIndicator()
                    Text(com.example.beatpulse.utils.getLocalizedString("checking_dependencies"))
                } else if (!isReady) {
                    Text(com.example.beatpulse.utils.getLocalizedString("ffmpeg_required_desc"), color = colorText)
                    if (isDownloadingDeps) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(com.example.beatpulse.utils.getLocalizedString("downloading_ffmpeg"))
                            LinearProgressIndicator(progress = downloadProgress, modifier = Modifier.fillMaxWidth())
                            Text("${(downloadProgress * 100).toInt()}%")
                        }
                    } else {
                        Button(
                            onClick = {
                                isDownloadingDeps = true
                                trimError = null
                                coroutineScope.launch {
                                    try {
                                        com.example.beatpulse.utils.downloadAudioTrimmerDependencies { progress ->
                                            downloadProgress = progress
                                        }
                                        isReady = true
                                    } catch (e: Exception) {
                                        trimError = "Error en la descarga: ${e.message}"
                                    } finally {
                                        isDownloadingDeps = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colorVibrant)
                        ) {
                            Text(com.example.beatpulse.utils.getLocalizedString("download_ffmpeg_btn"), color = colorSurface)
                        }
                    }
                    if (trimError != null) {
                        Text(trimError!!, color = Color.Red)
                    }
                } else {
                    if (maxDurationMs <= 0) {
                        Text(com.example.beatpulse.utils.getLocalizedString("duration_unknown_note"), color = Color.Yellow)
                    }
                    
                    Text(
                        text = com.example.beatpulse.utils.getLocalizedString("trim_audio_anchor").replace("%1\$s", formatTime(anchorMs.toLong())).replace("%s", formatTime(anchorMs.toLong())),
                        fontWeight = FontWeight.Bold,
                        color = colorText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(com.example.beatpulse.utils.getLocalizedString("start_time").replace("%1\$s", formatTime(startMs.toLong())).replace("%s", formatTime(startMs.toLong())), color = colorVibrant)
                        Text(com.example.beatpulse.utils.getLocalizedString("end_time").replace("%1\$s", formatTime(endMs.toLong())).replace("%s", formatTime(endMs.toLong())), color = colorVibrant)
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
                        modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 16.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { playFrom(startMs) },
                                modifier = Modifier.background(colorVibrant.copy(alpha=0.15f), shape=androidx.compose.foundation.shape.CircleShape).size(48.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = colorVibrant, modifier = Modifier.size(24.dp))
                            }
                            Text(com.example.beatpulse.utils.getLocalizedString("trim_audio_start_btn"), fontSize = 10.sp, color = colorText.copy(alpha=0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FloatingActionButton(
                                onClick = { if (isPlaying) isPlaying = false else playFrom(anchorMs) },
                                containerColor = if (isPlaying) MaterialTheme.colorScheme.error else colorVibrant,
                                contentColor = colorSurface,
                                shape = androidx.compose.foundation.shape.CircleShape
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Anchor",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(if (isPlaying) com.example.beatpulse.utils.getLocalizedString("trim_audio_stop") else com.example.beatpulse.utils.getLocalizedString("trim_audio_play_anchor"), fontSize = 10.sp, color = colorText.copy(alpha=0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { playFrom((endMs - 10000f).coerceAtLeast(startMs)) },
                                modifier = Modifier.background(colorVibrant.copy(alpha=0.15f), shape=androidx.compose.foundation.shape.CircleShape).size(48.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "-10s", tint = colorVibrant, modifier = Modifier.size(24.dp))
                            }
                            Text(com.example.beatpulse.utils.getLocalizedString("trim_audio_minus_10s"), fontSize = 10.sp, color = colorText.copy(alpha=0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                        }
                    }

                    if (trimError != null) {
                        Text(trimError!!, color = Color.Red)
                    }
                    if (isTrimming) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isTrimming && isReady,
                onClick = {
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
                            trimError = trimErrorMsg
                        }
                    }
                }
            ) {
                Text(com.example.beatpulse.utils.getLocalizedString("trim_audio_trim_btn"), color = if (!isTrimming && isReady) colorVibrant else Color.Gray)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isTrimming) onDismiss() },
                enabled = !isTrimming
            ) {
                Text(com.example.beatpulse.utils.getLocalizedString("cancel"), color = colorText)
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

    val currentStart by androidx.compose.runtime.rememberUpdatedState(startMs)
    val currentEnd by androidx.compose.runtime.rememberUpdatedState(endMs)
    val currentAnchor by androidx.compose.runtime.rememberUpdatedState(anchorMs)
    val currentMax by androidx.compose.runtime.rememberUpdatedState(maxDurationMs)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                var draggingHandle: String? = null
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        width = size.width.toFloat()
                        if (width == 0f) return@detectHorizontalDragGestures
                        
                        val startX = (currentStart / currentMax) * width
                        val endX = (currentEnd / currentMax) * width
                        val anchorX = (currentAnchor / currentMax) * width
                        
                        val touchX = offset.x
                        
                        val distStart = kotlin.math.abs(touchX - startX)
                        val distEnd = kotlin.math.abs(touchX - endX)
                        val distAnchor = kotlin.math.abs(touchX - anchorX)
                        
                        // Hitbox logic: prioritize start and end handles
                        draggingHandle = when {
                            distStart < 50f -> "start"
                            distEnd < 50f -> "end"
                            distAnchor < 60f -> "anchor"
                            else -> null
                        }
                    },
                    onDragEnd = { draggingHandle = null },
                    onDragCancel = { draggingHandle = null },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (width == 0f) return@detectHorizontalDragGestures
                        val msPerPixel = currentMax / width
                        val msChange = dragAmount * msPerPixel
                        
                        when (draggingHandle) {
                            "start" -> {
                                val newStart = (currentStart + msChange).coerceIn(0f, currentEnd - 500f)
                                onStartChange(newStart)
                            }
                            "end" -> {
                                val newEnd = (currentEnd + msChange).coerceIn(currentStart + 500f, currentMax)
                                onEndChange(newEnd)
                            }
                            "anchor" -> {
                                val newAnchor = (currentAnchor + msChange).coerceIn(currentStart, currentEnd)
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
