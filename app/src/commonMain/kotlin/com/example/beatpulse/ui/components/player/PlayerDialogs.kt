package com.example.beatpulse.ui.components.player

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import com.example.beatpulse.data.TrackEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerQueueSheet(
    showQueue: Boolean, onDismissRequest: () -> Unit,
    colorDominant: Color, colorVibrant: Color,
    currentQueue: List<TrackEntity>, currentTrack: TrackEntity?, onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit
) {
    if (!showQueue) return
    ModalBottomSheet(
        onDismissRequest = { onDismissRequest() }, 
        containerColor = colorDominant.copy(alpha = 0.95f),
        scrimColor = Color.Black.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().height(400.dp).padding(16.dp)
        ) {
            Text("Cola de Reproducción", color = colorVibrant, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(currentQueue) { track ->
                    val isPlaying = currentTrack?.id == track.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPlayTrack(track, currentQueue)
                                onDismissRequest()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = track.title ?: "Desconocido",
                            color = if (isPlaying) colorVibrant else Color.White,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            text = track.artist ?: "Desconocido",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerEffectsDialog(
    showEffectsDialog: Boolean, onDismissRequest: () -> Unit,
    colorVibrant: Color, colorDominant: Color,
    reverbEnabled: Boolean, onSetReverb: (Boolean) -> Unit,
    playbackSpeed: Float, onSetSpeed: (Float) -> Unit,
    playbackPitch: Float, onSetPitch: (Float) -> Unit,
    effectsPreset: String, onApplyPreset: (String) -> Unit
) {
    if (!showEffectsDialog) return
    AlertDialog(onDismissRequest = onDismissRequest, confirmButton = { TextButton(onClick = onDismissRequest) { Text("OK") } }, text = { Text("Effects") })
}
