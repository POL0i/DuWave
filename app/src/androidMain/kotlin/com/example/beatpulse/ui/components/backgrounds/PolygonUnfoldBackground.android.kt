package com.example.beatpulse.ui.components.backgrounds

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager

private const val SHADER_SRC = """
    uniform float2 u_resolution;
    uniform float u_time;
    uniform float u_energy;
    uniform half4 u_dominant;
    uniform half4 u_vibrant;
    uniform half4 u_muted;

    const float N = 10.0;
    const float PI = 3.14159265;

    half3 palette(in float t) {
        half3 a = mix(u_dominant.rgb, u_muted.rgb, 0.5);
        half3 b = mix(u_vibrant.rgb, u_dominant.rgb, 0.5);
        half3 c = half3(1.0, 1.0, 1.0);
        half3 d = half3(0.0, 0.33, 0.67);
        return a + b * half3(cos(6.283185 * (c * t + d)));
    }

    float2 rot(float2 p, float t){
        return float2(p.x * cos(t) + p.y * sin(t), p.y * cos(t) - p.x * sin(t));
    }

    half4 main(float2 fragCoord) {
        float2 p = (fragCoord.xy - u_resolution.xy / 2.0) / u_resolution.y;
        
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
            float2 tp1 = rot(p, i / N * 2.0 * PI);
            float2 tp2 = rot(p, (i + 1.0) / N * 2.0 * PI);

            if(tp1.x > 0.1 * tx && tp2.x < 0.1 * tx){
                float2 off = rot(float2(1.0, 0.0), PI - (i + 0.5) / N * 2.0 * PI) * 0.1 / cos(PI / N);
                p += (tx - 1.0) * off;

                float r = min(abs(tp1.x - 0.1 * tx), abs(tp2.x - 0.1 * tx));
                highlight = (0.9 * mod(i, 2.0)) * pow((1.0 + th) * (1.0 - th), 0.5) * (1.0 - min(1.0, 5.0 * sqrt(r)));

                inside = true;
                break;
            }
        }
        
        if(!inside){
            return half4(u_dominant.rgb * 0.1, 1.0);
        }
        
        half3 col = half3(0.0);
        float steps = max(0.0, nsteps - it);
        for(float ms = 0.0; ms < 20.0; ms += 1.0){
            if (ms >= steps) break;
            for(float i = 0.0; i < 10.0; i += 1.0){
                float2 tp1 = rot(p, i / N * 2.0 * PI);
                float2 tp2 = rot(p, (i + 1.0) / N * 2.0 * PI);

                if(tp1.x > 0.1 && tp2.x < 0.1){
                    float2 off = rot(float2(1.0, 0.0), PI - (i + 0.5) / N * 2.0 * PI) * 0.1 / cos(PI / N);
                    p += 2.0 * off;
                    if(abs(ms - (steps - 1.0)) < 0.5) {
                        col += palette(i / N) * half(0.9 + tp1.x * 0.4 + u_energy * 0.8);
                    }
                    break;
                }
            }
        }
        
        half3 finalColor = mix(col, u_vibrant.rgb, half(highlight));
        return half4(clamp(finalColor, half3(0.0), half3(1.0)), 1.0);
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(SHADER_SRC) }
        var time by remember { mutableStateOf(0f) }
        var dynamicEnergy by remember { mutableStateOf(0f) }
        val brush = remember(shader) { ShaderBrush(shader) }
        
        val amplitudesState = visualizerManager.combinedAmplitudes.collectAsState()
        var smoothEnergy by remember { mutableStateOf(0f) }

        LaunchedEffect(isPlayerScreen) {
            var lastTime = withInfiniteAnimationFrameMillis { it }
            while (true) {
                val currentTime = withInfiniteAnimationFrameMillis { it }
                val frameTime = currentTime - lastTime
                lastTime = currentTime
                
                if (frameTime > 0) {
                    val dt = (frameTime / 1000f).coerceAtMost(0.1f)
                    
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
            modifier = modifier.drawBehind {
                shader.setFloatUniform("u_resolution", size.width, size.height)
                shader.setFloatUniform("u_time", time)
                shader.setFloatUniform("u_energy", dynamicEnergy)
                
                val dom = paletteColors.dominant ?: Color.DarkGray
                shader.setColorUniform("u_dominant", dom.value.toLong())

                val vib = paletteColors.vibrant ?: dom
                shader.setColorUniform("u_vibrant", vib.value.toLong())

                val mut = paletteColors.muted ?: dom
                shader.setColorUniform("u_muted", mut.value.toLong())

                drawRect(brush = brush, size = size)
            }
        ) {
            content()
        }
    } else {
        // Fallback for older Android versions
        val dom = paletteColors.dominant ?: Color.DarkGray
        Box(modifier = modifier.drawBehind { drawRect(dom) }) {
            content()
        }
    }
}
