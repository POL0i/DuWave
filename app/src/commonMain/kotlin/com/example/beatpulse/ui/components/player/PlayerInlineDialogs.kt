package com.example.beatpulse.ui.components.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
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
                            onCheckedChange = {  }
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
    dynamicTextColor: Color
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
            scrimColor = Color.Black.copy(alpha = 0.2f) // Let visualizer shine through
        ) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 800.dp) // Make the modal wider to fit elements
                        .fillMaxHeight(0.85f) // Responsive height so it scrolls on small screens
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                Text("Opciones de Reproductor", color = colorVibrant, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Spacer(modifier = Modifier.height(16.dp))

                
                Spacer(modifier = Modifier.height(16.dp))
                
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

                Text("Estilo Visual", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                val infiniteTransition = rememberInfiniteTransition()
                val phase by infiniteTransition.animateFloat(
                    initialValue = 0f, targetValue = 2f * kotlin.math.PI.toFloat(),
                    animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val styles = VisualizerStyle.values()
                    val chunked = styles.toList().chunked(4)
                    for (rowStyles in chunked) {
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            for (style in rowStyles) {
                                val isSelected = currentStyle == style
                                
                                androidx.compose.material3.Surface(
                                    modifier = Modifier
                                        .weight(1f) // Evenly spread
                                        .clickable { onStyleChange(style, styleNames[style] ?: style.name) }
                                        .padding(horizontal = 4.dp),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                    color = if (isSelected) colorVibrant.copy(alpha=0.2f) else Color.Transparent,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, colorVibrant) else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha=0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.size(36.dp)) {
                                            val w = size.width
                                            val h = size.height
                                            val color = if (isSelected) colorVibrant else Color.Gray
                                            val isCircle = thumbnailShapeIdx == 0
                                            val basePath = androidx.compose.ui.graphics.Path().apply {
                                                val r = w * 0.22f
                                                if (isCircle) {
                                                    addOval(androidx.compose.ui.geometry.Rect(w/2 - r, h/2 - r, w/2 + r, h/2 + r))
                                                } else {
                                                    val cr = if (thumbnailShapeIdx == 2) 4f else if (thumbnailShapeIdx == 3) 8f else 0f
                                                    addRoundRect(androidx.compose.ui.geometry.RoundRect(w/2 - r, h/2 - r, w/2 + r, h/2 + r, androidx.compose.ui.geometry.CornerRadius(cr, cr)))
                                                }
                                            }
                                            
                                            val pathMeasure = androidx.compose.ui.graphics.PathMeasure()
                                            pathMeasure.setPath(basePath, false)
                                            val pathLen = pathMeasure.length
                                            
                                            val numBars = 16
                                            val distStep = (pathLen / 2f) / (numBars - 1).coerceAtLeast(1)
                                            
                                            val getPointAndNormal = { d: Float ->
                                                val dMod = ((d % pathLen) + pathLen) % pathLen
                                                val pos = pathMeasure.getPosition(dMod)
                                                val tan = pathMeasure.getTangent(dMod)
                                                if (pos != androidx.compose.ui.geometry.Offset.Unspecified && tan != androidx.compose.ui.geometry.Offset.Unspecified) {
                                                    var nx = tan.y
                                                    var ny = -tan.x
                                                    val dx = pos.x - w/2
                                                    val dy = pos.y - h/2
                                                    if (nx * dx + ny * dy < 0) {
                                                        nx = -nx
                                                        ny = -ny
                                                    }
                                                    androidx.compose.ui.geometry.Offset(pos.x, pos.y) to androidx.compose.ui.geometry.Offset(nx, ny)
                                                } else {
                                                    androidx.compose.ui.geometry.Offset(w/2, h/2) to androidx.compose.ui.geometry.Offset(0f, -1f)
                                                }
                                            }

                                            when (style) {
                                                VisualizerStyle.WAVE -> {
                                                    val layers = if (visualizerArchetype == 1) 1 else 3
                                                    for (layer in 0 until layers) {
                                                        val layerColorAlpha = color.copy(alpha = if (visualizerArchetype == 1) 1f else 1f - layer * 0.3f)
                                                        val amps = FloatArray(numBars) { i ->
                                                            (kotlin.math.sin(i * 0.3f + phase * 2f + layer).toFloat() * 0.5f + 0.5f) * kotlin.math.sin(i.toFloat() / (numBars - 1) * kotlin.math.PI).toFloat() * 1.8f
                                                        }
                                                        val totalPoints = (numBars * 2 - 2).coerceAtLeast(0)
                                                        val wPath = androidx.compose.ui.graphics.Path()
                                                        for (i in 0 until totalPoints) {
                                                            val ampIndex = if (i < numBars) i else (totalPoints - i)
                                                            val dDist = if (i < numBars) i * distStep else pathLen - ampIndex * distStep
                                                            val amplitude = amps[ampIndex]
                                                            val dist = 2f + (amplitude * 12f)
                                                            val (pos, norm) = getPointAndNormal(dDist)
                                                            val pt = androidx.compose.ui.geometry.Offset(pos.x + norm.x * dist, pos.y + norm.y * dist)
                                                            if (i == 0) wPath.moveTo(pt.x, pt.y) else wPath.lineTo(pt.x, pt.y)
                                                        }
                                                        wPath.close()
                                                        drawPath(wPath, layerColorAlpha, style = androidx.compose.ui.graphics.drawscope.Stroke(if (visualizerArchetype == 1) 2.5f else 1.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                                                    }
                                                }
                                                VisualizerStyle.SLIME -> {
                                                    val layers = if (visualizerArchetype == 1) 1 else 2
                                                    for (layer in 0 until layers) {
                                                        val layerColorAlpha = color.copy(alpha = if (visualizerArchetype == 1) 0.8f else 0.8f - layer * 0.3f)
                                                        val amps = FloatArray(numBars) { i ->
                                                            (kotlin.math.sin(i * 0.5f + phase * 1.5f + layer).toFloat() * 0.5f + 0.5f) * kotlin.math.sin(i.toFloat() / (numBars - 1) * kotlin.math.PI).toFloat() * 0.8f
                                                        }
                                                        val totalPoints = (numBars * 2 - 2).coerceAtLeast(0)
                                                        val slimeX = FloatArray(totalPoints)
                                                        val slimeY = FloatArray(totalPoints)
                                                        for (i in 0 until totalPoints) {
                                                            val ampIndex = if (i < numBars) i else (totalPoints - i)
                                                            val dDist = if (i < numBars) i * distStep else pathLen - ampIndex * distStep
                                                            val amplitude = amps[ampIndex]
                                                            val extrude = 1f + (amplitude * 10f)
                                                            val (pos, norm) = getPointAndNormal(dDist)
                                                            slimeX[i] = pos.x + norm.x * extrude
                                                            slimeY[i] = pos.y + norm.y * extrude
                                                        }
                                                        if (totalPoints > 0) {
                                                            val sPath = androidx.compose.ui.graphics.Path()
                                                            sPath.moveTo((slimeX[0] + slimeX[totalPoints - 1]) / 2f, (slimeY[0] + slimeY[totalPoints - 1]) / 2f)
                                                            for (i in 0 until totalPoints) {
                                                                val nextIndex = (i + 1) % totalPoints
                                                                sPath.quadraticTo(slimeX[i], slimeY[i], (slimeX[i] + slimeX[nextIndex]) / 2f, (slimeY[i] + slimeY[nextIndex]) / 2f)
                                                            }
                                                            sPath.close()
                                                            drawPath(sPath, layerColorAlpha.copy(alpha = 0.3f))
                                                            drawPath(sPath, layerColorAlpha, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                                                        }
                                                    }
                                                }
                                                VisualizerStyle.BARS -> {
                                                    val amps = FloatArray(numBars) { i ->
                                                        (kotlin.math.sin(i * 0.4f + phase * 3f).toFloat() * 0.5f + 0.5f) * kotlin.math.sin(i.toFloat() / (numBars - 1) * kotlin.math.PI).toFloat() * 0.8f
                                                    }
                                                    for (i in 0 until numBars) {
                                                        val amplitude = amps[i]
                                                        val dist = 2f + (amplitude * 8f)
                                                        val barLength = 2f + (amplitude * 6f)
                                                        
                                                        val dRight = 0f + i * distStep
                                                        val (posR, normR) = getPointAndNormal(dRight)
                                                        drawLine(color, androidx.compose.ui.geometry.Offset(posR.x + normR.x * dist, posR.y + normR.y * dist), androidx.compose.ui.geometry.Offset(posR.x + normR.x * (dist + barLength), posR.y + normR.y * (dist + barLength)), strokeWidth = 1.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                                        
                                                        if (i != 0 && i != numBars - 1) {
                                                            val dLeft = pathLen - i * distStep
                                                            val (posL, normL) = getPointAndNormal(dLeft)
                                                            drawLine(color, androidx.compose.ui.geometry.Offset(posL.x + normL.x * dist, posL.y + normL.y * dist), androidx.compose.ui.geometry.Offset(posL.x + normL.x * (dist + barLength), posL.y + normL.y * (dist + barLength)), strokeWidth = 1.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                                        }
                                                    }
                                                }
                                                VisualizerStyle.DOTS -> {
                                                    val amps = FloatArray(numBars) { i ->
                                                        (kotlin.math.sin(i * 0.3f + phase * 2f).toFloat() * 0.5f + 0.5f) * kotlin.math.sin(i.toFloat() / (numBars - 1) * kotlin.math.PI).toFloat() * 0.8f
                                                    }
                                                    for (i in 0 until numBars) {
                                                        val amplitude = amps[i]
                                                        val dotCount = 1 + (amplitude * 5).toInt()
                                                        val dotSpacing = 2.5f
                                                        
                                                        val dRight = 0f + i * distStep
                                                        val (posR, normR) = getPointAndNormal(dRight)
                                                        for (j in 0 until dotCount) {
                                                            val dist = 2f + (j * dotSpacing)
                                                            drawCircle(color.copy(alpha = (1f - j/5f).coerceAtLeast(0.2f)), radius = 1f, center = androidx.compose.ui.geometry.Offset(posR.x + normR.x * dist, posR.y + normR.y * dist))
                                                        }
                                                        
                                                        if (i != 0 && i != numBars - 1) {
                                                            val dLeft = pathLen - i * distStep
                                                            val (posL, normL) = getPointAndNormal(dLeft)
                                                            for (j in 0 until dotCount) {
                                                                val dist = 2f + (j * dotSpacing)
                                                                drawCircle(color.copy(alpha = (1f - j/5f).coerceAtLeast(0.2f)), radius = 1f, center = androidx.compose.ui.geometry.Offset(posL.x + normL.x * dist, posL.y + normL.y * dist))
                                                            }
                                                        }
                                                    }
                                                }
                                                VisualizerStyle.PARTICLES -> {
                                                    val amps = FloatArray(numBars) { i ->
                                                        (kotlin.math.sin(i * 0.6f + phase * 3f).toFloat() * 0.5f + 0.5f) * kotlin.math.sin(i.toFloat() / (numBars - 1) * kotlin.math.PI).toFloat() * 0.8f
                                                    }
                                                    for (i in 0 until numBars) {
                                                        val amplitude = amps[i]
                                                        val boostedAmplitude = amplitude * (1f + (i.toFloat() / numBars) * 1.5f)
                                                        val extrude = 2f + (boostedAmplitude * 18f)
                                                        val sz = 0.5f + (boostedAmplitude * 1f)
                                                        
                                                        val dRight = 0f + i * distStep
                                                        val (posR, normR) = getPointAndNormal(dRight)
                                                        drawCircle(color, radius = sz, center = androidx.compose.ui.geometry.Offset(posR.x + normR.x * extrude, posR.y + normR.y * extrude))
                                                        if (boostedAmplitude > 0.1f) {
                                                            val sparkEx = extrude - 3f * kotlin.math.abs(kotlin.math.sin(phase * 4f + i)).toFloat()
                                                            drawCircle(color.copy(alpha=0.5f), radius = sz*0.5f, center = androidx.compose.ui.geometry.Offset(posR.x + normR.x * sparkEx, posR.y + normR.y * sparkEx))
                                                        }
                                                        
                                                        if (i != 0 && i != numBars - 1) {
                                                            val dLeft = pathLen - i * distStep
                                                            val (posL, normL) = getPointAndNormal(dLeft)
                                                            drawCircle(color, radius = sz, center = androidx.compose.ui.geometry.Offset(posL.x + normL.x * extrude, posL.y + normL.y * extrude))
                                                            if (boostedAmplitude > 0.1f) {
                                                                val sparkEx = extrude - 3f * kotlin.math.abs(kotlin.math.sin(phase * 4f + i + 1)).toFloat()
                                                                drawCircle(color.copy(alpha=0.5f), radius = sz*0.5f, center = androidx.compose.ui.geometry.Offset(posL.x + normL.x * sparkEx, posL.y + normL.y * sparkEx))
                                                            }
                                                        }
                                                    }
                                                }
                                                VisualizerStyle.RINGS -> {
                                                    val p = phase
                                                    val layers = if (visualizerArchetype == 1) 1 else 2
                                                    for (layer in 0 until layers) {
                                                        val r1 = (w/4) + (w/10) * kotlin.math.abs(kotlin.math.sin(p + layer)).toFloat()
                                                        val rPath = androidx.compose.ui.graphics.Path().apply {
                                                            if (isCircle) {
                                                                addOval(androidx.compose.ui.geometry.Rect(w/2 - r1, h/2 - r1, w/2 + r1, h/2 + r1))
                                                            } else {
                                                                val cr = if (thumbnailShapeIdx == 2) 4f else if (thumbnailShapeIdx == 3) 8f else 0f
                                                                addRoundRect(androidx.compose.ui.geometry.RoundRect(w/2 - r1, h/2 - r1, w/2 + r1, h/2 + r1, androidx.compose.ui.geometry.CornerRadius(cr, cr)))
                                                            }
                                                        }
                                                        drawPath(rPath, color.copy(alpha = 1f - layer*0.4f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f - layer))
                                                    }
                                                }
                                                VisualizerStyle.AURA -> {
                                                    val p = phase
                                                    val layers = if (visualizerArchetype == 1) 1 else 2
                                                    for (layer in 0 until layers) {
                                                        val r1 = (w/4) + (w/8) * kotlin.math.abs(kotlin.math.sin(p + layer)).toFloat()
                                                        val aPath = androidx.compose.ui.graphics.Path().apply {
                                                            if (isCircle) {
                                                                addOval(androidx.compose.ui.geometry.Rect(w/2 - r1, h/2 - r1, w/2 + r1, h/2 + r1))
                                                            } else {
                                                                val cr = if (thumbnailShapeIdx == 2) 4f else if (thumbnailShapeIdx == 3) 8f else 0f
                                                                addRoundRect(androidx.compose.ui.geometry.RoundRect(w/2 - r1, h/2 - r1, w/2 + r1, h/2 + r1, androidx.compose.ui.geometry.CornerRadius(cr, cr)))
                                                            }
                                                        }
                                                        drawPath(aPath, color.copy(alpha = 0.4f - layer * 0.2f))
                                                    }
                                                }
                                                VisualizerStyle.BANDS -> {
                                                    val amps = FloatArray(numBars) { i ->
                                                        (kotlin.math.sin(i * 0.5f + phase * 4f).toFloat() * 0.5f + 0.5f) * kotlin.math.sin(i.toFloat() / (numBars - 1) * kotlin.math.PI).toFloat() * 1.2f
                                                    }
                                                    val stepY = h / numBars.toFloat()
                                                    val drawBandsEdge = { isLeft: Boolean, colorLayer: androidx.compose.ui.graphics.Color ->
                                                        for (i in 0 until numBars) {
                                                            val amp = amps[i]
                                                            val bandWidth = amp * w * 0.35f
                                                            if (bandWidth <= 1f) continue
                                                            val startX = if (isLeft) 0f else w - bandWidth
                                                            drawRect(color = colorLayer, topLeft = androidx.compose.ui.geometry.Offset(startX, i * stepY + stepY*0.1f), size = androidx.compose.ui.geometry.Size(bandWidth, stepY * 0.8f))
                                                        }
                                                    }
                                                    if (visualizerArchetype == 1) {
                                                        drawBandsEdge(true, color)
                                                        drawBandsEdge(false, color)
                                                    } else {
                                                        drawBandsEdge(true, color.copy(alpha = 0.8f))
                                                        drawBandsEdge(false, color.copy(alpha = 0.8f))
                                                    }
                                                }
                                            }
                                            
                                            // Mock Album Cover to hide internal generation
                                            drawPath(basePath, color = Color(0xFF222222))
                                            drawPath(basePath, color.copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(styleNames[style] ?: style.name, color = if (isSelected) colorVibrant else Color.Gray, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                }
                            }
                        }
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
                        onValueChange = { visualizerManager.sensitivity.value = it }, // FIXED logic
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
                        steps = 4, // 10, 20, 30, 40, 50, 60
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
