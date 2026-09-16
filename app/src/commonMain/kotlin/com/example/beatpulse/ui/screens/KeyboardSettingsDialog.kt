package com.example.beatpulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IPreferencesManager

@Composable
fun KeyboardSettingsDialog(
    onDismiss: () -> Unit,
    paletteColors: PaletteColors,
    prefs: IPreferencesManager
) {
    var keyMapNextPage by remember { mutableStateOf(prefs.keyMapNextPage) }
    var keyMapPrevPage by remember { mutableStateOf(prefs.keyMapPrevPage) }
    var keyMapSeekForward by remember { mutableStateOf(prefs.keyMapSeekForward) }
    var keyMapSeekBackward by remember { mutableStateOf(prefs.keyMapSeekBackward) }
    var keyMapNavigateUp by remember { mutableStateOf(prefs.keyMapNavigateUp) }
    var keyMapNavigateDown by remember { mutableStateOf(prefs.keyMapNavigateDown) }
    var keyMapNavigateLeft by remember { mutableStateOf(prefs.keyMapNavigateLeft) }
    var keyMapNavigateRight by remember { mutableStateOf(prefs.keyMapNavigateRight) }
    var keyMapTabNext by remember { mutableStateOf(prefs.keyMapTabNext) }
    var keyMapTabPrev by remember { mutableStateOf(prefs.keyMapTabPrev) }
    var keyMapAction by remember { mutableStateOf(prefs.keyMapAction) }
    var keyMapPlayPause by remember { mutableStateOf(prefs.keyMapPlayPause) }

    var keyMapGlobalList by remember { mutableStateOf(prefs.keyMapGlobalList) }
    var keyMapGlobalSearch by remember { mutableStateOf(prefs.keyMapGlobalSearch) }
    var keyMapRecommendations by remember { mutableStateOf(prefs.keyMapRecommendations) }
    var keyMapLibraryPlaylists by remember { mutableStateOf(prefs.keyMapLibraryPlaylists) }
    var keyMapLibraryArtists by remember { mutableStateOf(prefs.keyMapLibraryArtists) }
    var keyMapLibraryAlbums by remember { mutableStateOf(prefs.keyMapLibraryAlbums) }
    var keyMapLibraryFolders by remember { mutableStateOf(prefs.keyMapLibraryFolders) }
    var keyMapPlayerScreen by remember { mutableStateOf(prefs.keyMapPlayerScreen) }
    
    var keyMapOpenStats by remember { mutableStateOf(prefs.keyMapOpenStats) }
    var keyMapOpenDesign by remember { mutableStateOf(prefs.keyMapOpenDesign) }
    var keyMapOpenKeyboard by remember { mutableStateOf(prefs.keyMapOpenKeyboard) }
    var keyMapOpenTimer by remember { mutableStateOf(prefs.keyMapOpenTimer) }
    var keyMapOpenEqualizer by remember { mutableStateOf(prefs.keyMapOpenEqualizer) }
    var keyMapOpenAudioEffects by remember { mutableStateOf(prefs.keyMapOpenAudioEffects) }
    var keyMapOpenPatreon by remember { mutableStateOf(prefs.keyMapOpenPatreon) }

    var capturingKeyFor by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(capturingKeyFor) {
        if (capturingKeyFor != null) {
            focusRequester.requestFocus()
        }
    }

    val outerFocusRequester = remember { FocusRequester() }

    LaunchedEffect(capturingKeyFor) {
        if (capturingKeyFor == null) {
            outerFocusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .focusRequester(outerFocusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(paletteColors.dominant)
                    .padding(32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Atajos de Teclado", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (capturingKeyFor != null) {
                        var accumulatedModifiers by remember { mutableStateOf(setOf<String>()) }
                        var accumulatedKeys by remember { mutableStateOf(setOf<String>()) }
                        var lastActivityCounter by remember { mutableStateOf(0) }
                        var progress by remember { mutableStateOf(0f) }
                        var conflictMessage by remember { mutableStateOf<String?>(null) }

                        val currentCombinationStr = buildString {
                            if (accumulatedModifiers.contains("Ctrl")) append("Ctrl+")
                            if (accumulatedModifiers.contains("Alt")) append("Alt+")
                            if (accumulatedModifiers.contains("Meta")) append("Meta+")
                            if (accumulatedModifiers.contains("Shift")) append("Shift+")
                            if (accumulatedKeys.isNotEmpty()) {
                                append(accumulatedKeys.joinToString("+"))
                            }
                        }

                        LaunchedEffect(lastActivityCounter) {
                            if (accumulatedKeys.isEmpty()) {
                                progress = 0f
                                return@LaunchedEffect
                            }
                            
                            val steps = 300
                            for (i in 1..steps) {
                                progress = i / steps.toFloat()
                                kotlinx.coroutines.delay(10)
                            }
                            
                            val finalCombination = currentCombinationStr
                            
                            val allBindings = mapOf(
                                "NextPage" to keyMapNextPage,
                                "PrevPage" to keyMapPrevPage,
                                "SeekForward" to keyMapSeekForward,
                                "SeekBackward" to keyMapSeekBackward,
                                "NavigateUp" to keyMapNavigateUp,
                                "NavigateDown" to keyMapNavigateDown,
                                "NavigateLeft" to keyMapNavigateLeft,
                                "NavigateRight" to keyMapNavigateRight,
                                "TabNext" to keyMapTabNext,
                                "TabPrev" to keyMapTabPrev,
                                "Action" to keyMapAction,
                                "PlayPause" to keyMapPlayPause,
                                "GlobalList" to keyMapGlobalList,
                                "GlobalSearch" to keyMapGlobalSearch,
                                "Recommendations" to keyMapRecommendations,
                                "LibraryPlaylists" to keyMapLibraryPlaylists,
                                "LibraryArtists" to keyMapLibraryArtists,
                                "LibraryAlbums" to keyMapLibraryAlbums,
                                "LibraryFolders" to keyMapLibraryFolders,
                                "PlayerScreen" to keyMapPlayerScreen,
                                "OpenStats" to keyMapOpenStats,
                                "OpenDesign" to keyMapOpenDesign,
                                "OpenKeyboard" to keyMapOpenKeyboard,
                                "OpenTimer" to keyMapOpenTimer,
                                "OpenEqualizer" to keyMapOpenEqualizer,
                                "OpenAudioEffects" to keyMapOpenAudioEffects,
                                "OpenPatreon" to keyMapOpenPatreon
                            )
                            
                            val conflict = allBindings.entries.firstOrNull { it.value == finalCombination && it.key != capturingKeyFor }
                            if (conflict != null) {
                                conflictMessage = "¡Esta combinación ya está en uso por ${conflict.key}!"
                                kotlinx.coroutines.delay(2000)
                                conflictMessage = null
                                accumulatedKeys = setOf()
                                accumulatedModifiers = setOf()
                                progress = 0f
                            } else {
                                when (capturingKeyFor) {
                                    "NextPage" -> { keyMapNextPage = finalCombination; prefs.keyMapNextPage = finalCombination }
                                    "PrevPage" -> { keyMapPrevPage = finalCombination; prefs.keyMapPrevPage = finalCombination }
                                    "SeekForward" -> { keyMapSeekForward = finalCombination; prefs.keyMapSeekForward = finalCombination }
                                    "SeekBackward" -> { keyMapSeekBackward = finalCombination; prefs.keyMapSeekBackward = finalCombination }
                                    "NavigateUp" -> { keyMapNavigateUp = finalCombination; prefs.keyMapNavigateUp = finalCombination }
                                    "NavigateDown" -> { keyMapNavigateDown = finalCombination; prefs.keyMapNavigateDown = finalCombination }
                                    "NavigateLeft" -> { keyMapNavigateLeft = finalCombination; prefs.keyMapNavigateLeft = finalCombination }
                                    "NavigateRight" -> { keyMapNavigateRight = finalCombination; prefs.keyMapNavigateRight = finalCombination }
                                    "TabNext" -> { keyMapTabNext = finalCombination; prefs.keyMapTabNext = finalCombination }
                                    "TabPrev" -> { keyMapTabPrev = finalCombination; prefs.keyMapTabPrev = finalCombination }
                                    "Action" -> { keyMapAction = finalCombination; prefs.keyMapAction = finalCombination }
                                    "PlayPause" -> { keyMapPlayPause = finalCombination; prefs.keyMapPlayPause = finalCombination }
                                    "GlobalList" -> { keyMapGlobalList = finalCombination; prefs.keyMapGlobalList = finalCombination }
                                    "GlobalSearch" -> { keyMapGlobalSearch = finalCombination; prefs.keyMapGlobalSearch = finalCombination }
                                    "Recommendations" -> { keyMapRecommendations = finalCombination; prefs.keyMapRecommendations = finalCombination }
                                    "LibraryPlaylists" -> { keyMapLibraryPlaylists = finalCombination; prefs.keyMapLibraryPlaylists = finalCombination }
                                    "LibraryArtists" -> { keyMapLibraryArtists = finalCombination; prefs.keyMapLibraryArtists = finalCombination }
                                    "LibraryAlbums" -> { keyMapLibraryAlbums = finalCombination; prefs.keyMapLibraryAlbums = finalCombination }
                                    "LibraryFolders" -> { keyMapLibraryFolders = finalCombination; prefs.keyMapLibraryFolders = finalCombination }
                                    "PlayerScreen" -> { keyMapPlayerScreen = finalCombination; prefs.keyMapPlayerScreen = finalCombination }
                                    "OpenStats" -> { keyMapOpenStats = finalCombination; prefs.keyMapOpenStats = finalCombination }
                                    "OpenDesign" -> { keyMapOpenDesign = finalCombination; prefs.keyMapOpenDesign = finalCombination }
                                    "OpenKeyboard" -> { keyMapOpenKeyboard = finalCombination; prefs.keyMapOpenKeyboard = finalCombination }
                                    "OpenTimer" -> { keyMapOpenTimer = finalCombination; prefs.keyMapOpenTimer = finalCombination }
                                    "OpenEqualizer" -> { keyMapOpenEqualizer = finalCombination; prefs.keyMapOpenEqualizer = finalCombination }
                                    "OpenAudioEffects" -> { keyMapOpenAudioEffects = finalCombination; prefs.keyMapOpenAudioEffects = finalCombination }
                                    "OpenPatreon" -> { keyMapOpenPatreon = finalCombination; prefs.keyMapOpenPatreon = finalCombination }
                                }
                                capturingKeyFor = null
                            }
                        }

                        Dialog(onDismissRequest = { capturingKeyFor = null }) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(paletteColors.dominant)
                                    .padding(32.dp)
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown) {
                                            val shift = event.isShiftPressed
                                            val alt = event.isAltPressed
                                            val ctrl = event.isCtrlPressed
                                            val meta = event.isMetaPressed
                                            
                                            val baseKey = when(event.key) {
                                                Key.DirectionRight -> "DirectionRight"
                                                Key.DirectionLeft -> "DirectionLeft"
                                                Key.DirectionUp -> "DirectionUp"
                                                Key.DirectionDown -> "DirectionDown"
                                                Key.Enter, Key.NumPadEnter -> "Enter"
                                                Key.Spacebar -> "Spacebar"
                                                Key.Escape -> "Escape"
                                                Key.Tab -> "Tab"
                                                else -> {
                                                    val name = event.key.toString()
                                                    var ext = name.substringAfterLast("Key: ").substringBefore(")")
                                                    if (!name.contains("Key:")) ext = event.key.keyCode.toString()
                                                    if (ext.contains("Unknown")) ext = "Unknown"
                                                    ext
                                                }
                                            }

                                            if (baseKey == "Escape") {
                                                capturingKeyFor = null
                                                return@onKeyEvent true
                                            }

                                            val newModifiers = mutableSetOf<String>().apply { addAll(accumulatedModifiers) }
                                            if (shift) newModifiers.add("Shift")
                                            if (alt) newModifiers.add("Alt")
                                            if (ctrl) newModifiers.add("Ctrl")
                                            if (meta) newModifiers.add("Meta")
                                            accumulatedModifiers = newModifiers

                                            val isModifierStr = baseKey.lowercase() in listOf("shift", "mayús", "mayus", "ctrl", "control", "alt", "meta", "super", "command", "cmd", "windows")
                                            val isModifier = event.key == Key.ShiftLeft || event.key == Key.ShiftRight ||
                                                             event.key == Key.CtrlLeft || event.key == Key.CtrlRight ||
                                                             event.key == Key.AltLeft || event.key == Key.AltRight ||
                                                             event.key == Key.MetaLeft || event.key == Key.MetaRight ||
                                                             isModifierStr

                                            if (!isModifier) {
                                                val newKeys = mutableSetOf<String>().apply { addAll(accumulatedKeys) }
                                                newKeys.add(baseKey)
                                                accumulatedKeys = newKeys
                                            }
                                            
                                            if (accumulatedKeys.isNotEmpty()) {
                                                lastActivityCounter++
                                            }
                                            return@onKeyEvent true
                                        }
                                        false
                                    }
                                    .focusRequester(focusRequester)
                                    .focusable(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        "Asignando nueva tecla para:\n$capturingKeyFor",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.1f))
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = currentCombinationStr.ifEmpty { "Presiona una tecla..." },
                                            color = if (conflictMessage != null) Color.Red else paletteColors.vibrant,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    if (conflictMessage != null) {
                                        Text(conflictMessage!!, color = Color.Red, fontSize = 14.sp)
                                    } else if (accumulatedKeys.isEmpty() && accumulatedModifiers.isNotEmpty()) {
                                        Text("Presiona una tecla adicional...", color = Color.Yellow, fontSize = 14.sp)
                                    } else {
                                        androidx.compose.material3.LinearProgressIndicator(
                                            progress = progress,
                                            modifier = Modifier.fillMaxWidth(0.8f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                            color = paletteColors.vibrant,
                                            trackColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    }
                                    
                                    Text("Presiona Escape para cancelar", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Text("Navegación de Secciones (Global)", style = MaterialTheme.typography.titleMedium, color = paletteColors.vibrant)
                    ShortcutRow("Lista Global", keyMapGlobalList) { capturingKeyFor = "GlobalList" }
                    ShortcutRow("Búsqueda Global", keyMapGlobalSearch) { capturingKeyFor = "GlobalSearch" }
                    ShortcutRow("Recomendaciones", keyMapRecommendations) { capturingKeyFor = "Recommendations" }
                    ShortcutRow("Biblioteca: Playlists", keyMapLibraryPlaylists) { capturingKeyFor = "LibraryPlaylists" }
                    ShortcutRow("Biblioteca: Artistas", keyMapLibraryArtists) { capturingKeyFor = "LibraryArtists" }
                    ShortcutRow("Biblioteca: Álbumes", keyMapLibraryAlbums) { capturingKeyFor = "LibraryAlbums" }
                    ShortcutRow("Biblioteca: Carpetas", keyMapLibraryFolders) { capturingKeyFor = "LibraryFolders" }
                    ShortcutRow("Pantalla Reproductor", keyMapPlayerScreen) { capturingKeyFor = "PlayerScreen" }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Paneles y Ajustes", style = MaterialTheme.typography.titleMedium, color = paletteColors.vibrant)
                    ShortcutRow("Estadísticas", keyMapOpenStats) { capturingKeyFor = "OpenStats" }
                    ShortcutRow("Diseño", keyMapOpenDesign) { capturingKeyFor = "OpenDesign" }
                    ShortcutRow("Atajos de Teclado", keyMapOpenKeyboard) { capturingKeyFor = "OpenKeyboard" }
                    ShortcutRow("Temporizador", keyMapOpenTimer) { capturingKeyFor = "OpenTimer" }
                    ShortcutRow("Ecualizador", keyMapOpenEqualizer) { capturingKeyFor = "OpenEqualizer" }
                    ShortcutRow("Efectos de Audio", keyMapOpenAudioEffects) { capturingKeyFor = "OpenAudioEffects" }
                    ShortcutRow("Patreon", keyMapOpenPatreon) { capturingKeyFor = "OpenPatreon" }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Navegación Global", style = MaterialTheme.typography.titleMedium, color = paletteColors.vibrant)
                    ShortcutRow("Página Siguiente", keyMapNextPage) { capturingKeyFor = "NextPage" }
                    ShortcutRow("Página Anterior", keyMapPrevPage) { capturingKeyFor = "PrevPage" }
                    ShortcutRow("Play/Pause", keyMapPlayPause) { capturingKeyFor = "PlayPause" }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Reproductor", style = MaterialTheme.typography.titleMedium, color = paletteColors.vibrant)
                    ShortcutRow("Adelantar 10s", keyMapSeekForward) { capturingKeyFor = "SeekForward" }
                    ShortcutRow("Retroceder 10s", keyMapSeekBackward) { capturingKeyFor = "SeekBackward" }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Navegación por Elementos (Foco)", style = MaterialTheme.typography.titleMedium, color = paletteColors.vibrant)
                    ShortcutRow("Siguiente Sección (Tab)", keyMapTabNext) { capturingKeyFor = "TabNext" }
                    ShortcutRow("Sección Anterior", keyMapTabPrev) { capturingKeyFor = "TabPrev" }
                    ShortcutRow("Arriba", keyMapNavigateUp) { capturingKeyFor = "NavigateUp" }
                    ShortcutRow("Abajo", keyMapNavigateDown) { capturingKeyFor = "NavigateDown" }
                    ShortcutRow("Izquierda", keyMapNavigateLeft) { capturingKeyFor = "NavigateLeft" }
                    ShortcutRow("Derecha", keyMapNavigateRight) { capturingKeyFor = "NavigateRight" }
                    ShortcutRow("Acción (Enter)", keyMapAction) { capturingKeyFor = "Action" }
                }
            }
        }
    }
}

@Composable
private fun ShortcutRow(label: String, currentValue: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(currentValue, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
