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

    var capturingKeyFor by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(capturingKeyFor) {
        if (capturingKeyFor != null) {
            focusRequester.requestFocus()
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
                .onKeyEvent { event ->
                    if (capturingKeyFor != null && event.type == KeyEventType.KeyDown) {
                        val shift = if (event.isShiftPressed) "Shift+" else ""
                        val alt = if (event.isAltPressed) "Alt+" else ""
                        val ctrl = if (event.isCtrlPressed) "Ctrl+" else ""
                        val meta = if (event.isMetaPressed) "Meta+" else ""
                        
                        val baseKey = when(event.key) {
                            androidx.compose.ui.input.key.Key.DirectionRight -> "DirectionRight"
                            androidx.compose.ui.input.key.Key.DirectionLeft -> "DirectionLeft"
                            androidx.compose.ui.input.key.Key.DirectionUp -> "DirectionUp"
                            androidx.compose.ui.input.key.Key.DirectionDown -> "DirectionDown"
                            androidx.compose.ui.input.key.Key.Enter, androidx.compose.ui.input.key.Key.NumPadEnter -> "Enter"
                            androidx.compose.ui.input.key.Key.Spacebar -> "Spacebar"
                            androidx.compose.ui.input.key.Key.Escape -> "Escape"
                            androidx.compose.ui.input.key.Key.Tab -> "Tab"
                            else -> {
                                val name = event.key.toString()
                                var ext = name.substringAfterLast("Key: ").substringBefore(")")
                                if (!name.contains("Key:")) ext = event.key.nativeKeyCode.toString()
                                if (ext.contains("Unknown")) ext = "Unknown"
                                ext
                            }
                        }

                        // Don't capture just modifiers
                        if (baseKey != "ShiftLeft" && baseKey != "ShiftRight" && 
                            baseKey != "CtrlLeft" && baseKey != "CtrlRight" &&
                            baseKey != "AltLeft" && baseKey != "AltRight") {
                            
                            val combination = "$ctrl$alt$meta$shift$baseKey"
                            when (capturingKeyFor) {
                                "NextPage" -> { keyMapNextPage = combination; prefs.keyMapNextPage = combination }
                                "PrevPage" -> { keyMapPrevPage = combination; prefs.keyMapPrevPage = combination }
                                "SeekForward" -> { keyMapSeekForward = combination; prefs.keyMapSeekForward = combination }
                                "SeekBackward" -> { keyMapSeekBackward = combination; prefs.keyMapSeekBackward = combination }
                                "NavigateUp" -> { keyMapNavigateUp = combination; prefs.keyMapNavigateUp = combination }
                                "NavigateDown" -> { keyMapNavigateDown = combination; prefs.keyMapNavigateDown = combination }
                                "NavigateLeft" -> { keyMapNavigateLeft = combination; prefs.keyMapNavigateLeft = combination }
                                "NavigateRight" -> { keyMapNavigateRight = combination; prefs.keyMapNavigateRight = combination }
                                "TabNext" -> { keyMapTabNext = combination; prefs.keyMapTabNext = combination }
                                "TabPrev" -> { keyMapTabPrev = combination; prefs.keyMapTabPrev = combination }
                                "Action" -> { keyMapAction = combination; prefs.keyMapAction = combination }
                                "PlayPause" -> { keyMapPlayPause = combination; prefs.keyMapPlayPause = combination }
                            }
                            capturingKeyFor = null
                            return@onKeyEvent true
                        }
                    }
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        if (capturingKeyFor != null) {
                            capturingKeyFor = null
                            return@onKeyEvent true
                        } else {
                            onDismiss()
                            return@onKeyEvent true
                        }
                    }
                    false
                }
                .focusRequester(focusRequester)
                .focusable()
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(paletteColors.dominant)
                    .align(Alignment.Center)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Atajos de Teclado",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (capturingKeyFor != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(paletteColors.vibrant.copy(alpha = 0.2f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Presiona la combinación de teclas para: $capturingKeyFor",
                                color = paletteColors.vibrant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

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
