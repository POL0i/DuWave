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
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager
import androidx.compose.runtime.collectAsState
import com.example.beatpulse.ui.LocalCoverOffset

// Radial tunnel shader — anonymous author (GLSL Sandbox)
private const val LUMINOUS_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iEnergy;
    uniform half4 colorVibrant;
    uniform half4 colorLightVibrant;
    uniform float iCenterX;
    uniform float iCenterY;
    
    half4 main(in float2 fragCoord) {
        float2 centerPx = float2(iCenterX, iCenterY);
        float2 uv = (fragCoord.xy - centerPx) / (0.5 * iResolution.y);
        
        // Gentle drift
        uv += float2(cos(iTime * 0.25), sin(iTime * 0.5)) * 0.4;
        
        float u = sqrt(dot(uv, uv));
        float v = atan(uv.y, uv.x);
        
        float t = iTime + 1.0 / u;
        
        float val_f = smoothstep(0.0, 1.0, sin(5.0 * (iTime + sin(11.0 * u * 3.7)) + 10.0 * v) + cos(t * 10.0));
        
        // Ensure baseline brightness for dark palettes
        half3 brightColor = max(colorVibrant.rgb, vec3(0.15));
        brightColor = brightColor + (1.0 - brightColor) * 0.15;
        
        half3 dimColor = max(colorLightVibrant.rgb, vec3(0.05)) * 0.15 + vec3(0.05);
        
        half3 colour = brightColor * val_f + (0.9 - val_f) * dimColor;
        colour *= clamp(u / 1.0, 0.0, 1.0);
        
        // Energy adds subtle brightness pulse
        colour *= 1.0 + iEnergy * 0.6;
        
        return half4(colour, 1.0);
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
    
    var time by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        var smoothEnergy = 0f
        var lastTime = 0L
        while (true) {
            withFrameMillis { frameTime ->
                if (lastTime == 0L) lastTime = frameTime
                val dt = ((frameTime - lastTime) / 1000f).coerceAtMost(0.1f)
                lastTime = frameTime
                
                val currentAmps = amplitudesState.value
                var sumAmps = 0f
                val limit = if (currentAmps.size < 24) currentAmps.size else 24
                for (i in 0 until limit) {
                    sumAmps += currentAmps[i]
                }
                val rawEnergy = if (limit > 0) sumAmps / limit else 0f
                
                smoothEnergy += (rawEnergy - smoothEnergy) * (1f - kotlin.math.exp(-5f * dt))
                val reactFactor = if (currentIsPlayerScreen) 1.0f else 0.2f
                dynamicEnergy = smoothEnergy * reactFactor
                
                time += dt * 10f
            }
            if (!currentIsPlayerScreen) kotlinx.coroutines.delay(24L)
        }
    }

    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try { RuntimeShader(LUMINOUS_SHADER_SRC) } catch (e: Exception) { null }
        } else null
    }
    
    val shaderBrush = remember(runtimeShader) {
        runtimeShader?.let { ShaderBrush(it) }
    }


    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        
        
        val currentAlbumArtCenterY = com.example.beatpulse.ui.LocalAlbumArtCenterY.current
        val currentCoverOffset = com.example.beatpulse.ui.LocalCoverOffset.current
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderBrush != null && runtimeShader != null) {
                val vib = vibrantState.value
                val lVib = lightVibrantState.value
                
                val finalSpeed = if (isPlayerScreen) 1.0f else 0.2f
                
                val centerX = size.width / 2f + currentCoverOffset.x
                val centerY = (currentAlbumArtCenterY ?: (size.height / 2f)) + currentCoverOffset.y
                
                runtimeShader.setFloatUniform("iResolution", size.width, size.height)
                runtimeShader.setFloatUniform("iTime", time * 0.05f * finalSpeed)
                runtimeShader.setFloatUniform("iEnergy", dynamicEnergy)
                runtimeShader.setFloatUniform("colorVibrant", vib.red, vib.green, vib.blue, vib.alpha)
                runtimeShader.setFloatUniform("colorLightVibrant", lVib.red, lVib.green, lVib.blue, lVib.alpha)
                runtimeShader.setFloatUniform("iCenterX", centerX)
                runtimeShader.setFloatUniform("iCenterY", centerY)
                
                drawRect(brush = shaderBrush, size = size)
            } else {
                val width = size.width
                val currentAlbumArtCenterY = if (isPlayerScreen && currentAlbumArtCenterY != null) currentAlbumArtCenterY else size.height / 2f
                val center = androidx.compose.ui.geometry.Offset(width / 2f, currentAlbumArtCenterY)
                drawCircle(color = vibrantState.value.copy(alpha = 0.5f), radius = 200f + dynamicEnergy * 100f, center = center)
            }
        }
        

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            content()
        }
    }
}
