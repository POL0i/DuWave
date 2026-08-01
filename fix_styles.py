import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt"
with open(file_path, "r", encoding='utf-8') as f:
    content = f.read()

# First replace DOTS block to include SLIME and PARTICLES
dots_start = content.find("VisualizerStyle.DOTS -> {")
dots_end = content.find("                        else -> {}", dots_start)

if dots_start != -1 and dots_end != -1:
    old_dots = content[dots_start:dots_end]
    new_dots = old_dots + """                        VisualizerStyle.SLIME -> {
                            val totalPoints = numBars * 2
                            val slimeX = FloatArray(totalPoints)
                            val slimeY = FloatArray(totalPoints)
                            
                            for (i in 0 until totalPoints) {
                                val isRightSide = i < numBars
                                val ampIndex = if (isRightSide) i else (totalPoints - 1 - i)
                                val amplitude = amps[ampIndex]

                                val offsetDist = if (isRightSide) {
                                    0f + ampIndex * distStep
                                } else {
                                    pathLength - ampIndex * distStep
                                }

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

                                drawPath(
                                    path = wavePathR,
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(layerColor.copy(alpha = opacity), layerColor.copy(alpha = opacity * 0.5f)),
                                        center = center,
                                        radius = radius + 250f
                                    )
                                )
                                drawPath(path = wavePathR, color = layerColor.copy(alpha = opacity), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                        }
                        VisualizerStyle.PARTICLES -> {
                            for (i in 0 until numBars) {
                                val amplitude = amps[i]
                                val multiplicador = 1f + (i.toFloat() / numBars) * 1.5f
                                val boostedAmplitude = amplitude * multiplicador

                                val extrude = 20f + (boostedAmplitude * 300f)
                                val size = 2f + (boostedAmplitude * 15f)

                                val dRight = 0f + i * distStep
                                val dLeft = pathLength - i * distStep

                                computePointAndNormal(dRight)
                                drawCircle(color = colorVibrantLayer, radius = size, center = androidx.compose.ui.geometry.Offset(outPx + outNx * extrude, outPy + outNy * extrude))
                                
                                computePointAndNormal(dLeft)
                                drawCircle(color = colorVibrantLayer, radius = size, center = androidx.compose.ui.geometry.Offset(outPx + outNx * extrude, outPy + outNy * extrude))
                            }
                        }
"""
    content = content.replace(old_dots, new_dots)
    print("Injected SLIME and PARTICLES into drawLayer.")
else:
    print("Failed to find DOTS block.")


# Second, replace the RINGS and AURA block
rings_block_start = content.find("                if (currentStyle == VisualizerStyle.RINGS || currentStyle == VisualizerStyle.AURA) {")
if rings_block_start != -1:
    rings_block_end = content.find("                } else {", rings_block_start)
    if rings_block_end != -1:
        old_rings = content[rings_block_start:rings_block_end]
        new_rings = """                if (currentStyle == VisualizerStyle.RINGS || currentStyle == VisualizerStyle.AURA) {
                    when (currentStyle) {
                        VisualizerStyle.RINGS -> {
                            val dynamicRotation = rotationAngle + (bassAvg * 90f)
                            val dynamicFastRotation = fastRotationAngle - (midAvg * 90f)

                            fun drawGlitchRing(r: Float, thickness: Float, gapAngle: Float, startOffset: Float, brushColor: Color) {
                                val sweep = 360f / 4f - gapAngle
                                for (i in 0 until 4) {
                                    drawArc(
                                        color = brushColor, startAngle = startOffset + (i * 90f), sweepAngle = sweep,
                                        useCenter = false, topLeft = androidx.compose.ui.geometry.Offset(center.x - r, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = thickness, cap = androidx.compose.ui.graphics.StrokeCap.Square)
                                    )
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
                                drawPath(
                                    path = basePath,
                                    color = colorVibrant.copy(alpha = 0.2f + 0.2f * maxPulse.coerceIn(0f, 1f)),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 60f + maxPulse * 150f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                )
                            } else {
                                val bassPulse = (bassAvg * 1.5f).coerceIn(0f, 1f)
                                val midPulse = (midAvg * 1.5f).coerceIn(0f, 1f)
                                val treblePulse = (trebleAvg * 1.5f).coerceIn(0f, 1f)

                                drawPath(
                                    path = basePath,
                                    color = colorDominant.copy(alpha = 0.1f + 0.1f * bassPulse),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 80f + bassAvg * 200f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                )
                                drawPath(
                                    path = basePath,
                                    color = colorVibrant.copy(alpha = 0.15f + 0.15f * midPulse),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 40f + midAvg * 100f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                )
                                drawPath(
                                    path = basePath,
                                    color = colorMuted.copy(alpha = 0.25f + 0.25f * treblePulse),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 15f + trebleAvg * 50f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                )
                            }
                        }
                        else -> {}
                    }
"""
        content = content.replace(old_rings, new_rings)
        print("Injected RINGS and AURA logic.")
    else:
        print("Failed to find end of RINGS block.")
else:
    print("Failed to find RINGS block.")

with open(file_path, "w", encoding='utf-8') as f:
    f.write(content)
