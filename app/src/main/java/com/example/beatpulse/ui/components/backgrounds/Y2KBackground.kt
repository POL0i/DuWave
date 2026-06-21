package com.example.beatpulse.ui.components.backgrounds

import android.graphics.RuntimeShader
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.visualizer.AudioVisualizerManager

private const val Y2K_KAWAII_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iEnergy;
    uniform float iSpeed;
    uniform half4 colorDominant;
    uniform half4 colorVibrant;
    
    float hash12(float2 p) {
        float3 p3  = fract(float3(p.xyx) * .1031);
        p3 += dot(p3, p3.yzx + 33.33);
        return fract((p3.x + p3.y) * p3.z);
    }
    
    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;
        float time = iTime * 0.5 * iSpeed;
        
        // Very soft background
        half3 bg = mix(colorDominant.rgb, half3(1.0, 1.0, 1.0), 0.9); 
        
        float2 gridUv = uv * 3.0;
        gridUv.y += time * 0.5; // Scroll up
        
        float2 id = floor(gridUv);
        float2 f = fract(gridUv) - 0.5;
        
        if (mod(id.y, 2.0) > 0.5) {
            gridUv.x += 0.5;
            id = floor(gridUv);
            f = fract(gridUv) - 0.5;
        }
        
        float r = hash12(id); // random per rose
        float2 p = f;
        
        // Bounce
        float bounce = sin(time * 5.0 + r * 6.28) * 0.05 * iEnergy;
        p *= 1.0 - bounce;
        
        float a = atan(p.y, p.x);
        float dRad = length(p);
        
        float petals = 5.0 + floor(r * 1.99) * 2.0; // 5 or 7 petals
        
        // Add rotation
        float rotA = a + time * (r > 0.5 ? 0.8 : -0.8);
        
        // The rose envelope (Rhodonea curve)
        float warpedRad = dRad - 0.08 * abs(cos(petals * 0.5 * rotA));
        
        // The continuous spiral (Turtle Graphics Simulation)
        float spiralFreq = 12.0; 
        float spiralPhase = (a / 6.28318) + warpedRad * spiralFreq - time * (0.5 + iEnergy*0.2);
        
        // Distance to the nearest spiral line
        float spiralDist = abs(fract(spiralPhase) - 0.5);
        
        // Adjust line thickness
        float thickness = 0.06;
        float line = smoothstep(thickness, thickness - 0.015, spiralDist);
        
        // Fade out at edges to isolate the flower
        line *= smoothstep(0.4, 0.35, dRad);
        
        // Colors
        half3 lineColor = mix(colorVibrant.rgb, half3(1.0, 0.08, 0.58), 0.6); // DeepPink hue
        
        half3 finalColor = bg;
        
        // Composite the vector line art
        finalColor = mix(finalColor, lineColor, line);
        
        // Center dot
        float centerMask = smoothstep(0.015, 0.005, dRad - 0.02);
        finalColor = mix(finalColor, colorVibrant.rgb, centerMask);
        
        // Y2K Grid Dots background overlay
        float dDot = length(fract(uv * 12.0) - 0.5) - 0.05;
        finalColor = mix(finalColor, half3(1.0, 1.0, 1.0), smoothstep(0.02, -0.02, dDot) * 0.4);
        
        return half4(finalColor, 1.0);
    }
"""

@SuppressLint("NewApi")
@Composable
fun Y2KBackground(
    paletteColors: PaletteColors,
    visualizerManager: AudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    val accentColorState = animateColorAsState(targetValue = paletteColors.lightVibrant, tween(1500), label = "y2k_acc")
    val dominantColorState = animateColorAsState(targetValue = paletteColors.dominant, tween(1500), label = "y2k_dom")
    val vibrantColorState = animateColorAsState(targetValue = paletteColors.vibrant, tween(1500), label = "y2k_vib")

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

    val infiniteTransition = rememberInfiniteTransition(label = "y2k_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(Y2K_KAWAII_SHADER_SRC)
        } else null
    }
    
    val shaderBrush = remember(runtimeShader) {
        runtimeShader?.let { ShaderBrush(it) }
    }

    val finalSpeed = if (isPlayerScreen) 1.0f else 0.3f
    val bg = Color(0xFFF7F4F9) // Lila/gris muy claro

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .drawBehind { drawRect(color = dominantColorState.value.copy(alpha = 0.1f)) }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 1.dp, color = Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.5f))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderBrush != null && runtimeShader != null) {
                    val dom = dominantColorState.value
                    val vib = vibrantColorState.value
                    
                    runtimeShader.setFloatUniform("iResolution", size.width, size.height)
                    runtimeShader.setFloatUniform("iTime", time * 0.5f)
                    runtimeShader.setFloatUniform("iEnergy", dynamicEnergy)
                    runtimeShader.setFloatUniform("iSpeed", finalSpeed)
                    runtimeShader.setFloatUniform("colorDominant", dom.red, dom.green, dom.blue, dom.alpha)
                    runtimeShader.setFloatUniform("colorVibrant", vib.red, vib.green, vib.blue, vib.alpha)
                    
                    drawRect(brush = shaderBrush, size = size)
                } else {
                    val width = size.width
                    val height = size.height
                    val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
                    drawCircle(color = vibrantColorState.value.copy(alpha = 0.5f), radius = 200f + dynamicEnergy * 100f, center = center)
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .border(width = 1.dp, color = Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
                    .drawBehind { drawRect(color = accentColorState.value.copy(alpha = 0.15f)) }
            ) {
                content()
            }
        }
    }
}
