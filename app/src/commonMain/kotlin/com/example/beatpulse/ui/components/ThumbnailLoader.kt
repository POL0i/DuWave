package com.example.beatpulse.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.theme.PaletteColors

@Composable
expect fun rememberAlbumArt(track: TrackEntity): ImageBitmap?

@Composable
expect fun rememberFullAlbumArt(track: TrackEntity): ImageBitmap?

@Composable
expect fun rememberTrackPalette(track: TrackEntity): PaletteColors

@Composable
expect fun rememberStreamAvatar(uri: String?): ImageBitmap?
