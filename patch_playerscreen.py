import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

# 1. Replace states
content = content.replace(
    "val amplitudesState = visualizerManager.amplitudes.collectAsState()",
    "val bassAmplitudesState = visualizerManager.bassAmplitudes.collectAsState()\n    val midAmplitudesState = visualizerManager.midAmplitudes.collectAsState()\n    val highAmplitudesState = visualizerManager.highAmplitudes.collectAsState()"
)

# 2. Replace LaunchedEffect
old_launched_effect = """            LaunchedEffect(visualizerManager.amplitudes) {
                visualizerManager.amplitudes.collect { amps ->
                    if (amps.isNotEmpty()) {
                        val rawBass = amps.take(amps.size / 3).average().toFloat().let { if(it.isNaN()) 0f else it }
                        val rawMid = amps.drop(amps.size / 3).take(amps.size / 3).average().toFloat().let { if(it.isNaN()) 0f else it }
                        val rawTreble = amps.takeLast(amps.size / 3).average().toFloat().let { if(it.isNaN()) 0f else it }
                        
                        launch { bassAvgAnim.animateTo(rawBass, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        launch { midAvgAnim.animateTo(rawMid, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        launch { trebleAvgAnim.animateTo(rawTreble, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        
                        val bassIntensity = rawBass.coerceIn(0f, 1f)
                        launch { animatedScaleAnim.animateTo(1f + (bassIntensity * 0.45f), androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)) }
                    }
                }
            }"""
new_launched_effect = """            LaunchedEffect(visualizerManager.bassAmplitudes, visualizerManager.midAmplitudes, visualizerManager.highAmplitudes) {
                launch {
                    visualizerManager.bassAmplitudes.collect { amps ->
                        if (amps.isNotEmpty()) {
                            val rawBass = amps.average().toFloat().let { if(it.isNaN()) 0f else it }
                            launch { bassAvgAnim.animateTo(rawBass, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                            val bassIntensity = rawBass.coerceIn(0f, 1f)
                            launch { animatedScaleAnim.animateTo(1f + (bassIntensity * 0.45f), androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)) }
                        }
                    }
                }
                launch {
                    visualizerManager.midAmplitudes.collect { amps ->
                        if (amps.isNotEmpty()) {
                            val rawMid = amps.average().toFloat().let { if(it.isNaN()) 0f else it }
                            launch { midAvgAnim.animateTo(rawMid, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        }
                    }
                }
                launch {
                    visualizerManager.highAmplitudes.collect { amps ->
                        if (amps.isNotEmpty()) {
                            val rawTreble = amps.average().toFloat().let { if(it.isNaN()) 0f else it }
                            launch { trebleAvgAnim.animateTo(rawTreble, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        }
                    }
                }
            }"""
content = content.replace(old_launched_effect, new_launched_effect)

# 3. Canvas rendering
old_canvas_start = "                val amplitudes = amplitudesState.value\n                val ghosts = ghostsState.value"
new_canvas_start = """                val bassAmplitudes = bassAmplitudesState.value
                val midAmplitudes = midAmplitudesState.value
                val highAmplitudes = highAmplitudesState.value
                val ghosts = ghostsState.value"""
content = content.replace(old_canvas_start, new_canvas_start)

# Replace the drawing block
old_draw_block = """                val numBars = amplitudes.size
                if (numBars > 0) {
                    val distStep = (pathLength / 2f) / (numBars - 1).coerceAtLeast(1).toFloat()
                    
                    when (currentStyle) {"""

