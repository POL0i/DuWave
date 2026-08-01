import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt"
with open(file_path, "r", encoding='utf-8') as f:
    content = f.read()

# Locate the start of "Física del Visualizador"
start_str = 'Text("Física del Visualizador"'
end_str = 'Spacer(modifier = Modifier.height(24.dp))\n            }\n        }\n    }'
s_idx = content.find(start_str)
e_idx = content.find(end_str, s_idx)
if s_idx != -1 and e_idx != -1:
    old_block = content[s_idx:e_idx + len(end_str)]
    
    new_block = """Text("Sensibilidad de Graves (Bajos): ${String.format("%.1f", bassMult)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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
    
    content = content.replace(old_block, new_block)
    with open(file_path, "w", encoding='utf-8') as f:
        f.write(content)
    print("Settings replaced successfully")
else:
    print("Could not find the settings block!")
