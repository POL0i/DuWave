package com.example.beatpulse.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.beatpulse.data.PreferencesManager
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.theme.PaletteColors
import kotlinx.coroutines.delay

@Composable
fun BottomNavigationBar(
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    currentTrack: TrackEntity?,
    isPlaying: Boolean,
    accentColor: Color,
    paletteColors: PaletteColors,
    bgStyle: Int,
    prefs: PreferencesManager,
    exoPlayer: androidx.media3.common.Player?,
    onPlayPauseClick: () -> Unit
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    var hasConsumedSwipe by remember { mutableStateOf(false) }
    val animatedDrag by animateFloatAsState(
        targetValue = accumulatedDrag,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "drag"
    )

    var hintingOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (!prefs.hasUsedMiniplayerGesture) {
            delay(4000)
            if (accumulatedDrag == 0f && !prefs.hasUsedMiniplayerGesture) {
                hintingOffset = 30f
                delay(150)
                hintingOffset = -30f
                delay(150)
                hintingOffset = 0f
            }
        }
    }

    val totalOffset by animateFloatAsState(
        targetValue = animatedDrag + hintingOffset,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "totalOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        val currentConfiguredPage by rememberUpdatedState(currentPage)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { accumulatedDrag = 0f; hasConsumedSwipe = false },
                        onDragCancel = { accumulatedDrag = 0f; hasConsumedSwipe = false },
                        onHorizontalDrag = { _, dragAmount ->
                            if (hasConsumedSwipe) return@detectHorizontalDragGestures
                            if (!prefs.hasUsedMiniplayerGesture) {
                                prefs.hasUsedMiniplayerGesture = true
                            }
                            accumulatedDrag += dragAmount
                            if (accumulatedDrag > 40f) {
                                onPageChange((currentConfiguredPage - 1 + 3) % 3)
                                accumulatedDrag = 0f
                                hasConsumedSwipe = true
                            } else if (accumulatedDrag < -40f) {
                                onPageChange((currentConfiguredPage + 1) % 3)
                                accumulatedDrag = 0f
                                hasConsumedSwipe = true
                            }
                        }
                    )
                }
                .offset { IntOffset(totalOffset.toInt(), 0) }
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }, label = "BottomBarContent"
            ) { page ->
                if (page == 2) {
                    // Pantalla del reproductor: solo mostrar 3 puntos con un marco resaltado
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            DotsIndicator(
                                currentPage = currentPage,
                                onPageChange = onPageChange,
                                accentColor = accentColor,
                                prefs = prefs,
                                showPlayHint = false
                            )
                        }
                    }
                } else {
                    // Pantallas de Biblioteca/Carpetas: mostrar Mini-Reproductor si hay canción, sino 3 puntos
                    if (currentTrack != null) {
                        MiniPlayer(
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            accentColor = accentColor,
                            paletteColors = paletteColors,
                            bgStyle = bgStyle,
                            exoPlayer = exoPlayer,
                            onClick = { onPageChange(2) },
                            onPlayPauseClick = onPlayPauseClick
                        )
                    } else {
                        // No hay canción, mostrar los puntos
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            DotsIndicator(
                                currentPage = currentPage,
                                onPageChange = onPageChange,
                                accentColor = accentColor,
                                prefs = prefs,
                                showPlayHint = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DotsIndicator(
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    accentColor: Color,
    prefs: PreferencesManager,
    showPlayHint: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(0, 1, 2).forEach { p ->
            val color by animateColorAsState(
                targetValue = if (currentPage == p) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                animationSpec = tween(300), label = "indicator"
            )
            val size by animateFloatAsState(
                targetValue = if (currentPage == p) 10f else 6f,
                animationSpec = tween(300), label = "size"
            )
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onPageChange(p) }
            )
        }

        if (showPlayHint && !prefs.hasSeenTutorial) {
            var playAlpha by remember { mutableFloatStateOf(0.1f) }
            LaunchedEffect(Unit) {
                while (true) {
                    playAlpha = 0.8f
                    delay(800)
                    playAlpha = 0.1f
                    delay(800)
                }
            }
            val animatedPlayAlpha by animateFloatAsState(
                targetValue = playAlpha,
                animationSpec = tween(800), label = "alpha"
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Swipe to Play",
                tint = accentColor.copy(alpha = animatedPlayAlpha),
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = 8.dp)
            )
        }
    }
}
