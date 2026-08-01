import re

path = 'app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix 1: ghostsFlow
content = content.replace("    val ghostsState = visualizerManager.ghostsFlow.collectAsState()\n", "")

# Fix 2: Typo in drawGlitchRing
bad_arc = """                            drawArc(
                                color = brushColor, startAngle = startOffset + (
i * 90f), sweepAngle = sweep,
                                useCenter = false, topLeft = Offset(ce
                                useCenter = false, topLeft = Offset(center.x - r
, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                                style = Stroke(width = thickness, cap = StrokeCa
p.Square)
                            )"""

good_arc = """                            drawArc(
                                color = brushColor, startAngle = startOffset + (i * 90f), sweepAngle = sweep,
                                useCenter = false, topLeft = androidx.compose.ui.geometry.Offset(center.x - r, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = thickness, cap = androidx.compose.ui.graphics.StrokeCap.Square)
                            )"""

content = content.replace(bad_arc, good_arc)

# Fix 3: Spark iteration typo
bad_spark = """                    if (spark.alpha <= 0f) {
                        iterator.remove()
                    } else {
                        drawCircle(color = spark.color.copy(alpha = spark.alpha)
, radius = 4f, center = O
, radius = 4f, center = Offset(spark.x, spark.y))
                    }"""

good_spark = """                    if (spark.alpha <= 0f) {
                        iterator.remove()
                    } else {
                        drawCircle(color = spark.color.copy(alpha = spark.alpha), radius = 4f, center = androidx.compose.ui.geometry.Offset(spark.x, spark.y))
                    }"""

content = content.replace(bad_spark, good_spark)

# Fix 4: Move lyrics status block INSIDE the visualizer Box
# Currently it's at the end of the visualizer block.
# Let's extract the block and re-insert it properly.

lyrics_block_start = "        // Lyrics status block"
lyrics_block_end = "        // Lyrics Overlay"

if lyrics_block_start in content and lyrics_block_end in content:
    start_idx = content.find(lyrics_block_start)
    end_idx = content.find(lyrics_block_end)
    if start_idx != -1 and end_idx != -1:
        block = content[start_idx:end_idx]
        
        # Remove it from its current position
        content = content[:start_idx] + content[end_idx:]
        
        # Now find the end of the visualizer box. It ends right before "// Lyrics Overlay" but we have some AnimatedVisibility
        # The easiest way is to find the AnimatedVisibility for the Play button which is at the end of the Box.
        # Then the Box closes.
        play_btn_end = """                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        }"""
        
        play_btn_end_new = """                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
""" + block + """
            }
        }"""
        
        content = content.replace(play_btn_end, play_btn_end_new)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Applied all fixes!")
