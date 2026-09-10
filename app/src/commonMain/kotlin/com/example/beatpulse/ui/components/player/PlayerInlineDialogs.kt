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
        title = { Text(getLocalizedString("edit_tag"), color = colorVibrant) },
        text = {
            Column {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text(getLocalizedString("title"), color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editArtist,
                    onValueChange = { editArtist = it },
                    label = { Text(getLocalizedString("artist"), color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editAlbum,
                    onValueChange = { editAlbum = it },
                    label = { Text("Álbum", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        showCoverPicker = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorVibrant)
                ) {
                    Text(getLocalizedString("choose_cover"))
                }
                if (editCoverPath != null) {
                    Text(getLocalizedString("custom_cover_selected"), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdateTrackMetadata(currentTrack.id, editTitle, editArtist, editAlbum, editCoverPath)
                onDismissRequest()
            }) { Text(getLocalizedString("save"), color = colorVibrant) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(getLocalizedString("cancel"), color = Color.Gray) }
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

var lastSelectedSettingsTab by androidx.compose.runtime.mutableStateOf(0)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsSheet(
    showSettingsMenu: Boolean,
    onDismissRequest: () -> Unit,
    colorDominant: Color,
    colorVibrant: Color,
    colorMuted: Color,
    playerViewModel: IPlayerViewModel,
    visualizerManager: IAudioVisualizerManager,
    prefs: IPreferencesManager,
    currentStyle: VisualizerStyle,
    onStyleChange: (VisualizerStyle, String) -> Unit,
    styleNames: Map<VisualizerStyle, String>
) {
    if (!showSettingsMenu) return
    
    val dynamicTextColor = if (colorDominant.luminance() < 0.5f) Color.White else Color.Black

    val colorVibrant = androidx.compose.runtime.remember(colorVibrant, colorDominant) {
        val contrast = kotlin.math.abs(colorVibrant.luminance() - colorDominant.luminance())
        if (contrast < 0.25f) {
            if (colorDominant.luminance() < 0.5f) {
                // Lighten
                colorVibrant.copy(
                    red = colorVibrant.red + (1f - colorVibrant.red) * 0.6f,
                    green = colorVibrant.green + (1f - colorVibrant.green) * 0.6f,
                    blue = colorVibrant.blue + (1f - colorVibrant.blue) * 0.6f
                )
            } else {
                // Darken
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
    val thumbnailShapeIdx by prefs.thumbnailShapeFlow.collectAsState()
    val abRepeatModeEnabled by playerViewModel.abRepeatModeEnabled.collectAsState()
    val visualizerArchetype by visualizerManager.visualizerArchetype.collectAsState()
    val favoriteVisualizerStyles by prefs.favoriteVisualizerStylesFlow.collectAsState()
    val reactivity by visualizerManager.reactivity.collectAsState()
    val damping by visualizerManager.damping.collectAsState()
    val bassMult by visualizerManager.bassMultiplier.collectAsState()
    val elementSize by visualizerManager.elementSize.collectAsState()
    val midMult by visualizerManager.midMultiplier.collectAsState()
    val trebleMult by visualizerManager.trebleMultiplier.collectAsState()
    val sensitivity by visualizerManager.sensitivity.collectAsState()
    val isAdvancedMode by visualizerManager.isAdvancedMode.collectAsState()
    
    val coverVisibilityMode by playerViewModel.coverVisibilityMode.collectAsState()
    val chromaKeyColor by playerViewModel.chromaKeyColor.collectAsState()
    val coverDragEnabled by playerViewModel.coverDragEnabled.collectAsState()
    val cleanUiMode by playerViewModel.cleanUiMode.collectAsState()
    val dynamicColorsPlus by playerViewModel.dynamicColorsPlus.collectAsState()
    val dynamicColorsInterval by playerViewModel.dynamicColorsInterval.collectAsState()
    val coverScale by playerViewModel.coverScale.collectAsState()

    var isAdjusting by remember { androidx.compose.runtime.mutableStateOf(false) }
    val currentContainerAlpha by androidx.compose.animation.core.animateFloatAsState(if (isAdjusting) 0.3f else 0.95f, label = "containerAlpha")
    val currentScrimAlpha by androidx.compose.animation.core.animateFloatAsState(if (isAdjusting) 0.0f else 0.2f, label = "scrimAlpha")

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = { onDismissRequest() },
        containerColor = colorDominant.copy(alpha = currentContainerAlpha),
        scrimColor = Color.Black.copy(alpha = currentScrimAlpha)
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().animateContentSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 800.dp)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // No pager state needed, using lastSelectedSettingsTab directly
                Text(getLocalizedString("player_options"), color = colorVibrant, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                TabRow(
                    selectedTabIndex = lastSelectedSettingsTab,
                    containerColor = Color.Transparent,
                    contentColor = colorVibrant,
                    divider = {},
                    indicator = { tabPositions -> 
                        if (lastSelectedSettingsTab < tabPositions.size) {
                            androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[lastSelectedSettingsTab]),
                                color = colorVibrant
                            )
                        }
                    }
                ) {
                    Tab(selected = lastSelectedSettingsTab == 0, onClick = { lastSelectedSettingsTab = 0 }, text = { Text(getLocalizedString("basic_options")) })
                    Tab(selected = lastSelectedSettingsTab == 1, onClick = { lastSelectedSettingsTab = 1 }, text = { Text(getLocalizedString("advanced_options")) })
                    Tab(selected = lastSelectedSettingsTab == 2, onClick = { lastSelectedSettingsTab = 2 }, text = { Text(getLocalizedString("visuals")) })
                }
                Spacer(modifier = Modifier.height(24.dp))

                androidx.compose.animation.AnimatedContent(
                    targetState = lastSelectedSettingsTab,
                    label = "TabTransition"
                ) { page ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (page == 0) {
                    // TAB 1: Básicas
                    val volume by playerViewModel.systemVolume.collectAsState()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(getLocalizedString("volume"), color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val shapeIdx = thumbnailShapeIdx
                        val thumbnailShape = remember(shapeIdx) {
                            com.example.beatpulse.ui.utils.getShapeForIndex(shapeIdx)
                        }
                        
                        DynamicVolumeSlider(
                            value = volume,
                            onValueChange = { playerViewModel.setSystemVolume(it) },
                            colorNormal = colorVibrant,
                            colorBoost = Color.White,
                            shape = thumbnailShape,
                            modifier = Modifier.fillMaxWidth().height(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val isShuffleEnabled by playerViewModel.shuffleModeEnabled.collectAsState()
                    val currentMode by playerViewModel.repeatMode.collectAsState()
                    
                    Text(getLocalizedString("playback_options"), color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        // Aleatorio as an icon button instead of switch
                        androidx.compose.material3.IconButton(
                            onClick = { playerViewModel.shuffleModeEnabled.value = !isShuffleEnabled },
                            modifier = Modifier.clip(CircleShape).then(if (isShuffleEnabled) Modifier.border(1.dp, colorVibrant, CircleShape).background(colorVibrant.copy(alpha = 0.15f)) else Modifier)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Aleatorio", tint = if (isShuffleEnabled) colorVibrant else dynamicTextColor.copy(alpha = 0.6f))
                        }
                        
                        // Loop modes as icon buttons
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Apagado
                            val offActive = currentMode == 0
                            androidx.compose.material3.IconButton(
                                onClick = { playerViewModel.setRepeatMode(0) },
                                modifier = Modifier.clip(CircleShape).then(if (offActive) Modifier.border(1.dp, colorVibrant, CircleShape).background(colorVibrant.copy(alpha = 0.15f)) else Modifier)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Apagado", tint = if (offActive) colorVibrant else dynamicTextColor.copy(alpha = 0.6f))
                            }
                            // Lista
                            val listActive = currentMode == 2
                            androidx.compose.material3.IconButton(
                                onClick = { playerViewModel.setRepeatMode(2) },
                                modifier = Modifier.clip(CircleShape).then(if (listActive) Modifier.border(1.dp, colorVibrant, CircleShape).background(colorVibrant.copy(alpha = 0.15f)) else Modifier)
                            ) {
                                Icon(Icons.Default.Repeat, contentDescription = "Lista", tint = if (listActive) colorVibrant else dynamicTextColor.copy(alpha = 0.6f))
                            }
                            // Una
                            val oneActive = currentMode == 1
                            androidx.compose.material3.IconButton(
                                onClick = { playerViewModel.setRepeatMode(1) },
                                modifier = Modifier.clip(CircleShape).then(if (oneActive) Modifier.border(1.dp, colorVibrant, CircleShape).background(colorVibrant.copy(alpha = 0.15f)) else Modifier)
                            ) {
                                Icon(Icons.Default.RepeatOne, contentDescription = "Una", tint = if (oneActive) colorVibrant else dynamicTextColor.copy(alpha = 0.6f))
                            }
                            
                            // Separador
                            Box(modifier = Modifier.height(24.dp).width(1.dp).background(dynamicTextColor.copy(alpha = 0.3f)))
                            
                            // A-B
                            androidx.compose.material3.TextButton(
                                onClick = { 
                                    (playerViewModel.abRepeatModeEnabled as? kotlinx.coroutines.flow.MutableStateFlow)?.value = !abRepeatModeEnabled 
                                },
                                modifier = Modifier.clip(CircleShape).then(if (abRepeatModeEnabled) Modifier.border(1.dp, colorVibrant, CircleShape).background(colorVibrant.copy(alpha = 0.15f)) else Modifier)
                            ) {
                                Text("A-B", color = if (abRepeatModeEnabled) colorVibrant else dynamicTextColor.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val sortedStyles = remember(favoriteVisualizerStyles) {
                        VisualizerStyle.values().sortedByDescending { it.name in favoriteVisualizerStyles }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(getLocalizedString("visual_style"), color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                    }
                    
                    val infiniteTransition = rememberInfiniteTransition()
                    val phase by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 2f * kotlin.math.PI.toFloat(),
                        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            androidx.compose.foundation.lazy.grid.LazyHorizontalGrid(
                                rows = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                                modifier = Modifier.fillMaxWidth().height(185.dp).padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                    items(sortedStyles, key = { it.name }) { style ->
                                        val isSelected = currentStyle == style
                                        val isFavorite = style.name in favoriteVisualizerStyles
                                        val isLocked = (style == com.example.beatpulse.ui.components.player.VisualizerStyle.STAR || style == com.example.beatpulse.ui.components.player.VisualizerStyle.TERRAIN || style == com.example.beatpulse.ui.components.player.VisualizerStyle.SIDE_PERSPECTIVE_BANDS) && !prefs.isPatreonUnlocked
                                        
                                        var showPatreonUnlockDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                                        if (showPatreonUnlockDialog) {
                                            AlertDialog(
                                                onDismissRequest = { showPatreonUnlockDialog = false },
                                                title = { Text(getLocalizedString("patreon_exclusive_style").takeIf { it != "patreon_exclusive_style" } ?: "Estilo único para Patreons", color = colorVibrant) },
                                                text = { Text(getLocalizedString("support_patreon_desc").takeIf { it != "support_patreon_desc" } ?: "Apóyanos en Patreon para desbloquear.", color = dynamicTextColor) },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        showPatreonUnlockDialog = false
                                                        com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.OPEN_PATREON)
                                                    }) {
                                                        Text(getLocalizedString("unlock_patreon").takeIf { it != "unlock_patreon" } ?: "Desbloquear", color = colorVibrant)
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showPatreonUnlockDialog = false }) {
                                                        Text(getLocalizedString("cancel").takeIf { it != "cancel" } ?: "Cancelar", color = dynamicTextColor.copy(alpha=0.6f))
                                                    }
                                                },
                                                containerColor = colorDominant
                                            )
                                        }
                                        
                                        val starScale by androidx.compose.animation.core.animateFloatAsState(
                                            targetValue = if (isFavorite) 1.2f else 1.0f,
                                            animationSpec = androidx.compose.animation.core.spring(
                                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                            ),
                                            label = "starScale"
                                        )
                                        
                                        val animatedGradientBrush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                            colors = listOf(colorDominant, colorVibrant, colorMuted, colorDominant),
                                            center = androidx.compose.ui.geometry.Offset(12f, 12f)
                                        )

                                        Column(
                                            modifier = Modifier.animateItem().padding(horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            androidx.compose.foundation.layout.Box(
                                                modifier = Modifier.width(76.dp).height(70.dp)
                                            ) {
                                                // The clipped container for the preview
                                                androidx.compose.foundation.layout.Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(end = 6.dp, top = 6.dp) // Leave room for the star
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) colorVibrant else dynamicTextColor.copy(alpha = 0.6f).copy(alpha = 0.5f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .background(if (isSelected) colorVibrant.copy(alpha=0.15f) else Color.Transparent)
                                                        .clickable { 
                                                            if (isLocked) {
                                                                showPatreonUnlockDialog = true
                                                            } else {
                                                                onStyleChange(style, styleNames[style] ?: style.name)
                                                            }
                                                        }
                                                ) {
                                                    // Draw the wave preview in center
                                                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        WavePreview(style = style, color = if (isSelected) colorVibrant else dynamicTextColor.copy(alpha = 0.6f))
                                                    }
                                                    
                                                    if (isLocked) {
                                                        androidx.compose.foundation.layout.Box(
                                                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Lock,
                                                                contentDescription = "Patreon Locked",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                    
                                                    // The animated star placed outside the clip bounds
                                                    androidx.compose.foundation.Canvas(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(x = 6.dp, y = (-6).dp)
                                                            .graphicsLayer {
                                                                scaleX = starScale
                                                                scaleY = starScale
                                                            }
                                                            .size(24.dp)
                                                            .clickable { 
                                                                val newSet = favoriteVisualizerStyles.toMutableSet()
                                                                if (isFavorite) newSet.remove(style.name) else newSet.add(style.name)
                                                                prefs.favoriteVisualizerStyles = newSet
                                                            }
                                                    ) {
                                                        drawContext.canvas.save()
                                                        val pxCenterX = center.x
                                                        val pxCenterY = center.y
                                                        drawContext.canvas.translate(pxCenterX, pxCenterY)
                                                        drawContext.canvas.rotate(phase * 180f / 3.14159f)
                                                        drawContext.canvas.translate(-pxCenterX, -pxCenterY)
                                                        
                                                        val path = androidx.compose.ui.graphics.vector.PathParser().parsePathString("M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z").toPath()
                                                        if (isFavorite) {
                                                            drawPath(path = path, brush = animatedGradientBrush)
                                                        } else {
                                                            drawPath(path = path, color = dynamicTextColor.copy(alpha = 0.3f))
                                                        }
                                                        drawContext.canvas.restore()
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = styleNames[style] ?: style.name,
                                                    color = if (isSelected) colorVibrant else dynamicTextColor.copy(alpha = 0.6f),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else if (page == 1) {
                    // TAB 2: Avanzados
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(getLocalizedString("visual_waves_archetype"), color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { visualizerManager.visualizerArchetype.value = 0 }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Waves, contentDescription = null, tint = if (visualizerArchetype == 0) colorVibrant else dynamicTextColor.copy(alpha = 0.6f))
                                Text(getLocalizedString("three_overlapping_waves"), color = if (visualizerArchetype == 0) colorVibrant else dynamicTextColor.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                        TextButton(onClick = { visualizerManager.visualizerArchetype.value = 1 }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = if (visualizerArchetype == 1) colorVibrant else dynamicTextColor.copy(alpha = 0.6f))
                                Text(getLocalizedString("one_combined_wave"), color = if (visualizerArchetype == 1) colorVibrant else dynamicTextColor.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                        visualizerManager.isAdvancedMode.value = !isAdvancedMode
                    }) {
                        Text(if (isAdvancedMode) getLocalizedString("advanced_frequency_sensitivity") else getLocalizedString("general_sensitivity"), color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Switch(
                            checked = isAdvancedMode,
                            onCheckedChange = { visualizerManager.isAdvancedMode.value = it },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAdvancedMode) {
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            CircularKnob(
                                value = bassMult,
                                onValueChange = { visualizerManager.bassMultiplier.value = it },
                                onAdjustingChange = { isAdjusting = it },
                                valueRange = 0.5f..3.0f,
                                label = getLocalizedString("bass"),
                                color = colorDominant,
                                modifier = Modifier.weight(1f)
                            )
                            CircularKnob(
                                value = midMult,
                                onValueChange = { visualizerManager.midMultiplier.value = it },
                                onAdjustingChange = { isAdjusting = it },
                                valueRange = 0.5f..3.0f,
                                label = getLocalizedString("mids"),
                                color = colorVibrant,
                                modifier = Modifier.weight(1f)
                            )
                            CircularKnob(
                                value = trebleMult,
                                onValueChange = { visualizerManager.trebleMultiplier.value = it },
                                onAdjustingChange = { isAdjusting = it },
                                valueRange = 0.5f..3.0f,
                                label = getLocalizedString("trebles"),
                                color = colorMuted,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        ThickGradientSlider(
                            value = sensitivity,
                            onValueChange = { visualizerManager.sensitivity.value = it },
                            onAdjustingChange = { isAdjusting = it },
                            valueRange = 0.5f..3.0f,
                            colors = listOf(colorMuted, colorDominant, colorVibrant),
                            modifier = Modifier.fillMaxWidth().height(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(getLocalizedString("fluidity"), color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = damping,
                                onValueChange = { visualizerManager.damping.value = it },
                                valueRange = 0.05f..2.0f,
                                colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorVibrant)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(getLocalizedString("reactivity"), color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = reactivity,
                                onValueChange = { visualizerManager.reactivity.value = it },
                                valueRange = 0.1f..1.5f,
                                colors = SliderDefaults.colors(thumbColor = colorDominant, activeTrackColor = colorDominant)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("${getLocalizedString("graph_size")}: ${String.format("%.2fx", elementSize)}", color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                        CustomThickSlider(
                            value = elementSize,
                            onValueChange = { visualizerManager.elementSize.value = it },
                            onAdjustingChange = { isAdjusting = it },
                            valueRange = 0.5f..2.0f,
                            activeColor = colorVibrant,
                            inactiveColor = colorVibrant.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                } else if (page == 2) {
                    // TAB 3: Visuales
                    Text(getLocalizedString("advanced_cover_options"), color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            IconToggleButton(
                                checked = cleanUiMode,
                                onCheckedChange = { playerViewModel.setCleanUiMode(it) },
                                modifier = Modifier.clip(CircleShape).then(if (cleanUiMode) Modifier.border(1.dp, colorVibrant, CircleShape).background(colorVibrant.copy(alpha = 0.15f)) else Modifier)
                            ) {
                                Icon(
                                    imageVector = if (cleanUiMode) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Modo Limpio",
                                    tint = if (cleanUiMode) colorVibrant else dynamicTextColor.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(getLocalizedString("clean_ui"), color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            IconToggleButton(
                                checked = coverDragEnabled,
                                onCheckedChange = { playerViewModel.setCoverDragEnabled(it) },
                                modifier = Modifier.clip(CircleShape).then(if (coverDragEnabled) Modifier.border(1.dp, colorVibrant, CircleShape).background(colorVibrant.copy(alpha = 0.15f)) else Modifier)
                            ) {
                                Icon(
                                    imageVector = if (coverDragEnabled) Icons.Filled.OpenWith else Icons.Filled.Lock,
                                    contentDescription = "Mover portada",
                                    tint = if (coverDragEnabled) colorVibrant else dynamicTextColor.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(getLocalizedString("move"), color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            IconToggleButton(
                                checked = dynamicColorsPlus,
                                onCheckedChange = { playerViewModel.setDynamicColorsPlus(it) },
                                modifier = Modifier.clip(CircleShape).then(if (dynamicColorsPlus) Modifier.border(1.dp, colorVibrant, CircleShape).background(colorVibrant.copy(alpha = 0.15f)) else Modifier)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "Dinamicidad Plus",
                                    tint = if (dynamicColorsPlus) colorVibrant else dynamicTextColor.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(getLocalizedString("dynamic_plus"), color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    if (dynamicColorsPlus) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Intervalo: $dynamicColorsInterval s", color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = dynamicColorsInterval.toFloat(),
                            onValueChange = { playerViewModel.setDynamicColorsInterval(it.toInt()) },
                            valueRange = 10f..60f,
                            steps = 4,
                            colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(getLocalizedString("cover_mode"), color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            IconToggleButton(
                                checked = coverVisibilityMode == "NORMAL",
                                onCheckedChange = { playerViewModel.setCoverVisibilityMode("NORMAL") }
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = getLocalizedString("normal"), tint = if (coverVisibilityMode == "NORMAL") colorVibrant else dynamicTextColor.copy(alpha = 0.6f))
                            }
                            Text(getLocalizedString("normal"), color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            IconToggleButton(
                                checked = coverVisibilityMode == "CHROMA_KEY",
                                onCheckedChange = { playerViewModel.setCoverVisibilityMode("CHROMA_KEY") }
                            ) {
                                Icon(Icons.Filled.Colorize, contentDescription = "Chroma Key", tint = if (coverVisibilityMode == "CHROMA_KEY") colorVibrant else dynamicTextColor.copy(alpha = 0.6f))
                            }
                            Text("Chroma", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            IconToggleButton(
                                checked = coverVisibilityMode == "HIDDEN",
                                onCheckedChange = { playerViewModel.setCoverVisibilityMode("HIDDEN") }
                            ) {
                                Icon(Icons.Filled.Clear, contentDescription = getLocalizedString("hidden"), tint = if (coverVisibilityMode == "HIDDEN") colorVibrant else dynamicTextColor.copy(alpha = 0.6f))
                            }
                            Text(getLocalizedString("hidden"), color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    if (coverVisibilityMode == "CHROMA_KEY") {
                        Spacer(modifier = Modifier.height(8.dp))
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
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    val showFps by playerViewModel.showFps.collectAsState()
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(0.5f)) {
                            Text(getLocalizedString("size"), color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = coverScale,
                                onValueChange = { playerViewModel.setCoverScale(it) },
                                valueRange = 0.5f..2.5f,
                                colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorVibrant)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.weight(0.5f).padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text("FPS", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.Switch(
                                checked = showFps,
                                onCheckedChange = {
                                    prefs.showFps = it
                                    (playerViewModel.showFps as? MutableStateFlow)?.value = it
                                },
                                colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorDominant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
                }
                } // End AnimatedContent
            }
        }
    }
}

@Composable
fun CircularKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
    color: Color,
    onAdjustingChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        val pct = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(64.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onAdjustingChange(true) },
                        onDragEnd = { onAdjustingChange(false) },
                        onDragCancel = { onAdjustingChange(false) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                            val touchPos = change.position
                            var angle = kotlin.math.atan2(touchPos.y - center.y, touchPos.x - center.x) * 180f / kotlin.math.PI.toFloat()
                            if (angle < 0) angle += 360f
                            
                            var mappedAngle = angle - 135f
                            if (mappedAngle < 0) mappedAngle += 360f
                            
                            var newPct = mappedAngle / 270f
                            if (newPct > 1f) {
                                newPct = if (mappedAngle < 315f) 1f else 0f
                            }
                            
                            val newVal = valueRange.start + newPct * (valueRange.endInclusive - valueRange.start)
                            onValueChange(newVal.coerceIn(valueRange))
                        }
                    )
                }
        ) {
            val r = size.width / 2f
            val center = androidx.compose.ui.geometry.Offset(r, r)
            
            // Background circle
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = r,
                center = center
            )
            
            // Track arc
            val angle = 270f * pct
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = angle,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            
            // Knob Dot
            val dotAngle = 135f + angle
            val dotRad = (dotAngle * kotlin.math.PI / 180).toFloat()
            val dotR = r - 12.dp.toPx()
            val dotX = center.x + kotlin.math.cos(dotRad) * dotR
            val dotY = center.y + kotlin.math.sin(dotRad) * dotR
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(dotX, dotY)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        Text(String.format("%.1f", value), color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DynamicVolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    colorNormal: Color,
    colorBoost: Color,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.CircleShape,
    modifier: Modifier = Modifier
) {
    var isAdjusting by remember { mutableStateOf(false) }
    var internalValue by remember { mutableStateOf(value) }
    
    LaunchedEffect(value) {
        if (!isAdjusting) {
            internalValue = value
        }
    }
    
    val isBoostMode = internalValue >= 0.99f
    
    // Animation of the "split" point between normal and boost
    val sliderWeight by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isBoostMode) 0.8f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 400f)
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val percentageStr = "${(internalValue * 100).toInt()}%"
        
        Box(
            modifier = Modifier
                .weight(if (isBoostMode) sliderWeight else 1f, fill = false)
                .fillMaxWidth(if (isBoostMode) 1f else 0.85f)
                .height(48.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isAdjusting = true },
                        onDragEnd = { isAdjusting = false },
                        onDragCancel = { isAdjusting = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val deltaPct = dragAmount.x / size.width
                            val startVal = if (internalValue > 1.0f) 1.0f else internalValue
                            internalValue = (startVal + deltaPct).coerceIn(0f, 1f)
                            onValueChange(internalValue)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val tapPct = offset.x / size.width
                            internalValue = tapPct.coerceIn(0f, 1f)
                            onValueChange(internalValue)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val corner = androidx.compose.ui.geometry.CornerRadius(h/2, h/2)

                drawRoundRect(
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    size = size,
                    cornerRadius = corner
                )

                val normalPct = internalValue.coerceIn(0f, 1f)
                val normalWidth = w * normalPct
                if (normalWidth > 0) {
                    drawRoundRect(
                        color = colorNormal,
                        size = androidx.compose.ui.geometry.Size(normalWidth, h),
                        cornerRadius = corner
                    )
                }
            }
        }
        
        if (!isBoostMode) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = percentageStr,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp)
            )
        }
        
        if (sliderWeight < 0.99f) {
            val boostPct = (internalValue - 1.0f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f - sliderWeight)
                    .height(64.dp)
                    .padding(start = 12.dp)
                    .pointerInput(isBoostMode) {
                        if (!isBoostMode) return@pointerInput
                        var lastAngle: Float? = null
                        detectDragGestures(
                            onDragStart = { lastAngle = null; isAdjusting = true },
                            onDragEnd = { isAdjusting = false; lastAngle = null },
                            onDragCancel = { isAdjusting = false; lastAngle = null },
                            onDrag = { change, _ ->
                                change.consume()
                                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                                val touchPos = change.position
                                val dx = touchPos.x - center.x
                                val dy = touchPos.y - center.y
                                val currentAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                                val prevAngle = lastAngle
                                if (prevAngle != null) {
                                    var deltaAngle = currentAngle - prevAngle
                                    if (deltaAngle > 180f) deltaAngle -= 360f
                                    if (deltaAngle < -180f) deltaAngle += 360f
                                    
                                    val deltaPct = deltaAngle / 180f
                                    internalValue = (internalValue + deltaPct).coerceIn(1f, 2f)
                                    onValueChange(internalValue)
                                }
                                lastAngle = currentAngle
                            }
                        )
                    }
                    .pointerInput(isBoostMode) {
                        if (!isBoostMode) return@pointerInput
                        detectTapGestures(
                            onPress = { offset ->
                                val h = size.height
                                val yPct = 1f - (offset.y / h)
                                internalValue = (1f + yPct).coerceIn(1f, 2f)
                                onValueChange(internalValue)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    val strokeWidth = 6.dp.toPx()
                    drawArc(
                        color = Color.DarkGray.copy(alpha = 0.5f),
                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                    )
                    if (boostPct > 0f) {
                        drawArc(
                            color = colorBoost,
                            startAngle = -90f, sweepAngle = boostPct * 360f, useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                }
                Text(
                    text = percentageStr,
                    color = colorBoost,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun ThickGradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    colors: List<Color>,
    onAdjustingChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pct = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onAdjustingChange(true) },
                    onDragEnd = { onAdjustingChange(false) },
                    onDragCancel = { onAdjustingChange(false) },
                    onDrag = { change, _ ->
                        change.consume()
                        val newPct = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newVal = valueRange.start + newPct * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newVal)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val corner = androidx.compose.ui.geometry.CornerRadius(h/2, h/2)
        
        // Background
        drawRoundRect(
            color = Color.DarkGray.copy(alpha = 0.5f),
            size = size,
            cornerRadius = corner
        )
        
        // Gradient fill
        if (pct > 0) {
            drawRoundRect(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = colors,
                    startX = 0f,
                    endX = w
                ),
                size = androidx.compose.ui.geometry.Size(w * pct, h),
                cornerRadius = corner
            )
        }
    }
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

@Composable
fun WavePreview(style: VisualizerStyle, color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path()
        val paintColor = color.copy(alpha = 0.8f)

        when (style) {
            VisualizerStyle.WAVE -> {
                path.moveTo(0f, h / 2f)
                path.quadraticBezierTo(w * 0.25f, 0f, w * 0.5f, h / 2f)
                path.quadraticBezierTo(w * 0.75f, h, w, h / 2f)
                drawPath(path, paintColor, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
            VisualizerStyle.BARS, VisualizerStyle.BANDS -> {
                val barW = w / 7f
                val spacing = barW * 0.5f
                val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f)
                var x = (w - (barW * 5 + spacing * 4)) / 2f
                for (ratio in heights) {
                    val barH = h * ratio
                    drawRoundRect(
                        color = paintColor,
                        topLeft = androidx.compose.ui.geometry.Offset(x, h - barH),
                        size = androidx.compose.ui.geometry.Size(barW, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                    )
                    x += barW + spacing
                }
            }
            VisualizerStyle.RINGS -> {
                drawCircle(color = paintColor, radius = h * 0.35f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                drawCircle(color = paintColor.copy(alpha = 0.4f), radius = h * 0.15f, center = center)
            }
            VisualizerStyle.DOTS, VisualizerStyle.PARTICLES -> {
                val dotR = w * 0.08f
                drawCircle(color = paintColor, radius = dotR, center = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.7f))
                drawCircle(color = paintColor, radius = dotR * 1.5f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.3f))
                drawCircle(color = paintColor, radius = dotR * 0.8f, center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.6f))
                drawCircle(color = paintColor.copy(alpha=0.5f), radius = dotR * 1.2f, center = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.5f))
            }
            VisualizerStyle.SLIME -> {
                path.moveTo(0f, h * 0.8f)
                path.quadraticBezierTo(w * 0.2f, h * 0.4f, w * 0.5f, h * 0.7f)
                path.quadraticBezierTo(w * 0.8f, h * 1.0f, w, h * 0.5f)
                path.lineTo(w, h)
                path.lineTo(0f, h)
                path.close()
                drawPath(path, paintColor)
            }
            VisualizerStyle.AURA -> {
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(paintColor, Color.Transparent),
                        center = center,
                        radius = h * 0.6f
                    ),
                    radius = h * 0.6f,
                    center = center
                )
            }
            VisualizerStyle.TERRAIN -> {
                path.moveTo(0f, h)
                path.lineTo(w * 0.2f, h * 0.5f)
                path.lineTo(w * 0.5f, h * 0.8f)
                path.lineTo(w * 0.8f, h * 0.3f)
                path.lineTo(w, h * 0.9f)
                path.lineTo(w, h)
                path.close()
                drawPath(path, paintColor, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
            VisualizerStyle.STAR -> {
                path.moveTo(w * 0.5f, 0f)
                path.lineTo(w * 0.65f, h * 0.35f)
                path.lineTo(w, h * 0.4f)
                path.lineTo(w * 0.75f, h * 0.65f)
                path.lineTo(w * 0.85f, h)
                path.lineTo(w * 0.5f, h * 0.8f)
                path.lineTo(w * 0.15f, h)
                path.lineTo(w * 0.25f, h * 0.65f)
                path.lineTo(0f, h * 0.4f)
                path.lineTo(w * 0.35f, h * 0.35f)
                path.close()
                drawPath(path, paintColor, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
            VisualizerStyle.OSCILLOSCOPE -> {
                path.moveTo(0f, h / 2f)
                var cx = 0f
                while(cx < w) {
                    val y = h/2f + kotlin.math.sin(cx / 4f) * (h/3f)
                    path.lineTo(cx, y)
                    cx += 4f
                }
                drawPath(path, paintColor, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
            }
            VisualizerStyle.TRAP_NATION -> {
                drawCircle(color = paintColor, radius = h * 0.25f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                val outerRadius = h * 0.35f
                for (i in 0..7) {
                    val angle = (i * 45) * (kotlin.math.PI / 180)
                    val sx = center.x + kotlin.math.cos(angle).toFloat() * (h * 0.25f)
                    val sy = center.y + kotlin.math.sin(angle).toFloat() * (h * 0.25f)
                    val ex = center.x + kotlin.math.cos(angle).toFloat() * outerRadius
                    val ey = center.y + kotlin.math.sin(angle).toFloat() * outerRadius
                    drawLine(color = paintColor, start = androidx.compose.ui.geometry.Offset(sx, sy), end = androidx.compose.ui.geometry.Offset(ex, ey), strokeWidth = 4f)
                }
            }
            VisualizerStyle.SIDE_PERSPECTIVE_BANDS -> {
                val barW = w / 4f
                val spacing = barW * 0.2f
                val heights = listOf(0.8f, 0.6f, 0.4f)
                var x = (w - (barW * 3 + spacing * 2)) / 2f
                for ((i, ratio) in heights.withIndex()) {
                    val barH = h * ratio
                    val skew = (2 - i) * 2f
                    path.moveTo(x - skew, h)
                    path.lineTo(x, h - barH)
                    path.lineTo(x + barW, h - barH)
                    path.lineTo(x + barW - skew, h)
                    path.close()
                    drawPath(path, paintColor)
                    x += barW + spacing
                }
            }
        }
    }
}

@Composable
fun CustomThickSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    activeColor: Color,
    inactiveColor: Color,
    onAdjustingChange: ((Boolean) -> Unit)? = null
) {
    var width by remember { mutableStateOf(1f) }
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(inactiveColor, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .onGloballyPositioned { width = it.size.width.toFloat() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { onAdjustingChange?.invoke(true) },
                    onDragEnd = { onAdjustingChange?.invoke(false) },
                    onDragCancel = { onAdjustingChange?.invoke(false) }
                ) { change, _ ->
                    val newFraction = (change.position.x / width).coerceIn(0f, 1f)
                    val newValue = valueRange.start + (newFraction * (valueRange.endInclusive - valueRange.start))
                    onValueChange(newValue)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newFraction = (offset.x / width).coerceIn(0f, 1f)
                    val newValue = valueRange.start + (newFraction * (valueRange.endInclusive - valueRange.start))
                    onValueChange(newValue)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(
                    activeColor, 
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = 12.dp, bottomStart = 12.dp, 
                        topEnd = if (fraction > 0.95f) 12.dp else 0.dp, 
                        bottomEnd = if (fraction > 0.95f) 12.dp else 0.dp
                    )
                )
        )
        
        // Thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { 
                    val thumbW = 16.dp.toPx()
                    val xPos = ((width - thumbW) * fraction).toInt()
                    androidx.compose.ui.unit.IntOffset(xPos, 0) 
                }
                .size(width = 16.dp, height = 24.dp)
                .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .border(2.dp, activeColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        )
    }
}

