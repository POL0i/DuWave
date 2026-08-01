import re

with open("app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt", "r") as f:
    content = f.read()

# 1. Add amplitudeMultiplier and apply it to bassAvg, midAvg, trebleAvg
replacement1 = """            val amplitudeMultiplier by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPlayingState) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 500, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
            )
            
            val bassAvg = bassAvgAnim.value * amplitudeMultiplier
            val midAvg = midAvgAnim.value * amplitudeMultiplier
            val trebleAvg = trebleAvgAnim.value * amplitudeMultiplier"""

content = content.replace("            val bassAvg = bassAvgAnim.value\n            val midAvg = midAvgAnim.value\n            val trebleAvg = trebleAvgAnim.value", replacement1)

# 2. Also apply amplitudeMultiplier to the raw amplitudes inside Canvas
replacement2 = """                val amplitudeMultiplier = this@Box.amplitudeMultiplier
                val bassAmplitudes = bassAmplitudesState.value.map { it * amplitudeMultiplier }.toFloatArray()
                val midAmplitudes = midAmplitudesState.value.map { it * amplitudeMultiplier }.toFloatArray()
                val highAmplitudes = highAmplitudesState.value.map { it * amplitudeMultiplier }.toFloatArray()"""

content = content.replace("                val bassAmplitudes = bassAmplitudesState.value\n                val midAmplitudes = midAmplitudesState.value\n                val highAmplitudes = highAmplitudesState.value", replacement2)

# 3. Reduce the bass circle thickness
content = content.replace("style = Stroke(width = 80f + bassAvg * 200f", "style = Stroke(width = 30f + bassAvg * 100f")

# 4. In SLIME visualizer, reduce the layer index 0 extrude
# Find: val extrude = 5f + (amplitude * 150f)
replacement3 = """val layerMultiplier = if (layerIndex == 0) 0.6f else 1f
                                val extrude = 5f + (amplitude * 150f * layerMultiplier)"""
content = content.replace("val extrude = 5f + (amplitude * 150f)", replacement3)

with open("app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt", "w") as f:
    f.write(content)
