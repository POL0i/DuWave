package com.example.beatpulse.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import com.example.beatpulse.theme.PaletteColors

fun extractPaletteFast(bitmap: ImageBitmap): PaletteColors {
    try {
        val pixelMap = bitmap.toPixelMap()
        val width = pixelMap.width
        val height = pixelMap.height
        
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0
        
        val step = maxOf(1, width / 30)
        
        var maxSat = -1f
        var vibR = 0
        var vibG = 0
        var vibB = 0
        
        for (x in 0 until width step step) {
            for (y in 0 until height step step) {
                val pixel = pixelMap[x, y]
                val r = (pixel.red * 255).toInt()
                val g = (pixel.green * 255).toInt()
                val b = (pixel.blue * 255).toInt()
                
                rSum += r
                gSum += g
                bSum += b
                count++
                
                val maxC = maxOf(r, g, b)
                val minC = minOf(r, g, b)
                val sat = if (maxC == 0) 0f else (maxC - minC) / maxC.toFloat()
                if (sat > maxSat && maxC > 50) { 
                    maxSat = sat
                    vibR = r
                    vibG = g
                    vibB = b
                }
            }
        }
        
        val fallback = PaletteColors(
            dominant = Color(0xFF1E1E1E),
            vibrant = Color(0xFF00E5FF),
            darkVibrant = Color(0xFF00B8D4),
            lightVibrant = Color(0xFF84FFFF),
            muted = Color(0xFF9E9E9E),
            darkMuted = Color(0xFF616161)
        )
        
        if (count == 0) return fallback
        
        val avgR = (rSum / count).toInt()
        val avgG = (gSum / count).toInt()
        val avgB = (bSum / count).toInt()
        
        val dominant = Color(avgR, avgG, avgB)
        val vibrant = if (maxSat < 0) dominant else Color(vibR, vibG, vibB)
        
        fun mixColor(c: Color, mix: Color, ratio: Float): Color {
            return Color(
                red = c.red * (1 - ratio) + mix.red * ratio,
                green = c.green * (1 - ratio) + mix.green * ratio,
                blue = c.blue * (1 - ratio) + mix.blue * ratio,
                alpha = c.alpha
            )
        }
        
        return PaletteColors(
            dominant = dominant,
            vibrant = vibrant,
            muted = mixColor(dominant, Color.Gray, 0.4f),
            darkVibrant = mixColor(vibrant, Color.Black, 0.4f),
            lightVibrant = mixColor(vibrant, Color.White, 0.4f),
            darkMuted = mixColor(dominant, Color.Black, 0.6f)
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return PaletteColors(
            dominant = Color(0xFF1E1E1E),
            vibrant = Color(0xFF00E5FF),
            darkVibrant = Color(0xFF00B8D4),
            lightVibrant = Color(0xFF84FFFF),
            muted = Color(0xFF9E9E9E),
            darkMuted = Color(0xFF616161)
        )
    }
}
