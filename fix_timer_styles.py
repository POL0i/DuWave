import re

with open("app/src/main/java/com/example/beatpulse/ui/components/player/MegaminxSleepTimer.kt", "r") as f:
    content = f.read()

# 1. Update MegaminxInteractiveFace signature to take bgStyle: Int
content = content.replace("""    onCenterTap: () -> Unit,
    colorVibrant: Color,
    colorDominant: Color""", """    onCenterTap: () -> Unit,
    colorVibrant: Color,
    colorDominant: Color,
    bgStyle: Int""")

# 2. Update MegaminxSleepTimerDialog signature to take bgStyle: Int
content = content.replace("""    onSetSleepTimer: (Int) -> Unit,
    colorVibrant: Color,
    colorDominant: Color""", """    onSetSleepTimer: (Int) -> Unit,
    colorVibrant: Color,
    colorDominant: Color,
    bgStyle: Int = 0""")

# 3. Pass bgStyle to MegaminxInteractiveFace inside the dialog
content = content.replace("""                onCenterTap = {
                    if (sleepTimerSeconds > 0) {
                        onSetSleepTimer(0) // Cancel
                    } else {
                        onSetSleepTimer(300) // Add 5 mins
                    }
                },
                colorVibrant = colorVibrant,
                colorDominant = colorDominant""", """                onCenterTap = {
                    if (sleepTimerSeconds > 0) {
                        onSetSleepTimer(0) // Cancel
                    } else {
                        onSetSleepTimer(300) // Add 5 mins
                    }
                },
                colorVibrant = colorVibrant,
                colorDominant = colorDominant,
                bgStyle = bgStyle""")

# 4. Modify the drawing logic in MegaminxInteractiveFace Canvas.
# I will replace the whole Canvas block from `Canvas(modifier = Modifier.fillMaxSize()) {` to the end of the Box.
start_idx = content.find("Canvas(modifier = Modifier.fillMaxSize()) {")
end_idx = content.find("// Timer Text in the center")

