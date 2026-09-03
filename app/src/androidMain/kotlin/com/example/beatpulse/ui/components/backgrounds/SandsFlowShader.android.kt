package com.example.beatpulse.ui.components.backgrounds

import android.os.Build
import android.graphics.RuntimeShader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush

@Composable
actual fun SandsFlowShader(
    modifier: Modifier,
    time: Float,
    bass: Float,
    treble: Float,
    dominantColor: Color,
    vibrantColor: Color,
    mutedColor: Color
) {
    if (Build.VERSION.SDK_INT >= 33) {
        val runtimeShader = remember {
            RuntimeShader(SANDS_FLOW_SKSL)
        }
        
        Canvas(modifier = modifier) {
            runtimeShader.setFloatUniform("u_resolution", size.width, size.height)
            runtimeShader.setFloatUniform("u_time", time)
            runtimeShader.setFloatUniform("u_bass", bass)
            runtimeShader.setFloatUniform("u_treble", treble)
            runtimeShader.setFloatUniform("u_dominant", dominantColor.red, dominantColor.green, dominantColor.blue, dominantColor.alpha)
            runtimeShader.setFloatUniform("u_vibrant", vibrantColor.red, vibrantColor.green, vibrantColor.blue, vibrantColor.alpha)
            runtimeShader.setFloatUniform("u_muted", mutedColor.red, mutedColor.green, mutedColor.blue, mutedColor.alpha)
            
            drawRect(brush = ShaderBrush(runtimeShader))
        }
    } else {
        // Fallback for older Android devices
        Canvas(modifier = modifier) {
            drawRect(color = dominantColor)
        }
    }
}
