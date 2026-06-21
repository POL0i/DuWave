package com.example.beatpulse.ui.components.backgrounds

import android.graphics.RuntimeShader
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.visualizer.AudioVisualizerManager

private const val LUMINOUS_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iEnergy;
    uniform half4 colorVibrant;
    uniform half4 colorLightVibrant;
    
    // Hash function for random values
    float hash12(float2 p) {
        float3 p3  = fract(float3(p.xyx) * .1031);
        p3 += dot(p3, p3.yzx + 33.33);
        return fract((p3.x + p3.y) * p3.z);
    }
    
    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord.xy / iResolution.xy;
        float2 p = uv * 2.0 - 1.0;
        p.x *= iResolution.x / iResolution.y;
        
        half3 col = half3(0.0, 0.0, 0.0);
        
        float time = iTime * 0.15 + iEnergy * 0.5;
        
        for (int i = 0; i < 4; i++) {
            float fi = float(i);
            float2 q = p * (1.5 - fi * 0.2); // Different scales for depth
            q.y -= time * (0.4 + fi * 0.15); // Move upwards
            q.x += sin(time * 0.2 + fi) * 0.2; // Gentle sway
            
            float2 id = floor(q);
            float2 f = fract(q) - 0.5;
            
            float r = hash12(id + fi * 10.0);
            float r2 = hash12(id + fi * 20.0);
            
            // Spawn orb if random > threshold
            if (r > 0.3) {
                float2 offset = float2(r - 0.5, r2 - 0.5) * 0.4;
                float d = length(f - offset);
                
                float radius = 0.05 + r * 0.1 + iEnergy * 0.05;
                
                // Soft glowing edges (Bokeh look)
                float circle = smoothstep(radius, radius * 0.4, d);
                float ring = smoothstep(radius, radius * 0.9, d) - smoothstep(radius * 0.9, radius * 0.4, d);
                
                float intensity = (0.2 + r * 0.8) * (1.0 + iEnergy * 2.0);
                
                half3 orbColor = mix(colorVibrant.rgb, colorLightVibrant.rgb, r2);
                
                col += orbColor * (circle * 0.3 + ring * 0.4) * intensity;
            }
        }
        
        // Background gradient based on vibrant color
        col += mix(half3(0.05, 0.05, 0.05), colorVibrant.rgb * 0.3, 1.0 - min(1.0, length(p * 0.6))) * (1.0 + iEnergy * 0.5);
        
        return half4(col, 1.0);
    }
"""

@SuppressLint("NewApi")
@Composable
fun LuminousBackground(
    paletteColors: PaletteColors,
    visualizerManager: AudioVisualizerManager,
    isPlayerScreen: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val lightVibrantState = animateColorAsState(paletteColors.lightVibrant, tween(1500), label = "lum_lv")
    val vibrantState = animateColorAsState(paletteColors.vibrant, tween(1500), label = "lum_v")
    val darkMutedState = animateColorAsState(paletteColors.darkMuted, tween(1500), label = "lum_dm")
    
    val amplitudesState = visualizerManager.amplitudes.collectAsState()

    val reactFactor = if (isPlayerScreen) 0.8f else 0.2f
    var dynamicEnergy by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(isPlayerScreen) {
        var smoothEnergy = 0f
        var lastTime = 0L
        while(true) {
            withFrameMillis { time ->
                if (lastTime == 0L) lastTime = time
                val dt = ((time - lastTime) / 1000f).coerceAtMost(0.1f)
                lastTime = time
                
                val currentAmps = amplitudesState.value
                var sumAmps = 0f
                val limit = if (currentAmps.size < 24) currentAmps.size else 24
                for (i in 0 until limit) {
                    sumAmps += currentAmps[i]
                }
                val rawEnergy = if (limit > 0) sumAmps / limit else 0f
                
                smoothEnergy += (rawEnergy - smoothEnergy) * (1f - kotlin.math.exp(-15f * dt))
                dynamicEnergy = smoothEnergy * reactFactor
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bokeh_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(LUMINOUS_SHADER_SRC)
        } else null
    }
    
    val shaderBrush = remember(runtimeShader) {
        runtimeShader?.let { ShaderBrush(it) }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderBrush != null && runtimeShader != null) {
                val vib = vibrantState.value
                val lVib = lightVibrantState.value
                
                val finalSpeed = if (isPlayerScreen) 1.0f else 0.2f
                
                runtimeShader.setFloatUniform("iResolution", size.width, size.height)
                runtimeShader.setFloatUniform("iTime", time * 0.5f * finalSpeed)
                runtimeShader.setFloatUniform("iEnergy", dynamicEnergy)
                runtimeShader.setFloatUniform("colorVibrant", vib.red, vib.green, vib.blue, vib.alpha)
                runtimeShader.setFloatUniform("colorLightVibrant", lVib.red, lVib.green, lVib.blue, lVib.alpha)
                
                drawRect(brush = shaderBrush, size = size)
            } else {
                val width = size.width
                val height = size.height
                val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
                drawCircle(color = vibrantState.value.copy(alpha = 0.5f), radius = 200f + dynamicEnergy * 100f, center = center)
            }
        }
        
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            content()
        }
    }
}
