package com.example.beatpulse.ui.components.player

import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.List as AutoMirroredList
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.beatpulse.utils.getLocalizedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beatpulse.data.AppPreferences
import com.example.beatpulse.data.TrackEntity

@Composable
fun PlayerTimerDialog(
    showTimerDialog: Boolean,
    onDismissRequest: () -> Unit,
    colorVibrant: Color,
    colorDominant: Color,
    sleepTimerSeconds: Int,
    onSetSleepTimer: (Int) -> Unit
) {
    if (!showTimerDialog) return
    com.example.beatpulse.utils.SystemBackHandler { onDismissRequest() }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(getLocalizedString("timer"), color = colorVibrant) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(200.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val x = change.position.x - size.width / 2
                                    val y = change.position.y - size.height / 2
                                    var newAngle = Math.toDegrees(kotlin.math.atan2(y.toDouble(), x.toDouble())).toFloat()
                                    newAngle = (newAngle + 90f) % 360f
                                    if (newAngle < 0) newAngle += 360f
                                    val newMinutes = ((newAngle / 360f) * 120f).toInt()
                                    onSetSleepTimer(newMinutes * 60)
                                }
                            }
                    ) {
                        val strokeWidth = 20f
                        drawArc(
                            color = Color.DarkGray,
                            startAngle = -90f, sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        val currentAngle = ((sleepTimerSeconds / 60f) / 120f) * 360f
                        drawArc(
                            color = colorVibrant,
                            startAngle = -90f, sweepAngle = currentAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    val minutes = sleepTimerSeconds / 60
                    val seconds = sleepTimerSeconds % 60
                    val timeString = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                    Text(timeString, color = Color.White, style = MaterialTheme.typography.titleLarge)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(getLocalizedString("ok"), color = colorVibrant) }
        },
        containerColor = colorDominant.copy(alpha = 0.95f)
    )
}

@Composable
fun PlayerEqDialog(
    showEqDialog: Boolean,
    onDismissRequest: () -> Unit,
    colorVibrant: Color,
    colorDominant: Color,
    equalizerManager: IEqualizerManager
) {
    if (!showEqDialog) return
    com.example.beatpulse.utils.SystemBackHandler { onDismissRequest() }
    val isEqEnabled by equalizerManager.isEnabled.collectAsState()
    val isAutoMode by equalizerManager.isAutoMode.collectAsState()
    val presets by equalizerManager.presets.collectAsState()
    val currentPreset by equalizerManager.currentPreset.collectAsState()
    val bands by equalizerManager.bands.collectAsState()
    val bandLevels by equalizerManager.bandLevels.collectAsState()
    val minLevel by equalizerManager.minLevel.collectAsState()
    val maxLevel by equalizerManager.maxLevel.collectAsState()

    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(getLocalizedString("equalizer"), color = colorVibrant, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(getLocalizedString("enable_equalizer"), color = Color.White)
                    Switch(
                        checked = isEqEnabled,
                        onCheckedChange = { equalizerManager.setEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorDominant)
                    )
                }

                if (isEqEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(getLocalizedString("auto_loudness"), color = Color.White)
                        Switch(
                            checked = isAutoMode,
                            onCheckedChange = { equalizerManager.setAutoMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorDominant)
                        )
                    }

                    if (!isAutoMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(getLocalizedString("presets"), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val currentName = if (currentPreset.toInt() == -1) getLocalizedString("preset_custom") else presets.find { it.first == currentPreset }?.second ?: getLocalizedString("normal")
                                Text(currentName, color = Color.White)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color.DarkGray)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(getLocalizedString("preset_custom"), color = Color.White) },
                                    onClick = { expanded = false }
                                )
                                presets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(preset.second, color = Color.White) },
                                        onClick = {
                                            equalizerManager.setPreset(preset.first)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val range = (maxLevel - minLevel).coerceAtLeast(1)
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            bands.forEach { band ->
                                val level = bandLevels[band] ?: 0.toShort()
                                val freqHz = equalizerManager.getCenterFreq(band) / 1000
                                val freqStr = if (freqHz >= 1000) "${freqHz / 1000}k" else "$freqHz"

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = freqStr,
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.width(48.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Slider(
                                        value = level.toFloat(),
                                        onValueChange = { equalizerManager.setBandLevel(band, it.toInt().toShort()) },
                                        valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                                        colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorVibrant),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text(getLocalizedString("close"), color = colorVibrant) } },
        containerColor = colorDominant.copy(alpha = 0.95f)
    )
}