new_draw_block = """                
                val maxAnim = maxOf(bassAvg, midAvg, trebleAvg, 0.001f)
                val bassOpacity = (bassAvg / maxAnim).coerceIn(0.2f, 1.0f)
                val midOpacity = (midAvg / maxAnim).coerceIn(0.2f, 1.0f)
                val highOpacity = (trebleAvg / maxAnim).coerceIn(0.2f, 1.0f)

                fun drawLayer(amps: FloatArray, layerColor: Color, opacity: Float, isBassLayer: Boolean) {
                    val numBars = amps.size
                    if (numBars == 0) return
                    val distStep = (pathLength / 2f) / (numBars - 1).coerceAtLeast(1).toFloat()
                    
                    val layerBrush = Brush.sweepGradient(
                        colors = listOf(
                            layerColor.copy(alpha = opacity),
                            layerColor.copy(alpha = opacity * 0.5f),
                            layerColor.copy(alpha = opacity),
                            layerColor.copy(alpha = opacity * 0.8f),
                            layerColor.copy(alpha = opacity)
                        )
                    )
                    
                    val colorVibrant = layerColor.copy(alpha = opacity)
                    val colorDominant = layerColor.copy(alpha = opacity * 0.8f)
                    val colorMuted = layerColor.copy(alpha = opacity * 0.5f)
                    val sweepGradient = layerBrush

                    when (currentStyle) {"""

content = content.replace(old_draw_block, new_draw_block)

# Replace amplitude references inside the when block so it uses the passed 'amps' array.
# The when block ends around line 908, we can just replace 'amplitudes[' with 'amps[' 
# since there are no other 'amplitudes' references after the 'when' inside the canvas.
# Also 'val numBars = amplitudes.size' was removed above.
content = content.replace("amplitudes[", "amps[")
content = content.replace("val numBars = amps.size", "val numBars = amps.size") # keep safe
content = content.replace("val amplitude = amps[i]", "val amplitude = amps[i]") # keep safe

# Finally, we need to call drawLayer or the special styles after the when block.
# Wait! In python, it's easier to just use Regex to find the `when (currentStyle) { ... }` block.
import re

when_pattern = re.compile(r'(when \(currentStyle\) \{.*?\n\s{24}\})', re.DOTALL)
match = when_pattern.search(content)

if match:
    when_block = match.group(1)
    # Wrap it inside drawLayer, then add the calls!
    # Note: RINGS and AURA are inside the when_block, we need to extract them!
    
    # Actually, if we just call drawLayer 3 times, RINGS and AURA will be drawn 3 times.
    # To fix this, we can add `if (currentStyle == VisualizerStyle.RINGS || currentStyle == VisualizerStyle.AURA) return` inside drawLayer, 
    # and then draw them separately outside.
    
    # Let's extract RINGS and AURA strings
    rings_pattern = re.compile(r'(\s+VisualizerStyle\.RINGS -> \{.*?\n\s{24}\})', re.DOTALL)
    aura_pattern = re.compile(r'(\s+VisualizerStyle\.AURA -> \{.*?\n\s{24}\})', re.DOTALL)
    
    rings_match = rings_pattern.search(when_block)
    aura_match = aura_pattern.search(when_block)
    
    rings_code = rings_match.group(1) if rings_match else ""
    aura_code = aura_match.group(1) if aura_match else ""
    
    # Remove them from when_block
    new_when_block = when_block
    if rings_code:
        new_when_block = new_when_block.replace(rings_code, "")
    if aura_code:
        new_when_block = new_when_block.replace(aura_code, "")
        
    replacement = new_when_block + """
                }
                
                if (currentStyle == VisualizerStyle.RINGS || currentStyle == VisualizerStyle.AURA) {
                    when (currentStyle) {
                        """ + rings_code.strip() + """
                        """ + aura_code.strip() + """
                        else -> {}
                    }
                } else {
                    drawLayer(bassAmplitudes, paletteColors.dominant, bassOpacity, true)
                    drawLayer(midAmplitudes, paletteColors.vibrant, midOpacity, false)
                    drawLayer(highAmplitudes, paletteColors.muted, highOpacity, false)
                }
"""
    content = content.replace(when_block, replacement)

with open(file_path, "w") as f:
    f.write(content)
print("Updated successfully")
