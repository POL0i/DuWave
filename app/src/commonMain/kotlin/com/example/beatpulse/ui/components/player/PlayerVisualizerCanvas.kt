package com.example.beatpulse.ui.components.player

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.beatpulse.theme.PaletteColors
import kotlin.random.Random

class TrapParticle(
    var x: Float, var y: Float, 
    var vx: Float, var vy: Float, 
    var life: Float, val maxLife: Float, 
    val size: Float, val color: Color
)

@Composable
fun PlayerVisualizerCanvas(
    modifier: Modifier = Modifier,
    coverOffsetX: Float = 0f,
    coverOffsetY: Float = 0f,
    currentStyle: VisualizerStyle,
    thumbnailShapeIdx: Int,
    bassAmplitudes: () -> FloatArray,
    midAmplitudes: () -> FloatArray,
    highAmplitudes: () -> FloatArray,
    combinedAmplitudes: () -> FloatArray,
    bassAvg: () -> Float,
    midAvg: () -> Float,
    trebleAvg: () -> Float,
    bassOpacity: () -> Float,
    midOpacity: () -> Float,
    highOpacity: () -> Float,
    visualizerArchetype: Int,
    colorDominant: Color,
    colorVibrant: Color,
    colorMuted: Color,
    paletteColors: PaletteColors,
    rotationAngle: () -> Float,
    fastRotationAngle: () -> Float,
    currentPosition: Long,
    duration: Long,
    dragSeekTimeMs: Long?,
    abRepeatModeEnabled: Boolean,
    abPointA: Float,
    abPointB: Float,
    activeDraggingHandle: String?,
    animatedScale: () -> Float,
    coverScale: Float,
    cleanUiMode: Boolean = false,
    elementSize: Float = 1f,
    onPlayheadPosChanged: (Offset) -> Unit,
    onMarkerAPosChanged: ((Offset) -> Unit)? = null,
    onMarkerBPosChanged: ((Offset) -> Unit)? = null
) {
    val basePath = remember { Path() }
    val wavePathR = remember { Path() }
    val progressPath = remember { Path() }
    val progressMeasure = remember { androidx.compose.ui.graphics.PathMeasure() }
    val sharedSlimeX = remember { FloatArray(1024) }
    val sharedSlimeY = remember { FloatArray(1024) }
    val starPeaks = remember { FloatArray(512) }
    val starPeakTimes = remember { LongArray(512) }
    val trapParticles = remember { ArrayList<TrapParticle>() }

    class CanvasCache {
        var lastSize: Size = Size.Zero
        var lastShape: Int = -1
    }
    val cache = remember { CanvasCache() }

    val sweepGradient = remember(colorVibrant, colorDominant, colorMuted) {
        Brush.sweepGradient(colors = listOf(colorVibrant, colorDominant, colorMuted, colorVibrant))
    }

    Canvas(modifier = modifier
        .offset { androidx.compose.ui.unit.IntOffset(coverOffsetX.toInt(), coverOffsetY.toInt()) }
        .size(320.dp)
        .graphicsLayer {
            val ascale = animatedScale()
            scaleX = ascale * coverScale
            scaleY = ascale * coverScale
        }) {
        val radius = size.minDimension / 4f
        val center = Offset(size.width / 2, size.height / 2)
        val coverSize = 160.dp.toPx()
        val rPx = coverSize / 2f

        val aggressiveElementSize = if (elementSize >= 1f) 1f + (elementSize - 1f) * 4f else elementSize * elementSize

        if (size != cache.lastSize || thumbnailShapeIdx != cache.lastShape) {
            basePath.reset()
            when (thumbnailShapeIdx) {
                0 -> basePath.addOval(Rect(center.x - rPx, center.y - rPx, center.x + rPx, center.y + rPx))
                1 -> basePath.addRect(Rect(center.x - rPx, center.y - rPx, center.x + rPx, center.y + rPx))
                2 -> basePath.addRoundRect(RoundRect(
                    left = center.x - rPx, top = center.y - rPx, right = center.x + rPx, bottom = center.y + rPx,
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                ))
                3 -> basePath.addRoundRect(RoundRect(
                    left = center.x - rPx, top = center.y - rPx, right = center.x + rPx, bottom = center.y + rPx,
                    cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx())
                ))
                4 -> {
                    val w = rPx * 2f; val h = rPx * 2f
                    val l = center.x - rPx; val t = center.y - rPx
                    basePath.moveTo(l + w / 2f, t)
                    basePath.quadraticTo(l + w, t, l + w, t + h * 0.4f)
                    basePath.lineTo(l + w, t + h)
                    basePath.lineTo(l, t + h)
                    basePath.lineTo(l, t + h * 0.4f)
                    basePath.quadraticTo(l, t, l + w / 2f, t)
                }
                5 -> {
                    val w = rPx * 2f; val h = rPx * 2f
                    val l = center.x - rPx; val t = center.y - rPx
                    basePath.moveTo(l + w / 2f, t)
                    basePath.lineTo(l + w, t + h / 2f)
                    basePath.lineTo(l + w / 2f, t + h)
                    basePath.lineTo(l, t + h / 2f)
                    basePath.lineTo(l + w / 2f, t)
                }
                6 -> {
                    val w = rPx * 2f; val h = rPx * 2f
                    val l = center.x - rPx; val t = center.y - rPx
                    basePath.moveTo(l + w * 0.5f, t)
                    basePath.lineTo(l + w, t + h * 0.25f)
                    basePath.lineTo(l + w, t + h * 0.75f)
                    basePath.lineTo(l + w * 0.5f, t + h)
                    basePath.lineTo(l, t + h * 0.75f)
                    basePath.lineTo(l, t + h * 0.25f)
                    basePath.lineTo(l + w * 0.5f, t)
                }
            }
            basePath.close()
            progressMeasure.setPath(basePath, forceClosed = false)
            cache.lastSize = size
            cache.lastShape = thumbnailShapeIdx
        }

        val pathLength = if (thumbnailShapeIdx == 0) rPx * 2f * Math.PI.toFloat() else progressMeasure.length

        // Draw layer function
        fun drawLayer(amps: FloatArray, layerColor: Color, opacity: Float, isBassLayer: Boolean, layerScale: Float = 1f) {
            val numBars = amps.size
            if (numBars == 0 || currentStyle == VisualizerStyle.RINGS || currentStyle == VisualizerStyle.AURA || currentStyle == VisualizerStyle.BANDS) return
            val distStep = (pathLength / 2f) / (numBars - 1).coerceAtLeast(1).toFloat()
            var outPx = 0f; var outPy = 0f; var outNx = 0f; var outNy = 0f

            fun computePointAndNormal(d: Float) {
                if (thumbnailShapeIdx == 0) {
                    val angle = (d / pathLength) * 2 * Math.PI - Math.PI / 2
                    outNx = kotlin.math.cos(angle).toFloat()
                    outNy = kotlin.math.sin(angle).toFloat()
                    outPx = center.x + rPx * outNx
                    outPy = center.y + rPx * outNy
                } else {
                    val pathOffsetRatio = if (thumbnailShapeIdx in 1..3) 0.125f else 0f
                    val dMod = (d + pathLength * pathOffsetRatio) % pathLength
                    val pos = progressMeasure.getPosition(dMod)
                    val tan = progressMeasure.getTangent(dMod)
                    if (pos != androidx.compose.ui.geometry.Offset.Unspecified && tan != androidx.compose.ui.geometry.Offset.Unspecified) {
                        outPx = pos.x; outPy = pos.y
                        outNx = tan.y; outNy = -tan.x
                        // Ensure normal points OUTWARD
                        val dx = outPx - center.x
                        val dy = outPy - center.y
                        if (outNx * dx + outNy * dy < 0) {
                            outNx = -outNx
                            outNy = -outNy
                        }
                        val len = kotlin.math.hypot(outNx, outNy)
                        if (len > 0) { outNx /= len; outNy /= len }
                    } else {
                        outPx = center.x; outPy = center.y; outNx = 0f; outNy = -1f
                    }
                }
            }

            val colorVibrantLayer = layerColor.copy(alpha = opacity)

            when (currentStyle) {
                VisualizerStyle.BARS -> {
                    for (i in 0 until numBars) {
                        val amplitude = amps[i]
                        val dist = 30f + (amplitude * 180f)
                        val barLength = (8f + (amplitude * 60f)) * aggressiveElementSize
                        val dRight = 0f + i * distStep
                        val dLeft = pathLength - i * distStep
                        computePointAndNormal(dRight)
                        val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                        computePointAndNormal(dLeft)
                        val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy
                        drawLine(color = colorVibrantLayer, start = Offset(pxR + nxR * dist, pyR + nyR * dist), end = Offset(pxR + nxR * (dist + barLength), pyR + nyR * (dist + barLength)), strokeWidth = 3f * aggressiveElementSize, cap = StrokeCap.Round)
                        if (i != 0 && i != numBars - 1) {
                            drawLine(color = colorVibrantLayer, start = Offset(pxL + nxL * dist, pyL + nyL * dist), end = Offset(pxL + nxL * (dist + barLength), pyL + nyL * (dist + barLength)), strokeWidth = 3f * aggressiveElementSize, cap = StrokeCap.Round)
                        }
                    }
                }
                VisualizerStyle.WAVE -> {
                    val totalPoints = (numBars * 2 - 2).coerceAtLeast(0)
                    wavePathR.reset()
                    for (i in 0 until totalPoints) {
                        val ampIndex = if (i < numBars) i else (totalPoints - i)
                        val dDist = if (i < numBars) i * distStep else pathLength - ampIndex * distStep
                        val amplitude = amps[ampIndex]
                        val dist = 30f + (amplitude * 180f)
                        computePointAndNormal(dDist)
                        val px = outPx + outNx * dist; val py = outPy + outNy * dist
                        if (i == 0) wavePathR.moveTo(px, py) else wavePathR.lineTo(px, py)
                    }
                    wavePathR.close()
                    drawPath(wavePathR, color = colorVibrantLayer, style = Stroke(width = 3f * aggressiveElementSize, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                VisualizerStyle.DOTS -> {
                    for (i in 0 until numBars) {
                        val amplitude = amps[i]
                        val dRight = 0f + i * distStep
                        val dLeft = pathLength - i * distStep
                        computePointAndNormal(dRight)
                        val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                        val dotCount = 1 + (amplitude * 8).toInt()
                        val dotSpacing = 16f * aggressiveElementSize
                        val dotRadius = 1.2f * aggressiveElementSize
                        for (j in 0 until dotCount) {
                            val currentDist = 30f + (j * dotSpacing)
                            val alphaVal = opacity * (1f - (j.toFloat() / 8f)).coerceAtLeast(0.6f)
                            drawCircle(color = layerColor.copy(alpha = alphaVal), radius = dotRadius, center = Offset(pxR + nxR * currentDist, pyR + nyR * currentDist))
                        }
                        if (i != 0 && i != numBars - 1) {
                            computePointAndNormal(dLeft)
                            val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy
                            for (j in 0 until dotCount) {
                                val currentDist = 30f + (j * dotSpacing)
                                val alphaVal = opacity * (1f - (j.toFloat() / 8f)).coerceAtLeast(0.6f)
                                drawCircle(color = layerColor.copy(alpha = alphaVal), radius = dotRadius, center = Offset(pxL + nxL * currentDist, pyL + nyL * currentDist))
                            }
                        }
                    }
                }
                VisualizerStyle.SLIME -> {
                    val totalPoints = (numBars * 2 - 2).coerceAtLeast(0)
                    if (totalPoints > sharedSlimeX.size) return
                    val slimeX = sharedSlimeX
                    val slimeY = sharedSlimeY
                    for (i in 0 until totalPoints) {
                        val ampIndex = if (i < numBars) i else (totalPoints - i)
                        val amplitude = amps[ampIndex]
                        val offsetDist = if (i < numBars) 0f + ampIndex * distStep else pathLength - ampIndex * distStep
                        computePointAndNormal(offsetDist)
                        val extrude = 5f + (amplitude * 150f)
                        slimeX[i] = outPx + outNx * extrude
                        slimeY[i] = outPy + outNy * extrude
                    }
                    wavePathR.reset()
                    if (totalPoints > 0) {
                        var prevMidX = (slimeX[0] + slimeX[totalPoints - 1]) / 2f
                        var prevMidY = (slimeY[0] + slimeY[totalPoints - 1]) / 2f
                        wavePathR.moveTo(prevMidX, prevMidY)
                        for (i in 0 until totalPoints) {
                            val nextIndex = (i + 1) % totalPoints
                            val midX = (slimeX[i] + slimeX[nextIndex]) / 2f
                            val midY = (slimeY[i] + slimeY[nextIndex]) / 2f
                            wavePathR.quadraticTo(slimeX[i], slimeY[i], midX, midY)
                        }
                        wavePathR.close()
                        val slimeBrush = Brush.radialGradient(colors = listOf(layerColor, layerColor.copy(alpha = 0.5f)), center = center, radius = radius + 250f)
                        drawPath(path = wavePathR, brush = slimeBrush, alpha = opacity)
                        drawPath(path = wavePathR, color = layerColor.copy(alpha = opacity), style = Stroke(width = 4f * aggressiveElementSize, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                VisualizerStyle.STAR -> {
                    val totalPoints = (numBars * 2 - 2).coerceAtLeast(0)
                    if (totalPoints == 0) return
                    wavePathR.reset()
                    val currentTime = System.currentTimeMillis()
                    val points = Array(totalPoints) { Offset.Zero }
                    val flareAlphas = FloatArray(totalPoints)
                    
                    for (i in 0 until totalPoints) {
                        val ampIndex = if (i < numBars) i else (totalPoints - i)
                        val dDist = if (i < numBars) i * distStep else pathLength - ampIndex * distStep
                        val amplitude = amps[ampIndex]
                        
                        // Peak hold logic with aggressive sensitivity
                        val decayRate = 0.04f
                        if (amplitude >= starPeaks[i]) {
                            starPeaks[i] = amplitude
                            starPeakTimes[i] = currentTime
                        } else {
                            val elapsed = currentTime - starPeakTimes[i]
                            if (elapsed > 200) { // Guardar por un instante (200ms peak hold)
                                starPeaks[i] = (starPeaks[i] - decayRate).coerceAtLeast(amplitude)
                            }
                        }
                        
                        val heldAmp = starPeaks[i]
                        // Multiply heldAmp aggressively for star spikes
                        val dist = 5f + (heldAmp * heldAmp * 280f)
                        
                        computePointAndNormal(dDist)
                        val px = outPx + outNx * dist; val py = outPy + outNy * dist
                        points[i] = Offset(px, py)
                        if (i == 0) wavePathR.moveTo(px, py) else wavePathR.lineTo(px, py)
                        
                        // Calculate flash intensity for this vertex
                        val elapsed = currentTime - starPeakTimes[i]
                        if (elapsed < 150 && heldAmp > 0.4f) {
                            flareAlphas[i] = (1f - (elapsed / 150f)).coerceIn(0f, 1f) * heldAmp * opacity
                        }
                    }
                    wavePathR.close()
                    // Draw base star
                    drawPath(wavePathR, color = colorVibrantLayer, style = androidx.compose.ui.graphics.drawscope.Fill)
                    // Draw base contour
                    drawPath(wavePathR, color = Color.White.copy(alpha = 0.3f * opacity), style = Stroke(width = 2f * aggressiveElementSize, join = StrokeJoin.Round))
                    
                    // Draw luminous line segments on active peaks instead of circles
                    for (i in 0 until totalPoints) {
                        val alpha = flareAlphas[i]
                        if (alpha > 0f) {
                            val pPrev = points[if (i == 0) totalPoints - 1 else i - 1]
                            val pCurr = points[i]
                            val pNext = points[if (i == totalPoints - 1) 0 else i + 1]
                            
                            val luminousPath = Path()
                            luminousPath.moveTo(pPrev.x, pPrev.y)
                            luminousPath.lineTo(pCurr.x, pCurr.y)
                            luminousPath.lineTo(pNext.x, pNext.y)
                            
                            drawPath(
                                path = luminousPath,
                                color = Color.White.copy(alpha = alpha),
                                style = Stroke(width = (3f + alpha * 10f) * aggressiveElementSize, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
                VisualizerStyle.PARTICLES -> {
                    for (i in 0 until numBars) {
                        val amplitude = amps[i]
                        val multiplicador = 1f + (i.toFloat() / numBars) * 1.5f
                        val boostedAmplitude = amplitude * multiplicador
                        val extrude = 20f + (boostedAmplitude * 300f)
                        val sz = (1.5f + (boostedAmplitude * 4f)) * aggressiveElementSize
                        val dRight = 0f + i * distStep
                        val dLeft = pathLength - i * distStep
                        val timeMs = currentPosition
                        computePointAndNormal(dRight)
                        drawCircle(color = colorVibrantLayer, radius = sz, center = Offset(outPx + outNx * extrude, outPy + outNy * extrude))
                        if (boostedAmplitude > 0.1f) {
                            val sparkOffset = ((timeMs + i * 40) % 800) / 800f
                            val sparkExtrude = extrude + (sparkOffset * 100f)
                            val sparkSize = (1f + (1f - sparkOffset) * 2f) * aggressiveElementSize
                            drawCircle(color = colorVibrantLayer.copy(alpha = opacity * (1f - sparkOffset)), radius = sparkSize, center = Offset(outPx + outNx * sparkExtrude, outPy + outNy * sparkExtrude))
                        }
                        if (i != 0 && i != numBars - 1) {
                            computePointAndNormal(dLeft)
                            drawCircle(color = colorVibrantLayer, radius = sz, center = Offset(outPx + outNx * extrude, outPy + outNy * extrude))
                            if (boostedAmplitude > 0.1f) {
                                val sparkOffset = ((timeMs + i * 40) % 800) / 800f
                                val sparkExtrude = extrude + (sparkOffset * 100f)
                                val sparkSize = (1f + (1f - sparkOffset) * 2f) * aggressiveElementSize
                                drawCircle(color = colorVibrantLayer.copy(alpha = opacity * (1f - sparkOffset)), radius = sparkSize, center = Offset(outPx + outNx * sparkExtrude, outPy + outNy * sparkExtrude))
                            }
                        }
                    }
                }
                VisualizerStyle.TRAP_NATION -> {
                    val rPx = size.minDimension * 0.15f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // 1. Smooth the amplitudes (After Effects Audio Smoothing)
                    val smoothedAmps = FloatArray(numBars)
                    for (i in 0 until numBars) {
                        var sum = 0f
                        var count = 0f
                        // Wider Gaussian-like smoothing window for more fluidity (After Effects style)
                        for (j in -3..3) {
                            val idx = i + j
                            if (idx in 0 until numBars) {
                                val weight = 1f / (1f + kotlin.math.abs(j))
                                sum += amps[idx] * weight
                                count += weight
                            }
                        }
                        smoothedAmps[i] = sum / count
                    }

                    // 2. Draw the continuous mirrored wave (Audio Spectrum style)
                    val totalPoints = (numBars * 2 - 1).coerceAtLeast(0)
                    val path = Path()
                    for (i in 0 until totalPoints) {
                        val isRightSide = i >= numBars - 1
                        
                        // Map bass (index 0) to the bottom, and treble (numBars - 1) to the top
                        val ampIndex = if (isRightSide) {
                            i - (numBars - 1)
                        } else {
                            (numBars - 1) - i
                        }
                        
                        val amplitude = smoothedAmps[ampIndex]
                        
                        // Square the amplitude (instead of cubic) for aggressive but fluid motion
                        val extrude = amplitude * amplitude * 300f * layerScale
                        
                        // Calculate position along the path:
                        // d = 0 is top. d = pathLength / 2 is bottom.
                        // Left side (isRightSide = false): top (d=0) down to bottom (d=pathLength/2)
                        // Right side (isRightSide = true): bottom (d=pathLength/2) up to top (d=pathLength)
                        val d = if (isRightSide) {
                            pathLength / 2f + (ampIndex.toFloat() / (numBars - 1).coerceAtLeast(1)) * (pathLength / 2f)
                        } else {
                            (1f - ampIndex.toFloat() / (numBars - 1).coerceAtLeast(1)) * (pathLength / 2f)
                        }
                        
                        computePointAndNormal(d)
                        
                        // Separate the 3 layers
                        val baseOffset = (layerScale - 1f) * 120f

                        // Push outward using the normal to preserve the exact cover art shape
                        val px = outPx + outNx * (extrude + baseOffset)
                        val py = outPy + outNy * (extrude + baseOffset)

                        if (i == 0) {
                            path.moveTo(px, py)
                        } else {
                            path.lineTo(px, py)
                        }
                    }
                    path.close()

                    val isClosestLayer = layerScale == 1.0f
                    if (visualizerArchetype == 1 || isClosestLayer) {
                        // For the closest layer, use the layerColor (which could be dominant/vibrant/muted depending on archetype) 
                        // to tint the fill dynamically
                        val fillAlpha = if (visualizerArchetype == 1) 1f else 0.4f * opacity
                        val tintedFill = Color(
                            red = 0.4f + layerColor.red * 0.6f,
                            green = 0.4f + layerColor.green * 0.6f,
                            blue = 0.4f + layerColor.blue * 0.6f,
                            alpha = fillAlpha
                        )

                        drawPath(
                            path = path,
                            color = tintedFill,
                            style = Fill
                        )
                        drawPath(
                            path = path,
                            color = layerColor.copy(alpha = if (visualizerArchetype == 1) 1f else 0.9f * opacity),
                            style = Stroke(width = 4f, join = StrokeJoin.Round)
                        )
                    } else {
                        // Distinct outlines instead of solid overlap
                        drawPath(
                            path = path,
                            color = layerColor.copy(alpha = 0.9f * opacity),
                            style = Stroke(width = 4f, join = StrokeJoin.Round)
                        )
                    }

                    // 3. Update and draw particles (ONLY on bass layer so we don't duplicate work)
                    if (isBassLayer) {
                        val bAvg = bassAvg()
                        // Fix for low sensitivity: spawn even at very low bassAvg
                        if (bAvg > 0.05f && Random.nextFloat() < 0.2f + bAvg * 0.5f) {
                            val numToSpawn = (bAvg * 6).toInt().coerceIn(1, 3)
                            for (s in 0 until numToSpawn) {
                                val speed = 1f + Random.nextFloat() * 4f * bAvg
                                val pColor = Color.White
                                val maxLife = 120f + Random.nextFloat() * 80f
                                
                                val randomD = Random.nextFloat() * pathLength
                                computePointAndNormal(randomD)
                                trapParticles.add(
                                    TrapParticle(
                                        x = outPx + outNx * 10f,
                                        y = outPy + outNy * 10f,
                                        vx = outNx * speed,
                                        vy = outNy * speed,
                                        life = maxLife,
                                        maxLife = maxLife,
                                        size = 0.3f,
                                        color = pColor
                                    )
                                )
                            }
                        }

                        val iterator = trapParticles.iterator()
                        while (iterator.hasNext()) {
                            val p = iterator.next()
                            p.life -= 1f
                            if (p.life <= 0) {
                                iterator.remove()
                                continue
                            }
                            
                            // Physics
                            p.x += p.vx
                            p.y += p.vy
                            // Slight deceleration
                            p.vx *= 0.99f
                            p.vy *= 0.99f
                            
                            // Size grows as it moves outward
                            val lifeProgress = 1f - (p.life / p.maxLife)
                            val currentSize = p.size + (lifeProgress * 3f)
                            
                            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                            drawCircle(
                                color = p.color.copy(alpha = alpha * opacity),
                                radius = currentSize,
                                center = Offset(p.x, p.y)
                            )
                        }
                    }
                }
                else -> {}
            }
        }

        // Rings/Aura/Bands special drawing
        if (currentStyle == VisualizerStyle.RINGS || currentStyle == VisualizerStyle.AURA || currentStyle == VisualizerStyle.BANDS) {
            when (currentStyle) {
                VisualizerStyle.RINGS -> {
                    val bAvg = bassAvg()
                    val mAvg = midAvg()
                    val tAvg = trebleAvg()
                    val dynamicRotation = rotationAngle() + (bAvg * 90f)
                    val dynamicFastRotation = fastRotationAngle() - (mAvg * 90f)
                    fun drawGlitchRing(r: Float, thickness: Float, gapAngle: Float, startOffset: Float, brushColor: Color) {
                        val ringPath = Path().apply {
                            when (thumbnailShapeIdx) {
                                0 -> addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
                                1 -> addRect(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
                                2 -> {
                                    val scale = if (rPx > 0) r / rPx else 1f
                                    addRoundRect(RoundRect(
                                        left = center.x - r, top = center.y - r, right = center.x + r, bottom = center.y + r,
                                        cornerRadius = CornerRadius(16.dp.toPx() * scale, 16.dp.toPx() * scale)
                                    ))
                                }
                                3 -> {
                                    val scale = if (rPx > 0) r / rPx else 1f
                                    addRoundRect(RoundRect(
                                        left = center.x - r, top = center.y - r, right = center.x + r, bottom = center.y + r,
                                        cornerRadius = CornerRadius(32.dp.toPx() * scale, 32.dp.toPx() * scale)
                                    ))
                                }
                                4 -> {
                                    val w = r * 2f; val h = r * 2f
                                    val l = center.x - r; val t = center.y - r
                                    moveTo(l + w / 2f, t)
                                    quadraticTo(l + w, t, l + w, t + h * 0.4f)
                                    lineTo(l + w, t + h)
                                    lineTo(l, t + h)
                                    lineTo(l, t + h * 0.4f)
                                    quadraticTo(l, t, l + w / 2f, t)
                                    close()
                                }
                                5 -> {
                                    val w = r * 2f; val h = r * 2f
                                    val l = center.x - r; val t = center.y - r
                                    moveTo(l + w / 2f, t)
                                    lineTo(l + w, t + h / 2f)
                                    lineTo(l + w / 2f, t + h)
                                    lineTo(l, t + h / 2f)
                                    close()
                                }
                                6 -> {
                                    val w = r * 2f; val h = r * 2f
                                    val l = center.x - r; val t = center.y - r
                                    moveTo(l + w * 0.5f, t)
                                    lineTo(l + w, t + h * 0.25f)
                                    lineTo(l + w, t + h * 0.75f)
                                    lineTo(l + w * 0.5f, t + h)
                                    lineTo(l, t + h * 0.75f)
                                    lineTo(l, t + h * 0.25f)
                                    close()
                                }
                            }
                        }

                        val ringMeasure = androidx.compose.ui.graphics.PathMeasure()
                        ringMeasure.setPath(ringPath, forceClosed = false)
                        val totalLen = ringMeasure.length
                        if (totalLen <= 0f) return

                        val dashLen = ((90f - gapAngle) / 360f) * totalLen
                        val gapLen = (gapAngle / 360f) * totalLen
                        
                        // Phase offset needs to be negative to rotate clockwise
                        val phase = -(startOffset / 360f) * totalLen
                        
                        drawPath(
                            path = ringPath, 
                            color = brushColor, 
                            style = Stroke(
                                width = thickness, 
                                cap = StrokeCap.Square,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLen, gapLen), phase)
                            )
                        )
                    }
                    if (visualizerArchetype == 1) {
                        val maxPulse = maxOf(bAvg, mAvg, tAvg)
                        val combinedRadius = radius + 60f + (maxPulse * 80f)
                        drawGlitchRing(combinedRadius, 8f + (maxPulse * 15f), 20f, dynamicRotation, colorVibrant.copy(alpha = 0.8f))
                    } else {
                        val bassRadius = radius + 30f + (bAvg * 80f)
                        val midRadius = radius + 60f + (mAvg * 70f)
                        val trebleRadius = radius + 90f + (tAvg * 60f)
                        drawGlitchRing(bassRadius, 8f + (bAvg * 15f), 20f - (bAvg * 10f), dynamicRotation, colorDominant.copy(alpha = 0.8f))
                        drawGlitchRing(midRadius, 4f + (mAvg * 10f), 30f, dynamicFastRotation, colorVibrant.copy(alpha = 0.6f))
                        drawGlitchRing(trebleRadius, 2f + (tAvg * 5f), 45f, dynamicRotation * 0.5f, colorMuted.copy(alpha = 0.5f))
                    }
                }
                VisualizerStyle.AURA -> {
                    val bAvg = bassAvg()
                    val mAvg = midAvg()
                    val tAvg = trebleAvg()
                    if (visualizerArchetype == 1) {
                        val maxPulse = maxOf(bAvg, mAvg, tAvg)
                        drawPath(path = basePath, color = colorVibrant.copy(alpha = 0.2f + 0.2f * maxPulse.coerceIn(0f, 1f)), style = Stroke(width = 60f + maxPulse * 150f, join = StrokeJoin.Round))
                    } else {
                        val bassPulse = (bAvg * 1.5f).coerceIn(0f, 1f)
                        val midPulse = (mAvg * 1.5f).coerceIn(0f, 1f)
                        val treblePulse = (tAvg * 1.5f).coerceIn(0f, 1f)
                        drawPath(path = basePath, color = colorDominant.copy(alpha = 0.1f + 0.1f * bassPulse), style = Stroke(width = 80f + bAvg * 200f, join = StrokeJoin.Round))
                        drawPath(path = basePath, color = colorVibrant.copy(alpha = 0.15f + 0.15f * midPulse), style = Stroke(width = 40f + mAvg * 100f, join = StrokeJoin.Round))
                        drawPath(path = basePath, color = colorMuted.copy(alpha = 0.25f + 0.25f * treblePulse), style = Stroke(width = 15f + tAvg * 50f, join = StrokeJoin.Round))
                    }
                }
                VisualizerStyle.BANDS -> { /* Drawn in background */ }
                else -> {}
            }
        } else {
            if (visualizerArchetype == 1) {
                drawLayer(combinedAmplitudes(), paletteColors.vibrant, maxOf(bassOpacity(), midOpacity(), highOpacity()), true, 1.0f)
            } else {
                if (currentStyle == VisualizerStyle.TRAP_NATION) {
                    drawLayer(bassAmplitudes(), paletteColors.dominant, bassOpacity(), true, 1.0f)
                    drawLayer(midAmplitudes(), paletteColors.vibrant, midOpacity(), false, 1.4f)
                    drawLayer(highAmplitudes(), paletteColors.muted, highOpacity(), false, 1.8f)
                } else {
                    drawLayer(bassAmplitudes(), paletteColors.dominant, bassOpacity(), true, 1.5f)
                    drawLayer(midAmplitudes(), paletteColors.vibrant, midOpacity(), false, 1.0f)
                    drawLayer(highAmplitudes(), paletteColors.muted, highOpacity(), false, 0.6f)
                }
            }
        }

        // Progress ring
        val activePosition = dragSeekTimeMs ?: currentPosition
        val progressFraction = if (duration > 0) activePosition.toFloat() / duration else 0f
        if (!cleanUiMode) drawPath(path = basePath, color = colorDominant.copy(alpha = 0.3f), style = Stroke(width = 4f))
        if (thumbnailShapeIdx == 0 && progressFraction > 0f) {
            val sweepAngle = progressFraction * 360f
            if (!cleanUiMode) {
                drawArc(
                    brush = sweepGradient,
                    startAngle = 90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 6f, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - rPx, center.y - rPx),
                    size = androidx.compose.ui.geometry.Size(rPx * 2f, rPx * 2f)
                )
            }
            val angle = (progressFraction * 2 * Math.PI) + Math.PI / 2
            val thumbPos = Offset(
                center.x + rPx * kotlin.math.cos(angle).toFloat(),
                center.y + rPx * kotlin.math.sin(angle).toFloat()
            )
            onPlayheadPosChanged(thumbPos)
            if (!cleanUiMode) drawCircle(color = Color.White, radius = 8f, center = thumbPos)
        } else if (progressFraction > 0f) {
            progressMeasure.setPath(basePath, forceClosed = false)
            val pLen = progressMeasure.length
            progressPath.reset()
            val targetLength = pLen * progressFraction
            val startD = when(thumbnailShapeIdx) { in 1..3 -> pLen * 0.125f; else -> 0f }
            val endD = startD + targetLength
            if (endD <= pLen) {
                progressMeasure.getSegment(startD, endD, progressPath, true)
            } else {
                progressMeasure.getSegment(startD, pLen, progressPath, true)
                progressMeasure.getSegment(0f, endD % pLen, progressPath, false)
            }
            if (!cleanUiMode) drawPath(path = progressPath, brush = sweepGradient, style = Stroke(width = 6f, cap = StrokeCap.Round))
            val thumbDist = endD % pLen
            var thumbPos = progressMeasure.getPosition(thumbDist)
            if (thumbPos == Offset.Unspecified || thumbPos == Offset.Zero) {
                val pos = progressMeasure.getPosition(thumbDist)
                if (pos != Offset.Unspecified) {
                    thumbPos = pos
                }
            }
            if (thumbPos != Offset.Unspecified && thumbPos != Offset.Zero) {
                onPlayheadPosChanged(thumbPos)
                if (!cleanUiMode) {
                    if (thumbnailShapeIdx == 0) {
                        drawCircle(color = Color.White, radius = 8f, center = thumbPos)
                    } else {
                        withTransform({
                            translate(left = thumbPos.x - center.x, top = thumbPos.y - center.y)
                            scale(scaleX = 8f / rPx, scaleY = 8f / rPx, pivot = center)
                        }) {
                            drawPath(path = basePath, color = Color.White)
                        }
                    }
                }
            }
        }

        // A-B Repeat markers
        if (abRepeatModeEnabled && !cleanUiMode) {
            val getPosFromProgress = { progress: Float ->
                val pl = progressMeasure.length
                if (pl <= 0f) center
                else {
                    if (thumbnailShapeIdx == 0) {
                        val angle = (progress * 2 * Math.PI) + Math.PI / 2
                        Offset(
                            center.x + rPx * kotlin.math.cos(angle).toFloat(),
                            center.y + rPx * kotlin.math.sin(angle).toFloat()
                        )
                    } else {
                        val sd = when(thumbnailShapeIdx) { in 1..3 -> pl * 0.125f; else -> 0f }
                        val dMod = (sd + progress * pl) % pl
                        val pos = progressMeasure.getPosition(dMod)
                        if (pos != Offset.Unspecified) pos else center
                    }
                }
            }
            val posA = getPosFromProgress(abPointA)
            val posB = getPosFromProgress(abPointB)
            onMarkerAPosChanged?.invoke(Offset(posA.x - center.x, posA.y - center.y))
            onMarkerBPosChanged?.invoke(Offset(posB.x - center.x, posB.y - center.y))
            val markerSizeA = if (activeDraggingHandle == "A") 16.dp.toPx() else 8.dp.toPx()
            val markerSizeB = if (activeDraggingHandle == "B") 16.dp.toPx() else 8.dp.toPx()
            if (thumbnailShapeIdx == 0) {
                drawCircle(color = colorMuted, radius = markerSizeA, center = posA)
                drawCircle(color = colorMuted.copy(alpha = 0.3f), radius = markerSizeA * 2, center = posA)
                drawCircle(color = colorVibrant, radius = markerSizeB, center = posB)
                drawCircle(color = colorVibrant.copy(alpha = 0.3f), radius = markerSizeB * 2, center = posB)
            } else {
                withTransform({
                    translate(left = posA.x - center.x, top = posA.y - center.y)
                    scale(scaleX = markerSizeA / rPx, scaleY = markerSizeA / rPx, pivot = center)
                }) {
                    drawPath(path = basePath, color = colorMuted)
                }
                withTransform({
                    translate(left = posA.x - center.x, top = posA.y - center.y)
                    scale(scaleX = (markerSizeA * 2) / rPx, scaleY = (markerSizeA * 2) / rPx, pivot = center)
                }) {
                    drawPath(path = basePath, color = colorMuted.copy(alpha = 0.3f))
                }
                withTransform({
                    translate(left = posB.x - center.x, top = posB.y - center.y)
                    scale(scaleX = markerSizeB / rPx, scaleY = markerSizeB / rPx, pivot = center)
                }) {
                    drawPath(path = basePath, color = colorVibrant)
                }
                withTransform({
                    translate(left = posB.x - center.x, top = posB.y - center.y)
                    scale(scaleX = (markerSizeB * 2) / rPx, scaleY = (markerSizeB * 2) / rPx, pivot = center)
                }) {
                    drawPath(path = basePath, color = colorVibrant.copy(alpha = 0.3f))
                }
            }
        }
    }
}
