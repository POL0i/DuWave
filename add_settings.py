import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt"
with open(file_path, "r", encoding='utf-8') as f:
    content = f.read()

# Add missing settings to SettingsBottomSheet
old_settings_end = """                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }"""

new_settings_end = """                Spacer(modifier = Modifier.height(16.dp))

                Text("Efectos de Audio", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Slow Reverb", color = Color.White)
                    androidx.compose.material3.Switch(
                        checked = reverbEnabled,
                        onCheckedChange = { onSetReverb(it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Velocidad: ${String.format("%.2f", playbackSpeed)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = playbackSpeed,
                    onValueChange = { onSetSpeed(it) },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Tono: ${String.format("%.2f", playbackPitch)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = playbackPitch,
                    onValueChange = { onSetPitch(it) },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Preajuste (Efectos predeterminados)", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }"""
content = content.replace(old_settings_end, new_settings_end)

# Add showEditorDialog and showLyricsMatches at the end of the file
old_file_end = """        }
    }
}
"""

new_file_end = """        }
    }

    if (showEditorDialog) {
        var newTitle by remember { mutableStateOf(currentTrack?.title ?: "") }
        var newArtist by remember { mutableStateOf(currentTrack?.artist ?: "") }
        var newAlbum by remember { mutableStateOf(currentTrack?.album ?: "") }
        var newGenre by remember { mutableStateOf(currentTrack?.genre ?: "") }
        
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEditorDialog = false },
            title = { Text("Editar Metadatos", color = colorVibrant) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Título") },
                        singleLine = true
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = newArtist,
                        onValueChange = { newArtist = it },
                        label = { Text("Artista") },
                        singleLine = true
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = newAlbum,
                        onValueChange = { newAlbum = it },
                        label = { Text("Álbum") },
                        singleLine = true
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = newGenre,
                        onValueChange = { newGenre = it },
                        label = { Text("Género") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    currentTrack?.let { track ->
                        onUpdateTrackMetadata(track.id, newTitle, newArtist, newAlbum, newGenre)
                    }
                    showEditorDialog = false
                }) {
                    Text("Guardar", color = colorVibrant)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditorDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = colorDominant
        )
    }

    if (showLyricsMatches) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLyricsMatches = false },
            title = { Text("Resultados de Líricas", color = colorVibrant) },
            text = {
                if (availableLyricsResults.isEmpty()) {
                    Text("No hay resultados", color = Color.White)
                } else {
                    LazyColumn {
                        items(availableLyricsResults) { result ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentTrack?.let { track ->
                                            playerViewModel.applyLyrics(track, result)
                                        }
                                        showLyricsMatches = false
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(result.trackName, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${result.artistName} - ${result.albumName}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                Text("Confianza: ${String.format("%.0f", result.score * 100)}%", color = colorVibrant, style = MaterialTheme.typography.labelSmall)
                            }
                            androidx.compose.material3.Divider(color = Color.DarkGray)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLyricsMatches = false }) {
                    Text("Cerrar", color = Color.Gray)
                }
            },
            containerColor = colorDominant
        )
    }
}
"""

content = content.replace(old_file_end, new_file_end)

with open(file_path, "w", encoding='utf-8') as f:
    f.write(content)
print("Updated successfully!")
