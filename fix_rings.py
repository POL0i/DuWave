import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt"
with open(file_path, "r", encoding='utf-8') as f:
    content = f.read()

anchor = "                if (visualizerArchetype == 1) {"

if anchor in content:
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
                } else {
                    if (visualizerArchetype == 1) {
"""
    content = content.replace("                if (visualizerArchetype == 1) {\n                    // 1 Onda Combinada", new_rings + "                    // 1 Onda Combinada")
    with open(file_path, "w", encoding='utf-8') as f:
        f.write(content)
    print("Injected RINGS and AURA block before visualizerArchetype check.")
else:
    print("Failed to find anchor.")
