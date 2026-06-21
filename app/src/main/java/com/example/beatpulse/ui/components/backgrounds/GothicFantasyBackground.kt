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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.visualizer.AudioVisualizerManager

private const val DARK_FANTASY_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iEnergy;
    uniform float iSpeed;
    uniform float iIntensity;
    uniform half4 colorDominant;
    uniform half4 colorVibrant;
    uniform half4 colorMuted;

    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord.xy - 0.5 * iResolution.xy) / iResolution.y;
        float d = length(uv);
        
        float sunRadius = 0.15 + iEnergy * 0.1;
        float sunCore = smoothstep(sunRadius + 0.05, sunRadius - 0.05, d);
        float sunGlow = exp(-d * (5.0 - iEnergy * 2.0));
        
        // Slower base speed (1.5 instead of 3.0), multiplied by dynamic speed parameter
        float wavePhase = d * 15.0 - (iTime * 1.5 * iSpeed);
        float waveDist = abs(sin(wavePhase));
        float wavePulse = 1.0 - smoothstep(0.0, 0.3, waveDist);
        wavePulse *= exp(-d * 2.0);
        
        float waveIndex = floor(wavePhase / 3.1415926);
        float colorIndex = mod(abs(waveIndex), 3.0);
        
        half3 col0 = colorDominant.rgb;
        half3 col1 = colorVibrant.rgb;
        half3 col2 = colorMuted.rgb;
        
        float t1 = step(1.0, colorIndex);
        float t2 = step(2.0, colorIndex);
        
        half3 waveColor = mix(col0, col1, t1);
        waveColor = mix(waveColor, col2, t2);
        
        half3 finalColor = half3(0.0, 0.0, 0.0);
        finalColor += waveColor * wavePulse * (0.5 + iEnergy);
        finalColor += colorVibrant.rgb * sunGlow * (0.5 + iEnergy);
        finalColor = mix(finalColor, half3(1.0, 1.0, 1.0), sunCore);
        
        return half4(finalColor * iIntensity, 1.0);
    }
"""

@SuppressLint("NewApi")
@Composable
fun GothicFantasyBackground(
    paletteColors: PaletteColors,
    visualizerManager: AudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    val dominantColorState = animateColorAsState(targetValue = paletteColors.dominant, animationSpec = tween(1500), label = "dom")
    val vibrantColorState = animateColorAsState(targetValue = paletteColors.vibrant, animationSpec = tween(1500), label = "vib")
    val mutedColorState = animateColorAsState(targetValue = paletteColors.muted, animationSpec = tween(1500), label = "mut")

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

    val infiniteTransition = rememberInfiniteTransition(label = "sun_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(DARK_FANTASY_SHADER_SRC)
        } else null
    }
    
    val shaderBrush = remember(runtimeShader) {
        runtimeShader?.let { ShaderBrush(it) }
    }

    val finalSpeed = if (isPlayerScreen) 1.0f else 0.3f
    val finalIntensity = if (isPlayerScreen) 1.0f else 0.5f

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderBrush != null && runtimeShader != null) {
                val dom = dominantColorState.value
                val vib = vibrantColorState.value
                val mut = mutedColorState.value
                
                runtimeShader.setFloatUniform("iResolution", size.width, size.height)
                runtimeShader.setFloatUniform("iTime", time * 0.5f)
                runtimeShader.setFloatUniform("iEnergy", dynamicEnergy)
                runtimeShader.setFloatUniform("iSpeed", finalSpeed)
                runtimeShader.setFloatUniform("iIntensity", finalIntensity)
                runtimeShader.setFloatUniform("colorDominant", dom.red, dom.green, dom.blue, dom.alpha)
                runtimeShader.setFloatUniform("colorVibrant", vib.red, vib.green, vib.blue, vib.alpha)
                runtimeShader.setFloatUniform("colorMuted", mut.red, mut.green, mut.blue, mut.alpha)
                
                drawRect(brush = shaderBrush, size = size)
            } else {
                val width = size.width
                val height = size.height
                val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
                drawCircle(color = vibrantColorState.value.copy(alpha = 0.5f * finalIntensity), radius = 200f + dynamicEnergy * 100f, center = center)
                drawCircle(color = Color.White.copy(alpha = finalIntensity), radius = 50f + dynamicEnergy * 50f, center = center)
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
