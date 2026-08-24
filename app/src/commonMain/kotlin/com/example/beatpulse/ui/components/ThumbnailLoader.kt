package com.example.beatpulse.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.example.beatpulse.data.TrackEntity

@Composable
expect fun rememberAlbumArt(track: TrackEntity): ImageBitmap?