@Composable
fun PlayerEditorDialog(
    showEditorDialog: Boolean,
    onDismissRequest: () -> Unit,
    colorVibrant: Color,
    colorDominant: Color,
    currentTrack: TrackEntity?,
    onUpdateTrackMetadata: (Long, String?, String?, String?, String?) -> Unit
) {
    if (!showEditorDialog || currentTrack == null) return
    com.example.beatpulse.utils.SystemBackHandler { onDismissRequest() }
    
    val dynamicTextColor = if (colorDominant.luminance() < 0.5f) Color.White else Color.Black
    val adjustedVibrant = androidx.compose.runtime.remember(colorVibrant, colorDominant) {
        val contrast = kotlin.math.abs(colorVibrant.luminance() - colorDominant.luminance())
        if (contrast < 0.25f) {
            if (colorDominant.luminance() < 0.5f) {
                colorVibrant.copy(
                    red = colorVibrant.red + (1f - colorVibrant.red) * 0.6f,
                    green = colorVibrant.green + (1f - colorVibrant.green) * 0.6f,
                    blue = colorVibrant.blue + (1f - colorVibrant.blue) * 0.6f
                )
            } else {
                colorVibrant.copy(
                    red = colorVibrant.red * 0.4f,
                    green = colorVibrant.green * 0.4f,
                    blue = colorVibrant.blue * 0.4f
                )
            }
        } else {
            colorVibrant
        }
    }

    var editTitle by remember { mutableStateOf(currentTrack.customTitle ?: currentTrack.title) }
    var editArtist by remember { mutableStateOf(currentTrack.customArtist ?: currentTrack.artist) }
    var editAlbum by remember { mutableStateOf(currentTrack.customAlbum ?: currentTrack.album) }
    var editCoverPath by remember { mutableStateOf(currentTrack.customCoverPath) }
    var showCoverPicker by remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showCoverPicker) {
        com.example.beatpulse.utils.SystemImagePicker(
            onFileSelected = { path ->
                showCoverPicker = false
                if (path != null) {
                    editCoverPath = path
                }
            },
            colorDominant = colorDominant,
            colorVibrant = colorVibrant
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(getLocalizedString("edit_tag"), color = adjustedVibrant) },
        text = {
            Column {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text(getLocalizedString("title"), color = dynamicTextColor.copy(alpha=0.7f)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = dynamicTextColor, unfocusedTextColor = dynamicTextColor, focusedBorderColor = adjustedVibrant, cursorColor = adjustedVibrant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editArtist,
                    onValueChange = { editArtist = it },
                    label = { Text(getLocalizedString("artist"), color = dynamicTextColor.copy(alpha=0.7f)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = dynamicTextColor, unfocusedTextColor = dynamicTextColor, focusedBorderColor = adjustedVibrant, cursorColor = adjustedVibrant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editAlbum,
                    onValueChange = { editAlbum = it },
                    label = { Text("Álbum", color = dynamicTextColor.copy(alpha=0.7f)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = dynamicTextColor, unfocusedTextColor = dynamicTextColor, focusedBorderColor = adjustedVibrant, cursorColor = adjustedVibrant)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        showCoverPicker = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = adjustedVibrant)
                ) {
                    Text(getLocalizedString("choose_cover"), color = dynamicTextColor)
                }
                if (editCoverPath != null) {
                    Text(getLocalizedString("custom_cover_selected"), color = dynamicTextColor.copy(alpha=0.7f), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdateTrackMetadata(currentTrack.id, editTitle, editArtist, editAlbum, editCoverPath)
                onDismissRequest()
            }) { Text(getLocalizedString("save"), color = adjustedVibrant) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(getLocalizedString("cancel"), color = dynamicTextColor.copy(alpha=0.7f)) }
        },
        containerColor = colorDominant.copy(alpha = 0.95f)
    )
}

@Composable
fun PlayerStreamConfigDialog(
    showStreamConfigDialog: Boolean,
    onDismissRequest: () -> Unit,
    colorVibrant: Color,
    colorDominant: Color,
    playerViewModel: IPlayerViewModel
) {
    if (!showStreamConfigDialog) return
    com.example.beatpulse.utils.SystemBackHandler { onDismissRequest() }
    var showAvatarPreview by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedAvatarPath by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    
    if (showAvatarPreview && selectedAvatarPath != null) {
        AvatarPreviewDialog(
            path = selectedAvatarPath!!,
            onDismiss = { showAvatarPreview = false; selectedAvatarPath = null },
            onApply = { path ->
                playerViewModel.updateStreamAvatar(path)
                showAvatarPreview = false
                selectedAvatarPath = null
            },
            colorVibrant = colorVibrant,
            colorDominant = colorDominant
        )
    }
    val streamConfigEffectsVisible by playerViewModel.streamConfigEffectsVisible.collectAsState()
    val streamConfigAspectRatio by playerViewModel.streamConfigAspectRatio.collectAsState()
    val cleanUiMode by playerViewModel.cleanUiMode.collectAsState()
    val coverDragEnabled by playerViewModel.coverDragEnabled.collectAsState()
    val coverVisibilityMode by playerViewModel.coverVisibilityMode.collectAsState()
    val chromaKeyColor by playerViewModel.chromaKeyColor.collectAsState()
    val coverScale by playerViewModel.coverScale.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(getLocalizedString("playback_settings"), color = colorVibrant) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        IconToggleButton(
                            checked = cleanUiMode,
                            onCheckedChange = { playerViewModel.setCleanUiMode(it) }
                        ) {
                            Icon(
                                imageVector = if (cleanUiMode) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Modo Limpio",
                                tint = if (cleanUiMode) colorVibrant else Color.Gray
                            )
                        }
                        Text(getLocalizedString("clean_ui"), color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        IconToggleButton(
                            checked = coverDragEnabled,
                            onCheckedChange = { playerViewModel.setCoverDragEnabled(it) }
                        ) {
                            Icon(
                                imageVector = if (coverDragEnabled) Icons.Filled.OpenWith else Icons.Filled.Lock,
                                contentDescription = "Mover portada",
                                tint = if (coverDragEnabled) colorVibrant else Color.Gray
                            )
                        }
                        Text(getLocalizedString("move"), color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        IconToggleButton(
                            checked = streamConfigEffectsVisible,
                            onCheckedChange = { playerViewModel.toggleStreamConfigEffects() }
                        ) {
                            Icon(
                                imageVector = if (streamConfigEffectsVisible) Icons.Filled.AutoAwesome else Icons.Filled.Block,
                                contentDescription = getLocalizedString("effects"),
                                tint = if (streamConfigEffectsVisible) colorVibrant else Color.Gray
                            )
                        }
                        Text(getLocalizedString("effects"), color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                HorizontalDivider(color = Color.DarkGray)

                Text("Modo de Portada", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        val isNormal = coverVisibilityMode == "NORMAL"
                        IconToggleButton(
                            checked = isNormal,
                            onCheckedChange = { playerViewModel.setCoverVisibilityMode("NORMAL") },
                            modifier = Modifier.clip(CircleShape).then(if (isNormal) Modifier.border(1.dp, colorVibrant, CircleShape).background(colorVibrant.copy(alpha = 0.15f)) else Modifier)
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = getLocalizedString("normal"), tint = if (isNormal) colorVibrant else Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(getLocalizedString("normal"), color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        val isChroma = coverVisibilityMode == "CHROMA_KEY"
                        IconToggleButton(
                            checked = isChroma,
                            onCheckedChange = { playerViewModel.setCoverVisibilityMode("CHROMA_KEY") },
                            modifier = Modifier.clip(CircleShape).then(if (isChroma) Modifier.border(1.dp, colorVibrant, CircleShape).background(colorVibrant.copy(alpha = 0.15f)) else Modifier)
                        ) {
                            Icon(Icons.Filled.Colorize, contentDescription = "Chroma Key", tint = if (isChroma) colorVibrant else Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Chroma", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        val isHidden = coverVisibilityMode == "HIDDEN"
                        IconToggleButton(
                            checked = isHidden,
                            onCheckedChange = { playerViewModel.setCoverVisibilityMode("HIDDEN") },
                            modifier = Modifier.clip(CircleShape).then(if (isHidden) Modifier.border(1.dp, colorVibrant, CircleShape).background(colorVibrant.copy(alpha = 0.15f)) else Modifier)
                        ) {
                            Icon(Icons.Filled.Clear, contentDescription = getLocalizedString("hidden"), tint = if (isHidden) colorVibrant else Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(getLocalizedString("hidden"), color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                if (coverVisibilityMode == "CHROMA_KEY") {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val colors = listOf("Green", "Magenta", "Blue")
                        items(colors.size) { i ->
                            val c = colors[i]
                            FilterChip(
                                selected = chromaKeyColor == c,
                                onClick = { playerViewModel.setChromaKeyColor(c) },
                                label = { Text(c) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorVibrant, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
                
                var showSystemPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                
                if (showSystemPicker) {
                    com.example.beatpulse.utils.SystemImagePicker(
                        onFileSelected = { path ->
                            showSystemPicker = false
                            if (path != null) {
                                selectedAvatarPath = path
                                showAvatarPreview = true
                            }
                        },
                        colorDominant = colorDominant,
                        colorVibrant = colorVibrant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { 
                        showSystemPicker = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorDominant.copy(alpha=0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorVibrant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(getLocalizedString("select_custom_cover"), color = Color.White)
                }

                HorizontalDivider(color = Color.DarkGray)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("${getLocalizedString("cover_size")}: ${String.format("%.2fx", coverScale)}", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = coverScale,
                        onValueChange = { playerViewModel.setCoverScale(it) },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorVibrant)
                    )
                }
                
                val availableAudioDevices by playerViewModel.availableAudioDevices.collectAsState()
                val selectedAudioDevice by playerViewModel.selectedAudioDevice.collectAsState()
                if (availableAudioDevices.isNotEmpty()) {
                    HorizontalDivider(color = Color.DarkGray)
                    Text("Dispositivo de Audio (Solo Escritorio)", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedAudioDevice ?: "Seleccionar dispositivo", color = Color.White)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.DarkGray)
                        ) {
                            availableAudioDevices.forEach { device ->
                                DropdownMenuItem(
                                    text = { Text(device, color = Color.White) },
                                    onClick = {
                                        playerViewModel.selectAudioDevice(device)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        "Nota: Para capturar el audio del sistema (loopback):\n- En Linux: Usa 'pavucontrol' (Control de volumen), ve a la pestaña 'Grabación', y cambia la fuente de captura de Java a 'Monitor de...'\n- En Windows: Habilita 'Mezcla estéreo' (Stereo Mix) en tus dispositivos de grabación y selecciónalo aquí.",
                        color = Color.Gray.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        playerViewModel.toggleMicMode() // Exit Mic Mode
                        onDismissRequest() // Close dialog
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(getLocalizedString("exit_streamer_mode"), color = Color.White)
                }

            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(getLocalizedString("close"), color = colorVibrant)
            }
        },
        containerColor = colorDominant
    )
}

@Composable
fun PlayerSupportDialog(
    showSupportDialog: Boolean,
    onDismissRequest: () -> Unit,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    dynamicTextColor: Color,
    prefs: IPreferencesManager
) {
    if (!showSupportDialog) return
    com.example.beatpulse.utils.SystemBackHandler { onDismissRequest() }
    
    val colorVibrant by animateColorAsState(paletteColors.vibrant, label = "sv")
    val colorDominant by animateColorAsState(paletteColors.dominant, label = "sd")
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val animatedColor by infiniteTransition.animateColor(
        initialValue = paletteColors.vibrant,
        targetValue = paletteColors.dominant,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween<Color>(1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = ""
    )
    
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    var patreonCode by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(getLocalizedString("support_suggestions"), color = animatedColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    onClick = {
                        /* no-op */
                        onDismissRequest()
                    },
                    colors = CardDefaults.cardColors(containerColor = colorVibrant.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Menu,
                            contentDescription = null,
                            tint = animatedColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Sugerir Idea", fontWeight = FontWeight.Bold, color = animatedColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(getLocalizedString("send_feedback"), color = dynamicTextColor, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                Card(
                    onClick = {
                        uriHandler.openUri("https://www.patreon.com/c/aldearius/membership")
                    },
                    colors = CardDefaults.cardColors(containerColor = colorVibrant.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Star,
                            contentDescription = null,
                            tint = animatedColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Patreon", fontWeight = FontWeight.Bold, color = animatedColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(getLocalizedString("support_patreon"), color = dynamicTextColor, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = patreonCode,
                    onValueChange = { 
                        patreonCode = it
                        codeError = false
                        showSuccess = false
                    },
                    label = { Text(getLocalizedString("patreon_code"), color = dynamicTextColor.copy(alpha = 0.7f)) },
                    isError = codeError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = animatedColor,
                        unfocusedBorderColor = dynamicTextColor.copy(alpha = 0.3f),
                        focusedTextColor = dynamicTextColor,
                        unfocusedTextColor = dynamicTextColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val lockoutDuration = 15 * 60 * 1000L // 15 minutos
                    
                    if (prefs.patreonFailedAttempts >= 5 && now - prefs.patreonLockoutTime < lockoutDuration) {
                        prefs.showToast("Demasiados intentos. Inténtalo en 15 minutos.")
                        return@Button
                    }
                    
                    if (now - prefs.patreonLockoutTime >= lockoutDuration) {
                        prefs.patreonFailedAttempts = 0
                    }

                    // Hash esperado para "PatreonWave26"
                    val expectedHash = "00989f959041896e737d732b9f722eec266b966fcdffcf7053d0a7802a2153ee"
                    val inputHash = com.example.beatpulse.utils.sha256Hash(patreonCode.trim())
                    
                    if (inputHash == expectedHash) {
                        prefs.isPatreonUnlocked = true
                        prefs.patreonFailedAttempts = 0
                        showSuccess = true
                    } else {
                        prefs.patreonFailedAttempts += 1
                        if (prefs.patreonFailedAttempts >= 5) {
                            prefs.patreonLockoutTime = now
                            prefs.showToast("Has sido bloqueado por 15 minutos.")
                        }
                        codeError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorVibrant)
            ) {
                Text(if (showSuccess) "¡Desbloqueado!" else getLocalizedString("redeem_code"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(getLocalizedString("cancel"), color = dynamicTextColor.copy(alpha = 0.7f))
            }
        },
        containerColor = paletteColors.dominant
    )
}

@Composable
fun AvatarPreviewDialog(
    path: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
    colorVibrant: Color,
    colorDominant: Color
) {
    val bitmap = com.example.beatpulse.ui.components.rememberStreamAvatar(path)
    val palette = androidx.compose.runtime.remember(bitmap) {
        bitmap?.let { com.example.beatpulse.utils.extractPaletteFast(it) }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = colorDominant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Previsualización de Portada", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Preview",
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (palette != null) {
                        Text("Paleta Generada:", color = Color.LightGray, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val colorsToShow = listOf(palette.dominant, palette.vibrant, palette.muted)
                            colorsToShow.forEach { c ->
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(c))
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.size(200.dp).background(Color.DarkGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text("Cargando o Error", color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text(getLocalizedString("cancel"), color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onApply(path) },
                        enabled = bitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = colorVibrant)
                    ) {
                        Text(getLocalizedString("apply"), color = Color.White)
                    }
                }
            }
        }
    }
}

