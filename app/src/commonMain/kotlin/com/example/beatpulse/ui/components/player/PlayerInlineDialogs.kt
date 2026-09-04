package com.example.beatpulse.ui.components.player

import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Temporizador", color = colorVibrant) },
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
            TextButton(onClick = onDismissRequest) { Text("Aceptar", color = colorVibrant) }
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
        title = { Text("Ecualizador", color = colorVibrant, fontWeight = FontWeight.Bold) },
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
                    Text("Activar Ecualizador", color = Color.White)
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
                        Text("Modo Auto Loudness", color = Color.White)
                        Switch(
                            checked = isAutoMode,
                            onCheckedChange = { equalizerManager.setAutoMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorDominant)
                        )
                    }

                    if (!isAutoMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ajustes Preestablecidos", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val currentName = if (currentPreset.toInt() == -1) "Personalizado" else presets.find { it.first == currentPreset }?.second ?: "Normal"
                                Text(currentName, color = Color.White)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color.DarkGray)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Personalizado", color = Color.White) },
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
        confirmButton = { TextButton(onClick = onDismissRequest) { Text("Cerrar", color = colorVibrant) } },
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
    var editTitle by remember { mutableStateOf(currentTrack.customTitle ?: currentTrack.title) }
    var editArtist by remember { mutableStateOf(currentTrack.customArtist ?: currentTrack.artist) }
    var editAlbum by remember { mutableStateOf(currentTrack.customAlbum ?: currentTrack.album) }
    var editCoverPath by remember { mutableStateOf(currentTrack.customCoverPath) }

    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Editar Etiqueta", color = colorVibrant) },
        text = {
            Column {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Título", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editArtist,
                    onValueChange = { editArtist = it },
                    label = { Text("Artista", color = Color.Gray) },
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
                        val path = com.example.beatpulse.utils.SystemUtils.pickImageFile()
                        if (path != null) {
                            editCoverPath = path
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorVibrant)
                ) {
                    Text("Elegir Portada")
                }
                if (editCoverPath != null) {
                    Text("Portada personalizada seleccionada", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdateTrackMetadata(currentTrack.id, editTitle, editArtist, editAlbum, editCoverPath)
                onDismissRequest()
            }) { Text("Guardar", color = colorVibrant) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancelar", color = Color.Gray) }
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
        title = { Text("Ajustes de Reproducción", color = colorVibrant) },
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
                        Text("UI Limpia", color = Color.White, style = MaterialTheme.typography.bodySmall)
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
                        Text("Mover", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        IconToggleButton(
                            checked = streamConfigEffectsVisible,
                            onCheckedChange = { playerViewModel.toggleStreamConfigEffects() }
                        ) {
                            Icon(
                                imageVector = if (streamConfigEffectsVisible) Icons.Filled.AutoAwesome else Icons.Filled.Block,
                                contentDescription = "Efectos",
                                tint = if (streamConfigEffectsVisible) colorVibrant else Color.Gray
                            )
                        }
                        Text("Efectos", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                HorizontalDivider(color = Color.DarkGray)

                Text("Modo de Portada", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        IconToggleButton(
                            checked = coverVisibilityMode == "NORMAL",
                            onCheckedChange = { playerViewModel.setCoverVisibilityMode("NORMAL") }
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = "Normal", tint = if (coverVisibilityMode == "NORMAL") colorVibrant else Color.Gray)
                        }
                        Text("Normal", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        IconToggleButton(
                            checked = coverVisibilityMode == "CHROMA_KEY",
                            onCheckedChange = { playerViewModel.setCoverVisibilityMode("CHROMA_KEY") }
                        ) {
                            Icon(Icons.Filled.Colorize, contentDescription = "Chroma Key", tint = if (coverVisibilityMode == "CHROMA_KEY") colorVibrant else Color.Gray)
                        }
                        Text("Chroma", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        IconToggleButton(
                            checked = coverVisibilityMode == "HIDDEN",
                            onCheckedChange = { playerViewModel.setCoverVisibilityMode("HIDDEN") }
                        ) {
                            Icon(Icons.Filled.Clear, contentDescription = "Oculta", tint = if (coverVisibilityMode == "HIDDEN") colorVibrant else Color.Gray)
                        }
                        Text("Oculta", color = Color.White, style = MaterialTheme.typography.bodySmall)
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
                    Text("Seleccionar Imagen de Portada Personalizada", color = Color.White)
                }

                HorizontalDivider(color = Color.DarkGray)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Tamaño de la Portada: ${String.format("%.2fx", coverScale)}", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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
                    Text("Salir del Modo Streamer/Micrófono", color = Color.White)
                }

            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cerrar", color = colorVibrant)
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
    
    var patreonCode by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Apoyo y Sugerencias", color = animatedColor) },
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
                            Text("Envía comentarios o sugerencias para nuevas funciones", color = dynamicTextColor, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = patreonCode,
                    onValueChange = { 
                        patreonCode = it
                        codeError = false
                        showSuccess = false
                    },
                    label = { Text("Código de Patreon", color = dynamicTextColor.copy(alpha = 0.7f)) },
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
                
                Button(
                    onClick = {
                        val expected = listOf(68, 85, 87, 65, 86, 69, 50, 48, 50, 54)
                        val isValid = patreonCode.length == expected.size && patreonCode.map { it.code } == expected
                        if (isValid) {
                            prefs.isPatreonUnlocked = true
                            showSuccess = true
                        } else {
                            codeError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorVibrant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showSuccess) "¡Desbloqueado!" else "Canjear código", color = Color.White)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar", color = dynamicTextColor.copy(alpha = 0.7f))
            }
        },
        containerColor = paletteColors.dominant
    )
}

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

    val thumbnailShapeIdx by prefs.thumbnailShapeFlow.collectAsState()
    val abRepeatModeEnabled by playerViewModel.abRepeatModeEnabled.collectAsState()
    val visualizerArchetype by visualizerManager.visualizerArchetype.collectAsState()
    val favoriteVisualizerStyles by prefs.favoriteVisualizerStylesFlow.collectAsState()
    val reactivity by visualizerManager.reactivity.collectAsState()
    val damping by visualizerManager.damping.collectAsState()
    val bassMult by visualizerManager.bassMultiplier.collectAsState()
    val midMult by visualizerManager.midMultiplier.collectAsState()
    val trebleMult by visualizerManager.trebleMultiplier.collectAsState()
    val sensitivity by visualizerManager.sensitivity.collectAsState()
    
    val coverVisibilityMode by playerViewModel.coverVisibilityMode.collectAsState()
    val chromaKeyColor by playerViewModel.chromaKeyColor.collectAsState()
    val coverDragEnabled by playerViewModel.coverDragEnabled.collectAsState()
    val cleanUiMode by playerViewModel.cleanUiMode.collectAsState()
    val dynamicColorsPlus by playerViewModel.dynamicColorsPlus.collectAsState()
    val dynamicColorsInterval by playerViewModel.dynamicColorsInterval.collectAsState()
    val coverScale by playerViewModel.coverScale.collectAsState()

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = { onDismissRequest() },
        containerColor = colorDominant.copy(alpha = 0.95f),
        scrimColor = Color.Black.copy(alpha = 0.2f)
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 800.dp)
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                var selectedTab by remember { mutableStateOf(0) }
                
                Text("Opciones de Reproductor", color = colorVibrant, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = colorVibrant,
                    divider = {},
                    indicator = { tabPositions -> 
                        if (selectedTab < tabPositions.size) {
                            androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = colorVibrant
                            )
                        }
                    }
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Básicas") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Avanzadas") })
                }
                Spacer(modifier = Modifier.height(24.dp))

                if (selectedTab == 0) {
                    // TAB 1: Básicas
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.7f)) {
                            val isShuffleEnabled by playerViewModel.shuffleModeEnabled.collectAsState()
                            androidx.compose.material3.Switch(
                                checked = isShuffleEnabled,
                                onCheckedChange = { playerViewModel.shuffleModeEnabled.value = it },
                                colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aleatorio", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Modo de Bucle", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        val currentMode by playerViewModel.repeatMode.collectAsState()
                        TextButton(onClick = { playerViewModel.repeatMode.value = 0 }) {
                            Text("Apagado", color = if (!abRepeatModeEnabled && currentMode == 0) colorVibrant else Color.Gray)
                        }
                        TextButton(onClick = { playerViewModel.repeatMode.value = 2 }) {
                            Text("Lista", color = if (!abRepeatModeEnabled && currentMode == 2) colorVibrant else Color.Gray)
                        }
                        TextButton(onClick = { playerViewModel.repeatMode.value = 1 }) {
                            Text("Una", color = if (!abRepeatModeEnabled && currentMode == 1) colorVibrant else Color.Gray)
                        }
                        TextButton(onClick = { /* AB handled elsewhere */ }) {
                            Text("A-B", color = if (abRepeatModeEnabled) colorVibrant else Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Estilo Visual", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    val infiniteTransition = rememberInfiniteTransition()
                    val phase by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 2f * kotlin.math.PI.toFloat(),
                        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        val sortedStyles = remember(favoriteVisualizerStyles) {
                            VisualizerStyle.values().sortedByDescending { it.name in favoriteVisualizerStyles }
                        }
                        
                        val pages = sortedStyles.chunked(8)
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pages.size })
                        
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        ) { pageIdx ->
                            val pageStyles = pages[pageIdx]
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                val chunkedRows = pageStyles.chunked(4)
                                for (rowStyles in chunkedRows) {
                                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                        for (style in rowStyles) {
                                            val isSelected = currentStyle == style
                                            val isFavorite = style.name in favoriteVisualizerStyles
                                            
                                            val starScale by androidx.compose.animation.core.animateFloatAsState(
                                                targetValue = if (isFavorite) 1.2f else 1.0f,
                                                animationSpec = androidx.compose.animation.core.spring(
                                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                                ),
                                                label = "starScale"
                                            )
    
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                androidx.compose.foundation.layout.Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(70.dp)
                                                ) {
                                                    // The clipped container for the preview
                                                    androidx.compose.foundation.layout.Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(end = 6.dp, top = 6.dp) // Leave room for the star
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .border(
                                                                width = if (isSelected) 2.dp else 1.dp,
                                                                color = if (isSelected) colorVibrant else Color.Gray.copy(alpha = 0.5f),
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .background(if (isSelected) colorVibrant.copy(alpha=0.15f) else Color.Transparent)
                                                            .clickable { onStyleChange(style, styleNames[style] ?: style.name) }
                                                    ) {
                                                        // Draw the wave preview in center
                                                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                            WavePreview(style = style, color = if (isSelected) colorVibrant else Color.Gray)
                                                        }
                                                    }
                                                    
                                                    // The animated star placed outside the clip bounds
                                                    Icon(
                                                        imageVector = Icons.Filled.Star,
                                                        contentDescription = "Favorite",
                                                        tint = if (isFavorite) colorVibrant else Color.Gray.copy(alpha = 0.3f),
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
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = styleNames[style] ?: style.name,
                                                    color = if (isSelected) colorVibrant else Color.Gray,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        // Fill remaining space if less than 4 items
                                        if (rowStyles.size < 4) {
                                            for (i in 0 until (4 - rowStyles.size)) {
                                                Spacer(modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // Pager indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(pages.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) colorVibrant else Color.Gray.copy(alpha = 0.5f)
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(color)
                                        .size(6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Ondas Visuales (Archetype)", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { visualizerManager.visualizerArchetype.value = 0 }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Waves, contentDescription = null, tint = if (visualizerArchetype == 0) colorVibrant else Color.Gray)
                                Text("3 Ondas superpuestas", color = if (visualizerArchetype == 0) colorVibrant else Color.Gray, fontSize = 12.sp)
                            }
                        }
                        TextButton(onClick = { visualizerManager.visualizerArchetype.value = 1 }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = if (visualizerArchetype == 1) colorVibrant else Color.Gray)
                                Text("1 Onda combinada", color = if (visualizerArchetype == 1) colorVibrant else Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                } else {
                    // TAB 2: Avanzadas
                    Text("Fluidez / Phantom (Damping)", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = damping,
                        onValueChange = { visualizerManager.damping.value = it },
                        valueRange = 0.05f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Sensibilidad (% Reactividad)", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = reactivity,
                        onValueChange = { visualizerManager.reactivity.value = it },
                        valueRange = 0.1f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var isAdvancedMode by remember { androidx.compose.runtime.mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { 
                        isAdvancedMode = !isAdvancedMode
                    }) {
                        Text(if (isAdvancedMode) "Sensibilidad por frecuencias (Avanzado)" else "Sensibilidad General", color = Color.Gray, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Switch(
                            checked = isAdvancedMode,
                            onCheckedChange = { isAdvancedMode = it },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                        )
                    }
                    
                    if (isAdvancedMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.layout.Box(modifier = Modifier.size(12.dp).background(colorDominant, androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bajos: %.1f".format(bassMult), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        }
                        Slider(
                            value = bassMult,
                            onValueChange = { visualizerManager.bassMultiplier.value = it },
                            valueRange = 0.5f..3.0f,
                            colors = SliderDefaults.colors(thumbColor = colorDominant, activeTrackColor = colorDominant)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.layout.Box(modifier = Modifier.size(12.dp).background(colorVibrant, androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Medios: %.1f".format(midMult), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        }
                        Slider(
                            value = midMult,
                            onValueChange = { visualizerManager.midMultiplier.value = it },
                            valueRange = 0.5f..3.0f,
                            colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorVibrant)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.layout.Box(modifier = Modifier.size(12.dp).background(colorMuted, androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agudos: %.1f".format(trebleMult), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        }
                        Slider(
                            value = trebleMult,
                            onValueChange = { visualizerManager.trebleMultiplier.value = it },
                            valueRange = 0.5f..3.0f,
                            colors = SliderDefaults.colors(thumbColor = colorMuted, activeTrackColor = colorMuted)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("General: %.1f".format(sensitivity), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = sensitivity,
                            onValueChange = { visualizerManager.sensitivity.value = it },
                            valueRange = 0.5f..3.0f,
                            colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Opciones Avanzadas de Portada", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(16.dp))

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
                            Text("UI Limpia", color = Color.White, style = MaterialTheme.typography.bodySmall)
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
                            Text("Mover", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            IconToggleButton(
                                checked = dynamicColorsPlus,
                                onCheckedChange = { playerViewModel.setDynamicColorsPlus(it) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "Dinamicidad Plus",
                                    tint = if (dynamicColorsPlus) colorVibrant else Color.Gray
                                )
                            }
                            Text("Dinámico+", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    if (dynamicColorsPlus) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Intervalo: $dynamicColorsInterval s", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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

                    Text("Modo de Portada", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            IconToggleButton(
                                checked = coverVisibilityMode == "NORMAL",
                                onCheckedChange = { playerViewModel.setCoverVisibilityMode("NORMAL") }
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = "Normal", tint = if (coverVisibilityMode == "NORMAL") colorVibrant else Color.Gray)
                            }
                            Text("Normal", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            IconToggleButton(
                                checked = coverVisibilityMode == "CHROMA_KEY",
                                onCheckedChange = { playerViewModel.setCoverVisibilityMode("CHROMA_KEY") }
                            ) {
                                Icon(Icons.Filled.Colorize, contentDescription = "Chroma Key", tint = if (coverVisibilityMode == "CHROMA_KEY") colorVibrant else Color.Gray)
                            }
                            Text("Chroma", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            IconToggleButton(
                                checked = coverVisibilityMode == "HIDDEN",
                                onCheckedChange = { playerViewModel.setCoverVisibilityMode("HIDDEN") }
                            ) {
                                Icon(Icons.Filled.Clear, contentDescription = "Oculta", tint = if (coverVisibilityMode == "HIDDEN") colorVibrant else Color.Gray)
                            }
                            Text("Oculta", color = Color.White, style = MaterialTheme.typography.bodySmall)
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

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Tamaño de la Portada: ${String.format("%.2fx", coverScale)}", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = coverScale,
                            onValueChange = { playerViewModel.setCoverScale(it) },
                            valueRange = 0.5f..2.5f,
                            colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorVibrant)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val showFps by playerViewModel.showFps.collectAsState()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mostrar Contador de FPS", color = Color.White)
                        Switch(
                            checked = showFps,
                            onCheckedChange = {
                                prefs.showFps = it
                                (playerViewModel.showFps as? MutableStateFlow)?.value = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorDominant)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
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
                        Text("Cancelar", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onApply(path) },
                        enabled = bitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = colorVibrant)
                    ) {
                        Text("Aplicar", color = Color.White)
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
