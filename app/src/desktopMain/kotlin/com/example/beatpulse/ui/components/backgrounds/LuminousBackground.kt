package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ShaderBrush


import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.*


import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.Data
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager
import com.example.beatpulse.ui.LocalCoverOffset

private const val LUMINOUS_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iEnergy;
    uniform half4 colorVibrant;
    uniform half4 colorLightVibrant;
    uniform float iOffsetX;
    uniform float iOffsetY;
    
    // Hash function for random values
    float hash12(float2 p) {
        float3 p3  = fract(float3(p.xyx) * .1031);
        p3 += dot(p3, p3.yzx + 33.33);
        return fract((p3.x + p3.y) * p3.z);
    }
    
    half4 main(float2 fragCoord) {
                float2 uv = (fragCoord.xy + float2(iOffsetX * iResolution.y, iOffsetY * iResolution.y)) / iResolution.xy;
        float2 uv_screen = (fragCoord.xy - 0.5 * iResolution.xy) / iResolution.y;
        float r_screen = length(uv_screen);
        float2 p = uv * 2.0 - 1.0;
        p.x *= iResolution.x / iResolution.y;
        
        float2 p_screen = uv_screen * 2.0 - 1.0;
        p_screen.x *= iResolution.x / iResolution.y;
        
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
        col += mix(half3(0.05, 0.05, 0.05), colorVibrant.rgb * 0.3, 1.0 - min(1.0, length(p_screen * 0.6))) * (1.0 + iEnergy * 0.5);
        
        return half4(col, 1.0);
    }
"""

actual @Composable
fun LuminousBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val coverOffset = LocalCoverOffset.current
    val lightVibrantState = animateColorAsState(paletteColors.lightVibrant, tween(1500), label = "lum_lv")
    val vibrantState = animateColorAsState(paletteColors.vibrant, tween(1500), label = "lum_v")
    val darkMutedState = animateColorAsState(paletteColors.darkMuted, tween(1500), label = "lum_dm")
    
    val amplitudesState = visualizerManager.combinedAmplitudes.collectAsState()

    val currentIsPlayerScreen by rememberUpdatedState(isPlayerScreen)
    var dynamicEnergy by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        var smoothEnergy = 0f
        var lastTime = 0L
        while (true) {
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
                val reactFactor = if (currentIsPlayerScreen) 0.8f else 0.2f
                dynamicEnergy = smoothEnergy * reactFactor
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bokeh_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100000f,
        animationSpec = infiniteRepeatable(tween(10000000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

    val runtimeShader = remember {
        try { RuntimeEffect.makeForShader(LUMINOUS_SHADER_SRC) } catch (e: Exception) { null }
    }


    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        
        val currentAlbumArtCenterY = com.example.beatpulse.ui.LocalAlbumArtCenterY.current
        val currentCoverOffset = com.example.beatpulse.ui.LocalCoverOffset.current
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (runtimeShader != null) {

                val vib = vibrantState.value
                val lVib = lightVibrantState.value
                
                val finalSpeed = if (isPlayerScreen) 1.0f else 0.2f
                val dynamicOffsetX = if (isPlayerScreen) -(currentCoverOffset.x / size.height) else 0f
                val dynamicOffsetY = if (isPlayerScreen) {
                    val base = if (currentAlbumArtCenterY != null) ((size.height / 2f) - currentAlbumArtCenterY) / size.height else 0f
                    base - (currentCoverOffset.y / size.height)
                } else 0f
                
                val buffer = ByteBuffer.allocate(56).order(ByteOrder.LITTLE_ENDIAN)
                buffer.putFloat(size.width)
                buffer.putFloat(size.height)
                buffer.putFloat(time * 0.5f * finalSpeed)
                buffer.putFloat(dynamicEnergy)
                buffer.putFloat(vib.red)
                buffer.putFloat(vib.green)
                buffer.putFloat(vib.blue)
                buffer.putFloat(vib.alpha)
                buffer.putFloat(lVib.red)
                buffer.putFloat(lVib.green)
                buffer.putFloat(lVib.blue)
                buffer.putFloat(lVib.alpha)
                buffer.putFloat(dynamicOffsetX)
                buffer.putFloat(dynamicOffsetY)

                val shader = runtimeShader.makeShader(
                    uniforms = Data.makeFromBytes(buffer.array()),
                    children = null,
                    localMatrix = null
                )
                drawRect(brush = ShaderBrush(shader), size = size)
            } else {
                val width = size.width
                val currentAlbumArtCenterY = if (isPlayerScreen && currentAlbumArtCenterY != null) currentAlbumArtCenterY else size.height / 2f
                val center = androidx.compose.ui.geometry.Offset(width / 2f, currentAlbumArtCenterY)
                drawCircle(color = vibrantState.value.copy(alpha = 0.5f), radius = 200f + dynamicEnergy * 100f, center = center)
            }
        }
        

    Box(modifier = modifier.fillMaxSize()) {
            content()
        }
    }
}
