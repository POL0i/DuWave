package com.example.beatpulse.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

actual val VT323 = try {
    FontFamily(
        Font("font/vt323.ttf", FontWeight.Normal),
        Font("font/vt323.ttf", FontWeight.Bold),
        Font("font/vt323.ttf", FontWeight.SemiBold),
        Font("font/vt323.ttf", FontWeight.Medium)
    )
} catch (e: Exception) {
    FontFamily.Default
}
