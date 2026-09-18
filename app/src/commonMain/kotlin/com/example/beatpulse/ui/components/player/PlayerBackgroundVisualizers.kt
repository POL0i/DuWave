package com.example.beatpulse.ui.components.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

// ==================================================================================
// Pre-allocated buffers for 60 FPS rendering without GC thrashing.
// These are shared across all background visualizer engines.
// ==================================================================================

// Buffers for PlayerSidePerspectiveBandsBackground
private val sidePerspectiveSmoothedAmpsBuf = FloatArray(256)
private val sidePerspectiveXLArr = FloatArray(256)
private val sidePerspectiveYLArr = FloatArray(256)
private val sidePerspectiveXRArr = FloatArray(256)
private val sidePerspectiveYRArr = FloatArray(256)

// Pre-allocated paths for Terrain 3D and SidePerspective
internal val oscilloscopeSharedPath = Path()
private val terrainSharedQuadPath = Path()
private val terrainSharedLinePath = Path()
private val sidePerspectivePathLeft = Path()
private val sidePerspectivePathRight = Path()
private val sidePerspectivePathFill = Path()

// ==================================================================================
// Terrain State
// ==================================================================================

class TerrainState(
    var history: FloatArray = FloatArray(24 * 40),
    var lastZOffsetInt: Int = 0,
    var accumulatedAngle: Float = 0f,
    var previousAngle: Float = -1f
)

// ==================================================================================
// BANDS Visualizer — Horizontal frequency bars on left/right edges
// ==================================================================================

/** Draws BANDS visualizer in the background (drawBehind scope) */
fun PlayerBandsBackground(
    scope: DrawScope,
    bassAmps: FloatArray, midAmps: FloatArray, highAmps: FloatArray, combinedAmps: FloatArray,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    bassMult: Float, midMult: Float, trebleMult: Float, reactivity: Float, visualizerArchetype: Int
) {
    with(scope) {
        val w = size.width; val h = size.height
        val bassOpacity = (0.4f + bassMult * 0.4f + reactivity * 0.2f).coerceIn(0f, 1f)
        val midOpacity = (0.5f + midMult * 0.3f + reactivity * 0.2f).coerceIn(0f, 1f)
        val highOpacity = (0.6f + trebleMult * 0.2f + reactivity * 0.2f).coerceIn(0f, 1f)

        fun drawBandsEdge(amps: FloatArray, color: Color, widthMult: Float, isLeft: Boolean) {
            val count = amps.size; if (count == 0) return
            val stepY = h / count.coerceAtLeast(1).toFloat()
            for (i in 0 until count) {
                val amp = amps[i]
                val bandWidth = amp * w * 0.45f * widthMult
                if (bandWidth <= 1f) continue
                val startX = if (isLeft) 0f else w - bandWidth
                drawRect(color = color, topLeft = Offset(startX, i * stepY), size = androidx.compose.ui.geometry.Size(bandWidth, stepY), style = Stroke(width = 4f))
            }
        }

        val drawBandLayer = { amps: FloatArray, color: Color, widthMult: Float ->
            drawBandsEdge(amps, color, widthMult, true)
            drawBandsEdge(amps, color, widthMult, false)
        }

        if (visualizerArchetype == 1) {
            drawBandLayer(combinedAmps, paletteColors.vibrant.copy(alpha = maxOf(bassOpacity, midOpacity, highOpacity)), 1.5f)
        } else {
            drawBandLayer(bassAmps, paletteColors.dominant.copy(alpha = bassOpacity * 0.7f), 0.9f)
            drawBandLayer(midAmps, paletteColors.vibrant.copy(alpha = midOpacity), 1.3f)
            drawBandLayer(highAmps, paletteColors.muted.copy(alpha = highOpacity), 0.7f)
        }
    }
}

// ==================================================================================
// TERRAIN Visualizer — Synthwave 3D wireframe grid
// ==================================================================================

