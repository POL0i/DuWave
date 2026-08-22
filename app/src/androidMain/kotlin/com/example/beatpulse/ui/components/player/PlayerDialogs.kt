package com.example.beatpulse.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beatpulse.R

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsSheet(
    showSettingsMenu: Boolean,
    onDismissRequest: () -> Unit,
    colorDominant: Color,
    colorVibrant: Color,
    colorMuted: Color,
    exoPlayer: androidx.media3.common.Player?,
    playerViewModel: PlayerViewModel,
    visualizerManager: com.example.beatpulse.visualizer.AudioVisualizerManager,
    prefs: com.example.beatpulse.data.PreferencesManager,
    currentStyle: VisualizerStyle,
    onStyleChange: (VisualizerStyle, String?) -> Unit,
    styleNames: Map<VisualizerStyle, String>
) {
    if (!showSettingsMenu) return

    val autoAnalyzeLyrics by playerViewModel.autoAnalyzeLyrics.collectAsState()
    val abRepeatModeEnabled by playerViewModel.abRepeatModeEnabled.collectAsState()
    val currentMode by playerViewModel.repeatMode.collectAsState()
    val visualizerArchetype by visualizerManager.visualizerArchetype.collectAsState()
    val fftMode by visualizerManager.fftMode.collectAsState()
    val reactivity by visualizerManager.reactivity.collectAsState()
    val bassMult by visualizerManager.bassMultiplier.collectAsState()
    val midMult by visualizerManager.midMultiplier.collectAsState()
    val trebleMult by visualizerManager.trebleMultiplier.collectAsState()
    val sensitivity by visualizerManager.sensitivity.collectAsState()


        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { onDismissRequest() },
            containerColor = colorDominant.copy(alpha = 0.95f)
        ) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxHeight(0.9f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                Text(stringResource(R.string.playback_settings), color = colorVibrant, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.7f)) {
                        var isShuffleEnabled by remember { androidx.compose.runtime.mutableStateOf(exoPlayer?.shuffleModeEnabled == true) }
                        androidx.compose.material3.Switch(
                            checked = isShuffleEnabled,
                            onCheckedChange = {
                                isShuffleEnabled = it
                                exoPlayer?.shuffleModeEnabled = it 
                            },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.shuffle_playback), color = Color.White)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.weight(0.3f)) {
                        androidx.compose.material3.IconToggleButton(
                            checked = autoAnalyzeLyrics,
                            onCheckedChange = { playerViewModel.toggleAutoAnalyze() }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "Auto-Analyze Lyrics",
                                tint = if (autoAnalyzeLyrics) colorVibrant else Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.visualizer_style_opt), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val styles = VisualizerStyle.values()
                    items(styles.size) { i ->
                        val style = styles[i]
                        val isSelected = currentStyle == style
                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = { 
                                onStyleChange(style, styleNames[style])
                                
                            },
                            label = { Text(style.name) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(selectedContainerColor = colorVibrant, selectedLabelColor = Color.White)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.repeat_method), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val currentMode by playerViewModel.repeatMode.collectAsState()
                    TextButton(onClick = { 
                        exoPlayer?.repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
                        playerViewModel.abRepeatModeEnabled.value = false
                    }) {
                        Text(stringResource(R.string.off), color = if (!abRepeatModeEnabled && currentMode == androidx.media3.common.Player.REPEAT_MODE_OFF) colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = { 
                        exoPlayer?.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                        playerViewModel.abRepeatModeEnabled.value = false
                    }) {
                        Text(stringResource(R.string.repeat_list), color = if (!abRepeatModeEnabled && currentMode == androidx.media3.common.Player.REPEAT_MODE_ALL) colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = { 
                        exoPlayer?.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                        playerViewModel.abRepeatModeEnabled.value = false
                    }) {
                        Text(stringResource(R.string.repeat_one), color = if (!abRepeatModeEnabled && currentMode == androidx.media3.common.Player.REPEAT_MODE_ONE) colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = { 
                        exoPlayer?.repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
                        playerViewModel.abRepeatModeEnabled.value = true
                        playerViewModel.abPointA.value = 0f
                        playerViewModel.abPointB.value = 0.5f
                        onDismissRequest()
                    }) {
                        Text(stringResource(R.string.repeat_ab), color = if (abRepeatModeEnabled) colorVibrant else Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.wave_archetype), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = {
                        visualizerManager.visualizerArchetype.value = 0
                        prefs.visualizerArchetype = 0
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Waves, contentDescription = null, tint = if (visualizerArchetype == 0) colorVibrant else Color.Gray)
                            Text(stringResource(R.string.waves_3_overlapping), color = if (visualizerArchetype == 0) colorVibrant else Color.Gray, fontSize = 12.sp)
                        }
                    }
                    TextButton(onClick = {
                        visualizerManager.visualizerArchetype.value = 1
                        prefs.visualizerArchetype = 1
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = if (visualizerArchetype == 1) colorVibrant else Color.Gray)
                            Text(stringResource(R.string.wave_1_combined), color = if (visualizerArchetype == 1) colorVibrant else Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.calculation_mode_fft), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = {
                        visualizerManager.fftMode.value = "AVERAGE"
                        prefs.visualizerFftMode = "AVERAGE"
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Equalizer, contentDescription = null, tint = if (fftMode == "AVERAGE") colorVibrant else Color.Gray)
                            Text(stringResource(R.string.fft_average), color = if (fftMode == "AVERAGE") colorVibrant else Color.Gray, fontSize = 12.sp)
                        }
                    }
                    TextButton(onClick = {
                        visualizerManager.fftMode.value = "MAX"
                        prefs.visualizerFftMode = "MAX"
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, tint = if (fftMode == "MAX") colorVibrant else Color.Gray)
                            Text(stringResource(R.string.fft_maximum), color = if (fftMode == "MAX") colorVibrant else Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.chaos_reactivity_format, String.format("%.2f", reactivity)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = reactivity,
                    onValueChange = {
                        visualizerManager.reactivity.value = it
                        prefs.reactivity = it
                    },
                    valueRange = 0.1f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                )
                Spacer(modifier = Modifier.height(16.dp))

                var isAdvancedMode by remember { androidx.compose.runtime.mutableStateOf(prefs.isAdvancedMode) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { 
                    isAdvancedMode = !isAdvancedMode
                    prefs.isAdvancedMode = isAdvancedMode
                    visualizerManager.isAdvancedMode.value = isAdvancedMode
                }) {
                    Text(if (isAdvancedMode) stringResource(R.string.advanced_sensitivity) else stringResource(R.string.visualizer_sensitivity), color = Color.Gray, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(
                        checked = isAdvancedMode,
                        onCheckedChange = { 
                            isAdvancedMode = it
                            prefs.isAdvancedMode = it
                            visualizerManager.isAdvancedMode.value = it
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                    )
                }
                
                if (isAdvancedMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.size(12.dp).background(colorDominant, androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.bass_sensitivity_format, String.format("%.1f", bassMult)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = bassMult,
                        onValueChange = {
                            visualizerManager.bassMultiplier.value = it
                            prefs.bassMultiplier = it
                        },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = colorDominant, activeTrackColor = colorDominant)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.size(12.dp).background(colorVibrant, androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.mid_sensitivity_format, String.format("%.1f", midMult)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = midMult,
                        onValueChange = {
                            visualizerManager.midMultiplier.value = it
                            prefs.midMultiplier = it
                        },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorVibrant)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.size(12.dp).background(colorMuted, androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.treble_sensitivity_format, String.format("%.1f", trebleMult)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = trebleMult,
                        onValueChange = {
                            visualizerManager.trebleMultiplier.value = it
                            prefs.trebleMultiplier = it
                        },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = colorMuted, activeTrackColor = colorMuted)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.general_sensitivity_format, String.format("%.1f", sensitivity)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = sensitivity,
                        onValueChange = {
                            visualizerManager.sensitivity.value = it
                            prefs.sensitivity = it
                            // Do NOT sync advanced settings to prevent overwriting user preferences
                        },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerQueueSheet(
    showQueue: Boolean,
    onDismissRequest: () -> Unit,
    colorDominant: Color,
    colorVibrant: Color,
    currentQueue: List<com.example.beatpulse.data.TrackEntity>,
    currentTrack: com.example.beatpulse.data.TrackEntity?,
    onPlayTrack: (com.example.beatpulse.data.TrackEntity, List<com.example.beatpulse.data.TrackEntity>) -> Unit
) {
    if (!showQueue) return

        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { onDismissRequest() },
            containerColor = colorDominant.copy(alpha = 0.95f)
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(currentTrack) {
                val index = currentQueue.indexOfFirst { it.id == currentTrack?.id }
                if (index != -1) {
                    listState.animateScrollToItem(index)
                }
            }
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(stringResource(R.string.playback_queue), color = colorVibrant, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(state = listState) {

                    items(currentQueue, key = { it.id }) { track ->
                        val isCurrent = track.id == currentTrack?.id
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onPlayTrack(track, currentQueue)
                                onDismissRequest()
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCurrent) Icons.Default.PlayArrow else Icons.Default.List,
                                contentDescription = null,
                                tint = if (isCurrent) colorVibrant else Color.LightGray
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(track.title, color = if (isCurrent) colorVibrant else Color.White, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                                Text(track.artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    
}

@Composable
fun PlayerEffectsDialog(
    showEffectsDialog: Boolean,
    onDismissRequest: () -> Unit,
    colorVibrant: Color,
    colorDominant: Color,
    reverbEnabled: Boolean,
    onSetReverb: (Boolean) -> Unit,
    playbackSpeed: Float,
    onSetSpeed: (Float) -> Unit,
    playbackPitch: Float,
    onSetPitch: (Float) -> Unit,
    effectsPreset: String,
    onApplyPreset: (String) -> Unit
) {
    if (!showEffectsDialog) return

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { onDismissRequest() },
            title = { Text(stringResource(R.string.audio_effects), color = colorVibrant) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.slow_reverb), color = Color.White)
                        androidx.compose.material3.Switch(
                            checked = reverbEnabled,
                            onCheckedChange = { onSetReverb(it) },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                        )
                    }

                    Column {
                        Text(stringResource(R.string.speed_format, String.format("%.2f", playbackSpeed)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = playbackSpeed,
                            onValueChange = { onSetSpeed(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                        )
                    }

                    Column {
                        Text(stringResource(R.string.pitch_format, String.format("%.2f", playbackPitch)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = playbackPitch,
                            onValueChange = { onSetPitch(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                        )
                    }

                    Column {
                        Text(stringResource(R.string.preset_effects), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val presets = listOf(
                                "NORMAL" to "Normal",
                                "NIGHTCORE" to "Nightcore",
                                "SLOWED" to "Slowed + Reverb",
                                "CHIPMUNK" to "Chipmunk",
                                "BASS" to "Bass Boost"
                            )
                            items(presets.size) { i ->
                                val (key, label) = presets[i]
                                androidx.compose.material3.FilterChip(
                                    selected = effectsPreset == key,
                                    onClick = { onApplyPreset(key) },
                                    label = { Text(label) },
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(selectedContainerColor = colorVibrant, selectedLabelColor = Color.White)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onDismissRequest() }) {
                    Text(stringResource(R.string.close), color = colorVibrant)
                }
            },
            containerColor = colorDominant
        )
    
}
