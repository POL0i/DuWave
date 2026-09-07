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
            Text(com.example.beatpulse.utils.getLocalizedString("playback_queue"), color = colorVibrant, style = MaterialTheme.typography.titleLarge)
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
                            text = track.title ?: com.example.beatpulse.utils.getLocalizedString("unknown_album"),
                            color = if (isPlaying) colorVibrant else Color.White,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            text = track.artist ?: com.example.beatpulse.utils.getLocalizedString("unknown_artist"),
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
    com.example.beatpulse.utils.SystemBackHandler { onDismissRequest() }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { TextButton(onClick = onDismissRequest) { Text(com.example.beatpulse.utils.getLocalizedString("ok"), color = colorVibrant) } },
        title = { Text(com.example.beatpulse.utils.getLocalizedString("audio_effects"), color = colorVibrant) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(com.example.beatpulse.utils.getLocalizedString("eq_reverb"), color = Color.White)
                    Switch(
                        checked = reverbEnabled,
                        onCheckedChange = onSetReverb,
                        colors = SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorDominant)
                    )
                }
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(com.example.beatpulse.utils.getLocalizedString("speed_and_pitch") + " - Velocidad: ${String.format("%.2fx", playbackSpeed)}", color = Color.Gray)
                    Slider(
                        value = playbackSpeed,
                        onValueChange = onSetSpeed,
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorVibrant)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(com.example.beatpulse.utils.getLocalizedString("speed_and_pitch") + " - Tono: ${String.format("%.2fx", playbackPitch)}", color = Color.Gray)
                    Slider(
                        value = playbackPitch,
                        onValueChange = onSetPitch,
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorVibrant)
                    )
                }
                
                Button(
                    onClick = {
                        onSetReverb(false)
                        onSetSpeed(1.0f)
                        onSetPitch(1.0f)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorDominant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorVibrant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(com.example.beatpulse.utils.getLocalizedString("reset_defaults") ?: "Restablecer a valores por defecto", color = Color.White)
                }
            }
        },
        containerColor = colorDominant.copy(alpha = 0.95f)
    )
}