/** Draws TERRAIN (Synthwave 3D Grid) in the background */
fun PlayerTerrainBackground(
    scope: DrawScope,
    bassAmps: FloatArray, midAmps: FloatArray, highAmps: FloatArray, combinedAmps: FloatArray,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    bassMult: Float, midMult: Float, trebleMult: Float, reactivity: Float, visualizerArchetype: Int,
    rotationAngle: Float,
    terrainState: TerrainState
) {
    with(scope) {
        val w = size.width; val h = size.height
        val bassOpacity = (0.3f + bassMult * 0.4f + reactivity * 0.2f).coerceIn(0f, 1f)
        val midOpacity = (0.4f + midMult * 0.3f + reactivity * 0.2f).coerceIn(0f, 1f)
        val highOpacity = (0.5f + trebleMult * 0.2f + reactivity * 0.2f).coerceIn(0f, 1f)
        
        val isMobile = com.example.beatpulse.utils.SystemUtils.isMobilePlatform
        // EXTREME OPTIMIZATION: Grid restored to 14x20 to keep peaks visible, but we remove the fill entirely on mobile for max FPS
        val numZ = if (isMobile) 14 else 24
        val numX = if (isMobile) 20 else 40
        
        if (terrainState.history.size != numZ * numX) {
            terrainState.history = FloatArray(numZ * numX)
        }
        
        val speed = 20f
        
        if (terrainState.previousAngle == -1f) terrainState.previousAngle = rotationAngle
        var delta = rotationAngle - terrainState.previousAngle
        if (delta < -180f) delta += 360f
        if (delta > 180f) delta -= 360f
        terrainState.accumulatedAngle += delta
        terrainState.previousAngle = rotationAngle
        
        val zOffset = (terrainState.accumulatedAngle / 360f) * speed
        val currentZInt = zOffset.toInt()
        val scrollZ = zOffset % 1f
        
        // Helper for smoothed sampling
        fun sampleSmoothed(array: FloatArray, index: Int): Float {
            val dataCount = array.size
            if (dataCount == 0) return 0f
            var sum = 0f
            var weightSum = 0f
            for (j in -2..2) {
                val idx = (index + j).coerceIn(0, dataCount - 1)
                val weight = 1f / (1f + abs(j))
                sum += array[idx] * weight
                weightSum += weight
            }
            return sum / weightSum
        }
        
        // Update history
        if (currentZInt != terrainState.lastZOffsetInt) {
            val diff = currentZInt - terrainState.lastZOffsetInt
            if (diff > 0 && diff < numZ) {
                // Shift array
                val shift = diff * numX
                terrainState.history.copyInto(terrainState.history, shift, 0, terrainState.history.size - shift)
            } else if (diff >= numZ) {
                terrainState.history.fill(0f)
            }
            terrainState.lastZOffsetInt = currentZInt
            
            // Insert new data at row 0 (which will be drawn at Z = far)
            if (combinedAmps.isNotEmpty()) {
                val dataCount = combinedAmps.size
                for (xi in 0 until numX) {
                    val xNormalized = xi.toFloat() / (numX - 1) // 0 to 1
                    val distanceFromCenter = abs(xNormalized - 0.5f) * 2f // 0 at center, 1 at edges
                    
                    var elevation = 0f
                    if (distanceFromCenter > 0.15f) { // Leave a flat road in the middle
                        val mountainPos = ((distanceFromCenter - 0.15f) / 0.85f).coerceIn(0f, 1f)
                        val ampIndex = (mountainPos * (dataCount - 1)).toInt().coerceIn(0, dataCount - 1)
                        
                        val blended = if (visualizerArchetype == 1) {
                            sampleSmoothed(combinedAmps, ampIndex)
                        } else {
                            // Three wave mode: Bass -> Outer, Mid -> Middle, High -> Inner
                            val highWeight = (1f - mountainPos * 2f).coerceIn(0f, 1f) // 1 at inner, 0 at mid
                            val midWeight = (1f - abs(mountainPos - 0.5f) * 2f).coerceIn(0f, 1f) // peak at 0.5
                            val bassWeight = ((mountainPos - 0.5f) * 2f).coerceIn(0f, 1f) // 0 at mid, 1 at outer
                            
                            val highVal = sampleSmoothed(highAmps, ampIndex)
                            val midVal = sampleSmoothed(midAmps, ampIndex)
                            val bassVal = sampleSmoothed(bassAmps, ampIndex)
                            
                            highVal * highWeight + midVal * midWeight + bassVal * bassWeight
                        }
                        
                        elevation = blended * (distanceFromCenter * distanceFromCenter)
                        if (elevation < 0.05f) elevation = 0f // Threshold noise
                    }
                    terrainState.history[xi] = elevation
                }
            } else {
                for (xi in 0 until numX) terrainState.history[xi] = 0f
            }
        }

           fun drawTerrainLayer(color: Color, isTop: Boolean, opacityMult: Float, pulseMult: Float) {
            val ampMult = if (isTop) 2.5f else 5.0f

            fun project(x: Float, y: Float, z: Float): Offset {
                val scale = h * 1.4f / z // Perspectiva incrementada (antes 0.9f)
                val px = w / 2f + x * scale
                val horizonOffset = h * 0.12f // Separación del horizonte
                val cameraY = 1.0f // Cámara un poco más baja para acentuar profundidad
                
                // Pulse the Y height dynamically with the music! 
                // Increased global bounce multiplier (1.5x) for more reaction
                val pulsedY = y * pulseMult * 1.5f
                
                // Floor (+Y goes down on screen). Ceiling (-Y goes up).
                val screenY = if (isTop) {
                    val baseScreenY = h / 2f - horizonOffset
                    baseScreenY - cameraY * scale + pulsedY * ampMult * scale
                } else {
                    val baseScreenY = h / 2f + horizonOffset
                    baseScreenY + cameraY * scale - pulsedY * ampMult * scale
                }
                return Offset(px, screenY)
            }

            val strokeColor = color.copy(alpha = opacityMult)
            
            // Helper for 3-wave gradient coloring
            fun getNeonColor(xi: Int): Color {
                if (visualizerArchetype == 1) return color
                
                val xNormalized = xi.toFloat() / (numX - 1)
                val distCenter = abs(xNormalized - 0.5f) * 2f
                val mountainPos = ((distCenter - 0.15f) / 0.85f).coerceIn(0f, 1f)
                
                val highW = (1f - mountainPos * 2f).coerceIn(0f, 1f)
                val midW = (1f - abs(mountainPos - 0.5f) * 2f).coerceIn(0f, 1f)
                val bassW = ((mountainPos - 0.5f) * 2f).coerceIn(0f, 1f)
                
                val total = (highW + midW + bassW).coerceAtLeast(0.01f)
                val hW = highW / total
                val mW = midW / total
                val bW = bassW / total
                
                // Use distinct colors from the cover art palette directly!
                val bassColor = paletteColors.vibrant
                val midColor = color
                val highColor = paletteColors.lightVibrant
                
                val r = highColor.red * hW + midColor.red * mW + bassColor.red * bW
                val g = highColor.green * hW + midColor.green * mW + bassColor.green * bW
                val b = highColor.blue * hW + midColor.blue * mW + bassColor.blue * bW
                return Color(r, g, b, 1f)
            }
            
            // ULTRA OPTIMIZATION: Combine entire wireframe into a single Path and draw ONCE with a solid color.
            // Eliminates 14+ JNI drawPath calls per frame and removes heavy Skia gradient shaders entirely.
            terrainSharedLinePath.reset()

            // Draw back-to-front for proper painter's algorithm occlusion.
            for (zi in 0 until numZ - 1) {
                val zBack = (numZ - zi).toFloat() - scrollZ
                val zFront = (numZ - zi - 1).toFloat() - scrollZ
                
                // Fade out near the camera and fade IN at the horizon (fog effect)
                val fadeBack = (zBack / 2f).coerceIn(0f, 1f) * (1f - (zBack / numZ)).coerceIn(0f, 1f)
                val fadeFront = (zFront / 2f).coerceIn(0f, 1f) * (1f - (zFront / numZ)).coerceIn(0f, 1f)
                
                // Grow from 0 elevation to full elevation over the first 4 chunks
                val growBack = ((numZ - zBack) / 4f).coerceIn(0f, 1f)
                val growFront = ((numZ - zFront) / 4f).coerceIn(0f, 1f)
                
                if (fadeBack <= 0.01f && fadeFront <= 0.01f) continue

                // 1. Draw filled geometry
                if (isMobile) {
                    // ULTRA OPTIMIZATION FOR MOBILE: 
                    // To guarantee 60 FPS on weak hardware, we skip drawing the filled quad entirely
                    // and just draw the wireframe neon lines (holographic grid effect).
                } else {
                    for (xi in 0 until numX - 1) {
                        val elBL = terrainState.history[zi * numX + xi] * growBack
                        val elBR = terrainState.history[zi * numX + xi + 1] * growBack
                        val elFL = terrainState.history[(zi + 1) * numX + xi] * growFront
                        val elFR = terrainState.history[(zi + 1) * numX + xi + 1] * growFront
                        
                        val xL = (xi.toFloat() / (numX - 1)) * 4f - 2f
                        val xR = ((xi + 1).toFloat() / (numX - 1)) * 4f - 2f

                        val ptBL = project(xL, elBL, zBack)
                        val ptBR = project(xR, elBR, zBack)
                        val ptFL = project(xL, elFL, zFront)
                        val ptFR = project(xR, elFR, zFront)

                        terrainSharedQuadPath.reset()
                        terrainSharedQuadPath.moveTo(ptBL.x, ptBL.y)
                        terrainSharedQuadPath.lineTo(ptBR.x, ptBR.y)
                        terrainSharedQuadPath.lineTo(ptFR.x, ptFR.y)
                        terrainSharedQuadPath.lineTo(ptFL.x, ptFL.y)
                        terrainSharedQuadPath.close()

                        // Vary color based on elevation for 3D depth effect
                        val avgY = (elBL + elBR + elFL + elFR) / 4f
                        val intensity = (avgY / 2.5f).coerceIn(0f, 1f)
                        
                        val avgNeon = getNeonColor(xi)
                        
                        // Blend quad color with neon based on elevation
                        val baseFill = lerp(
                            paletteColors.darkMuted,
                            avgNeon,
                            intensity * 0.4f
                        )
                        // Blend quad color with background based on fadeBack (creates horizon fog effect)
                        val quadFill = lerp(
                            paletteColors.dominant,
                            baseFill,
                            fadeBack
                        ).copy(alpha = 1.0f)

                        drawPath(terrainSharedQuadPath, color = quadFill, style = Fill)
                    }
                }

                // 2. Add to master wireframe path
                // Draw all horizontal lines first
                var first = true
                for (xi in 0 until numX) {
                    // Multiplicamos la elevación x1.5 para picos más pronunciados y visibles
                    val elB = terrainState.history[zi * numX + xi] * growBack * 1.5f
                    // Comprimimos el ancho total de 6 a 4 unidades (-2 a 2)
                    val x = (xi.toFloat() / (numX - 1)) * 4f - 2f
                    val ptB = project(x, elB, zBack)
                    if (first) {
                        terrainSharedLinePath.moveTo(ptB.x, ptB.y)
                        first = false
                    } else {
                        terrainSharedLinePath.lineTo(ptB.x, ptB.y)
                    }
                }

                // Draw all vertical lines
                for (xi in 0 until numX) {
                    val elB = terrainState.history[zi * numX + xi] * growBack * 1.5f
                    val elF = terrainState.history[(zi + 1) * numX + xi] * growFront * 1.5f
                    val x = (xi.toFloat() / (numX - 1)) * 4f - 2f
                    val ptB = project(x, elB, zBack)
                    val ptF = project(x, elF, zFront)
                    terrainSharedLinePath.moveTo(ptB.x, ptB.y)
                    terrainSharedLinePath.lineTo(ptF.x, ptF.y)
                }
            }
            
            // Draw the very front horizontal line
            val lastZ = numZ - 1
            val zFront = lastZ.toFloat() - scrollZ
            var firstFront = true
            for (xi in 0 until numX) {
                val elF = terrainState.history[lastZ * numX + xi] * 1.5f // Fully grown, taller
                val x = (xi.toFloat() / (numX - 1)) * 4f - 2f
                val ptF = project(x, elF, zFront)
                if (firstFront) {
                    terrainSharedLinePath.moveTo(ptF.x, ptF.y)
                    firstFront = false
                } else {
                    terrainSharedLinePath.lineTo(ptF.x, ptF.y)
                }
            }
            
            // Draw the master wireframe with a single blazing-fast native call.
            // Used a thicker stroke (4f on mobile) and solid bright color to make the lines pop as requested.
            drawPath(
                terrainSharedLinePath,
                color = color,
                alpha = (opacityMult * 1.5f).coerceIn(0.5f, 1f), // Siempre visible, nunca muy oscuro
                style = Stroke(
                    width = if (isMobile) 4f else 2f, 
                    join = StrokeJoin.Round
                )
            )
        }

        if (visualizerArchetype == 1) {
            val neonColor = paletteColors.vibrant
            val bassPulse = 1f + (bassMult * 1.5f)
            val midPulse = 1f + (midMult * 1.5f)
            
            drawTerrainLayer(neonColor, isTop = true, opacityMult = midOpacity, pulseMult = midPulse)
            drawTerrainLayer(neonColor, isTop = false, opacityMult = bassOpacity, pulseMult = bassPulse)
        } else {
            // Siempre forzar colores vibrantes para la malla de modo ahorro de recursos
            drawTerrainLayer(paletteColors.vibrant, false, bassOpacity, 1f)
            drawTerrainLayer(paletteColors.vibrant, true, highOpacity, 1f)
        }
    }
}

