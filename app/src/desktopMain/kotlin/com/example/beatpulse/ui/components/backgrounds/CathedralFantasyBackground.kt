package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.translate
import com.example.beatpulse.ui.LocalCoverOffset


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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iEnergy;
    uniform half4 colorDominant;
    uniform half4 colorVibrant;
    uniform half4 colorMuted;
    uniform float iOffsetY;
    uniform float iOffsetX;

    mat2 rot(float a) {
        float s = sin(a), c = cos(a);
        return mat2(c, -s, s, c);
    }

    half4 main(float2 fragCoord) {
        float2 uv = (fragCoord.xy - 0.5 * iResolution.xy + float2(iOffsetX * iResolution.y, iOffsetY * iResolution.y)) / iResolution.y;
        float2 uv_screen = (fragCoord.xy - 0.5 * iResolution.xy) / iResolution.y;
        float r_screen = length(uv_screen);
        float r = length(uv);
        float a = atan(uv.y, uv.x);
        
        float segments = 12.0;
        a = mod(a, 6.2831853 / segments);
        a = abs(a - 3.1415926 / segments);
        
        float2 polarUv = r * float2(cos(a), sin(a));
        polarUv = rot(iTime * 0.1) * polarUv;
        
        float f1 = sin(polarUv.x * 20.0 + iTime * 0.5);
        float f2 = cos(polarUv.y * 20.0 - iTime * 0.3);
        float f3 = sin((polarUv.x + polarUv.y) * 10.0);
        
        float pattern = f1 * f2 * f3;
        
        float leadThickness = 0.05 - (iEnergy * 0.03);
        float lead = smoothstep(0.0, leadThickness, abs(pattern));
        
        half4 glassColor = mix(colorDominant, colorVibrant, smoothstep(-1.0, 1.0, f1 + f2));
        glassColor = mix(glassColor, colorMuted, smoothstep(0.0, 1.0, f3));
        
        float glow = exp(-r * (4.0 - iEnergy * 2.5)) * iEnergy;
        glassColor.rgb += glow * colorVibrant.rgb;
        
        float vignette = 1.0 - smoothstep(0.3, 1.2, r_screen);
        
        // Deep space background color instead of absolute black where there is no lead
        half4 finalColor = half4(glassColor.rgb * lead * vignette, 1.0);
        return finalColor;
    }
