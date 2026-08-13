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

import androidx.compose.ui.window.Dialog

@Composable
fun AudioTrimmerDialog(
    track: TrackEntity,
    onDismiss: () -> Unit,
    onTrimSuccess: (String) -> Unit = {},
    playerViewModel: com.example.beatpulse.ui.components.player.PlayerViewModel = 
        androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    // Usamos la duración del track (en milisegundos). Si es 0, usamos un fallback de 1 min.
    val totalDuration = if (track.duration > 0) track.duration else 60000L
    var sliderValues by remember { mutableStateOf(0f..(totalDuration.toFloat().coerceAtLeast(1000f))) }
    
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    val paletteColors by playerViewModel.paletteColors.collectAsState()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val surfaceColor = if (isDark) {
        val color = paletteColors.darkMuted
        if (color == Color.Black) Color(0xFF1E1E1E) else color
    } else {
        val color = paletteColors.lightVibrant
        if (color == Color.White) Color(0xFFF5F5F5) else color
    }
    val textColor = if (isDark) Color.White else Color.Black

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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            contentColor = textColor,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(stringResource(R.string.trim_audio), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(track.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(stringResource(R.string.start_time, formatTime(sliderValues.start.toLong())))
                Text(stringResource(R.string.end_time, formatTime(sliderValues.endInclusive.toLong())))
                
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
                                if (currentPos < newStart) {
                                    player.seekTo(newStart.toInt())
                                    currentPos = newStart
                                }
                            }
                        }
                    },
                    onSeek = { newCurr, isFinished ->
                        currentPos = newCurr
                        if (isFinished) {
                            mediaPlayer?.seekTo(newCurr.toInt())
                        }
                    },
                    color = paletteColors.vibrant
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
                                } else {
                                    player.seekTo(currentPos.toInt()) // Ensure player seeks to currentPos before playing
                                }
                                playerViewModel.playerState.value?.pause()
                                player.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(containerColor = paletteColors.vibrant)
                ) {
                    Text(if (isPlaying) androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.pause_trimmed) else androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.play_trimmed), color = if (isDark) Color.Black else Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss, enabled = !isProcessing) {
                        Text(stringResource(R.string.cancel), color = textColor.copy(alpha=0.7f))
                    }
                    
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
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = paletteColors.vibrant)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = if (isDark) Color.Black else Color.White)
                        } else {
                            Text(stringResource(R.string.save_trim), color = if (isDark) Color.Black else Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrimSlider(
    startMs: Float,
    endMs: Float,
    currentMs: Float,
    totalMs: Float,
    onTrimChange: (Float, Float) -> Unit,
    onSeek: (Float, Boolean) -> Unit,
    color: Color
) {
    var dragging by remember { mutableStateOf<String?>(null) }
    
    val currentStart by rememberUpdatedState(startMs)
    val currentEnd by rememberUpdatedState(endMs)
    val currentCurr by rememberUpdatedState(currentMs)
    
    Canvas(modifier = Modifier.fillMaxWidth().height(64.dp).pointerInput(totalMs) {
        var localDragValue = 0f
        detectDragGestures(
            onDragStart = { offset ->
                val width = size.width.toFloat()
                val cy = size.height * 0.7f
                
                val startX = (currentStart / totalMs) * width
                val endX = (currentEnd / totalMs) * width
                
                if (offset.y < cy - 20f) {
                    localDragValue = ((offset.x / width) * totalMs).coerceIn(0f, totalMs)
                    onSeek(localDragValue, false)
                    dragging = "current"
                } else {
                    val dStart = abs(offset.x - startX)
                    val dEnd = abs(offset.x - endX)
                    dragging = if (dStart < dEnd) "start" else "end"
                    localDragValue = if (dragging == "start") currentStart else currentEnd
                }
            },
            onDrag = { change, dragAmount ->
                change.consume()
                val width = size.width.toFloat()
                val deltaMs = (dragAmount.x / width) * totalMs
                
                when (dragging) {
                    "start" -> {
                        localDragValue = (localDragValue + deltaMs).coerceIn(0f, currentEnd - 1000f)
                        onTrimChange(localDragValue, currentEnd)
                    }
                    "end" -> {
                        localDragValue = (localDragValue + deltaMs).coerceIn(currentStart + 1000f, totalMs)
                        onTrimChange(currentStart, localDragValue)
                    }
                    "current" -> {
                        localDragValue = (localDragValue + deltaMs).coerceIn(0f, totalMs)
                        onSeek(localDragValue, false)
                    }
                }
            },
            onDragEnd = { 
                if (dragging == "current") {
                    onSeek(localDragValue, true)
                }
                dragging = null 
            },
            onDragCancel = { 
                if (dragging == "current") {
                    onSeek(localDragValue, true)
                }
                dragging = null 
            }
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
