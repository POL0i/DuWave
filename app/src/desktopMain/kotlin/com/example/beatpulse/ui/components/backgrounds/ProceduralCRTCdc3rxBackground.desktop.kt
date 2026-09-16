package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

private const val PROCEDURAL_CRT_SHADER_SKIA = """
uniform float2 u_resolution;
uniform float u_time;
uniform float u_energy;
uniform half4 u_dominant;
uniform half4 u_vibrant;

mat2 rot(float th) {
    return mat2(cos(th), sin(th), -sin(th), cos(th));
}

half4 main(float2 fragCoord) {
    const int n_iter = 8;
    float s = 0.0;
    
    for (int k = 0; k < n_iter; k++) {
        // Tiempo intacto igual que el original de Shadertoy para el "movimiento característico"
        float t = u_time + 0.0001 * float(k); 
        
        // Empuje de velocidad (rotación) suavizado
        float th = 0.1 * t + (u_energy * 0.005);
        
        float a = 0.4 + 0.3 * mod(floor(t * 0.1 / 6.2832), 3.0);    
        
        vec2 p = (2.0 * fragCoord - u_resolution.xy) / min(u_resolution.x, u_resolution.y);
        
        // Zoom in when energy is high
        p *= (1.0 - u_energy * 0.15); 

        for (int i = 0; i < 50; ++i) {
            p.x += a * abs(p.y) - 0.3;
            p *= rot(th);
        }
        s += step(p.y, 0.0);
    }
    
    s /= float(n_iter);
    
    // Dos colores del sistema
    vec3 color = mix(u_dominant.rgb * 0.2, u_vibrant.rgb, s);

    return half4(half3(color), 1.0);
}
"""

@Composable
actual fun ProceduralCRTCdc3rxBackground(
    dominantColor: Color,
    vibrantColor: Color,
    mutedColor: Color,
    dynamicEnergy: Float,
    dynamicOffsetY: Float,
    dynamicOffsetX: Float,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    var time by remember { mutableStateOf(0f) }
    
    LaunchedEffect(isPlayerScreen) {
        var lastFrameTime = -1L
        while (true) {
            withInfiniteAnimationFrameMillis { frameTime ->
                if (lastFrameTime == -1L) lastFrameTime = frameTime
                val delta = (frameTime - lastFrameTime) / 1000f
                lastFrameTime = frameTime
                val speed = if (isPlayerScreen) 1.0f else 0.3f
                time += delta * speed
            }
        }
    }

    val runtimeEffect = remember {
        try {
            RuntimeEffect.makeForShader(PROCEDURAL_CRT_SHADER_SKIA)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val shaderBrush = remember(runtimeEffect, time, dominantColor, vibrantColor, dynamicEnergy) {
        runtimeEffect?.let { effect ->
            object : androidx.compose.ui.graphics.ShaderBrush() {
                override fun createShader(size: androidx.compose.ui.geometry.Size): androidx.compose.ui.graphics.Shader {
                    val builder = org.jetbrains.skia.RuntimeShaderBuilder(effect)
                    builder.uniform("u_time", time)
                    builder.uniform("u_resolution", size.width, size.height)
                    builder.uniform("u_energy", dynamicEnergy.coerceIn(0f, 1f))
                    
                    val targetDom = if (dominantColor == Color.Unspecified || dominantColor.alpha < 0.1f) Color(0xFF00FF00) else dominantColor
                    val targetVib = if (vibrantColor == Color.Unspecified || vibrantColor.alpha < 0.1f) Color(0xFFFFFFFF) else vibrantColor
                    
                    builder.uniform("u_dominant", targetDom.red, targetDom.green, targetDom.blue, targetDom.alpha)
                    builder.uniform("u_vibrant", targetVib.red, targetVib.green, targetVib.blue, targetVib.alpha)
                    
                    return builder.makeShader()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (shaderBrush != null) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(brush = shaderBrush)
            }
        } else {
            // Fallback just in case the shader crashes
            Box(modifier = Modifier.fillMaxSize().background(dominantColor.copy(alpha = 0.5f)))
        }
        
        content()
    }
}
