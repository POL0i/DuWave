package com.example.beatpulse.ui.components.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu

import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beatpulse.utils.LyricLine
import com.example.beatpulse.utils.getLocalizedString


@Composable
fun BoxScope.PlayerLyricsStatusIndicator(isFetchingLyrics: Boolean, searchFailed: Boolean, availableLyricsResults: kotlin.collections.List<Any>, onShowLyricsMatches: () -> Unit) {
    if (isFetchingLyrics) {
        Box(modifier = Modifier.align(Alignment.Center).offset(y = 110.dp).size(40.dp).clip(CircleShape).background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White.copy(alpha = 0.8f), strokeWidth = 2.dp)
        }
    } else if (availableLyricsResults.isNotEmpty()) {
        Box(modifier = Modifier.align(Alignment.Center).offset(y = 110.dp).size(40.dp).clip(CircleShape).background(Color(0xFF333333)).clickable { onShowLyricsMatches() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Menu, contentDescription = "Buscar Letras", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        }
    } else if (searchFailed) {
        Box(modifier = Modifier.align(Alignment.Center).offset(y = 110.dp).size(40.dp).clip(CircleShape).background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Close, contentDescription = "No hay letras", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PlayerLyricsOverlay(lyrics: kotlin.collections.List<LyricLine>, currentPosition: Long, colorVibrant: Color, onSeek: (Long) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f))) {
        val listState = rememberLazyListState()
        val activeLyricIndex = lyrics.indexOfLast { it.timeMs <= currentPosition }.coerceAtLeast(0)
        LaunchedEffect(activeLyricIndex) { if (activeLyricIndex >= 0 && lyrics.isNotEmpty()) listState.animateScrollToItem(activeLyricIndex, scrollOffset = -200) }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), contentPadding = PaddingValues(vertical = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            itemsIndexed(lyrics) { index, line ->
                val isActive = index == activeLyricIndex
                val alpha by animateFloatAsState(if (isActive) 1f else 0.4f)
                val scale by animateFloatAsState(if (isActive) 1.1f else 1f)
                val color = if (isActive) colorVibrant else Color.White
                Text(text = line.text, color = color.copy(alpha = alpha), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clickable { onSeek(line.timeMs) })
            }
        }
    }
}

@Composable
fun GestureFeedbackOverlay(show: Boolean, text: String, alignLeft: Boolean, coverOffsetX: Float = 0f, coverOffsetY: Float = 0f) {
    if (!show) return
    val alphaAnim = animateFloatAsState(targetValue = if (show) 1f else 0f, animationSpec = tween(durationMillis = 300))
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp).offset { androidx.compose.ui.unit.IntOffset(coverOffsetX.toInt(), coverOffsetY.toInt()) }, contentAlignment = if (alignLeft) Alignment.CenterStart else Alignment.CenterEnd) {
        Box(modifier = Modifier.size(100.dp).graphicsLayer(alpha = alphaAnim.value).background(Color.White.copy(alpha = 0.2f), shape = CircleShape), contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
    }
}

@Composable
fun GestureTutorialOverlay(showNextPrev: Boolean, showSeek10s: Boolean, showVinylSeek: Boolean, showPlaylistSwipe: Boolean) {
    if (!showNextPrev && !showSeek10s && !showVinylSeek && !showPlaylistSwipe) return
    val infiniteTransition = rememberInfiniteTransition()
    val dotOffset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart))

    Box(modifier = Modifier.fillMaxSize()) {
        if (showNextPrev) {
            val pulse = if (dotOffset % 0.5f < 0.25f) 1f else 0f
            Box(modifier = Modifier.align(Alignment.CenterStart).offset(x = 16.dp).graphicsLayer { alpha = pulse }) { Box(modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.6f), CircleShape)) }
            Box(modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-16).dp).graphicsLayer { alpha = pulse }) { Box(modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.6f), CircleShape)) }
            Text("Toca dos veces los bordes para cambiar de canción", color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp))
        }
        if (showSeek10s && !showNextPrev) {
            Box(modifier = Modifier.align(Alignment.Center).offset(x = -50.dp).graphicsLayer { alpha = 1f - dotOffset }) { Box(modifier = Modifier.size(20.dp).background(Color.White, CircleShape)) }
            Box(modifier = Modifier.align(Alignment.Center).offset(x = 50.dp).graphicsLayer { alpha = 1f - dotOffset }) { Box(modifier = Modifier.size(20.dp).background(Color.White, CircleShape)) }
            Text("Toca dos veces aquí para adelantar o retrasar 10s", color = Color.White, modifier = Modifier.align(Alignment.Center).offset(y = (-100).dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp))
        }
        if (showVinylSeek && !showSeek10s) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.align(Alignment.Center).size(80.dp).graphicsLayer { rotationZ = dotOffset * 360f })
            Text("Gira la portada para adelantar o retrasar", color = Color.White, modifier = Modifier.align(Alignment.Center).offset(y = 120.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp))
        }
        if (showPlaylistSwipe) {
            Box(modifier = Modifier.align(Alignment.Center).offset(y = 80.dp + (-dotOffset * 60).dp).graphicsLayer { alpha = 1f - dotOffset }) { Box(modifier = Modifier.size(24.dp).background(Color.White, CircleShape)) }
            Text(getLocalizedString("gesture_playlist_swipe"), color = Color.White, modifier = Modifier.align(Alignment.Center).offset(y = 160.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp))
        }
    }
}
