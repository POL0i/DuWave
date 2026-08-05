package com.example.beatpulse.ui.components.library

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.beatpulse.R
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.utils.AudioTrimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AudioTrimmerDialog(
    track: TrackEntity,
    onDismiss: () -> Unit,
    onTrimSuccess: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    // Usamos la duración del track (en milisegundos). Si es 0, usamos un fallback de 1 min.
    val totalDuration = if (track.duration > 0) track.duration else 60000L
    var sliderValues by remember { mutableStateOf(0f..(totalDuration.toFloat().coerceAtLeast(1000f))) }
    
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val player = MediaPlayer().apply {
            try {
                setDataSource(track.dataPath)
                prepare()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = player
        onDispose {
            player.release()
        }
    }

    // Use current position for scrubber
    var currentPos by remember { mutableStateOf(0f) }

    // Coroutine para pausar automáticamente al llegar al final del recorte y actualizar UI
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (mediaPlayer?.isPlaying == true) {
                val currentMs = mediaPlayer?.currentPosition ?: 0
                currentPos = currentMs.toFloat()
                if (currentPos >= sliderValues.endInclusive) {
                    mediaPlayer?.pause()
                    isPlaying = false
                    break
                }
                delay(50)
            }
        }
    }

    val formatTime = { ms: Long ->
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trim_audio)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(track.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(stringResource(R.string.start_time, formatTime(sliderValues.start.toLong())))
                Text(stringResource(R.string.end_time, formatTime(sliderValues.endInclusive.toLong())))
                
                Spacer(modifier = Modifier.height(16.dp))
                              Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.playback_progress, formatTime(currentPos.toLong())), style = MaterialTheme.typography.labelMedium)
                
                TrimSlider(
                    startMs = sliderValues.start,
                    endMs = sliderValues.endInclusive,
                    currentMs = currentPos,
                    totalMs = totalDuration.toFloat().coerceAtLeast(1000f),
                    onTrimChange = { newStart, newEnd ->
                        sliderValues = newStart..newEnd
                        mediaPlayer?.let { player ->
                            if (!isPlaying) {
                                // optional: update currentPos when trim start changes if currentPos is outside bounds
                                if (currentPos < newStart) {
                                    player.seekTo(newStart.toInt())
                                    currentPos = newStart
                                }
                            }
                        }
                    },
                    onSeek = { newCurr ->
                        currentPos = newCurr
                        mediaPlayer?.seekTo(newCurr.toInt())
                    },
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        mediaPlayer?.let { player ->
                            if (isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                if (currentPos < sliderValues.start || currentPos > sliderValues.endInclusive) {
                                    player.seekTo(sliderValues.start.toInt())
                                    currentPos = sliderValues.start
                                }
                                player.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(if (isPlaying) "Pausar" else "Reproducir Recorte")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isProcessing = true
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            val originalFile = File(track.dataPath)
                            val parentDir = originalFile.parentFile ?: context.filesDir
                            val newFileNameBase = "${originalFile.nameWithoutExtension} (trim)"
                            
                            val finalPath = AudioTrimmer.trimAudio(
                                inputPath = track.dataPath,
                                outputDir = parentDir.absolutePath,
                                outputFileNameBase = newFileNameBase,
                                startMs = sliderValues.start.toLong(),
                                endMs = sliderValues.endInclusive.toLong()
                            )
                            Pair(finalPath != null, finalPath)
                        }
                        
                        isProcessing = false
                        if (result.first) {
                            Toast.makeText(context, context.getString(R.string.trim_saved_success), Toast.LENGTH_SHORT).show()
                            onTrimSuccess(result.second!!)
                            onDismiss()
                        } else {
                            Toast.makeText(context, context.getString(R.string.trim_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.save_trim))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun TrimSlider(
    startMs: Float,
    endMs: Float,
    currentMs: Float,
    totalMs: Float,
    onTrimChange: (Float, Float) -> Unit,
    onSeek: (Float) -> Unit,
    color: Color
) {
    var dragging by remember { mutableStateOf<String?>(null) }
    
    Canvas(modifier = Modifier.fillMaxWidth().height(64.dp).pointerInput(totalMs, startMs, endMs, currentMs) {
        detectDragGestures(
            onDragStart = { offset ->
                val width = size.width.toFloat()
                val cy = size.height * 0.7f // Bar is in the lower part of the canvas
                
                val startX = (startMs / totalMs) * width
                val endX = (endMs / totalMs) * width
                val currX = (currentMs / totalMs) * width
                
                // If they touch in the upper half, target the progress pin
                if (offset.y < cy - 20f) {
                    // Clickeo en la mitad superior: siempre mueve el marcador de progreso
                    val newCurr = ((offset.x / width) * totalMs).coerceIn(0f, totalMs)
                    onSeek(newCurr)
                    dragging = "current"
                } else {
                    // Mitad inferior: siempre mueve el pulgar de recorte más cercano
                    val dStart = abs(offset.x - startX)
                    val dEnd = abs(offset.x - endX)
                    
                    dragging = if (dStart < dEnd) "start" else "end"
                }
            },
            onDrag = { change, dragAmount ->
                change.consume()
                val width = size.width.toFloat()
                val deltaMs = (dragAmount.x / width) * totalMs
                
                when (dragging) {
                    "start" -> {
                        val snapDistance = 2500f // Ampliado para mayor restricción magnética
                        var newStart = (startMs + deltaMs).coerceIn(0f, endMs - 1000f)
                        if (abs(newStart - currentMs) < snapDistance) newStart = currentMs
                        onTrimChange(newStart, endMs)
                    }
                    "end" -> {
                        val snapDistance = 2500f // Ampliado para mayor restricción magnética
                        var newEnd = (endMs + deltaMs).coerceIn(startMs + 1000f, totalMs)
                        if (abs(newEnd - currentMs) < snapDistance) newEnd = currentMs
                        onTrimChange(startMs, newEnd)
                    }
                    "current" -> {
                        val newCurr = (currentMs + deltaMs).coerceIn(0f, totalMs)
                        onSeek(newCurr)
                    }
                }
            },
            onDragEnd = { dragging = null },
            onDragCancel = { dragging = null }
        )
    }) {
        val width = size.width
        val cy = size.height * 0.7f // Move the bar lower to leave room for the pin
        
        drawLine(Color.Gray.copy(alpha=0.3f), Offset(0f, cy), Offset(width, cy), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
        
        val startX = (startMs / totalMs) * width
        val endX = (endMs / totalMs) * width
        drawLine(color, Offset(startX, cy), Offset(endX, cy), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
        
        drawCircle(color, radius = 12.dp.toPx(), center = Offset(startX, cy))
        drawCircle(color, radius = 12.dp.toPx(), center = Offset(endX, cy))
        
        val currX = (currentMs / totalMs) * width
        
        // Draw the location pin for progress
        val pinY = cy - 28.dp.toPx()
        val radius = 12.dp.toPx() // Hacer el botón de progreso más grande
        
        drawCircle(Color.White, radius = radius, center = Offset(currX, pinY))
        
        val path = Path().apply {
            moveTo(currX, cy - 4.dp.toPx()) // point just above the bar
            lineTo(currX - radius, pinY)
            lineTo(currX + radius, pinY)
            close()
        }
        drawPath(path, Color.White)
    }
}
