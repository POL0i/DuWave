package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

@Composable
actual fun DarkFountainShader(
    modifier: Modifier,
    time: Float,
    bass: Float,
    treble: Float,
    dominantColor: Color,
    vibrantColor: Color
) {
    val runtimeEffect = remember {
        RuntimeEffect.makeForShader(DARK_FOUNTAIN_SKSL)
    }
    
    val shaderBuilder = remember(runtimeEffect) {
        RuntimeShaderBuilder(runtimeEffect)
    }
    
    Canvas(modifier = modifier) {
        shaderBuilder.uniform("u_resolution", size.width, size.height)
        shaderBuilder.uniform("u_time", time)
        shaderBuilder.uniform("u_bass", bass)
        shaderBuilder.uniform("u_treble", treble)
        shaderBuilder.uniform("u_dominant", dominantColor.red, dominantColor.green, dominantColor.blue, dominantColor.alpha)
        shaderBuilder.uniform("u_vibrant", vibrantColor.red, vibrantColor.green, vibrantColor.blue, vibrantColor.alpha)
        
        val shader = shaderBuilder.makeShader()
        drawRect(brush = ShaderBrush(shader))
    }
}
