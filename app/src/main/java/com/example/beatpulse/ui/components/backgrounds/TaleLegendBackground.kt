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

private const val TALE_LEGEND_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iEnergy;
    uniform float iSpeed;
    uniform float isPlayer;
    uniform half4 colorDominant;
    uniform half4 colorVibrant;
    
    // Retro resolution
    float pixelate = 110.0;
    
    // Exact mathematical Heart SDF
    float sdHeart(float2 p) {
        p.y = -p.y; // Flip to match Android/Compose Y-axis direction
        p.x = abs(p.x);
        if( p.y + p.x > 1.0 )
            return sqrt(dot(p-float2(0.25,0.75), p-float2(0.25,0.75))) - sqrt(2.0)/4.0;
        return sqrt(min(dot(p-float2(0.00,1.00), p-float2(0.00,1.00)), 
                        dot(p-0.5*max(p.x+p.y,0.0), p-0.5*max(p.x+p.y,0.0)))) * sign(p.x-p.y);
    }
    
    half4 main(in float2 fragCoord) {
        float2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;
        
        // Quantize UV to create automatic pixel art
        float2 puv = floor(uv * pixelate) / pixelate;
        
        float time = iTime * 0.4 * iSpeed;
        
        // 1. The Battle Box
        float boxSize = 0.4 + iEnergy * 0.02;
        float dBox = max(abs(puv.x), abs(puv.y)) - boxSize;
        float boxLine = step(abs(abs(dBox) - 0.01), 0.005);
        
        // 2. The Bullet Hell Rings (Omega Flowey / Sans style)
        float dBullets = 1.0;
        float a = atan(puv.y, puv.x);
        
        // ULTRA SOFT Cooldown stutter effect (gentle drift backward and recover)
        // Occurs once every 2.0 seconds, lasts for 0.8 seconds (very slow and smooth).
        float cycle = fract(iTime / 2.0); 
        float stutterTrigger = 0.0;
        if (cycle < 0.4) { 
            // Sine wave squared for ultra smooth ease-in and ease-out
            float s = sin((cycle / 0.4) * 3.14159);
            stutterTrigger = s * s;
        }
        // Very small multiplier to prevent dizziness (REDUCED TO 0.08 FOR MAXIMUM SOFTNESS)
        float stutter = -stutterTrigger * iEnergy * 0.08;
        
        for(float i = 1.0; i <= 3.0; i++) {
            // Softer radius expansion on peaks (REDUCED TO 0.05)
            float ringRadius = 0.12 + (i * 0.1) + (iEnergy * 0.05); 
            
            // Apply stutter to speed
            float ringSpeed = (time * 1.5 + stutter) * (mod(i, 2.0) == 0.0 ? 1.0 : -1.0);
            
            float la = a + ringSpeed;
            float bullets = 6.0 + i * 2.0;
            float segment = 6.28318 / bullets;
            
            float nearestA = floor(la / segment) * segment + segment/2.0;
            float2 bCenter = float2(cos(nearestA - ringSpeed), sin(nearestA - ringSpeed)) * ringRadius;
            
            // Draw bullets as tiny white pixel hearts!
            float2 bulletUv = (puv - bCenter) * 20.0; 
            bulletUv.y -= 0.4; // center the tip
            
            float dBul = sdHeart(bulletUv);
            dBullets = min(dBullets, dBul);
        }
        
        float bulletMask = step(dBullets, 0.0);
        
        // 3. The SOUL (Player Heart) dodging
        float soulMask = 0.0;
        if (isPlayer < 0.5) {
            // Hide on player screen as requested. Show only in lists/folders.
            float2 soulPos = float2(sin(time * 3.0)*0.1, cos(time * 4.3)*0.1);
            float2 soulUv = puv - soulPos;
            soulUv *= 15.0; // scale heart
            soulUv.y -= 0.5; // center heart tip
            float dSoul = sdHeart(soulUv);
            soulMask = step(dSoul, 0.0);
        }
        
        // 4. Compositing and Colors
        half3 finalColor = half3(0.0, 0.0, 0.0);
        
        // Brighter dominant for the box
        half3 bColor = colorDominant.rgb * 1.5;
        finalColor = mix(finalColor, bColor, boxLine);
        
        // Bullets are white hearts
        finalColor = mix(finalColor, half3(1.0, 1.0, 1.0), bulletMask);
        
        // SOUL is vibrant red/color
        finalColor = mix(finalColor, colorVibrant.rgb * (1.0 + iEnergy), soulMask);
        
        // Very subtle scanline retro effect
        float scanline = sin(fragCoord.y * 0.5) * 0.05;
        finalColor += scanline;
        
        return half4(finalColor, 1.0);
    }
"""

@SuppressLint("NewApi")
@Composable
fun TaleLegendBackground(
    paletteColors: PaletteColors,
    visualizerManager: AudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    val dominantColorState = animateColorAsState(targetValue = paletteColors.dominant, tween(1500), label = "tl_dom")
    val vibrantColorState = animateColorAsState(targetValue = paletteColors.vibrant, tween(1500), label = "tl_vib")

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

    val infiniteTransition = rememberInfiniteTransition(label = "tl_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(TALE_LEGEND_SHADER_SRC)
        } else null
    }
    
    val shaderBrush = remember(runtimeShader) {
        runtimeShader?.let { ShaderBrush(it) }
    }

    val finalSpeed = if (isPlayerScreen) 0.25f else 0.05f
    val baseBlack = Color(0xFF000000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderBrush != null && runtimeShader != null) {
                val dom = dominantColorState.value
                val vib = vibrantColorState.value
                
                runtimeShader.setFloatUniform("iResolution", size.width, size.height)
                runtimeShader.setFloatUniform("iTime", time * 0.5f)
                runtimeShader.setFloatUniform("iEnergy", dynamicEnergy)
                runtimeShader.setFloatUniform("iSpeed", finalSpeed)
                runtimeShader.setFloatUniform("isPlayer", if (isPlayerScreen) 1f else 0f)
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
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}
