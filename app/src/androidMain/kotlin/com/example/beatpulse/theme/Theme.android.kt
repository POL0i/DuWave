package com.example.beatpulse.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun BeatPulseTheme(
  darkTheme: Boolean,
  dynamicColor: Boolean,
  isPixelArt: Boolean,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  val typographyToUse = if (isPixelArt) PixelTypography else Typography

  MaterialTheme(colorScheme = colorScheme, typography = typographyToUse, content = content)
}
