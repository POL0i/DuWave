package com.example.beatpulse.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.theme.PaletteColors

@Composable
fun MiniPlayer(
    currentTrack: TrackEntity?,
    isPlaying: Boolean,
    accentColor: Color,
    paletteColors: PaletteColors,
    bgStyle: Int,
    exoPlayer: androidx.media3.common.Player?,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {
    if (currentTrack == null) return

    val miniPlayerShape = when (bgStyle) {
        8 -> RectangleShape
        7 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        1 -> CutCornerShape(8.dp)
        2, 4 -> RoundedCornerShape(24.dp)
        5 -> RectangleShape
        6 -> CutCornerShape(16.dp)
        else -> RoundedCornerShape(16.dp)
    }

    val miniPlayerBorder = when (bgStyle) {
        8 -> BorderStroke(3.dp, accentColor)
        1 -> BorderStroke(2.dp, paletteColors.vibrant)
        7 -> BorderStroke(1.dp, paletteColors.dominant)
        3 -> BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
        2 -> BorderStroke(3.dp, Color(0xFFFFB7B2))
        4 -> BorderStroke(2.dp, Color(0xFFFF9CEE))
        5 -> BorderStroke(2.dp, Color.DarkGray)
        6 -> BorderStroke(1.dp, Color(0xFF8B0000))
        else -> BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    }

    val miniPlayerBg = when (bgStyle) {
        8 -> Brush.horizontalGradient(listOf(Color.Black, Color.DarkGray))
        1 -> Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.8f), paletteColors.dominant.copy(alpha = 0.8f)))
        3 -> Brush.horizontalGradient(listOf(paletteColors.lightVibrant.copy(alpha = 0.6f), Color.White.copy(alpha = 0.2f)))
        7 -> Brush.horizontalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D)))
        2 -> Brush.horizontalGradient(listOf(paletteColors.lightVibrant.copy(alpha = 0.9f), paletteColors.dominant.copy(alpha = 0.6f)))
        4 -> Brush.horizontalGradient(listOf(paletteColors.vibrant.copy(alpha = 0.8f), paletteColors.lightVibrant.copy(alpha = 0.8f)))
        5 -> Brush.horizontalGradient(listOf(Color.Black, Color(0xFF111111)))
        6 -> Brush.horizontalGradient(listOf(Color(0xFF1A0000), Color(0xFF0D0D0D)))
        else -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(miniPlayerBg, miniPlayerShape)
            .border(miniPlayerBorder, miniPlayerShape)
            .clip(miniPlayerShape)
            .clickable { onClick() }
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
            val albumArtBitmap = rememberFullAlbumArt(currentTrack)
            if (albumArtBitmap != null) {
                Image(
                    bitmap = albumArtBitmap,
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTrack.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = currentTrack.artist,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

            var currentPos by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }
            var duration by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(1L) }
            androidx.compose.runtime.LaunchedEffect(isPlaying, exoPlayer) {
                while(true) {
                    currentPos = exoPlayer?.currentPosition ?: 0L
                    val d = exoPlayer?.duration ?: 1L
                    duration = if (d > 0) d else 1L
                    kotlinx.coroutines.delay(500)
                }
            }
            val progress = (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formatTime = { ms: Long -> 
                    val totalSeconds = ms / 1000
                    val m = totalSeconds / 60
                    val s = totalSeconds % 60
                    String.format("%02d:%02d", m, s)
                }
                Text(
                    text = formatTime(currentPos),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black.copy(alpha=0.3f))
                ) {
                    Box(modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(paletteColors.dominant, accentColor)))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTime(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
