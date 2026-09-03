package com.example.beatpulse.ui.components.backgrounds

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.ui.graphics.lerp

private data class LullabyEye(
    val initialPos: Offset,
    val size: Float,
    val opacity: Float,
    val ellipseRadiusX: Float,
    val ellipseRadiusY: Float,
    val phaseOffset: Float,
    val speed: Float,
    val pupilSpeed: Float,
    val pupilPhaseOffset: Float,
    val colorType: Int, // 0, 1, 2 for the three palette colors
    val dirtAngles: List<Float>,
    val dirtDistances: List<Float>,
    val dirtSizes: List<Float>
)

@Composable
fun LullabyEyesBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    var time by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val startTime = withFrameNanos { it }
        while (true) {
            val frameTime = withFrameNanos { it }
            time = (frameTime - startTime) / 1_000_000_000f
        }
    }

    var screenWidth by remember { mutableStateOf(1000f) }
    var screenHeight by remember { mutableStateOf(1000f) }

    val eyes = remember(screenWidth, screenHeight) {
        if (screenWidth <= 0f || screenHeight <= 0f) return@remember emptyList<LullabyEye>()
        val rnd = Random(Random.nextInt()) // Aleatorio cada vez que se regenera (cambio de tamaño, etc.)
        
        // Create a mix of small background eyes and larger foreground eyes
        val generated = mutableListOf<LullabyEye>()
        
        val genDirt: (Float) -> Triple<List<Float>, List<Float>, List<Float>> = { size ->
            val count = if (size > 35f) rnd.nextInt(3, 8) else 0
            Triple(
                List(count) { rnd.nextFloat() * 2f * PI.toFloat() },
                List(count) { rnd.nextFloat() * 0.8f },
                List(count) { rnd.nextFloat() * 0.3f + 0.1f }
            )
        }
        
        // Small distant eyes
        for (i in 0..40) {
            val size = rnd.nextFloat() * 8f + 4f
            val dirt = genDirt(size)
            generated.add(
                LullabyEye(
                    initialPos = Offset(rnd.nextFloat() * screenWidth, rnd.nextFloat() * screenHeight),
                    size = size, // 4 to 12
                    opacity = rnd.nextFloat() * 0.4f + 0.2f, // 0.2 to 0.6
                    ellipseRadiusX = rnd.nextFloat() * 60f + 20f,
                    ellipseRadiusY = rnd.nextFloat() * 60f + 20f,
                    phaseOffset = rnd.nextFloat() * 2f * PI.toFloat(),
                    speed = rnd.nextFloat() * 0.8f + 0.2f,
                    pupilSpeed = rnd.nextFloat() * 2f + 1f,
                    pupilPhaseOffset = rnd.nextFloat() * 2f * PI.toFloat(),
                    colorType = rnd.nextInt(3),
                    dirtAngles = dirt.first,
                    dirtDistances = dirt.second,
                    dirtSizes = dirt.third
                )
            )
        }
        
        // Medium eyes
        for (i in 0..15) {
            val size = rnd.nextFloat() * 20f + 12f
            val dirt = genDirt(size)
            generated.add(
                LullabyEye(
                    initialPos = Offset(rnd.nextFloat() * screenWidth, rnd.nextFloat() * screenHeight),
                    size = size, // 12 to 32
                    opacity = rnd.nextFloat() * 0.5f + 0.4f, // 0.4 to 0.9
                    ellipseRadiusX = rnd.nextFloat() * 100f + 40f,
                    ellipseRadiusY = rnd.nextFloat() * 100f + 40f,
                    phaseOffset = rnd.nextFloat() * 2f * PI.toFloat(),
                    speed = rnd.nextFloat() * 0.6f + 0.4f,
                    pupilSpeed = rnd.nextFloat() * 2f + 1f,
                    pupilPhaseOffset = rnd.nextFloat() * 2f * PI.toFloat(),
                    colorType = rnd.nextInt(3),
                    dirtAngles = dirt.first,
                    dirtDistances = dirt.second,
                    dirtSizes = dirt.third
                )
            )
        }

        // Large foreground eyes
        for (i in 0..5) {
            val size = rnd.nextFloat() * 50f + 40f
            val dirt = genDirt(size)
            generated.add(
                LullabyEye(
                    initialPos = Offset(rnd.nextFloat() * screenWidth, rnd.nextFloat() * screenHeight),
                    size = size, // 40 to 90
                    opacity = rnd.nextFloat() * 0.3f + 0.7f, // 0.7 to 1.0
                    ellipseRadiusX = rnd.nextFloat() * 150f + 50f,
                    ellipseRadiusY = rnd.nextFloat() * 150f + 50f,
                    phaseOffset = rnd.nextFloat() * 2f * PI.toFloat(),
                    speed = rnd.nextFloat() * 0.4f + 0.2f,
                    pupilSpeed = rnd.nextFloat() * 1.5f + 0.5f,
                    pupilPhaseOffset = rnd.nextFloat() * 2f * PI.toFloat(),
                    colorType = rnd.nextInt(3),
                    dirtAngles = dirt.first,
                    dirtDistances = dirt.second,
                    dirtSizes = dirt.third
                )
            )
        }
        
        // Sort by size so smaller are drawn first (background) and large are foreground
        generated.sortedBy { it.size }
    }

    // Colors
    val bgColor = Color(0xFF050505) // Almost black
    val tintColor = paletteColors.dominant.copy(alpha = 0.2f)
    
    val primary = paletteColors.vibrant
    val secondary = paletteColors.muted
    val tertiary = paletteColors.dominant
    
    val colorOptions = listOf(primary, secondary, tertiary)

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (screenWidth != size.width || screenHeight != size.height) {
                screenWidth = size.width
                screenHeight = size.height
            }

            // Draw background
            drawRect(color = bgColor)
            drawRect(color = tintColor)

            // Draw eyes
            for (eye in eyes) {
                val t = time * eye.speed + eye.phaseOffset
                // Elliptical path
                val dx = cos(t) * eye.ellipseRadiusX
                val dy = sin(t) * eye.ellipseRadiusY
                
                // Bouncing effect or just elliptical
                val currentPos = eye.initialPos + Offset(dx, dy)

                // Select base color from palette
                val baseColor = colorOptions[eye.colorType % colorOptions.size]
                
                // Draw glow (reduced scaling and opacity)
                val glowRadius = eye.size + 60f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(baseColor.copy(alpha = eye.opacity * 0.15f), Color.Transparent),
                        center = currentPos,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = currentPos
                )
                
                // Draw eye body (sphere effect)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(baseColor, baseColor.copy(alpha = 0.7f)),
                        center = currentPos - Offset(eye.size * 0.2f, eye.size * 0.2f),
                        radius = eye.size
                    ),
                    radius = eye.size,
                    center = currentPos,
                    alpha = eye.opacity
                )

                // Dirt on the eye body for larger eyes (clipped to the eye body)
                clipPath(
                    androidx.compose.ui.graphics.Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(
                            currentPos.x - eye.size, 
                            currentPos.y - eye.size, 
                            currentPos.x + eye.size, 
                            currentPos.y + eye.size
                        ))
                    }
                ) {
                    for (j in eye.dirtAngles.indices) {
                        val angle = eye.dirtAngles[j]
                        val dist = (0.75f + eye.dirtDistances[j] * 0.25f) * eye.size
                        val dirtSize = eye.dirtSizes[j] * eye.size * 1.5f // Larger, softer
                        val dxD = cos(angle) * dist
                        val dyD = sin(angle) * dist
                        val dirtPos = currentPos + Offset(dxD, dyD)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.06f), Color.Transparent),
                                center = dirtPos,
                                radius = dirtSize
                            ),
                            radius = dirtSize,
                            center = dirtPos,
                            alpha = eye.opacity
                        )
                    }
                }

                // Sclera/Iris calculation (reduced movement)
                val scleraPt = time * eye.pupilSpeed + eye.pupilPhaseOffset
                val sdx = cos(scleraPt) * (eye.size * 0.18f)
                val sdy = sin(scleraPt) * (eye.size * 0.18f)
                val scleraPos = currentPos + Offset(sdx, sdy)

                val scleraRadius = eye.size * 0.4f
                
                // Draw white part (referred to as iris by the user)
                val scleraColor = lerp(Color.White, baseColor, 0.35f) // More tinted with baseColor
                drawCircle(
                    color = scleraColor,
                    radius = scleraRadius,
                    center = scleraPos,
                    alpha = 1f // Solid opacity
                )

                // Draw pupil (looks forward, moves very little)
                val pdx = cos(scleraPt * 1.3f) * (scleraRadius * 0.1f)
                val pdy = sin(scleraPt * 1.3f) * (scleraRadius * 0.1f)
                val pupilPos = scleraPos + Offset(pdx, pdy)
                
                drawCircle(
                    color = Color.Black,
                    radius = scleraRadius * 0.45f,
                    center = pupilPos,
                    alpha = 1f // Solid opacity
                )
            }
        }
        content()
    }
}