// ==================================================================================
// OSCILLOSCOPE Visualizer — 3D wireframe with depth-culled rendering
// ==================================================================================

fun PlayerOscilloscope(
    scope: DrawScope,
    bassAmps: FloatArray, midAmps: FloatArray, highAmps: FloatArray, combinedAmps: FloatArray,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    visualizerArchetype: Int,
    rotationAngle: Float,
    reactivity: Float,
    state: OscilloscopeState,
    isForeground: Boolean
) {
    val w = scope.size.width
    val h = scope.size.height
    val cx = w / 2f
    val cy = h / 2f
    val baseR = minOf(w, h) * 0.18f
    
    // Only update state once per frame (when rendering background)
    if (!isForeground) {
        var avgCombined = 0f
        if (combinedAmps.isNotEmpty()) avgCombined = combinedAmps.average().toFloat()
        
        var bassAvg = 0f
        if (bassAmps.isNotEmpty()) bassAvg = bassAmps.average().toFloat()
        
        val deltaAngle = rotationAngle - (state.accumulatedTime % 360f) // roughly 0.5 per frame
        state.accumulatedTime += (0.5f) * 0.1f + (avgCombined * 0.3f)
        state.dynamicPhase += bassAvg * 0.2f
    }
    
    fun getInterpolatedAmp(amps: FloatArray, tNormalized: Float): Float {
        if (amps.isEmpty()) return 0f
        val exactIdx = tNormalized * (amps.size - 1)
        val idx0 = exactIdx.toInt().coerceIn(0, amps.size - 1)
        val idx1 = (idx0 + 1).coerceIn(0, amps.size - 1)
        val fraction = exactIdx - idx0
        return amps[idx0] + (amps[idx1] - amps[idx0]) * fraction
    }

    val rotSpeedY = 0.005f
    val rotSpeedZ = 0.003f
    val ringAngle = rotationAngle * (kotlin.math.PI / 180.0).toFloat()

    fun drawSphere(amps: FloatArray, color: Color, numRings: Int, radiusMod: Float, rotSpeedYMult: Float, rotSpeedZMult: Float, lineW: Float, isHorizontal: Boolean) {
        if (amps.isEmpty()) return
        val timeDelta = state.accumulatedTime * 0.015f * rotSpeedZMult // Faster overall speed
        
        // Wobble on X and Y to keep the hole mostly facing the camera
        val currentXRot = sin(timeDelta * 0.5f) * 0.4f
        val currentYRot = cos(timeDelta * 0.7f) * 0.4f
        val currentZRot = ringAngle + timeDelta * 2.0f
        val numPoints = if (com.example.beatpulse.utils.SystemUtils.isMobilePlatform) 40 else 90
        
        val cosZ = cos(currentZRot)
        val sinZ = sin(currentZRot)
        val cosX = cos(currentXRot)
        val sinX = sin(currentXRot)
        val cosY = cos(currentYRot)
        val sinY = sin(currentYRot)
        
        oscilloscopeSharedPath.reset()
        
        for (ring in 0 until numRings) {
            val ringT = ring.toFloat() / numRings
            val ringOffset = ringT * kotlin.math.PI.toFloat() * 2f
            val cosRing = cos(ringOffset)
            val sinRing = sin(ringOffset)
            
            var prevX = 0f
            var prevY = 0f
            var prevZ = 0f
            var hasPrev = false
            var isDrawingSegment = false
            
            for (i in 0..numPoints) {
                val t = (i.toFloat() / numPoints) * kotlin.math.PI.toFloat() * 2f
                val normalizedT = i.toFloat() / numPoints
                val mirroredT = if (normalizedT < 0.5f) normalizedT * 2f else (1f - normalizedT) * 2f
                
                val amp = getInterpolatedAmp(amps, mirroredT) * reactivity
                val stretch = 1f + (amp * 4.0f)
                val r = baseR * radiusMod * stretch
                
                val pX = cos(t) * r
                val pY = sin(t) * r
                
                var sX = pX
                var sY = pY * cosRing
                var sZ = pY * sinRing
                
                if (!isHorizontal) {
                    sY = pX * cosRing
                    sX = pY
                    sZ = pX * sinRing
                }
                
                // 1. Z-axis rotation (spin around the cover)
                val x1 = sX * cosZ - sY * sinZ
                val y1 = sX * sinZ + sY * cosZ
                val z1 = sZ
                
                // 2. X-axis rotation (tilt up/down)
                val x2 = x1
                val y2 = y1 * cosX - z1 * sinX
                val z2 = y1 * sinX + z1 * cosX
                
                // 3. Y-axis rotation (tilt left/right)
                val x3 = x2 * cosY - z2 * sinY
                val y3 = y2
                val z3 = x2 * sinY + z2 * cosY
                
                val px = cx + x3.toFloat()
                val py = cy + y3.toFloat()
                
                if (hasPrev) {
                    val avgZ = (prevZ + z3) / 2f
                    val shouldDraw = if (isForeground) avgZ >= 0 else avgZ < 0
                    if (shouldDraw) {
                        if (!isDrawingSegment) {
                            oscilloscopeSharedPath.moveTo(prevX, prevY)
                            isDrawingSegment = true
                        }
                        oscilloscopeSharedPath.lineTo(px, py)
                    } else {
                        isDrawingSegment = false
                    }
                }
                prevX = px
                prevY = py
                prevZ = z3.toFloat()
                hasPrev = true
            }
        }
        
        scope.drawPath(
            path = oscilloscopeSharedPath,
            color = color,
            style = Stroke(
                width = lineW,
                cap = StrokeCap.Round
            )
        )
    }

    fun drawTorusKnot(amps: FloatArray, color: Color, p: Float, q: Float, lineW: Float, rotSpeedMult: Float, pathEffect: androidx.compose.ui.graphics.PathEffect? = null) {
        if (amps.isEmpty()) return
        val timeDelta = state.accumulatedTime * 0.015f * rotSpeedMult // Faster flowing
        
        // Wobble on X and Y to keep the hole facing us, spin on Z
        val currentXRot = sin(timeDelta * 0.5f) * 0.4f
        val currentYRot = cos(timeDelta * 0.7f) * 0.4f
        val currentZRot = ringAngle + timeDelta * 1.5f
        val numPoints = if (com.example.beatpulse.utils.SystemUtils.isMobilePlatform) 60 else 150 // Reduced for mobile
        
        val cosZ = cos(currentZRot)
        val sinZ = sin(currentZRot)
        val cosX = cos(currentXRot)
        val sinX = sin(currentXRot)
        val cosY = cos(currentYRot)
        val sinY = sin(currentYRot)
        
        var prevX = 0f
        var prevY = 0f
        var prevZ = 0f
        var hasPrev = false
        var isDrawingSegment = false
        
        oscilloscopeSharedPath.reset()
        
        for (i in 0..numPoints) {
            val normalizedT = i.toFloat() / numPoints
            val t = normalizedT * kotlin.math.PI.toFloat() * 2f
            val mirroredT = if (normalizedT < 0.5f) normalizedT * 2f else (1f - normalizedT) * 2f
            
            val amp = getInterpolatedAmp(amps, mirroredT) * reactivity
            val stretch = 1f + (amp * 1.5f)
            
            // Torus knot equations
            val r1 = baseR * 1.4f // Main radius (wraps around the album art)
            val r2 = baseR * 0.4f * stretch // Tube radius (reacts to audio)
            
            val rTorus = r1 + r2 * cos(q * t + timeDelta * 1.5f)
            val lX = rTorus * cos(p * t + timeDelta)
            val lY = rTorus * sin(p * t + timeDelta)
            val lZ = r2 * sin(q * t + timeDelta * 1.5f)
            
            // 1. Z-axis rotation (spin around the cover)
            val x1 = lX * cosZ - lY * sinZ
            val y1 = lX * sinZ + lY * cosZ
            val z1 = lZ
            
            // 2. X-axis rotation (tilt up/down)
            val x2 = x1
            val y2 = y1 * cosX - z1 * sinX
            val z2 = y1 * sinX + z1 * cosX
            
            // 3. Y-axis rotation (tilt left/right)
            val x3 = x2 * cosY - z2 * sinY
            val y3 = y2
            val z3 = x2 * sinY + z2 * cosY
            
            val px = cx + x3.toFloat()
            val py = cy + y3.toFloat()
            
            if (hasPrev) {
                val avgZ = (prevZ + z3) / 2f
                val shouldDraw = if (isForeground) avgZ >= 0 else avgZ < 0
                if (shouldDraw) {
                    if (!isDrawingSegment) {
                        oscilloscopeSharedPath.moveTo(prevX, prevY)
                        isDrawingSegment = true
                    }
                    oscilloscopeSharedPath.lineTo(px, py)
                } else {
                    isDrawingSegment = false
                }
            }
            prevX = px
            prevY = py
            prevZ = z3.toFloat()
            hasPrev = true
        }
        
        scope.drawPath(
            path = oscilloscopeSharedPath,
            color = color,
            style = Stroke(
                width = lineW,
                cap = StrokeCap.Round,
                pathEffect = pathEffect
            )
        )
    }

    if (visualizerArchetype == 1) { // 3 waves (Torus Knots)
        drawTorusKnot(bassAmps, paletteColors.dominant, 3f, 8f, 6f, 0.8f) // 3 lobes, 8 twists
        drawTorusKnot(midAmps, paletteColors.vibrant, 5f, 3f, 4f, 1.2f)   // 5 lobes, 3 twists
        
        val dashPhase = -state.accumulatedTime * 80f // Scroll faster
        val highEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(25f, 15f), dashPhase)
        drawTorusKnot(highAmps, paletteColors.muted.copy(alpha = 0.7f), 7f, 4f, 2.5f, 1.6f, highEffect) // 7 lobes, 4 twists

    } else { // 1 wave
        drawSphere(bassAmps, paletteColors.dominant.copy(alpha=0.6f), 8, 1.2f, 0.8f, 0.5f, 3f, true)
        drawSphere(midAmps, paletteColors.vibrant.copy(alpha=0.8f), 6, 1.0f, 1.2f, 0.7f, 4f, false)
        drawSphere(highAmps, paletteColors.muted, 4, 0.8f, 1.5f, 1.0f, 6f, true)
    }
}

