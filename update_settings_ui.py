import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt"
with open(file_path, "r", encoding='utf-8') as f:
    content = f.read()

# We need to replace the section from "Sensibilidad de Graves" up to the end of the SettingsBottomSheet
start_str = 'Text("Sensibilidad de Graves (Bajos)'
end_str = 'Spacer(modifier = Modifier.height(24.dp))\n            }\n        }\n    }'

s_idx = content.find(start_str)
e_idx = content.find(end_str, s_idx)
if s_idx != -1 and e_idx != -1:
    old_block = content[s_idx:e_idx + len(end_str)]
    
    new_block = """Text("Arquetipo de Ondas", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = {
                        visualizerManager.visualizerArchetype.value = 0
                        prefs.visualizerArchetype = 0
                    }) {
                        Text("3 Ondas Superpuestas", color = if (visualizerArchetype == 0) colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = {
                        visualizerManager.visualizerArchetype.value = 1
                        prefs.visualizerArchetype = 1
                    }) {
                        Text("1 Onda Combinada", color = if (visualizerArchetype == 1) colorVibrant else Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Modo de Cálculo (FFT)", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = {
                        visualizerManager.fftMode.value = "AVERAGE"
                        prefs.visualizerFftMode = "AVERAGE"
                    }) {
                        Text("Promedio (Estable)", color = if (fftMode == "AVERAGE") colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = {
                        visualizerManager.fftMode.value = "MAX"
                        prefs.visualizerFftMode = "MAX"
                    }) {
                        Text("Máximo (Dinámico)", color = if (fftMode == "MAX") colorVibrant else Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Sensibilidad General: ${String.format("%.1f", sensitivity)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = sensitivity,
                    onValueChange = {
                        visualizerManager.sensitivity.value = it
                        prefs.sensitivity = it
                    },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Reactividad (Ghosts): ${String.format("%.2f", reactivity)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.size(12.dp).background(colorDominant, androidx.compose.foundation.shape.CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sensibilidad de Graves (Bajos): ${String.format("%.1f", bassMult)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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
                    Text("Sensibilidad de Medios: ${String.format("%.1f", midMult)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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
                    Text("Sensibilidad de Altos (Agudos): ${String.format("%.1f", trebleMult)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }"""
    
    content = content.replace(old_block, new_block)
    with open(file_path, "w", encoding='utf-8') as f:
        f.write(content)
    print("Settings UI replaced successfully")
else:
    print("Could not find the settings block!")
