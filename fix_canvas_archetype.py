import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt"
with open(file_path, "r", encoding='utf-8') as f:
    content = f.read()

# We need to find where the layers are drawn
start_str = "                if (currentStyle == VisualizerStyle.RINGS || currentStyle == VisualizerStyle.AURA) {"
end_str = """                } else {
                    drawLayer(bassAmplitudes, paletteColors.dominant, bassOpacity, true)
                    drawLayer(midAmplitudes, paletteColors.vibrant, midOpacity, false)
                    drawLayer(highAmplitudes, paletteColors.muted, highOpacity, false)
                }"""

s_idx = content.find(start_str)
e_idx = content.find(end_str, s_idx)
if s_idx != -1 and e_idx != -1:
    old_block = content[s_idx:e_idx + len(end_str)]
    
    new_block = """                if (currentStyle == VisualizerStyle.RINGS || currentStyle == VisualizerStyle.AURA) {
                    when (currentStyle) {
                        VisualizerStyle.RINGS -> {
                            drawLayer(bassAmplitudes, paletteColors.dominant, bassOpacity, true)
                            drawLayer(midAmplitudes, paletteColors.vibrant, midOpacity, false)
                            drawLayer(highAmplitudes, paletteColors.muted, highOpacity, false)
                        }
                        VisualizerStyle.AURA -> {
                            drawLayer(bassAmplitudes, paletteColors.dominant, bassOpacity, true)
                            drawLayer(midAmplitudes, paletteColors.vibrant, midOpacity, false)
                            drawLayer(highAmplitudes, paletteColors.muted, highOpacity, false)
                        }
                        else -> {}
                    }
                } else {
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
    
    content = content.replace(old_block, new_block)
    with open(file_path, "w", encoding='utf-8') as f:
        f.write(content)
    print("Canvas archetype logic updated")
else:
    print("Could not find Canvas block")
