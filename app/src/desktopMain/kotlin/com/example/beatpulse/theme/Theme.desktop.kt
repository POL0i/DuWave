package com.example.beatpulse.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
actual fun BeatPulseTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    isPixelArt: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typographyToUse = if (isPixelArt) PixelTypography else Typography

    MaterialTheme(colorScheme = colorScheme, typography = typographyToUse, content = content)
}