new_canvas = """Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeStyle = Stroke(width = 6f, join = StrokeJoin.Round)
            val center = Offset(size.width / 2f, size.height / 2f)
            
            val rOutOuter = size.width * 0.45f
            val rOutInner = size.width * 0.35f
            val rMidOuter = size.width * 0.32f
            val rMidInner = size.width * 0.22f
            val rCenter = size.width * 0.18f

            fun Path.drawHeart(cx: Float, cy: Float, s: Float) {
                moveTo(cx, cy - s/4)
                cubicTo(cx - s, cy - s, cx - s*1.2f, cy + s/4, cx, cy + s*0.8f)
                cubicTo(cx + s*1.2f, cy + s/4, cx + s, cy - s, cx, cy - s/4)
                close()
            }

            fun Path.drawStar(cx: Float, cy: Float, outer: Float, inner: Float, points: Int) {
                for (i in 0 until points * 2) {
                    val a = (i * 180f / points - 90f) * kotlin.math.PI.toFloat() / 180f
                    val r = if (i % 2 == 0) outer else inner
                    val px = cx + r * kotlin.math.cos(a)
                    val py = cy + r * kotlin.math.sin(a)
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            
            // Background glow
            drawCircle(Brush.radialGradient(listOf(colorDominant.copy(alpha=0.3f), Color.Transparent), center, rOutOuter), radius = rOutOuter, center = center)

            // Draw outer triangles (Minutes Ring)
            rotate(animatedOuterRotation.value, center) {
                for (i in 0 until 5) {
                    val angle = (i * 72f - 90f) * kotlin.math.PI.toFloat() / 180f
                    val nextAngle = ((i + 1) * 72f - 90f) * kotlin.math.PI.toFloat() / 180f
                    val midAngle = (angle + nextAngle) / 2f
                    
                    val path = Path().apply {
                        when (bgStyle) {
                            8 -> { // Hearts
                                val cx = center.x + (rOutInner + rOutOuter)/2 * kotlin.math.cos(midAngle)
                                val cy = center.y + (rOutInner + rOutOuter)/2 * kotlin.math.sin(midAngle)
                                drawHeart(cx, cy, (rOutOuter - rOutInner) * 0.7f)
                            }
                            2 -> { // Anime (stars)
                                val cx = center.x + (rOutInner + rOutOuter)/2 * kotlin.math.cos(midAngle)
                                val cy = center.y + (rOutInner + rOutOuter)/2 * kotlin.math.sin(midAngle)
                                drawStar(cx, cy, (rOutOuter - rOutInner) * 0.5f, (rOutOuter - rOutInner) * 0.25f, 5)
                            }
                            else -> { // Default Megaminx Triangles / Cyberpunk polygons
                                val innerP1 = Offset(center.x + rOutInner * kotlin.math.cos(angle + 0.1f), center.y + rOutInner * kotlin.math.sin(angle + 0.1f))
                                val innerP2 = Offset(center.x + rOutInner * kotlin.math.cos(nextAngle - 0.1f), center.y + rOutInner * kotlin.math.sin(nextAngle - 0.1f))
                                val outerP = Offset(center.x + rOutOuter * kotlin.math.cos(midAngle), center.y + rOutOuter * kotlin.math.sin(midAngle))
                                moveTo(innerP1.x, innerP1.y)
                                lineTo(outerP.x, outerP.y)
                                lineTo(innerP2.x, innerP2.y)
                                close()
                            }
                        }
                    }
                    drawPath(path = path, color = colorDominant)
                    drawPath(path = path, color = colorVibrant, style = strokeStyle)
                }
            }

            // Draw middle quads (Hours Ring)
            rotate(animatedInnerRotation.value, center) {
                for (i in 0 until 5) {
                    val angle = (i * 72f - 90f) * kotlin.math.PI.toFloat() / 180f
                    val nextAngle = ((i + 1) * 72f - 90f) * kotlin.math.PI.toFloat() / 180f
                    val midAngle = (angle + nextAngle) / 2f
                    
                    val path = Path().apply {
                        val gap = 0.15f
                        when (bgStyle) {
                            8 -> { // Hearts
                                val cx = center.x + (rMidInner + rMidOuter)/2 * kotlin.math.cos(midAngle)
                                val cy = center.y + (rMidInner + rMidOuter)/2 * kotlin.math.sin(midAngle)
                                drawHeart(cx, cy, (rMidOuter - rMidInner) * 0.8f)
                            }
                            2 -> { // Anime (stars)
                                val cx = center.x + (rMidInner + rMidOuter)/2 * kotlin.math.cos(midAngle)
                                val cy = center.y + (rMidInner + rMidOuter)/2 * kotlin.math.sin(midAngle)
                                drawStar(cx, cy, (rMidOuter - rMidInner) * 0.6f, (rMidOuter - rMidInner) * 0.3f, 4)
                            }
                            else -> {
                                val inP1 = Offset(center.x + rMidInner * kotlin.math.cos(angle + gap), center.y + rMidInner * kotlin.math.sin(angle + gap))
                                val inP2 = Offset(center.x + rMidInner * kotlin.math.cos(nextAngle - gap), center.y + rMidInner * kotlin.math.sin(nextAngle - gap))
                                val outP2 = Offset(center.x + rMidOuter * kotlin.math.cos(nextAngle - gap/2), center.y + rMidOuter * kotlin.math.sin(nextAngle - gap/2))
                                val outP1 = Offset(center.x + rMidOuter * kotlin.math.cos(angle + gap/2), center.y + rMidOuter * kotlin.math.sin(angle + gap/2))
                                moveTo(inP1.x, inP1.y)
                                lineTo(outP1.x, outP1.y)
                                lineTo(outP2.x, outP2.y)
                                lineTo(inP2.x, inP2.y)
                                close()
                            }
                        }
                    }
                    drawPath(path = path, color = colorDominant)
                    drawPath(path = path, color = colorVibrant, style = strokeStyle)
                }
            }

            // Draw center shape
            val centerPath = Path().apply {
                when (bgStyle) {
                    8 -> { // Hearts
                        drawHeart(center.x, center.y, rCenter * 0.8f)
                    }
                    2 -> { // Anime
                        drawStar(center.x, center.y, rCenter, rCenter * 0.4f, 5)
                    }
                    1 -> { // Cyberpunk (Hexagon)
                        for (i in 0 until 6) {
                            val angle = (i * 60f - 90f) * kotlin.math.PI.toFloat() / 180f
                            val px = center.x + rCenter * kotlin.math.cos(angle)
                            val py = center.y + rCenter * kotlin.math.sin(angle)
                            if (i == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }
                    else -> { // Default Pentagon
                        for (i in 0 until 5) {
                            val angle = (i * 72f - 90f) * kotlin.math.PI.toFloat() / 180f
                            val px = center.x + rCenter * kotlin.math.cos(angle)
                            val py = center.y + rCenter * kotlin.math.sin(angle)
                            if (i == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }
                }
            }
            drawPath(path = centerPath, color = colorVibrant.copy(alpha = 0.8f))
            drawPath(path = centerPath, color = Color.White, style = strokeStyle)
        }
        
        """

content = content[:start_idx] + new_canvas + content[end_idx:]

with open("app/src/main/java/com/example/beatpulse/ui/components/player/MegaminxSleepTimer.kt", "w") as f:
    f.write(content)
