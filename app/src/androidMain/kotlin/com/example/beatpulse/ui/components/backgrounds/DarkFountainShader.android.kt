package com.example.beatpulse.ui.components.backgrounds

import android.os.Build
import android.graphics.RuntimeShader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shader

@Composable
actual fun DarkFountainShader(
    modifier: Modifier,
    time: Float,
    bass: Float,
    treble: Float,
    dominantColor: Color,
    vibrantColor: Color
) {
    if (Build.VERSION.SDK_INT >= 33) {
        val runtimeShader = remember {
            RuntimeShader(DARK_FOUNTAIN_SKSL)
        }
        
        Canvas(modifier = modifier) {
            runtimeShader.setFloatUniform("u_resolution", size.width, size.height)
            runtimeShader.setFloatUniform("u_time", time)
            runtimeShader.setFloatUniform("u_bass", bass)
            runtimeShader.setFloatUniform("u_treble", treble)
            runtimeShader.setFloatUniform("u_dominant", dominantColor.red, dominantColor.green, dominantColor.blue, dominantColor.alpha)
            runtimeShader.setFloatUniform("u_vibrant", vibrantColor.red, vibrantColor.green, vibrantColor.blue, vibrantColor.alpha)
            
            drawRect(brush = ShaderBrush(runtimeShader))
        }
    } else {
        // Fallback for older Android devices (API < 33)
        // A simple glowing pillar
        Canvas(modifier = modifier) {
            val centerWidth = size.width * 0.2f + bass * size.width * 0.1f
            drawRect(
                color = dominantColor.copy(alpha = 0.7f),
                topLeft = androidx.compose.ui.geometry.Offset(size.width / 2 - centerWidth / 2, 0f),
                size = androidx.compose.ui.geometry.Size(centerWidth, size.height)
            )
        }
    }
}
