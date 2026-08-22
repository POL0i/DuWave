package com.example.beatpulse.ui.components.player

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.beatpulse.R
import com.example.beatpulse.data.PreferencesManager
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
        title = { Text(stringResource(R.string.sleep_timer), color = colorVibrant) },
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
                        val currentAngle = ((sleepTimerSeconds / 60).toFloat() / 120f) * 360f
                        drawArc(
                            color = colorVibrant,
                            startAngle = -90f, sweepAngle = currentAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Text("${sleepTimerSeconds / 60}m", color = Color.White, style = MaterialTheme.typography.titleLarge)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.accept), color = colorVibrant) }
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
    equalizerManager: com.example.beatpulse.service.EqualizerManager
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
        title = { Text(stringResource(R.string.equalizer), color = colorVibrant, fontWeight = FontWeight.Bold) },
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
                    Text(stringResource(R.string.enable_equalizer), color = Color.White)
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
                        Text(stringResource(R.string.auto_mode_loudness), color = Color.White)
                        Switch(
                            checked = isAutoMode,
                            onCheckedChange = { equalizerManager.setAutoMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorDominant)
                        )
                    }

                    if (!isAutoMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.preset_label), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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
                                    text = { Text(stringResource(R.string.custom), color = Color.White) },
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
        confirmButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.close), color = colorVibrant) } },
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                val coversDir = java.io.File(context.filesDir, "custom_covers")
                if (!coversDir.exists()) coversDir.mkdirs()
                val destFile = java.io.File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(it)?.use { input ->
                    java.io.FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                editCoverPath = destFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e2: Exception) {}
                editCoverPath = it.toString()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.edit_tag), color = colorVibrant) },
        text = {
            Column {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text(stringResource(R.string.title), color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editArtist,
                    onValueChange = { editArtist = it },
                    label = { Text(stringResource(R.string.artist), color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editAlbum,
                    onValueChange = { editAlbum = it },
                    label = { Text(stringResource(R.string.album), color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { launcher.launch(arrayOf("image/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = colorVibrant)
                ) {
                    Text(stringResource(R.string.choose_cover))
                }
                if (editCoverPath != null) {
                    Text(stringResource(R.string.custom_cover_selected), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdateTrackMetadata(currentTrack.id, editTitle, editArtist, editAlbum, editCoverPath)
                onDismissRequest()
            }) { Text(stringResource(R.string.save), color = colorVibrant) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel), color = Color.Gray) }
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
    playerViewModel: PlayerViewModel
) {
    if (!showStreamConfigDialog) return
    val streamConfigUiVisible by playerViewModel.streamConfigUiVisible.collectAsState()
    val streamConfigEffectsVisible by playerViewModel.streamConfigEffectsVisible.collectAsState()
    val streamConfigAspectRatio by playerViewModel.streamConfigAspectRatio.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Configuración de Stream", color = colorVibrant, modifier = Modifier.weight(1f))
                
                val context = LocalContext.current
                val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: Exception) {}
                        playerViewModel.updateStreamAvatar(uri.toString())
                    }
                }
                
                IconButton(onClick = { launcher.launch("image/*") }) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = "Portada de Stream", tint = colorVibrant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Ocultar controles (UI limpia)", color = Color.White)
                    Switch(
                        checked = !streamConfigUiVisible,
                        onCheckedChange = { playerViewModel.streamConfigUiVisible.value = !it },
                        colors = SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha = 0.5f))
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Mostrar efectos visuales", color = Color.White)
                    Switch(
                        checked = streamConfigEffectsVisible,
                        onCheckedChange = { playerViewModel.streamConfigEffectsVisible.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha = 0.5f))
                    )
                }

                val isWifiStreamActive by playerViewModel.isWifiStreamActive.collectAsState()
                val wifiStreamFps by playerViewModel.wifiStreamFps.collectAsState()

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transmitir a OBS (Vía Wi-Fi)", color = Color.White)
                        if (isWifiStreamActive) {
                            Text(
                                text = "Pega esta URL como Fuente Multimedia en OBS:\nrtsp://${getLocalIpAddress()}:1935",
                                color = colorVibrant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Switch(
                        checked = isWifiStreamActive,
                        onCheckedChange = { 
                            playerViewModel.isWifiStreamActive.value = it 
                            if (it) playerViewModel.streamConfigUiVisible.value = false
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha = 0.5f))
                    )
                }

                if (isWifiStreamActive) {
                    Column {
                        Text("Fotogramas por segundo (FPS)", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val fpsOptions = listOf(24, 30)
                            items(fpsOptions.size) { index ->
                                val fps = fpsOptions[index]
                                FilterChip(
                                    selected = wifiStreamFps == fps,
                                    onClick = { playerViewModel.wifiStreamFps.value = fps },
                                    label = { Text("$fps FPS") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorVibrant, selectedLabelColor = Color.White)
                                )
                            }
                        }
                    }
                    
                    val wifiStreamQuality by playerViewModel.wifiStreamQuality.collectAsState()
                    Column {
                        Text("Calidad de imagen", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val qualityOptions = listOf(60 to "Baja (3 Mbps)", 85 to "Media (6 Mbps)", 100 to "Alta (10 Mbps)")
                            items(qualityOptions.size) { index ->
                                val (qualityValue, qualityName) = qualityOptions[index]
                                FilterChip(
                                    selected = wifiStreamQuality == qualityValue,
                                    onClick = { playerViewModel.wifiStreamQuality.value = qualityValue },
                                    label = { Text(qualityName) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorVibrant, selectedLabelColor = Color.White)
                                )
                            }
                        }
                    }
                    
                    val wifiStreamCustomWidth by playerViewModel.wifiStreamCustomWidth.collectAsState()
                    val wifiStreamCustomHeight by playerViewModel.wifiStreamCustomHeight.collectAsState()
                    Column {
                        Text("Resolución exacta de transmisión (Pixeles)", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            OutlinedTextField(
                                value = wifiStreamCustomWidth.toString(),
                                onValueChange = { newValue ->
                                    val intValue = newValue.toIntOrNull()
                                    if (intValue != null) playerViewModel.wifiStreamCustomWidth.value = intValue
                                },
                                label = { Text("Ancho (px)") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = wifiStreamCustomHeight.toString(),
                                onValueChange = { newValue ->
                                    val intValue = newValue.toIntOrNull()
                                    if (intValue != null) playerViewModel.wifiStreamCustomHeight.value = intValue
                                },
                                label = { Text("Alto (px)") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Column {
                    Text("Relación de Aspecto del Lienzo", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val aspectRatios = listOf("Libre", "16:9", "4:3", "1:1")
                        items(aspectRatios.size) { i ->
                            val ratio = aspectRatios[i]
                            FilterChip(
                                selected = streamConfigAspectRatio == ratio,
                                onClick = { playerViewModel.streamConfigAspectRatio.value = ratio },
                                label = { Text(ratio) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorVibrant, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.close), color = colorVibrant)
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val colorVibrant by animateColorAsState(paletteColors.vibrant, label = "sv")
    val colorDominant by animateColorAsState(paletteColors.dominant, label = "sd")
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val animatedColor by infiniteTransition.animateColor(
        initialValue = paletteColors.vibrant,
        targetValue = paletteColors.dominant,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween<Color>(1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.support_dialog_title), color = animatedColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/POL0i/DuWave/issues"))
                        context.startActivity(intent)
                        onDismissRequest()
                    },
                    colors = CardDefaults.cardColors(containerColor = colorVibrant.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_github),
                            contentDescription = null,
                            tint = animatedColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(id = R.string.support_ideas_title), fontWeight = FontWeight.Bold, color = animatedColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(id = R.string.support_ideas_desc), color = dynamicTextColor, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Card(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.patreon.com/c/aldearius/membership"))
                        context.startActivity(intent)
                        onDismissRequest()
                    },
                    colors = CardDefaults.cardColors(containerColor = colorVibrant.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_patreon),
                            contentDescription = null,
                            tint = animatedColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(id = R.string.support_patreon_title), fontWeight = FontWeight.Bold, color = animatedColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(id = R.string.support_patreon_desc), color = dynamicTextColor, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel), color = dynamicTextColor.copy(alpha = 0.7f))
            }
        },
        containerColor = paletteColors.dominant
    )
}

private fun getLocalIpAddress(): String {
    try {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.isLoopback || !networkInterface.isUp) continue
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                    return addr.hostAddress ?: "127.0.0.1"
                }
            }
        }
    } catch (e: Exception) { }
    return "127.0.0.1"
}
