package com.example.beatpulse.ui.components.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.utils.getLocalizedString

/** Track info header (title, artist, time, buttons) */
@Composable
fun PlayerTrackInfoHeader(
    currentTrack: TrackEntity?,
    cleanUiMode: Boolean,
    isMicModeActive: Boolean, streamConfigUiVisible: Boolean,
    colorVibrant: Color, paletteColors: com.example.beatpulse.theme.PaletteColors,
    abRepeatModeEnabled: Boolean, abPointA: Float, abPointB: Float,
    duration: Long, currentPosition: Long,
    showMicButton: Boolean, lyrics: List<com.example.beatpulse.utils.LyricLine>,
    showLyrics: Boolean, onToggleLyrics: () -> Unit,
    onShowSupport: () -> Unit, onAddToPlaylist: () -> Unit,
    onToggleMicMode: () -> Unit, onShowStreamConfig: () -> Unit,
    prefs: com.example.beatpulse.ui.components.player.IPreferencesManager
) {
    AnimatedContent(targetState = currentTrack, label = "track_info") { track ->
        if (track != null) {
            Box(modifier = Modifier.alpha(if (!isMicModeActive || streamConfigUiVisible) 1f else 0f).fillMaxWidth().padding(top = 24.dp, start = 24.dp, end = 24.dp)) {
                Column(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.6f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = track.title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, modifier = Modifier.basicMarquee())
                    var showRemainingTime by remember { mutableStateOf(prefs.showRemainingTime) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { 
                            showRemainingTime = !showRemainingTime 
                            prefs.showRemainingTime = showRemainingTime
                        }
                    ) {
                        Text(text = track.artist, style = MaterialTheme.typography.bodyMedium, color = colorVibrant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (!cleanUiMode) {
                            Spacer(modifier = Modifier.width(8.dp))
                            val formatTime = { ms: Long -> val s = ms / 1000; String.format("%02d:%02d", s / 60, s % 60) }
                            val timeText = if (abRepeatModeEnabled) {
                                val aTime = (abPointA * duration).toLong(); val bTime = (abPointB * duration).toLong()
                                val posStr = if (showRemainingTime) "-${formatTime(duration - currentPosition)}" else formatTime(currentPosition)
                                "$posStr / A:${formatTime(aTime)} - B:${formatTime(bTime)}"
                            } else { if (showRemainingTime) "-${formatTime(duration - currentPosition)} / ${formatTime(duration)}" else "${formatTime(currentPosition)} / ${formatTime(duration)}" }
                            Text(text = timeText, style = MaterialTheme.typography.bodyMedium, color = colorVibrant)
                        }
                    }
                }
                Row(modifier = Modifier.align(Alignment.CenterStart)) {
                    IconButton(onClick = onShowSupport, modifier = Modifier.size(36.dp).clip(CircleShape).background(paletteColors.dominant.copy(alpha = 0.5f))) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = getLocalizedString("support_suggestions"), tint = colorVibrant, modifier = Modifier.size(20.dp))
                    }
                }
                Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = if (isMicModeActive) 2 else if (showMicButton) 1 else 0,
                        transitionSpec = {
                            androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith
                                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                        },
                        label = "playerButtonTransition"
                    ) { state ->
                        when (state) {
                            0 -> IconButton(onClick = onAddToPlaylist, modifier = Modifier.padding(end = 8.dp).size(36.dp).clip(CircleShape).background(paletteColors.dominant.copy(alpha = 0.5f))) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Añadir a Playlist", tint = colorVibrant, modifier = Modifier.size(20.dp))
                            }
                            1 -> IconButton(onClick = onToggleMicMode, modifier = Modifier.padding(end = 8.dp).size(36.dp).clip(CircleShape).background(paletteColors.dominant.copy(alpha = 0.5f))) {
                                Icon(imageVector = Icons.Default.Mic, contentDescription = "Modo Streamer", tint = colorVibrant, modifier = Modifier.size(20.dp))
                            }
                            2 -> IconButton(onClick = onShowStreamConfig, modifier = Modifier.padding(end = 8.dp).size(36.dp).clip(CircleShape).background(paletteColors.dominant.copy(alpha = 0.5f))) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Configuración de Stream", tint = colorVibrant, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    if (lyrics.isNotEmpty()) {
                        IconButton(onClick = onToggleLyrics, modifier = Modifier.size(36.dp).clip(CircleShape).background(paletteColors.dominant.copy(alpha = 0.5f))) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Letras", tint = if (showLyrics) colorVibrant else colorVibrant.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
