package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager

private const val SHADER_SRC = """
    uniform vec2 u_resolution;
    uniform float u_time;
    uniform float u_energy;
    uniform vec4 u_dominant;
    uniform vec4 u_vibrant;
    uniform vec4 u_muted;

    const float N = 10.0;
    const float PI = 3.14159265;

    vec3 palette(in float t) {
        vec3 a = mix(u_dominant.rgb, u_muted.rgb, 0.5);
        vec3 b = mix(u_vibrant.rgb, u_dominant.rgb, 0.5);
        vec3 c = vec3(1.0, 1.0, 1.0);
        vec3 d = vec3(0.0, 0.33, 0.67);
        return a + b * cos(6.283185 * (c * t + d));
    }

    vec2 rot(vec2 p, float t){
        return vec2(p.x * cos(t) + p.y * sin(t), p.y * cos(t) - p.x * sin(t));
    }

    vec4 main(vec2 fragCoord) {
        vec2 p = (fragCoord.xy - u_resolution.xy / 2.0) / u_resolution.y;
        
        float nsteps = 20.0;
        float baseTime = mod(u_time * 0.8, nsteps * 2.0);
        float animTime = baseTime;
        if (animTime > nsteps) {
            animTime = 20.0 - (animTime - 20.0);
        }
        float time = max(nsteps - 0.01 - animTime, 0.0);

        float it = floor(time);
        time = mod(time, 1.0);

        float tx = cos(clamp(1.5 * time, 0.0, 1.0) * PI);
        float th = cos(time * PI);

        bool inside = false;
        float highlight = 0.0;

        for(float i = 0.0; i < 10.0; i += 1.0){ // N is 10
            vec2 tp1 = rot(p, i / N * 2.0 * PI);
            vec2 tp2 = rot(p, (i + 1.0) / N * 2.0 * PI);

            if(tp1.x > 0.1 * tx && tp2.x < 0.1 * tx){
                vec2 off = rot(vec2(1.0, 0.0), PI - (i + 0.5) / N * 2.0 * PI) * 0.1 / cos(PI / N);
                p += (tx - 1.0) * off;

                float r = min(abs(tp1.x - 0.1 * tx), abs(tp2.x - 0.1 * tx));
                highlight = (0.9 * mod(i, 2.0)) * pow((1.0 + th) * (1.0 - th), 0.5) * (1.0 - min(1.0, 5.0 * sqrt(r)));

                inside = true;
                break;
            }
        }
        
        if(!inside){
            return vec4(u_dominant.rgb * 0.1, 1.0);
        }
        
        vec3 col = vec3(0.0);
        float steps = max(0.0, nsteps - it);
        for(float ms = 0.0; ms < 20.0; ms += 1.0){
            if (ms >= steps) break;
            for(float i = 0.0; i < 10.0; i += 1.0){
                vec2 tp1 = rot(p, i / N * 2.0 * PI);
                vec2 tp2 = rot(p, (i + 1.0) / N * 2.0 * PI);

                if(tp1.x > 0.1 && tp2.x < 0.1){
                    vec2 off = rot(vec2(1.0, 0.0), PI - (i + 0.5) / N * 2.0 * PI) * 0.1 / cos(PI / N);
                    p += 2.0 * off;
                    if(abs(ms - (steps - 1.0)) < 0.5) {
                        col += palette(i / N) * (0.9 + tp1.x * 0.4 + u_energy * 0.8);
                    }
                    break;
                }
            }
        }
        
        vec3 finalColor = mix(col, u_vibrant.rgb, highlight);
        return vec4(clamp(finalColor, 0.0, 1.0), 1.0);
    }
"""

@Composable
actual fun PolygonUnfoldBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val effect = remember { RuntimeEffect.makeForShader(SHADER_SRC) }
    var time by remember { mutableStateOf(0f) }
    var dynamicEnergy by remember { mutableStateOf(0f) }
    
    val amplitudesState = visualizerManager.combinedAmplitudes.collectAsState()
    var smoothEnergy by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlayerScreen) {
        var lastTime = 0L
        while (true) {
            androidx.compose.runtime.withFrameMillis { frameTime ->
                if (lastTime == 0L) lastTime = frameTime
                val dt = ((frameTime - lastTime) / 1000f).coerceAtMost(0.1f)
                lastTime = frameTime
                
                val currentAmps = amplitudesState.value
                var sumAmps = 0f
                val limit = if (currentAmps.size < 6) currentAmps.size else 6
                for (i in 0 until limit) {
                    sumAmps += currentAmps[i]
                }
                val rawEnergy = if (limit > 0) sumAmps / limit else 0f
                
                smoothEnergy += (rawEnergy - smoothEnergy) * (1f - kotlin.math.exp(-15f * dt))
                dynamicEnergy = smoothEnergy * (if (isPlayerScreen) 1.5f else 0.4f)
                
                val speedBoost = smoothEnergy * 1.5f
                time += dt * (if (isPlayerScreen) 1.0f + speedBoost else 0.5f + speedBoost * 0.3f)
            }
            if (!isPlayerScreen) kotlinx.coroutines.delay(24L)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (effect != null) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val dom = paletteColors.dominant ?: Color.DarkGray
                val vib = paletteColors.vibrant ?: dom
                val mut = paletteColors.muted ?: dom

                val uniformsBuffer = java.nio.ByteBuffer.allocate(4 * 16).apply {
                    order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    putFloat(size.width)
                    putFloat(size.height)
                    putFloat(time)
                    putFloat(dynamicEnergy)
                    putFloat(dom.red); putFloat(dom.green); putFloat(dom.blue); putFloat(dom.alpha)
                    putFloat(vib.red); putFloat(vib.green); putFloat(vib.blue); putFloat(vib.alpha)
                    putFloat(mut.red); putFloat(mut.green); putFloat(mut.blue); putFloat(mut.alpha)
                }
                
                val currentShader = effect.makeShader(
                    uniforms = org.jetbrains.skia.Data.makeFromBytes(uniformsBuffer.array()),
                    children = null,
                    localMatrix = null
                )
                
                drawRect(brush = androidx.compose.ui.graphics.ShaderBrush(currentShader), size = size)
            }
        }
        content()
    }
}
