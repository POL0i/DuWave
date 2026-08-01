import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt"
with open(file_path, "r", encoding='utf-8') as f:
    content = f.read()

old_block = """                } else {
                    if (visualizerArchetype == 1) {
                    // 1 Onda Combinada Segmentada
                    drawLayer(combinedAmplitudesState.value, paletteColors.vibrant, maxOf(bassOpacity, midOpacity, highOpacity), true)
                } else {
                    // 3 Ondas Superpuestas
                    drawLayer(bassAmplitudes, paletteColors.dominant, bassOpacity, true)
                    drawLayer(midAmplitudes, paletteColors.vibrant, midOpacity, false)
                    drawLayer(highAmplitudes, paletteColors.muted, highOpacity, false)
                }"""

new_block = """                } else {
                    if (visualizerArchetype == 1) {
                        // 1 Onda Combinada Segmentada
                        drawLayer(combinedAmplitudesState.value, paletteColors.vibrant, maxOf(bassOpacity, midOpacity, highOpacity), true)
                    } else {
                        // 3 Ondas Superpuestas
                        drawLayer(bassAmplitudes, paletteColors.dominant, bassOpacity, true)
                        drawLayer(midAmplitudes, paletteColors.vibrant, midOpacity, false)
                        drawLayer(highAmplitudes, paletteColors.muted, highOpacity, false)
                    }
                }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open(file_path, "w", encoding='utf-8') as f:
        f.write(content)
    print("Fixed syntax error.")
else:
    print("Could not find the block to fix.")
