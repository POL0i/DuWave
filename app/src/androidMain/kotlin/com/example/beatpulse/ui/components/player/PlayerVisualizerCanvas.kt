package com.example.beatpulse.ui.components.player

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.beatpulse.theme.PaletteColors

@Composable
fun PlayerVisualizerCanvas(
    modifier: Modifier = Modifier,
    currentStyle: VisualizerStyle,
    thumbnailShapeIdx: Int,
    bassAmplitudes: FloatArray,
    midAmplitudes: FloatArray,
    highAmplitudes: FloatArray,
    combinedAmplitudes: FloatArray,
    bassAvg: Float,
    midAvg: Float,
    trebleAvg: Float,
    bassOpacity: Float,
    midOpacity: Float,
    highOpacity: Float,
    visualizerArchetype: Int,
    colorDominant: Color,
    colorVibrant: Color,
    colorMuted: Color,
    paletteColors: PaletteColors,
    rotationAngle: Float,
    fastRotationAngle: Float,
    currentPosition: Long,
    duration: Long,
    dragSeekTimeMs: Long?,
    abRepeatModeEnabled: Boolean,
    abPointA: Float,
    abPointB: Float,
    activeDraggingHandle: String?,
    animatedScale: Float,
    onPlayheadPosChanged: (Offset) -> Unit
) {
    val basePath = remember { Path() }
    val wavePathR = remember { Path() }
    val progressPath = remember { Path() }
    val progressMeasure = remember { androidx.compose.ui.graphics.PathMeasure() }
    val androidPathMeasure = remember { android.graphics.PathMeasure() }

    var lastSize = remember { Size.Zero }
    var lastShape = remember { -1 }

    val sweepGradient = Brush.sweepGradient(
        colors = listOf(colorVibrant, colorDominant, colorMuted, colorVibrant)
    )

    Canvas(modifier = modifier.size(320.dp).graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
    }) {
        val radius = size.minDimension / 4f
        val center = Offset(size.width / 2, size.height / 2)
        val coverSize = 160.dp.toPx()
        val rPx = coverSize / 2f

        if (size != lastSize || thumbnailShapeIdx != lastShape) {
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
            }
            basePath.close()
            progressMeasure.setPath(basePath, forceClosed = false)
            androidPathMeasure.setPath(basePath.asAndroidPath(), false)
            lastSize = size
            lastShape = thumbnailShapeIdx
        }

        val pathLength = if (thumbnailShapeIdx == 0) rPx * 2f * Math.PI.toFloat() else androidPathMeasure.length

        // Draw layer function
        fun drawLayer(amps: FloatArray, layerColor: Color, opacity: Float, isBassLayer: Boolean) {
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
                    val dMod = (d + pathLength * 0.125f) % pathLength
                    val localPosTan = FloatArray(2)
                    val localPosTanTan = FloatArray(2)
                    val success = androidPathMeasure.getPosTan(dMod, localPosTan, localPosTanTan)
                    if (success) {
                        outPx = localPosTan[0]; outPy = localPosTan[1]
                        outNx = -localPosTanTan[1]; outNy = localPosTanTan[0]
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
                        val barLength = 8f + (amplitude * 60f)
                        val dRight = 0f + i * distStep
                        val dLeft = pathLength - i * distStep
                        computePointAndNormal(dRight)
                        val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                        computePointAndNormal(dLeft)
                        val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy
                        drawLine(color = colorVibrantLayer, start = Offset(pxR + nxR * dist, pyR + nyR * dist), end = Offset(pxR + nxR * (dist + barLength), pyR + nyR * (dist + barLength)), strokeWidth = 8f, cap = StrokeCap.Round)
                        if (i != 0 && i != numBars - 1) {
                            drawLine(color = colorVibrantLayer, start = Offset(pxL + nxL * dist, pyL + nyL * dist), end = Offset(pxL + nxL * (dist + barLength), pyL + nyL * (dist + barLength)), strokeWidth = 8f, cap = StrokeCap.Round)
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
                    drawPath(wavePathR, color = colorVibrantLayer, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                VisualizerStyle.DOTS -> {
                    for (i in 0 until numBars) {
                        val amplitude = amps[i]
                        val dRight = 0f + i * distStep
                        val dLeft = pathLength - i * distStep
                        computePointAndNormal(dRight)
                        val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                        val dotCount = 1 + (amplitude * 8).toInt()
                        val dotSpacing = 16f
                        val dotRadius = 5f
                        for (j in 0 until dotCount) {
                            val currentDist = 30f + (j * dotSpacing)
                            val alphaVal = opacity * (1f - (j.toFloat() / 8f)).coerceAtLeast(0.3f)
                            drawCircle(color = layerColor.copy(alpha = alphaVal), radius = dotRadius, center = Offset(pxR + nxR * currentDist, pyR + nyR * currentDist))
                        }
                        if (i != 0 && i != numBars - 1) {
                            computePointAndNormal(dLeft)
                            val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy
                            for (j in 0 until dotCount) {
                                val currentDist = 30f + (j * dotSpacing)
                                val alphaVal = opacity * (1f - (j.toFloat() / 8f)).coerceAtLeast(0.3f)
                                drawCircle(color = layerColor.copy(alpha = alphaVal), radius = dotRadius, center = Offset(pxL + nxL * currentDist, pyL + nyL * currentDist))
                            }
                        }
                    }
                }
                VisualizerStyle.SLIME -> {
                    val totalPoints = (numBars * 2 - 2).coerceAtLeast(0)
                    val slimeX = FloatArray(totalPoints)
                    val slimeY = FloatArray(totalPoints)
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
                        drawPath(path = wavePathR, brush = Brush.radialGradient(colors = listOf(layerColor.copy(alpha = opacity), layerColor.copy(alpha = opacity * 0.5f)), center = center, radius = radius + 250f))
                        drawPath(path = wavePathR, color = layerColor.copy(alpha = opacity), style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                VisualizerStyle.PARTICLES -> {
                    for (i in 0 until numBars) {
                        val amplitude = amps[i]
                        val multiplicador = 1f + (i.toFloat() / numBars) * 1.5f
                        val boostedAmplitude = amplitude * multiplicador
                        val extrude = 20f + (boostedAmplitude * 300f)
                        val sz = 1f + (boostedAmplitude * 6f)
                        val dRight = 0f + i * distStep
                        val dLeft = pathLength - i * distStep
                        val timeMs = System.currentTimeMillis()
                        computePointAndNormal(dRight)
                        drawCircle(color = colorVibrantLayer, radius = sz, center = Offset(outPx + outNx * extrude, outPy + outNy * extrude))
                        if (boostedAmplitude > 0.1f) {
                            val sparkOffset = ((timeMs + i * 40) % 800) / 800f
                            val sparkExtrude = extrude - (sparkOffset * 50f)
                            drawCircle(color = colorVibrantLayer.copy(alpha = opacity * (1f - sparkOffset)), radius = sz * 0.5f, center = Offset(outPx + outNx * sparkExtrude, outPy + outNy * sparkExtrude))
                        }
                        if (i != 0 && i != numBars - 1) {
                            computePointAndNormal(dLeft)
                            drawCircle(color = colorVibrantLayer, radius = sz, center = Offset(outPx + outNx * extrude, outPy + outNy * extrude))
                            if (boostedAmplitude > 0.1f) {
                                val sparkOffset = ((timeMs + i * 40 + 400) % 800) / 800f
                                val sparkExtrude = extrude - (sparkOffset * 50f)
                                drawCircle(color = colorVibrantLayer.copy(alpha = opacity * (1f - sparkOffset)), radius = sz * 0.5f, center = Offset(outPx + outNx * sparkExtrude, outPy + outNy * sparkExtrude))
                            }
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
                    val dynamicRotation = rotationAngle + (bassAvg * 90f)
                    val dynamicFastRotation = fastRotationAngle - (midAvg * 90f)
                    fun drawGlitchRing(r: Float, thickness: Float, gapAngle: Float, startOffset: Float, brushColor: Color) {
                        val sweep = 360f / 4f - gapAngle
                        for (i in 0 until 4) {
                            drawArc(color = brushColor, startAngle = startOffset + (i * 90f), sweepAngle = sweep, useCenter = false, topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2), style = Stroke(width = thickness, cap = StrokeCap.Square))
                        }
                    }
                    if (visualizerArchetype == 1) {
                        val maxPulse = maxOf(bassAvg, midAvg, trebleAvg)
                        val combinedRadius = radius + 60f + (maxPulse * 80f)
                        drawGlitchRing(combinedRadius, 8f + (maxPulse * 15f), 20f, dynamicRotation, colorVibrant.copy(alpha = 0.8f))
                    } else {
                        val bassRadius = radius + 30f + (bassAvg * 80f)
                        val midRadius = radius + 60f + (midAvg * 70f)
                        val trebleRadius = radius + 90f + (trebleAvg * 60f)
                        drawGlitchRing(bassRadius, 8f + (bassAvg * 15f), 20f - (bassAvg * 10f), dynamicRotation, colorDominant.copy(alpha = 0.8f))
                        drawGlitchRing(midRadius, 4f + (midAvg * 10f), 30f, dynamicFastRotation, colorVibrant.copy(alpha = 0.6f))
                        drawGlitchRing(trebleRadius, 2f + (trebleAvg * 5f), 45f, dynamicRotation * 0.5f, colorMuted.copy(alpha = 0.5f))
                    }
                }
                VisualizerStyle.AURA -> {
                    if (visualizerArchetype == 1) {
                        val maxPulse = maxOf(bassAvg, midAvg, trebleAvg)
                        drawPath(path = basePath, color = colorVibrant.copy(alpha = 0.2f + 0.2f * maxPulse.coerceIn(0f, 1f)), style = Stroke(width = 60f + maxPulse * 150f, join = StrokeJoin.Round))
                    } else {
                        val bassPulse = (bassAvg * 1.5f).coerceIn(0f, 1f)
                        val midPulse = (midAvg * 1.5f).coerceIn(0f, 1f)
                        val treblePulse = (trebleAvg * 1.5f).coerceIn(0f, 1f)
                        drawPath(path = basePath, color = colorDominant.copy(alpha = 0.1f + 0.1f * bassPulse), style = Stroke(width = 80f + bassAvg * 200f, join = StrokeJoin.Round))
                        drawPath(path = basePath, color = colorVibrant.copy(alpha = 0.15f + 0.15f * midPulse), style = Stroke(width = 40f + midAvg * 100f, join = StrokeJoin.Round))
                        drawPath(path = basePath, color = colorMuted.copy(alpha = 0.25f + 0.25f * treblePulse), style = Stroke(width = 15f + trebleAvg * 50f, join = StrokeJoin.Round))
                    }
                }
                VisualizerStyle.BANDS -> { /* Drawn in background */ }
                else -> {}
            }
        } else {
            if (visualizerArchetype == 1) {
                drawLayer(combinedAmplitudes, paletteColors.vibrant, maxOf(bassOpacity, midOpacity, highOpacity), true)
            } else {
                drawLayer(bassAmplitudes, paletteColors.dominant, bassOpacity, true)
                drawLayer(midAmplitudes, paletteColors.vibrant, midOpacity, false)
                drawLayer(highAmplitudes, paletteColors.muted, highOpacity, false)
            }
        }

        // Progress ring
        val activePosition = dragSeekTimeMs ?: currentPosition
        val progressFraction = if (duration > 0) activePosition.toFloat() / duration else 0f
        drawPath(path = basePath, color = colorDominant.copy(alpha = 0.3f), style = Stroke(width = 4f))
        progressMeasure.setPath(basePath, forceClosed = false)
        val pLen = progressMeasure.length
        progressPath.reset()
        val targetLength = pLen * progressFraction
        if (targetLength > 0f) {
            val startD = if (thumbnailShapeIdx == 0) pLen * 0.75f else pLen * 0.125f
            val endD = startD + targetLength
            if (endD <= pLen) {
                progressMeasure.getSegment(startD, endD, progressPath, true)
            } else {
                progressMeasure.getSegment(startD, pLen, progressPath, true)
                progressMeasure.getSegment(0f, endD % pLen, progressPath, true)
            }
            drawPath(path = progressPath, brush = sweepGradient, style = Stroke(width = 6f, cap = StrokeCap.Round))
            val thumbDist = endD % pLen
            var thumbPos = progressMeasure.getPosition(thumbDist)
            if (thumbnailShapeIdx != 0 && (thumbPos == Offset.Unspecified || thumbPos == Offset.Zero)) {
                val localPosTan = FloatArray(2)
                if (androidPathMeasure.getPosTan(thumbDist, localPosTan, null)) {
                    thumbPos = Offset(localPosTan[0], localPosTan[1])
                }
            }
            if (thumbPos != Offset.Unspecified && thumbPos != Offset.Zero) {
                onPlayheadPosChanged(thumbPos)
                drawCircle(color = Color.White, radius = 8f, center = thumbPos)
            }
        }

        // A-B Repeat markers
        if (abRepeatModeEnabled) {
            val getPosFromProgress = { progress: Float ->
                val pl = progressMeasure.length
                if (pl <= 0f) center
                else {
                    val sd = if (thumbnailShapeIdx == 0) pl * 0.75f else pl * 0.125f
                    val dMod = (sd + progress * pl) % pl
                    val pos = progressMeasure.getPosition(dMod)
                    if (pos != Offset.Unspecified) pos else center
                }
            }
            val posA = getPosFromProgress(abPointA)
            val posB = getPosFromProgress(abPointB)
            val markerSizeA = if (activeDraggingHandle == "A") 16.dp.toPx() else 8.dp.toPx()
            val markerSizeB = if (activeDraggingHandle == "B") 16.dp.toPx() else 8.dp.toPx()
            if (thumbnailShapeIdx == 0) {
                drawCircle(color = colorVibrant, radius = markerSizeA, center = posA)
                drawCircle(color = colorVibrant.copy(alpha = 0.3f), radius = markerSizeA * 2, center = posA)
                drawCircle(color = colorMuted, radius = markerSizeB, center = posB)
                drawCircle(color = colorMuted.copy(alpha = 0.3f), radius = markerSizeB * 2, center = posB)
            } else {
                val cornerRadius = if (thumbnailShapeIdx == 2) 4.dp.toPx() else if (thumbnailShapeIdx == 3) 8.dp.toPx() else 0f
                drawRoundRect(color = colorVibrant, topLeft = Offset(posA.x - markerSizeA, posA.y - markerSizeA), size = Size(markerSizeA * 2, markerSizeA * 2), cornerRadius = CornerRadius(cornerRadius, cornerRadius))
                drawRoundRect(color = colorVibrant.copy(alpha = 0.3f), topLeft = Offset(posA.x - markerSizeA * 2, posA.y - markerSizeA * 2), size = Size(markerSizeA * 4, markerSizeA * 4), cornerRadius = CornerRadius(cornerRadius * 2, cornerRadius * 2))
                drawRoundRect(color = colorMuted, topLeft = Offset(posB.x - markerSizeB, posB.y - markerSizeB), size = Size(markerSizeB * 2, markerSizeB * 2), cornerRadius = CornerRadius(cornerRadius, cornerRadius))
                drawRoundRect(color = colorMuted.copy(alpha = 0.3f), topLeft = Offset(posB.x - markerSizeB * 2, posB.y - markerSizeB * 2), size = Size(markerSizeB * 4, markerSizeB * 4), cornerRadius = CornerRadius(cornerRadius * 2, cornerRadius * 2))
            }
        }
    }
}
