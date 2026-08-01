import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

new_canvas_block = """            Canvas(modifier = Modifier.size(320.dp).scale(animatedScale)) {
                val bassAmplitudes = bassAmplitudesState.value
                val midAmplitudes = midAmplitudesState.value
                val highAmplitudes = highAmplitudesState.value
                val ghosts = ghostsState.value

                val radius = size.minDimension / 4f
                val center = Offset(size.width / 2, size.height / 2)

                val coverSize = 160.dp.toPx()
                val rPx = coverSize / 2f
                if (size != lastSize || thumbnailShapeIdx != lastShape) {
                    lastSize = size
                    lastShape = thumbnailShapeIdx
                    basePath.reset()
                    val rect = androidx.compose.ui.geometry.Rect(center.x - rPx, center.y - rPx, center.x + rPx, center.y + rPx)
                    when (thumbnailShapeIdx) {
                        0 -> basePath.addOval(rect)
                        1 -> basePath.addRect(rect)
                        2 -> basePath.addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())))
                        3 -> basePath.addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, androidx.compose.ui.geometry.CornerRadius(32.dp.toPx())))
                        else -> basePath.addOval(rect)
                    }
                    androidPathMeasure.setPath(basePath.asAndroidPath(), false)
                    pathLength = when (thumbnailShapeIdx) {
                        0 -> 2f * Math.PI.toFloat() * rPx
                        1 -> 8f * rPx
                        2, 3 -> {
                            val cornerRadius = if (thumbnailShapeIdx == 2) 16.dp.toPx() else 32.dp.toPx()
                            val straightEdge = 2f * (rPx - cornerRadius)
                            val cornerLen = (Math.PI.toFloat() * cornerRadius) / 2f
                            4f * straightEdge + 4f * cornerLen
                        }
                        else -> 2f * Math.PI.toFloat() * rPx
                    }
                }

                var outPx = 0f
                var outPy = 0f
                var outNx = 0f
                var outNy = 0f

                fun computePointAndNormal(dist: Float) {
                    val d = (dist % pathLength + pathLength) % pathLength
                    if (thumbnailShapeIdx == 0) { 
                        val angle = (d / pathLength) * Math.PI * 2.0 - Math.PI / 2.0
                        outPx = center.x + rPx * kotlin.math.cos(angle).toFloat()
                        outPy = center.y + rPx * kotlin.math.sin(angle).toFloat()
                        outNx = kotlin.math.cos(angle).toFloat()
                        outNy = kotlin.math.sin(angle).toFloat()
                        return
                    }
                    if (thumbnailShapeIdx == 1) { 
                        var x = 0f; var y = 0f; var nx = 0f; var ny = 0f
                        if (d < rPx) { x = d; y = -rPx; nx = 0f; ny = -1f } 
                        else if (d < 3f * rPx) { x = rPx; y = d - 2f * rPx; nx = 1f; ny = 0f } 
                        else if (d < 5f * rPx) { x = 4f * rPx - d; y = rPx; nx = 0f; ny = 1f } 
                        else if (d < 7f * rPx) { x = -rPx; y = 6f * rPx - d; nx = -1f; ny = 0f } 
                        else { x = d - 8f * rPx; y = -rPx; nx = 0f; ny = -1f }
                        outPx = center.x + x; outPy = center.y + y; outNx = nx; outNy = ny
                        return
                    }
                    val cornerRadius = if (thumbnailShapeIdx == 2) 16.dp.toPx() else 32.dp.toPx()
                    val straightEdge = 2f * (rPx - cornerRadius)
                    val cornerLen = (Math.PI.toFloat() * cornerRadius) / 2f
                    var x = 0f; var y = 0f; var nx = 0f; var ny = 0f
                    var remaining = d
                    val halfTop = straightEdge / 2f
                    if (remaining <= halfTop) { x = remaining; y = -rPx; nx = 0f; ny = -1f } else {
                        remaining -= halfTop
                        if (remaining <= cornerLen) {
                            val angle = -Math.PI / 2.0 + (remaining / cornerLen) * (Math.PI / 2.0)
                            x = rPx - cornerRadius + cornerRadius * kotlin.math.cos(angle).toFloat()
                            y = -rPx + cornerRadius + cornerRadius * kotlin.math.sin(angle).toFloat()
                            nx = kotlin.math.cos(angle).toFloat(); ny = kotlin.math.sin(angle).toFloat()
                        } else {
                            remaining -= cornerLen
                            if (remaining <= straightEdge) { x = rPx; y = -rPx + cornerRadius + remaining; nx = 1f; ny = 0f } else {
                                remaining -= straightEdge
                                if (remaining <= cornerLen) {
                                    val angle = 0.0 + (remaining / cornerLen) * (Math.PI / 2.0)
                                    x = rPx - cornerRadius + cornerRadius * kotlin.math.cos(angle).toFloat()
                                    y = rPx - cornerRadius + cornerRadius * kotlin.math.sin(angle).toFloat()
                                    nx = kotlin.math.cos(angle).toFloat(); ny = kotlin.math.sin(angle).toFloat()
                                } else {
                                    remaining -= cornerLen
                                    if (remaining <= straightEdge) { x = rPx - cornerRadius - remaining; y = rPx; nx = 0f; ny = 1f } else {
                                        remaining -= straightEdge
                                        if (remaining <= cornerLen) {
                                            val angle = Math.PI / 2.0 + (remaining / cornerLen) * (Math.PI / 2.0)
                                            x = -rPx + cornerRadius + cornerRadius * kotlin.math.cos(angle).toFloat()
                                            y = rPx - cornerRadius + cornerRadius * kotlin.math.sin(angle).toFloat()
                                            nx = kotlin.math.cos(angle).toFloat(); ny = kotlin.math.sin(angle).toFloat()
                                        } else {
                                            remaining -= cornerLen
                                            if (remaining <= straightEdge) { x = -rPx; y = rPx - cornerRadius - remaining; nx = -1f; ny = 0f } else {
                                                remaining -= straightEdge
                                                if (remaining <= cornerLen) {
                                                    val angle = Math.PI + (remaining / cornerLen) * (Math.PI / 2.0)
                                                    x = -rPx + cornerRadius + cornerRadius * kotlin.math.cos(angle).toFloat()
                                                    y = -rPx + cornerRadius + cornerRadius * kotlin.math.sin(angle).toFloat()
                                                    nx = kotlin.math.cos(angle).toFloat(); ny = kotlin.math.sin(angle).toFloat()
                                                } else {
                                                    remaining -= cornerLen
                                                    x = -rPx + cornerRadius + remaining; y = -rPx; nx = 0f; ny = -1f
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    outPx = center.x + x; outPy = center.y + y; outNx = nx; outNy = ny
                }

                val maxAnim = maxOf(bassAvg, midAvg, trebleAvg, 0.001f)
                val bassOpacity = (bassAvg / maxAnim).coerceIn(0.2f, 1.0f)
                val midOpacity = (midAvg / maxAnim).coerceIn(0.2f, 1.0f)
                val highOpacity = (trebleAvg / maxAnim).coerceIn(0.2f, 1.0f)

                fun drawLayer(amps: FloatArray, layerColor: Color, opacity: Float, isBassLayer: Boolean) {
                    val numBars = amps.size
                    if (numBars == 0) return
                    val distStep = (pathLength / 2f) / (numBars - 1).coerceAtLeast(1).toFloat()
                    
                    val layerBrush = Brush.sweepGradient(
                        colors = listOf(
                            layerColor.copy(alpha = opacity),
                            layerColor.copy(alpha = opacity * 0.5f),
                            layerColor.copy(alpha = opacity),
                            layerColor.copy(alpha = opacity * 0.8f),
                            layerColor.copy(alpha = opacity)
                        )
                    )

                    // Glow gradient for Paths (WAVE, SLIME)
                    val glowBrush = Brush.radialGradient(
                        colors = listOf(
                            layerColor.copy(alpha = opacity * 0.3f), // Base transparent
                            layerColor.copy(alpha = 1.0f) // Fully glowing tips
                        ),
                        center = center,
                        radius = radius + 170f
                    )
                    
                    val colorVibrantLocal = layerColor.copy(alpha = opacity)
                    val colorDominantLocal = layerColor.copy(alpha = opacity * 0.8f)
                    val colorMutedLocal = layerColor.copy(alpha = opacity * 0.5f)

                    when (currentStyle) {
                        VisualizerStyle.SLIME -> {
                            val totalPoints = numBars * 2
                            for (i in 0 until totalPoints) {
                                val isRightSide = i < numBars
                                val ampIndex = if (isRightSide) i else (totalPoints - 1 - i)
                                val amplitude = amps[ampIndex]
                                val offsetDist = if (isRightSide) 0f + ampIndex * distStep else pathLength - ampIndex * distStep
                                computePointAndNormal(offsetDist)
                                val extrude = 5f + (amplitude * 150f)
                                slimeX[i] = outPx + outNx * extrude
                                slimeY[i] = outPy + outNy * extrude
                            }

                            sharedPath.reset()
                            if (totalPoints > 0) {
                                var prevMidX = (slimeX[0] + slimeX[totalPoints - 1]) / 2f
                                var prevMidY = (slimeY[0] + slimeY[totalPoints - 1]) / 2f
                                sharedPath.moveTo(prevMidX, prevMidY)
                                for (i in 0 until totalPoints) {
                                    val nextIndex = (i + 1) % totalPoints
                                    val midX = (slimeX[i] + slimeX[nextIndex]) / 2f
                                    val midY = (slimeY[i] + slimeY[nextIndex]) / 2f
                                    sharedPath.quadraticTo(slimeX[i], slimeY[i], midX, midY)
                                }
                                sharedPath.close()
                                drawPath(path = sharedPath, brush = Brush.radialGradient(listOf(colorDominantLocal, colorVibrantLocal.copy(alpha = opacity * 0.8f)), center, radius + 250f))
                                // Glow applied here
                                drawPath(path = sharedPath, brush = glowBrush, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }
                        VisualizerStyle.PARTICLES -> {
                            for (i in 0 until numBars) {
                                val amplitude = amps[i]
                                val localTipOpacity = (opacity + amplitude * 1.5f).coerceIn(0f, 1f)
                                val glowingColor = layerColor.copy(alpha = localTipOpacity)

                                val multiplicador = 1f + (i.toFloat() / numBars) * 1.5f
                                val boostedAmplitude = amplitude * multiplicador
                                val distFromEdge = 10f + (boostedAmplitude * 90f)
                                val dRight = 0f + i * distStep
                                val dLeft = pathLength - i * distStep

                                computePointAndNormal(dRight); val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                                computePointAndNormal(dLeft); val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy

                                val mainRadius = 4f + (amplitude * 6f)
                                // Glow applied to particles
                                drawCircle(color = glowingColor, radius = mainRadius, center = Offset(pxR + nxR * distFromEdge, pyR + nyR * distFromEdge))
                                drawCircle(color = glowingColor, radius = mainRadius, center = Offset(pxL + nxL * distFromEdge, pyL + nyL * distFromEdge))

                                val trail1Dist = distFromEdge * 0.6f
                                drawCircle(color = colorDominantLocal.copy(alpha = opacity * 0.6f), radius = mainRadius * 0.7f, center = Offset(pxR + nxR * trail1Dist, pyR + nyR * trail1Dist))
                                drawCircle(color = colorDominantLocal.copy(alpha = opacity * 0.6f), radius = mainRadius * 0.7f, center = Offset(pxL + nxL * trail1Dist, pyL + nyL * trail1Dist))

                                val trail2Dist = distFromEdge * 0.3f
                                drawCircle(color = colorMutedLocal.copy(alpha = opacity * 0.3f), radius = mainRadius * 0.4f, center = Offset(pxR + nxR * trail2Dist, pyR + nyR * trail2Dist))
                                drawCircle(color = colorMutedLocal.copy(alpha = opacity * 0.3f), radius = mainRadius * 0.4f, center = Offset(pxL + nxL * trail2Dist, pyL + nyL * trail2Dist))
                            }
                        }
                        VisualizerStyle.WAVE -> {
                            wavePath.reset()
                            for (i in 0 until numBars) {
                                val amplitude = amps[i]
                                val dRight = 0f + i * distStep
                                computePointAndNormal(dRight)
                                val extrude = 20f + (amplitude * 150f)
                                val px = outPx + outNx * extrude
                                val py = outPy + outNy * extrude
                                if (i == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
                            }
                            for (i in numBars - 1 downTo 0) {
                                val amplitude = amps[i]
                                val dLeft = pathLength - i * distStep
                                computePointAndNormal(dLeft)
                                val extrude = 20f + (amplitude * 150f)
                                wavePath.lineTo(outPx + outNx * extrude, outPy + outNy * extrude)
                            }
                            wavePath.close()
                            drawPath(path = wavePath, brush = Brush.radialGradient(listOf(colorVibrantLocal.copy(alpha = opacity * 0.3f), Color.Transparent), center, radius + 150f))
                            // Glow applied to wave stroke
                            drawPath(path = wavePath, brush = glowBrush, style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        VisualizerStyle.BARS -> {
                            for (i in 0 until numBars) {
                                val amplitude = amps[i]
                                val localTipOpacity = (opacity + amplitude * 1.5f).coerceIn(0f, 1f)
                                val glowingColor = layerColor.copy(alpha = localTipOpacity)

                                val barLength = 10f + (amplitude * 200f)
                                val dRight = 0f + i * distStep
                                val dLeft = pathLength - i * distStep
                                
                                computePointAndNormal(dRight); val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                                computePointAndNormal(dLeft); val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy
                                
                                // Base bar
                                drawLine(brush = layerBrush, start = Offset(pxR + nxR * 20f, pyR + nyR * 20f), end = Offset(pxR + nxR * (20f + barLength), pyR + nyR * (20f + barLength)), strokeWidth = 8f, cap = StrokeCap.Round)
                                drawLine(brush = layerBrush, start = Offset(pxL + nxL * 20f, pyL + nyL * 20f), end = Offset(pxL + nxL * (20f + barLength), pyL + nyL * (20f + barLength)), strokeWidth = 8f, cap = StrokeCap.Round)

                                // Glowing tip
                                drawCircle(color = glowingColor, radius = 4f, center = Offset(pxR + nxR * (20f + barLength), pyR + nyR * (20f + barLength)))
                                drawCircle(color = glowingColor, radius = 4f, center = Offset(pxL + nxL * (20f + barLength), pyL + nyL * (20f + barLength)))
                            }
                        }
                        VisualizerStyle.DOTS -> {
                            for (i in 0 until numBars) {
                                val amplitude = amps[i]
                                val localTipOpacity = (opacity + amplitude * 1.5f).coerceIn(0f, 1f)
                                val glowingColor = layerColor.copy(alpha = localTipOpacity)

                                val dist = 30f + (amplitude * 180f)
                                val capLen = 8f + (amplitude * 30f)
                                val ghostAmp = if (i < ghosts.size) ghosts[i] else 0f
                                val ghostDist = 30f + (ghostAmp * 180f)
                                
                                val dRight = 0f + i * distStep
                                val dLeft = pathLength - i * distStep
                                
                                computePointAndNormal(dRight); val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                                computePointAndNormal(dLeft); val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy
                                
                                drawLine(color = colorVibrantLocal, start = Offset(pxR + nxR * dist, pyR + nyR * dist), end = Offset(pxR + nxR * (dist + capLen), pyR + nyR * (dist + capLen)), strokeWidth = 6f + amplitude*4f, cap = StrokeCap.Round)
                                drawLine(color = colorVibrantLocal, start = Offset(pxL + nxL * dist, pyL + nyL * dist), end = Offset(pxL + nxL * (dist + capLen), pyL + nyL * (dist + capLen)), strokeWidth = 6f + amplitude*4f, cap = StrokeCap.Round)
                                
                                // Glowing tip (on the outer edge)
                                drawCircle(color = glowingColor, radius = 3f + amplitude*2f, center = Offset(pxR + nxR * (dist + capLen), pyR + nyR * (dist + capLen)))
                                drawCircle(color = glowingColor, radius = 3f + amplitude*2f, center = Offset(pxL + nxL * (dist + capLen), pyL + nyL * (dist + capLen)))

                                if (ghostAmp > amplitude + 0.05f) {
                                    drawLine(color = colorVibrantLocal.copy(alpha=opacity*0.3f), start = Offset(pxR + nxR * ghostDist, pyR + nyR * ghostDist), end = Offset(pxR + nxR * (ghostDist + 8f), pyR + nyR * (ghostDist + 8f)), strokeWidth = 6f + amplitude*4f, cap = StrokeCap.Round)
                                    drawLine(color = colorVibrantLocal.copy(alpha=opacity*0.3f), start = Offset(pxL + nxL * ghostDist, pyL + nyL * ghostDist), end = Offset(pxL + nxL * (ghostDist + 8f), pyL + nyL * (ghostDist + 8f)), strokeWidth = 6f + amplitude*4f, cap = StrokeCap.Round)
                                }
                            }
                        }
                        else -> {}
                    }
                }

                if (currentStyle == VisualizerStyle.RINGS) {
                    val bassRadius = radius + 30f + (bassAvg * 80f)
                    val midRadius = radius + 60f + (midAvg * 70f)
                    val trebleRadius = radius + 90f + (trebleAvg * 60f)
                    val dynamicRotation = rotationAngle + (bassAvg * 90f)
                    val dynamicFastRotation = fastRotationAngle - (midAvg * 90f)
                    fun drawGlitchRing(r: Float, thickness: Float, gapAngle: Float, startOffset: Float, brushColor: Color) {
                        val sweep = 360f / 4f - gapAngle
                        for (i in 0 until 4) {
                            drawArc(
                                color = brushColor, startAngle = startOffset + (i * 90f), sweepAngle = sweep,
                                useCenter = false, topLeft = Offset(center.x - r, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                                style = Stroke(width = thickness, cap = StrokeCap.Square)
                            )
                        }
                    }
                    drawGlitchRing(bassRadius, 8f + (bassAvg * 15f), 20f - (bassAvg * 10f), dynamicRotation, colorDominant.copy(alpha = 0.8f + bassAvg * 0.2f))
                    drawGlitchRing(midRadius, 4f + (midAvg * 10f), 30f, dynamicFastRotation, colorVibrant.copy(alpha = 0.6f + midAvg * 0.4f))
                    drawGlitchRing(trebleRadius, 2f + (trebleAvg * 5f), 45f, dynamicRotation * 0.5f, colorMuted.copy(alpha = 0.5f + trebleAvg * 0.5f))
                } else if (currentStyle == VisualizerStyle.AURA) {
                    val bassPulse = (bassAvg * 1.5f).coerceIn(0f, 1f)
                    val midPulse = (midAvg * 1.5f).coerceIn(0f, 1f)
                    val treblePulse = (trebleAvg * 1.5f).coerceIn(0f, 1f)
                    drawPath(path = basePath, color = colorDominant.copy(alpha = 0.1f + 0.1f * bassPulse), style = Stroke(width = 80f + bassAvg * 200f, join = StrokeJoin.Round))
                    drawPath(path = basePath, color = colorVibrant.copy(alpha = 0.15f + 0.15f * midPulse), style = Stroke(width = 40f + midAvg * 100f, join = StrokeJoin.Round))
                    drawPath(path = basePath, color = colorMuted.copy(alpha = 0.25f + 0.25f * treblePulse), style = Stroke(width = 15f + trebleAvg * 50f, join = StrokeJoin.Round))
                } else {
                    if (bassAmplitudes.isNotEmpty()) drawLayer(bassAmplitudes, paletteColors.dominant, bassOpacity, true)
                    if (midAmplitudes.isNotEmpty()) drawLayer(midAmplitudes, paletteColors.vibrant, midOpacity, false)
                    if (highAmplitudes.isNotEmpty()) drawLayer(highAmplitudes, paletteColors.muted, highOpacity, false)
                }

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
                    val thumbPos = progressMeasure.getPosition(thumbDist)
                    if (thumbPos != androidx.compose.ui.geometry.Offset.Unspecified) {
                        playheadPos = thumbPos
                        drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 8f, center = thumbPos)
                    }
                }

                val iterator = sparks.iterator()
                while (iterator.hasNext()) {
                    val spark = iterator.next()
                    spark.x += spark.vx
                    spark.y += spark.vy
                    spark.alpha -= 0.03f
                    if (spark.alpha <= 0f) {
                        iterator.remove()
                    } else {
                        drawCircle(color = spark.color.copy(alpha = spark.alpha), radius = 4f, center = Offset(spark.x, spark.y))
                    }
                }
            }"""

start_pattern = r'Canvas\(modifier = Modifier\.size\(320\.dp\)\.scale\(animatedScale\)\) \{'
end_pattern = r'\s*// Central Album Art'

match = re.search(start_pattern + r'.*?' + end_pattern, content, re.DOTALL)
if match:
    old_canvas_block = match.group(0).replace("// Central Album Art", "").rstrip()
    content = content.replace(old_canvas_block, new_canvas_block)
    
    with open(file_path, "w") as f:
        f.write(content)
    print("Replaced Canvas block successfully with glowing peaks")
else:
    print("Could not find Canvas block")
