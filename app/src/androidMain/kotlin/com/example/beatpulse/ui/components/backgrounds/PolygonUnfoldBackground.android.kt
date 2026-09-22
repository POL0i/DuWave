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
import androidx.compose.ui.graphics.toArgb
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager

    const val POLYGON_UNFOLD_AGSL = """
    uniform float2 u_resolution;
    uniform float u_time;
    uniform float u_energy;
    uniform half4 u_dominant;
    uniform half4 u_vibrant;
    uniform half4 u_muted;

    const float N = 10.0;
    const float PI = 3.14159265;


    float2 rot(float2 p, float t){
        return float2(p.x * cos(t) + p.y * sin(t), p.y * cos(t) - p.x * sin(t));
    }

    half4 main(float2 fragCoord) {
        float2 center = u_resolution.x < u_resolution.y 
                        ? float2(u_resolution.x * 0.5, u_resolution.y * 0.45) 
                        : float2(u_resolution.x * 0.25, u_resolution.y * 0.5);
        float2 p = (fragCoord.xy - center) / min(u_resolution.x, u_resolution.y);
        
        float nsteps = MAX_STEPS;
        float baseTime = mod(u_time * 0.8, nsteps * 2.0);
        float animTime = baseTime;
        if (animTime > nsteps) {
            animTime = nsteps - (animTime - nsteps);
        }
        float time = max(nsteps - 0.01 - animTime, 0.0);

        float it = floor(time);
        time = mod(time, 1.0);

        float tx = cos(clamp(1.5 * time, 0.0, 1.0) * PI);
        float th = cos(time * PI);

        bool inside = false;
        float highlight = 0.0;

        float angle = 2.0 * PI / N;
        float ca = cos(angle);
        float sa = sin(angle);
        
        float2 tp1_1 = p;
        for(float i = 0.0; i < 10.0; i += 1.0){ // N is 10
            float2 tp2 = float2(tp1_1.x * ca + tp1_1.y * sa, tp1_1.y * ca - tp1_1.x * sa);

            if(tp1_1.x > 0.1 * tx && tp2.x < 0.1 * tx){
                float2 off = rot(float2(1.0, 0.0), PI - (i + 0.5) / N * 2.0 * PI) * 0.1 / cos(PI / N);
                p += (tx - 1.0) * off;

                float r = min(abs(tp1_1.x - 0.1 * tx), abs(tp2.x - 0.1 * tx));
                highlight = (0.9 * mod(i, 2.0)) * pow((1.0 + th) * (1.0 - th), 0.5) * (1.0 - min(1.0, 5.0 * sqrt(r)));

                inside = true;
                break;
            }
            tp1_1 = tp2;
        }
        
        if(!inside){
            return half4(u_dominant.rgb * 0.15, 1.0);
        }
        
        half3 col = half3(0.0);
        float steps = max(0.0, nsteps - it);
        
        for(float ms = 0.0; ms < MAX_STEPS; ms += 1.0){
            if (ms >= steps) break;
            float2 tp1_2 = p;
            for(float i = 0.0; i < 10.0; i += 1.0){
                float2 tp2 = float2(tp1_2.x * ca + tp1_2.y * sa, tp1_2.y * ca - tp1_2.x * sa);

                if(tp1_2.x > 0.1 && tp2.x < 0.1){
                    float2 off = rot(float2(1.0, 0.0), PI - (i + 0.5) / N * 2.0 * PI) * 0.1 / cos(PI / N);
                    p += 2.0 * off;
                    if(abs(ms - (steps - 1.0)) < 0.5) {
                        half3 stepCol = mod(i, 3.0) == 0.0 ? u_dominant.rgb : (mod(i, 3.0) == 1.0 ? u_vibrant.rgb : u_muted.rgb);
                        col += stepCol * half(0.9 + tp1_2.x * 0.4 + u_energy * 0.8);
                    }
                    break;
                }
                tp1_2 = tp2;
            }
        }
        
        half3 finalColor = col + half(highlight) * u_vibrant.rgb * 0.4;
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
        var runtimeShader by remember { mutableStateOf<RuntimeShader?>(null) }
        LaunchedEffect(isPlayerScreen) {
            if (!isPlayerScreen) kotlinx.coroutines.delay(100L)
            val shader = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    val shaderCode = POLYGON_UNFOLD_AGSL
                        .replace("MAX_STEPS", if (isPlayerScreen) "4.0" else "1.0")
                    RuntimeShader(shaderCode)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            runtimeShader = shader
        }
        var time by remember { mutableStateOf(0f) }
        var dynamicEnergy by remember { mutableStateOf(0f) }
        val brush = remember(runtimeShader) { runtimeShader?.let { ShaderBrush(it) } }
        
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
                    time += dt * (if (isPlayerScreen) 1.0f + speedBoost else 1.0f + speedBoost * 0.5f)
                }
                if (!isPlayerScreen) kotlinx.coroutines.delay(48L) else kotlinx.coroutines.delay(24L)
            }
        }

        Box(
            modifier = modifier.drawBehind {
                runtimeShader?.setFloatUniform("u_resolution", size.width, size.height)
                runtimeShader?.setFloatUniform("u_time", time)
                runtimeShader?.setFloatUniform("u_energy", dynamicEnergy)
                
                val dom = paletteColors.dominant ?: Color.DarkGray
                runtimeShader?.setFloatUniform("u_dominant", dom.red, dom.green, dom.blue, dom.alpha)

                val vib = paletteColors.vibrant ?: dom
                runtimeShader?.setFloatUniform("u_vibrant", vib.red, vib.green, vib.blue, vib.alpha)

                val mut = paletteColors.muted ?: dom
                runtimeShader?.setFloatUniform("u_muted", mut.red, mut.green, mut.blue, mut.alpha)

                brush?.let { drawRect(brush = it, size = size) }
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
