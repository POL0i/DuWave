import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt"
with open(file_path, "r", encoding='utf-8') as f:
    content = f.read()

# Replace amplitudes flow with 3 flows
old_flow = """    val amplitudesState = visualizerManager.amplitudes.collectAsState()"""
new_flow = """    val bassAmplitudesState = visualizerManager.bassAmplitudes.collectAsState()
    val midAmplitudesState = visualizerManager.midAmplitudes.collectAsState()
    val highAmplitudesState = visualizerManager.highAmplitudes.collectAsState()"""
content = content.replace(old_flow, new_flow)

# Replace LaunchedEffect
old_effect = """            LaunchedEffect(visualizerManager.amplitudes) {
                visualizerManager.amplitudes.collect { amps ->
                    if (amps.isNotEmpty()) {
                        val rawBass = amps.take(amps.size / 3).average().toFloat().let { if(it.isNaN()) 0f else it }
                        val rawMid = amps.drop(amps.size / 3).take(amps.size / 3).average().toFloat().let { if(it.isNaN()) 0f else it }
                        val rawTreble = amps.takeLast(amps.size / 3).average().toFloat().let { if(it.isNaN()) 0f else it }
                        
                        launch { bassAvgAnim.animateTo(rawBass, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        launch { midAvgAnim.animateTo(rawMid, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        launch { trebleAvgAnim.animateTo(rawTreble, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        
                        val bassIntensity = rawBass.coerceIn(0f, 1f)
                        launch { animatedScaleAnim.animateTo(1f + (bassIntensity * 0.45f), androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)) }
                    }
                }
            }"""

new_effect = """            LaunchedEffect(visualizerManager.bassAmplitudes, visualizerManager.midAmplitudes, visualizerManager.highAmplitudes) {
                launch {
                    visualizerManager.bassAmplitudes.collect { amps ->
                        if (amps.isNotEmpty()) {
                            val rawBass = amps.average().toFloat().let { if(it.isNaN()) 0f else it }
                            launch { bassAvgAnim.animateTo(rawBass, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                            val bassIntensity = rawBass.coerceIn(0f, 1f)
                            launch { animatedScaleAnim.animateTo(1f + (bassIntensity * 0.45f), androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)) }
                        }
                    }
                }
                launch {
                    visualizerManager.midAmplitudes.collect { amps ->
                        if (amps.isNotEmpty()) {
                            val rawMid = amps.average().toFloat().let { if(it.isNaN()) 0f else it }
                            launch { midAvgAnim.animateTo(rawMid, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        }
                    }
                }
                launch {
                    visualizerManager.highAmplitudes.collect { amps ->
                        if (amps.isNotEmpty()) {
                            val rawTreble = amps.average().toFloat().let { if(it.isNaN()) 0f else it }
                            launch { trebleAvgAnim.animateTo(rawTreble, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        }
                    }
                }
            }"""
content = content.replace(old_effect, new_effect)


# Replace the canvas code from line 558 to 680
import re
start_str = "                val amplitudes = amplitudesState.value"
end_str = """                        else -> {}
                    }
                }"""

