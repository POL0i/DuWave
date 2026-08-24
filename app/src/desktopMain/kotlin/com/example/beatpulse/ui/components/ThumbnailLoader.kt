package com.example.beatpulse.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.example.beatpulse.data.TrackEntity

@Composable
actual fun rememberAlbumArt(track: TrackEntity): ImageBitmap? {
    // For now, return null on Desktop. 
    // Later we can read from ID3 tags or local file paths.
    return null
}
