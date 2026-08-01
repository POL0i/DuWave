import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt"
with open(file_path, "r", encoding='utf-8') as f:
    content = f.read()

# 1. Fix distStep in Canvas
old_distStep = "val distStep = pathLength / numBars"
new_distStep = "val distStep = (pathLength / 2f) / (numBars - 1).coerceAtLeast(1).toFloat()"
content = content.replace(old_distStep, new_distStep)

# 2. Add showEffectsDialog state
old_showEq = "var showEqDialog by remember { mutableStateOf(false) }"
new_showEq = """var showEqDialog by remember { mutableStateOf(false) }
    var showEffectsDialog by remember { mutableStateOf(false) }"""
if "var showEffectsDialog by remember" not in content:
    content = content.replace(old_showEq, new_showEq)

# 3. Add Effects Button between EQ and Edit
old_icon_row = """                IconButton(onClick = { showEditorDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Track", tint = Color.Gray, modifier = Modifier.size(28.dp))
                }"""
new_icon_row = """                IconButton(onClick = { showEffectsDialog = true }) {
                    Icon(androidx.compose.material.icons.Icons.Default.AutoFixHigh, contentDescription = "Audio Effects", tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { showEditorDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Track", tint = Color.Gray, modifier = Modifier.size(28.dp))
                }"""
if "showEffectsDialog = true" not in content:
    content = content.replace(old_icon_row, new_icon_row)

# 4. Remove Physics, Frequency Filter, general Sensitivity, and Audio Effects from SettingsBottomSheet
# We'll replace the block from "Text("Físicas del Visualizador"" down to the end of the SettingsBottomSheet
old_settings_block_pattern = re.compile(r'Text\("Físicas del Visualizador",.*?\n\s+Spacer\(modifier = Modifier\.height\(24\.dp\)\)\n\s+\}\n\s+\}\n\s+\}', re.DOTALL)

new_settings_block = """Text("Sensibilidad de Graves (Bajos): ${String.format("%.1f", bassMult)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = bassMult,
                    onValueChange = {
                        visualizerManager.bassMultiplier.value = it
                        prefs.bassMultiplier = it
                    },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Sensibilidad de Medios: ${String.format("%.1f", midMult)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = midMult,
                    onValueChange = {
                        visualizerManager.midMultiplier.value = it
                        prefs.midMultiplier = it
                    },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Sensibilidad de Altos (Agudos): ${String.format("%.1f", trebleMult)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = trebleMult,
                    onValueChange = {
                        visualizerManager.trebleMultiplier.value = it
                        prefs.trebleMultiplier = it
                    },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }"""

content = re.sub(old_settings_block_pattern, new_settings_block, content)


# 5. Add Audio Effects dialog at the end of the file
effects_dialog = """    if (showEffectsDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEffectsDialog = false },
            title = { Text("Efectos de Audio", color = colorVibrant) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Slow Reverb", color = Color.White)
                        androidx.compose.material3.Switch(
                            checked = reverbEnabled,
                            onCheckedChange = { onSetReverb(it) },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                        )
                    }

                    Column {
                        Text("Velocidad: ${String.format("%.2f", playbackSpeed)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = playbackSpeed,
                            onValueChange = { onSetSpeed(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                        )
                    }

                    Column {
                        Text("Tono: ${String.format("%.2f", playbackPitch)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = playbackPitch,
                            onValueChange = { onSetPitch(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                        )
                    }

                    Column {
                        Text("Preajuste (Efectos)", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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
                TextButton(onClick = { showEffectsDialog = false }) {
                    Text("Cerrar", color = colorVibrant)
                }
            },
            containerColor = colorDominant
        )
    }
}
"""

old_end = """}
"""
content = content.replace("}\n}", "}\n" + effects_dialog) # Just append to the end before the last bracket

with open(file_path, "w", encoding='utf-8') as f:
    f.write(content)
print("Updated successfully")
