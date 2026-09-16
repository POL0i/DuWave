package com.example.beatpulse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beatpulse.data.AppDownloadManager
import com.example.beatpulse.data.AppDownloadTask
import com.example.beatpulse.data.DownloadState

@Composable
fun DownloadsFab(modifier: Modifier = Modifier, paletteColors: com.example.beatpulse.theme.PaletteColors) {
    val activeDownloads by AppDownloadManager.activeDownloads.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = activeDownloads.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.BottomEnd) {
            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                containerColor = paletteColors.vibrant,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Downloads")
            }

            if (isExpanded) {
                DownloadsOverlay(
                    downloads = activeDownloads,
                    onDismiss = { isExpanded = false },
                    paletteColors = paletteColors
                )
            }
        }
    }
}

@Composable
fun DownloadsOverlay(
    downloads: List<AppDownloadTask>,
    onDismiss: () -> Unit,
    paletteColors: com.example.beatpulse.theme.PaletteColors
) {
    Card(
        modifier = Modifier
            .padding(bottom = 16.dp, end = 16.dp)
            .width(350.dp)
            .heightIn(max = 400.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Descargas Activas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads) { task ->
                    DownloadItem(task, paletteColors)
                }
            }
        }
    }
}

@Composable
fun DownloadItem(task: AppDownloadTask, paletteColors: com.example.beatpulse.theme.PaletteColors) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, color = Color.White, fontSize = 14.sp, maxLines = 1)
                Text(task.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
            
            Row {
                if (task.state == DownloadState.DOWNLOADING) {
                    IconButton(onClick = { AppDownloadManager.pauseDownload(task.id) }) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = paletteColors.lightVibrant, modifier = Modifier.size(20.dp))
                    }
                } else if (task.state == DownloadState.PAUSED) {
                    IconButton(onClick = { AppDownloadManager.resumeDownload(task.id) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = paletteColors.lightVibrant, modifier = Modifier.size(20.dp))
                    }
                }
                
                IconButton(onClick = { AppDownloadManager.cancelDownload(task.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = paletteColors.vibrant, modifier = Modifier.size(20.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = { task.progress / 100f },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = paletteColors.vibrant,
            trackColor = Color.DarkGray
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val stateText = when (task.state) {
                DownloadState.DOWNLOADING -> "${task.progress}%"
                DownloadState.PAUSED -> "Pausado"
                DownloadState.CANCELED -> "Cancelando..."
                DownloadState.COMPLETED -> "Completado"
                DownloadState.ERROR -> "Error"
            }
            Text(stateText, color = Color.LightGray, fontSize = 10.sp)
            Text("${task.mbRead} / ${task.mbTotal} MB", color = Color.LightGray, fontSize = 10.sp)
        }
    }
}
