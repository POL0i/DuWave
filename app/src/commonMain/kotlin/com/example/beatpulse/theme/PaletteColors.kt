package com.example.beatpulse.theme

import androidx.compose.ui.graphics.Color

data class PaletteColors(
    val dominant: Color = Color(0xFF000000), // Negro para el fondo
    val vibrant: Color = Color(0xFFE50914), // Rojo para iconos/detalles
    val muted: Color = Color(0xFF222222),
    val darkVibrant: Color = Color(0xFF990000),
    val lightVibrant: Color = Color(0xFFFF5555),
    val darkMuted: Color = Color(0xFF121212),
    val extra1: Color = Color(0xFF333333),
    val extra2: Color = Color(0xFF444444),
    val extra3: Color = Color(0xFF555555)
)
