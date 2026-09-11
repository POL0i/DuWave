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
import androidx.compose.ui.graphics.drawscope.translate
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
    val dirtSizes: List<Float>,
    val baseColor: Color,
    val glowBrush: Brush,
    val bodyBrush: Brush,
    val dirtBrushes: List<Brush>
)

@Composable
fun LullabyEyesBackground(
    paletteColors: PaletteColors,
    visualizerManager: IAudioVisualizerManager,
    isPlayerScreen: Boolean,
    content: @Composable () -> Unit
) {
    val lifecycleState by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val isActiveApp = lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)

    var time by remember { mutableStateOf(0f) }
    LaunchedEffect(isActiveApp, isPlayerScreen) {
        if (!isActiveApp) return@LaunchedEffect
        val startTime = withFrameNanos { it } - (time * 1_000_000_000f).toLong()
        while (true) {
            val frameTime = withFrameNanos { it }
            val t = (frameTime - startTime) / 1_000_000_000f
            time = if (isPlayerScreen) t else t * 0.3f // Mover más lento en la librería
        }
    }

    var screenWidth by remember { mutableStateOf(1000f) }
    var screenHeight by remember { mutableStateOf(1000f) }

    val eyes = remember(screenWidth, screenHeight, paletteColors) {
        if (screenWidth <= 0f || screenHeight <= 0f) return@remember emptyList<LullabyEye>()
        val rnd = Random(Random.nextInt()) // Aleatorio cada vez que se regenera (cambio de tamaño, etc.)
        
        // Create a mix of small background eyes and larger foreground eyes
        val generated = mutableListOf<LullabyEye>()
        
        val primary = paletteColors.vibrant
        val secondary = paletteColors.muted
        val tertiary = paletteColors.dominant
        val colorOptions = listOf(primary, secondary, tertiary)

        val genDirt: (Float) -> Triple<List<Float>, List<Float>, List<Float>> = { size ->
            val count = if (size > 35f) rnd.nextInt(3, 8) else 0
            Triple(
                List(count) { rnd.nextFloat() * 2f * PI.toFloat() },
                List(count) { rnd.nextFloat() * 0.8f },
                List(count) { rnd.nextFloat() * 0.3f + 0.1f }
            )
        }
        
        // Helper to generate eye
        val createEye = { minSize: Float, maxSize: Float, minOp: Float, maxOp: Float ->
            val size = rnd.nextFloat() * (maxSize - minSize) + minSize
            val opacity = rnd.nextFloat() * (maxOp - minOp) + minOp
            val dirt = genDirt(size)
            val colorType = rnd.nextInt(3)
            val baseColor = colorOptions[colorType % colorOptions.size]
            val glowRadius = size + 60f
            
            LullabyEye(
                initialPos = Offset(rnd.nextFloat() * screenWidth, rnd.nextFloat() * screenHeight),
                size = size,
                opacity = opacity,
                ellipseRadiusX = rnd.nextFloat() * 100f + 20f,
                ellipseRadiusY = rnd.nextFloat() * 100f + 20f,
                phaseOffset = rnd.nextFloat() * 2f * PI.toFloat(),
                speed = rnd.nextFloat() * 0.8f + 0.2f,
                pupilSpeed = rnd.nextFloat() * 2f + 0.5f,
                pupilPhaseOffset = rnd.nextFloat() * 2f * PI.toFloat(),
                colorType = colorType,
                dirtAngles = dirt.first,
                dirtDistances = dirt.second,
                dirtSizes = dirt.third,
                baseColor = baseColor,
                glowBrush = Brush.radialGradient(
                    colors = listOf(baseColor.copy(alpha = opacity * 0.15f), Color.Transparent),
                    center = Offset.Zero,
                    radius = glowRadius
                ),
                bodyBrush = Brush.radialGradient(
                    colors = listOf(baseColor, baseColor.copy(alpha = 0.7f)),
                    center = Offset(-size * 0.2f, -size * 0.2f),
                    radius = size
                ),
                dirtBrushes = dirt.third.map { dirtSize ->
                    Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset.Zero,
                        radius = dirtSize * size * 1.5f
                    )
                }
            )
        }

        // Small distant eyes
        val bgEyesCount = if (isPlayerScreen) 40 else 10
        for (i in 0..bgEyesCount) {
            generated.add(createEye(4f, 12f, 0.2f, 0.6f))
        }
        
        // Medium eyes
        val mdEyesCount = if (isPlayerScreen) 15 else 4
        for (i in 0..mdEyesCount) {
            generated.add(createEye(12f, 32f, 0.4f, 0.9f))
        }

        // Large foreground eyes
        val fgEyesCount = if (isPlayerScreen) 5 else 2
        for (i in 0..fgEyesCount) {
            generated.add(createEye(40f, 90f, 0.7f, 1.0f))
        }
        
        // Sort by size so smaller are drawn first (background) and large are foreground
        generated.sortedBy { it.size }
    }

    // Colors
    val bgColor = Color(0xFF050505) // Almost black
    val tintColor = paletteColors.dominant.copy(alpha = 0.2f)
    


    val sharedPath = remember { androidx.compose.ui.graphics.Path() }

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
                val baseColor = eye.baseColor
                
                translate(left = currentPos.x, top = currentPos.y) {
                    val glowRadius = eye.size + 60f
                    drawCircle(
                        brush = eye.glowBrush,
                        radius = glowRadius,
                        center = Offset.Zero
                    )
                    
                    drawCircle(
                        brush = eye.bodyBrush,
                        radius = eye.size,
                        center = Offset.Zero,
                        alpha = eye.opacity
                    )
                    
                    sharedPath.reset()
                    sharedPath.addOval(androidx.compose.ui.geometry.Rect(
                        -eye.size, -eye.size, eye.size, eye.size
                    ))
                    
                    clipPath(sharedPath) {
                        for (j in eye.dirtAngles.indices) {
                            val angle = eye.dirtAngles[j]
                            val dist = (0.75f + eye.dirtDistances[j] * 0.25f) * eye.size
                            val dirtSize = eye.dirtSizes[j] * eye.size * 1.5f
                            val dxD = cos(angle) * dist
                            val dyD = sin(angle) * dist
                            
                            translate(left = dxD, top = dyD) {
                                drawCircle(
                                    brush = eye.dirtBrushes[j],
                                    radius = dirtSize,
                                    center = Offset.Zero,
                                    alpha = eye.opacity
                                )
                            }
                        }
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