// ==================================================================================
// SIDE_PERSPECTIVE_BANDS Visualizer — 3D angled perspective pillars
// ==================================================================================

/** Draws SIDE_PERSPECTIVE_BANDS visualizer in the background (drawBehind scope) */
fun PlayerSidePerspectiveBandsBackground(
    scope: DrawScope,
    bassAmps: FloatArray, midAmps: FloatArray, highAmps: FloatArray, combinedAmps: FloatArray,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    bassMult: Float, midMult: Float, trebleMult: Float, reactivity: Float, visualizerArchetype: Int
) {
    with(scope) {
        val w = size.width
        val h = size.height
        val bassOpacity = (0.5f + bassMult * 0.4f + reactivity * 0.2f).coerceIn(0f, 1f)
        val midOpacity = (0.6f + midMult * 0.3f + reactivity * 0.2f).coerceIn(0f, 1f)
        val highOpacity = (0.7f + trebleMult * 0.2f + reactivity * 0.2f).coerceIn(0f, 1f)
        val vpX = w * 0.5f
        val vpY = h * 0.5f

        fun drawPerspectivePillar(amps: FloatArray, color: Color, widthMult: Float, isLeft: Boolean) {
            val count = amps.size; if (count == 0) return
            val sAmps = if (sidePerspectiveSmoothedAmpsBuf.size >= count) sidePerspectiveSmoothedAmpsBuf else FloatArray(count)
            val sXL = if (sidePerspectiveXLArr.size >= count) sidePerspectiveXLArr else FloatArray(count)
            val sYL = if (sidePerspectiveYLArr.size >= count) sidePerspectiveYLArr else FloatArray(count)
            val sXR = if (sidePerspectiveXRArr.size >= count) sidePerspectiveXRArr else FloatArray(count)
            val sYR = if (sidePerspectiveYRArr.size >= count) sidePerspectiveYRArr else FloatArray(count)
            
            for (i in 0 until count) {
                var sum = 0f
                var weightSum = 0f
                // Wider smoothing radius (-8 to 8) for much more fluid organic curves
                for (j in -8..8) {
                    val idx = i + j
                    if (idx in 0 until count) {
                        val weight = 1f / (1f + abs(j))
                        sum += amps[idx] * weight
                        weightSum += weight
                    }
                }
                sAmps[i] = sum / weightSum
            }
            
            // Move pillars a bit away from the edges
            val cX = if (isLeft) w * 0.11f else w * 0.89f
            val baseWidth = w * 0.03f
            
            for (i in 0 until count) {
                val t = -0.4f + 1.8f * (i.toFloat() / (count - 1).coerceAtLeast(1))
                val yEdge = h * (1f - t)
                val amp = sAmps[i]
                
                // Deadzone to mute base noise, then a 2.8x linear multiplier.
                val processedAmp = max(0f, amp - 0.05f) * 2.8f
                
                val bandWidth = baseWidth + (processedAmp * w * 0.22f * widthMult)
                val xL = cX - bandWidth / 2f
                val xR = cX + bandWidth / 2f
                val slope = (vpY - yEdge) / (vpX - cX)
                sXL[i] = xL
                sYL[i] = yEdge + (xL - cX) * slope
                sXR[i] = xR
                sYR[i] = yEdge + (xR - cX) * slope
            }
            
            for (i in 0 until count) {
                drawLine(
                    color = color.copy(alpha = color.alpha * 0.5f),
                    start = Offset(sXL[i], sYL[i]),
                    end = Offset(sXR[i], sYR[i]),
                    strokeWidth = 2f
                )
            }
            
            if (count > 0) {
                sidePerspectivePathLeft.reset()
                sidePerspectivePathLeft.moveTo(sXL[0], sYL[0])
                for (i in 0 until count - 1) {
                    sidePerspectivePathLeft.lineTo(sXL[i], sYL[i+1])
                    sidePerspectivePathLeft.lineTo(sXL[i+1], sYL[i+1])
                }
                sidePerspectivePathRight.reset()
                sidePerspectivePathRight.moveTo(sXR[0], sYR[0])
                for (i in 0 until count - 1) {
                    sidePerspectivePathRight.lineTo(sXR[i], sYR[i+1])
                    sidePerspectivePathRight.lineTo(sXR[i+1], sYR[i+1])
                }
                sidePerspectivePathFill.reset()
                sidePerspectivePathFill.moveTo(sXL[0], sYL[0])
                for (i in 0 until count - 1) {
                    sidePerspectivePathFill.lineTo(sXL[i], sYL[i+1])
                    sidePerspectivePathFill.lineTo(sXL[i+1], sYL[i+1])
                }
                sidePerspectivePathFill.lineTo(sXR[count-1], sYR[count-1])
                for (i in count - 1 downTo 1) {
                    sidePerspectivePathFill.lineTo(sXR[i], sYR[i-1])
                    sidePerspectivePathFill.lineTo(sXR[i-1], sYR[i-1])
                }
                sidePerspectivePathFill.close()
                drawPath(sidePerspectivePathFill, color.copy(alpha = color.alpha * 0.2f), style = Fill)
                drawPath(sidePerspectivePathLeft, color, style = Stroke(width = 3f, join = StrokeJoin.Miter))
                drawPath(sidePerspectivePathRight, color, style = Stroke(width = 3f, join = StrokeJoin.Miter))
            }
        }

        val drawBandLayer = { amps: FloatArray, color: Color, widthMult: Float ->
            drawPerspectivePillar(amps, color, widthMult, true)
            drawPerspectivePillar(amps, color, widthMult, false)
        }

        if (visualizerArchetype == 1) {
            drawBandLayer(combinedAmps, paletteColors.vibrant.copy(alpha = maxOf(bassOpacity, midOpacity, highOpacity)), 1.5f)
        } else {
            drawBandLayer(bassAmps, paletteColors.dominant.copy(alpha = bassOpacity * 0.8f), 0.9f)
            drawBandLayer(midAmps, paletteColors.vibrant.copy(alpha = midOpacity), 1.3f)
            drawBandLayer(highAmps, paletteColors.muted.copy(alpha = highOpacity), 0.7f)
        }
    }
}