"""

actual @Composable
fun CathedralFantasyBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    modifier: Modifier,
    isLibraryScreen: Boolean,
    content: @Composable () -> Unit
) {
    val coverOffset = LocalCoverOffset.current
    val tintColorState = animateColorAsState(targetValue = paletteColors.dominant.copy(alpha = 0.5f), animationSpec = tween(2000), label = "cathedral_tint")
    val vibrantState = animateColorAsState(targetValue = paletteColors.vibrant.copy(alpha = 0.8f), animationSpec = tween(2000), label = "cathedral_vibrant")
    val lightVibrantState = animateColorAsState(targetValue = paletteColors.lightVibrant.copy(alpha = 0.8f), animationSpec = tween(2000), label = "cathedral_lv")
    val mutedState = animateColorAsState(targetValue = paletteColors.muted, animationSpec = tween(2000), label = "cathedral_m")

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
                dynamicEnergy = smoothEnergy * 0.8f // Keep it dynamic on both screens!
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ash_fall")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100000f,
        animationSpec = infiniteRepeatable(tween(10000000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

    val deepBlack = Color(0xFF030305)
    val darkBlueGrey = Color(0xFF10121A)

    val noisePoints = remember { List(200) { Offset(Random.nextFloat(), Random.nextFloat()) to Random.nextFloat() } }
    val archPath = remember { Path() }

    val runtimeShader = remember {
        try { RuntimeEffect.makeForShader(SHADER_SRC) } catch (e: Exception) { null }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo base si falla el shader o no es soportado
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(tintColorState.value.copy(alpha = 0.3f), deepBlack.copy(alpha = 0.8f), darkBlueGrey.copy(alpha = 0.9f), deepBlack)))
        )

        // Capa de dibujo de la catedral
        
        
        val currentAlbumArtCenterY = com.example.beatpulse.ui.LocalAlbumArtCenterY.current
        val currentCoverOffset = com.example.beatpulse.ui.LocalCoverOffset.current
        Canvas(modifier = Modifier.fillMaxSize()
            .graphicsLayer {
                if (!isPlayerScreen) {
                    scaleX = 1f + dynamicEnergy * 0.15f
                    scaleY = 1f + dynamicEnergy * 0.15f
                }
            }
        ) {
            if (runtimeShader != null && isPlayerScreen) {
                val dom = tintColorState.value
                val vib = vibrantState.value
                val mut = mutedState.value
                
                val dynamicOffsetY = if (isPlayerScreen) {
                    val baseOffsetY = if (currentAlbumArtCenterY != null) ((size.height / 2f) - currentAlbumArtCenterY) / size.height else 0f
                    baseOffsetY - (currentCoverOffset.y / size.height)
                } else 0f
                val dynamicOffsetX = if (isPlayerScreen) {
                    -(currentCoverOffset.x / size.height)
                } else 0f
                
                val buffer = ByteBuffer.allocate(72).order(ByteOrder.LITTLE_ENDIAN)
                buffer.putFloat(size.width)
                buffer.putFloat(size.height)
                buffer.putFloat(time * 0.5f)
                buffer.putFloat(dynamicEnergy)
                buffer.putFloat(dom.red)
                buffer.putFloat(dom.green)
                buffer.putFloat(dom.blue)
                buffer.putFloat(dom.alpha)
                buffer.putFloat(vib.red)
                buffer.putFloat(vib.green)
                buffer.putFloat(vib.blue)
                buffer.putFloat(vib.alpha)
                buffer.putFloat(mut.red)
                buffer.putFloat(mut.green)
                buffer.putFloat(mut.blue)
                buffer.putFloat(mut.alpha)
                buffer.putFloat(dynamicOffsetY)
                buffer.putFloat(dynamicOffsetX)
                
                val shader = runtimeShader.makeShader(
                    uniforms = Data.makeFromBytes(buffer.array()),
                    children = null,
                    localMatrix = null
                )
                drawRect(brush = ShaderBrush(shader), size = size)
            } else {
                // FALLBACK: Dibujo con canvas normal para Android viejos
                val vibrant = vibrantState.value
                val lightVibrant = lightVibrantState.value
                val width = size.width
                val height = size.height

                val strokeWidth = 2f + dynamicEnergy * 3f
                val architectColor = vibrant.copy(alpha = 0.4f + dynamicEnergy * 0.4f)
                val glowColor = lightVibrant.copy(alpha = 0.1f + dynamicEnergy * 0.2f)

                archPath.reset()
                val archHeight = height * 0.45f
                
                drawLine(architectColor, Offset(width * 0.15f, height), Offset(width * 0.15f, archHeight), strokeWidth)
                drawLine(architectColor, Offset(width * 0.2f, height), Offset(width * 0.2f, archHeight), strokeWidth)
                drawLine(architectColor, Offset(width * 0.85f, height), Offset(width * 0.85f, archHeight), strokeWidth)
                drawLine(architectColor, Offset(width * 0.8f, height), Offset(width * 0.8f, archHeight), strokeWidth)

                archPath.moveTo(width * 0.15f, archHeight)
                archPath.quadraticTo(width * 0.15f, height * 0.1f, width * 0.5f, height * 0.05f)
                archPath.quadraticTo(width * 0.85f, height * 0.1f, width * 0.85f, archHeight)
                drawPath(archPath, architectColor, style = Stroke(strokeWidth))
                drawPath(archPath, glowColor, style = Stroke(strokeWidth * 4)) 

                translate(left = if (isPlayerScreen) currentCoverOffset.x else 0f, top = if (isPlayerScreen) currentCoverOffset.y else 0f) {
                val currentAlbumArtCenterY = if (isPlayerScreen && currentAlbumArtCenterY != null) {
                    currentAlbumArtCenterY
                } else {
                    height * 0.25f
                }
                val roseCenter = Offset(width * 0.5f, currentAlbumArtCenterY)
                val roseOuterRadius = width * 0.25f
                val roseInnerRadius = width * 0.1f

                drawCircle(architectColor, roseOuterRadius, roseCenter, style = Stroke(strokeWidth))
                drawCircle(architectColor, roseInnerRadius, roseCenter, style = Stroke(strokeWidth))
                drawCircle(glowColor, roseOuterRadius, roseCenter, style = Stroke(strokeWidth * 3))

                val spokes = 12
                for (i in 0 until spokes) {
                    val angle = (i * 2 * PI / spokes).toFloat()
                    val start = Offset(roseCenter.x + cos(angle) * roseInnerRadius, roseCenter.y + sin(angle) * roseInnerRadius)
                    val end = Offset(roseCenter.x + cos(angle) * roseOuterRadius, roseCenter.y + sin(angle) * roseOuterRadius)
                    drawLine(architectColor, start, end, strokeWidth)
                }
                drawCircle(glowColor.copy(alpha = dynamicEnergy * 0.6f), roseOuterRadius, roseCenter)
                }
            }
        }

        // Capa de cenizas (por encima del shader para que se vean)
        if (isLibraryScreen) {
            
        
        val currentAlbumArtCenterY = com.example.beatpulse.ui.LocalAlbumArtCenterY.current
        val currentCoverOffset = com.example.beatpulse.ui.LocalCoverOffset.current
        Canvas(modifier = Modifier.fillMaxSize()
                .graphicsLayer {
                    scaleX = 1f + dynamicEnergy * 0.15f
                    scaleY = 1f + dynamicEnergy * 0.15f
                }
            ) {
                val lightVibrant = lightVibrantState.value
                val width = size.width
                val height = size.height
                val currentAmps = amplitudesState.value
                
                noisePoints.forEachIndexed { index, (normOffset, alpha) ->
                    val speed = 0.008f + (index % 10) * 0.005f 
                    val drift = sin(time * 0.05f + index) * 20f

                    val pseudoRandX = ((index * 13) % 100) / 100f
                    val audioShakeX = (pseudoRandX - 0.5f) * dynamicEnergy * 300f
                    val pseudoRandY = (((index + 1) * 17) % 100) / 100f
                    val audioShakeY = (pseudoRandY - 0.5f) * dynamicEnergy * 300f

                    val currentY = (normOffset.y * height + time * speed * height + audioShakeY) % height
                    val currentX = (normOffset.x * width + drift + audioShakeX + width) % width

                    val ampIndex = index % 120
                    val localAmp = if (currentAmps.size > ampIndex) currentAmps[ampIndex] else 0f
                    // Lower threshold and increase multiplier for much more visible pulsing
                    val flare = if (localAmp > 0.1f) (localAmp - 0.1f) * 40f else 0f

                    drawCircle(
                        color = lightVibrant.copy(alpha = (alpha * 0.6f + flare * 0.3f).coerceIn(0f, 1f)),
                        radius = (alpha * 1f) + (flare * 1.5f),
                        center = Offset(currentX, currentY)
                    )
                }
            }
        }

        // Ornamental dark glass panel (Para que el resto de la UI sea legible encima)
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp)
                .background(
                    Brush.linearGradient(colors = listOf(Color(0x1AFFFFFF), Color(0x05FFFFFF), Color.Transparent)),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(colors = listOf(vibrantState.value.copy(alpha = 0.3f), lightVibrantState.value.copy(alpha = 0.1f), deepBlack.copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            content()
        }
    }
}
