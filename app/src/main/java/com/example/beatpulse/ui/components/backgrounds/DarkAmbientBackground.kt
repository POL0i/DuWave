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
import kotlin.random.Random

private const val BLACK_METAL_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iEnergy;
    uniform half4 colorDominant;
    uniform half4 colorVibrant;
    
    float noise(float2 p) {
        return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    }
    
    float smoothNoise(float2 p) {
        float2 i = floor(p); float2 f = fract(p);
        f = f*f*(3.0-2.0*f);
        float a = noise(i); float b = noise(i + float2(1.0, 0.0));
        float c = noise(i + float2(0.0, 1.0)); float d = noise(i + float2(1.0, 1.0));
        return mix(mix(a,b,f.x), mix(c,d,f.x), f.y);
    }
    
    float fbm(float2 p) {
        float v = 0.0, f = 1.0, a = 0.5;
        for(int i=0; i<4; i++) {
            v += a * smoothNoise(p * f);
            f *= 2.0; a *= 0.5;
        }
        return v;
    }
    
    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord.xy - 0.5 * iResolution.xy) / iResolution.y;
        
        // --- VEINS BACKGROUND ---
        float2 vUv = uv * 3.0 + float2(0.0, -iTime * 0.2);
        float n = fbm(vUv + fbm(vUv * 2.0 + iTime*0.1));
        float veins = smoothstep(0.05 + iEnergy*0.1, 0.0, abs(n - 0.5));
        
        half3 veinColor = mix(colorDominant.rgb, colorVibrant.rgb, iEnergy * 0.5);
        half3 bgCol = mix(half3(0.0, 0.0, 0.0), veinColor * 0.8, veins * (0.5 + iEnergy));
        
        float r = length(uv);
        bgCol *= 1.0 - smoothstep(0.5, 0.9, r);
        
        return half4(bgCol, 1.0);
    }
"""

@SuppressLint("NewApi")
@Composable
fun DarkAmbientBackground(
    paletteColors: PaletteColors,
    visualizerManager: AudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    val dominantColorState = animateColorAsState(targetValue = paletteColors.dominant, animationSpec = tween(1500), label = "dominant_color")
    val vibrantColorState = animateColorAsState(targetValue = paletteColors.vibrant, animationSpec = tween(1500), label = "vibrant_color")

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

    val infiniteTransition = rememberInfiniteTransition(label = "skull_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(BLACK_METAL_SHADER_SRC)
        } else null
    }
    
    val shaderBrush = remember(runtimeShader) {
        runtimeShader?.let { ShaderBrush(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderBrush != null && runtimeShader != null) {
                val dom = dominantColorState.value
                val vib = vibrantColorState.value
                
                runtimeShader.setFloatUniform("iResolution", size.width, size.height)
                runtimeShader.setFloatUniform("iTime", time * 0.5f)
                runtimeShader.setFloatUniform("iEnergy", dynamicEnergy)
                runtimeShader.setFloatUniform("colorDominant", dom.red, dom.green, dom.blue, dom.alpha)
                runtimeShader.setFloatUniform("colorVibrant", vib.red, vib.green, vib.blue, vib.alpha)
                
                drawRect(brush = shaderBrush, size = size)
            } else {
                // FALLBACK for older Androids
                val width = size.width
                val height = size.height
                val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
                drawCircle(color = vibrantColorState.value, radius = 100f + dynamicEnergy * 100f, center = center)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            content()
        }
    }
}