s_idx = content.find(start_str)
e_idx = content.find(end_str, s_idx)
if s_idx != -1 and e_idx != -1:
    e_idx += len(end_str)
    old_canvas_inner = content[s_idx:e_idx]
    
    new_canvas_inner = """                val bassAmplitudes = bassAmplitudesState.value
                val midAmplitudes = midAmplitudesState.value
                val highAmplitudes = highAmplitudesState.value

                val maxAnim = maxOf(bassAvg, midAvg, trebleAvg, 0.001f)
                val bassOpacity = (bassAvg / maxAnim).coerceIn(0.2f, 1.0f)
                val midOpacity = (midAvg / maxAnim).coerceIn(0.2f, 1.0f)
                val highOpacity = (trebleAvg / maxAnim).coerceIn(0.2f, 1.0f)

                val radius = size.minDimension / 4f
                val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

                val coverSize = 160.dp.toPx()
                val rPx = coverSize / 2f
                val pathLength = rPx * 2f * Math.PI.toFloat()

                if (size != lastSize || thumbnailShapeIdx != lastShape) {
                    basePath.reset()
                    if (thumbnailShapeIdx == 0) {
                        basePath.addOval(androidx.compose.ui.geometry.Rect(center.x - rPx, center.y - rPx, center.x + rPx, center.y + rPx))
                    } else if (thumbnailShapeIdx == 1) {
                        basePath.addRect(androidx.compose.ui.geometry.Rect(center.x - rPx, center.y - rPx, center.x + rPx, center.y + rPx))
                    } else if (thumbnailShapeIdx == 2) {
                        basePath.addRoundRect(androidx.compose.ui.geometry.RoundRect(
                            left = center.x - rPx, top = center.y - rPx, right = center.x + rPx, bottom = center.y + rPx,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx(), 32.dp.toPx())
                        ))
                    }
                    progressMeasure.setPath(basePath, forceClosed = false)
                    lastSize = size
                    lastShape = thumbnailShapeIdx
                }

                fun drawLayer(amps: FloatArray, layerColor: Color, opacity: Float, isBassLayer: Boolean) {
                    val numBars = amps.size
                    if (numBars == 0 || currentStyle == VisualizerStyle.RINGS || currentStyle == VisualizerStyle.AURA) return
                    val distStep = pathLength / numBars
                    var outPx = 0f; var outPy = 0f; var outNx = 0f; var outNy = 0f
                    
                    fun computePointAndNormal(d: Float) {
                        if (thumbnailShapeIdx == 0) {
                            val angle = (d / pathLength) * 2 * Math.PI - Math.PI / 2
                            outNx = kotlin.math.cos(angle).toFloat()
                            outNy = kotlin.math.sin(angle).toFloat()
                            outPx = center.x + rPx * outNx
                            outPy = center.y + rPx * outNy
                        } else {
                            val pos = progressMeasure.getPosition(d % pathLength)
                            val tan = progressMeasure.getTangent(d % pathLength)
                            if (pos != androidx.compose.ui.geometry.Offset.Unspecified && tan != androidx.compose.ui.geometry.Offset.Unspecified) {
                                outPx = pos.x
                                outPy = pos.y
                                outNx = tan.y
                                outNy = -tan.x
                                val len = kotlin.math.hypot(outNx, outNy)
                                if (len > 0) { outNx /= len; outNy /= len }
                            } else {
                                outPx = center.x; outPy = center.y; outNx = 0f; outNy = -1f
                            }
                        }
                    }

                    val colorVibrantLayer = layerColor.copy(alpha = opacity)

                    when (currentStyle) {
                        VisualizerStyle.BARS, VisualizerStyle.WAVE -> {
                            val isWave = currentStyle == VisualizerStyle.WAVE
                            if (isWave) {
                                wavePathR.reset()
                                wavePathL.reset()
                            }

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
                                
                                if (isWave) {
                                    val rX = pxR + nxR * dist; val rY = pyR + nyR * dist
                                    val lX = pxL + nxL * dist; val lY = pyL + nyL * dist
                                    if (i == 0) {
                                        wavePathR.moveTo(rX, rY); wavePathL.moveTo(lX, lY)
                                    } else {
                                        wavePathR.lineTo(rX, rY); wavePathL.lineTo(lX, lY)
                                    }
                                } else {
                                    drawLine(color = colorVibrantLayer, start = androidx.compose.ui.geometry.Offset(pxR + nxR * dist, pyR + nyR * dist),
                                             end = androidx.compose.ui.geometry.Offset(pxR + nxR * (dist + barLength), pyR + nyR * (dist + barLength)), strokeWidth = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    drawLine(color = colorVibrantLayer, start = androidx.compose.ui.geometry.Offset(pxL + nxL * dist, pyL + nyL * dist),
                                             end = androidx.compose.ui.geometry.Offset(pxL + nxL * (dist + barLength), pyL + nyL * (dist + barLength)), strokeWidth = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                }
                            }

                            if (isWave) {
                                drawPath(wavePathR, color = colorVibrantLayer, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                                drawPath(wavePathL, color = colorVibrantLayer, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                        }
                        VisualizerStyle.DOTS -> {
                            for (i in 0 until numBars) {
                                val amplitude = amps[i]
                                val dist = 30f + (amplitude * 180f)
                                val capLen = 8f + (amplitude * 30f)

                                val dRight = 0f + i * distStep
                                val dLeft = pathLength - i * distStep

                                computePointAndNormal(dRight)
                                val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                                computePointAndNormal(dLeft)
                                val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy

                                drawLine(color = colorVibrantLayer, start = androidx.compose.ui.geometry.Offset(pxR + nxR * dist, pyR + nyR * dist),
                                         end = androidx.compose.ui.geometry.Offset(pxR + nxR * (dist + capLen), pyR + nyR * (dist + capLen)), strokeWidth = 6f + amplitude*4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                drawLine(color = colorVibrantLayer, start = androidx.compose.ui.geometry.Offset(pxL + nxL * dist, pyL + nyL * dist),
                                         end = androidx.compose.ui.geometry.Offset(pxL + nxL * (dist + capLen), pyL + nyL * (dist + capLen)), strokeWidth = 6f + amplitude*4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)

                                drawCircle(color = colorVibrantLayer, radius = 3f + amplitude*2f, center = androidx.compose.ui.geometry.Offset(pxR + nxR * (dist + capLen), pyR + nyR * (dist + capLen)))
                                drawCircle(color = colorVibrantLayer, radius = 3f + amplitude*2f, center = androidx.compose.ui.geometry.Offset(pxL + nxL * (dist + capLen), pyL + nyL * (dist + capLen)))
                            }
                        }
                        else -> {}
                    }
                }

                drawLayer(bassAmplitudes, paletteColors.dominant, bassOpacity, true)
                drawLayer(midAmplitudes, paletteColors.vibrant, midOpacity, false)
                drawLayer(highAmplitudes, paletteColors.muted, highOpacity, false)"""

    content = content.replace(old_canvas_inner, new_canvas_inner)
    with open(file_path, "w", encoding='utf-8') as f:
        f.write(content)
    print("Replaced Canvas successfully!")
else:
    print("Could not find canvas block")
