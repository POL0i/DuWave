package com.example.beatpulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.layout.BoxWithConstraints

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.beatpulse.data.AppPreferences
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.type
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.player.IPlayerViewModel
import com.example.beatpulse.ui.components.DesktopBottomNavigationBar
import com.example.beatpulse.ui.components.player.DesktopPlayerScreen
import com.example.beatpulse.ui.components.player.DesktopPlayerScreenCallbacks
import com.example.beatpulse.ui.components.player.DesktopPlayerScreenState
import com.example.beatpulse.ui.screens.DesktopLibraryScreen
import com.example.beatpulse.ui.screens.DesktopListaGeneralScreen


import com.example.beatpulse.isDesktopPlatform
import com.example.beatpulse.ui.viewmodels.ILibraryViewModel
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.utils.AppBackHandler
import kotlinx.coroutines.delay

@Composable
fun DesktopAppScreen(
    prefs: AppPreferences,
    libraryViewModel: ILibraryViewModel,
    playerViewModel: IPlayerViewModel,
    visualizerManager: Any? = null,
    equalizerManager: Any? = null,
    appPlayer: Any? = null
) {
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val currentQueue by playerViewModel.currentQueue.collectAsState()
    val paletteColors by playerViewModel.paletteColors.collectAsState()
    val repeatModeState by playerViewModel.repeatMode.collectAsState()
    val shuffleModeState by playerViewModel.shuffleModeEnabled.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()
    val playbackPitch by playerViewModel.playbackPitch.collectAsState()
    val reverbEnabled by playerViewModel.reverbEnabled.collectAsState()
    val effectsPreset by playerViewModel.effectsPreset.collectAsState()
    
    val isMicModeActive by playerViewModel.isMicModeActive.collectAsState()
    val streamConfigEffectsVisible by playerViewModel.streamConfigEffectsVisible.collectAsState()
    val bgStyle by prefs.backgroundStyleFlow.collectAsState(initial = 0)

    val dominantTint by animateColorAsState(targetValue = paletteColors.dominant.copy(alpha = 0.35f), animationSpec = tween(900), label = "dominant_tint")
    val vibrantTint by animateColorAsState(targetValue = paletteColors.vibrant.copy(alpha = 0.25f), animationSpec = tween(900), label = "vibrant_tint")
    val lightVibrantTint by animateColorAsState(targetValue = paletteColors.lightVibrant.copy(alpha = 0.25f), animationSpec = tween(1000), label = "lv")
    val accentColor by animateColorAsState(targetValue = paletteColors.vibrant, animationSpec = tween(700), label = "accent")
    
    val systemBg = MaterialTheme.colorScheme.background

    val bgModifier = when (bgStyle) {
        0 -> Modifier.background(systemBg).background(dominantTint).background(vibrantTint)
        3 -> Modifier.background(systemBg).background(lightVibrantTint)
        else -> Modifier.background(Color.Transparent)
    }

    var currentPage by remember { mutableStateOf(prefs.lastMainScreenPage) }

    var sleepTimerSeconds by remember { mutableStateOf(0) }
    var trackToAddToPlaylist by remember { mutableStateOf<TrackEntity?>(null) }

    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        try { focusRequester.requestFocus() } catch (e: Exception) {}
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .then(bgModifier)
            .focusRequester(focusRequester)
            .focusable()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { try { focusRequester.requestFocus() } catch(e: Exception) {} }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                    val isLeft = event.key == Key.DirectionLeft
                    val isRight = event.key == Key.DirectionRight
                    if (isLeft || isRight) {
                        if (isLeft && currentPage > 0) currentPage--
                        else if (isRight && currentPage < 2) currentPage++
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && !event.isCtrlPressed) {
                    val isLeft = event.key == Key.DirectionLeft
                    val isRight = event.key == Key.DirectionRight
                    if (isLeft || isRight) {
                        if (isLeft && currentPage > 0) currentPage--
                        else if (isRight && currentPage < 2) currentPage++
                        return@onKeyEvent true
                    }
                }
                false
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val streamConfigUiVisible by playerViewModel.streamConfigUiVisible.collectAsState()
                val hideBottomBar = currentPage == 2

                androidx.compose.animation.AnimatedVisibility(
                    visible = !hideBottomBar,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    DesktopBottomNavigationBar(
                        currentPage = currentPage,
                        onPageChange = { currentPage = it },
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        accentColor = accentColor,
                        paletteColors = paletteColors,
                        bgStyle = bgStyle,
                        hasUsedMiniplayerGesture = prefs.hasUsedMiniplayerGesture,
                        onMiniplayerGestureUsed = { prefs.hasUsedMiniplayerGesture = true },
                        hasSeenTutorial = prefs.hasSeenTutorial,
                        currentPos = 0L,
                        duration = 1L,
                        albumArtBitmap = currentTrack?.let { com.example.beatpulse.ui.components.rememberAlbumArt(it) },
                        onPlayPauseClick = { }
                    )
                }
            }
        ) { innerPadding ->
            AppScreenContent(
                currentPage = currentPage,
                initialState = currentPage,
                visualizerManager = visualizerManager,
                equalizerManager = equalizerManager,
                libraryViewModel = libraryViewModel,
                playerViewModel = playerViewModel,
                appPlayer = appPlayer,
                currentTrack = currentTrack,
                currentQueue = currentQueue,
                paletteColors = paletteColors,
                prefs = prefs,
                innerPadding = innerPadding,
                onPageChange = { currentPage = it }
            )
        }
    }
}

@Composable
fun AppScreenContent(
    currentPage: Int,
    initialState: Int,
    visualizerManager: Any?,
    equalizerManager: Any?,
    libraryViewModel: ILibraryViewModel,
    playerViewModel: com.example.beatpulse.player.IPlayerViewModel,
    appPlayer: Any?,
    currentTrack: com.example.beatpulse.data.TrackEntity?,
    currentQueue: List<com.example.beatpulse.data.TrackEntity>,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    prefs: com.example.beatpulse.data.AppPreferences,
    innerPadding: PaddingValues,
    onPageChange: (Int) -> Unit
) {
    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            val spec = tween<androidx.compose.ui.unit.IntOffset>(250)
            val specFloat = tween<Float>(250)
            if (targetState > initialState) {
                if (initialState == 0 && targetState == 2) {
                    slideInHorizontally(animationSpec = spec) { width -> -width } + fadeIn(animationSpec = specFloat) togetherWith slideOutHorizontally(animationSpec = spec) { width -> width } + fadeOut(animationSpec = specFloat)
                } else {
                    slideInHorizontally(animationSpec = spec) { width -> width } + fadeIn(animationSpec = specFloat) togetherWith slideOutHorizontally(animationSpec = spec) { width -> -width } + fadeOut(animationSpec = specFloat)
                }
            } else {
                if (initialState == 2 && targetState == 0) {
                    slideInHorizontally(animationSpec = spec) { width -> width } + fadeIn(animationSpec = specFloat) togetherWith slideOutHorizontally(animationSpec = spec) { width -> -width } + fadeOut(animationSpec = specFloat)
                } else {
                    slideInHorizontally(animationSpec = spec) { width -> -width } + fadeIn(animationSpec = specFloat) togetherWith slideOutHorizontally(animationSpec = spec) { width -> width } + fadeOut(animationSpec = specFloat)
                }
            }.using(SizeTransform(clip = false))
        },
        modifier = Modifier.fillMaxSize(),
        label = "page_transition"
    ) { page ->
        when (page) {
            0 -> Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                DesktopLibraryScreen(
                    libraryViewModel = libraryViewModel,
                    playerViewModel = playerViewModel,
                    paletteColors = paletteColors,
                    onPageChange = onPageChange
                )
            }
            1 -> Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                DesktopListaGeneralScreen(
                    libraryViewModel = libraryViewModel,
                    playerViewModel = playerViewModel,
                    paletteColors = paletteColors,
                    onPageChange = onPageChange
                )
            }
            2 -> Box(modifier = Modifier.fillMaxSize()) {
                DesktopPlayerScreen(
                    visualizerManager = visualizerManager as com.example.beatpulse.visualizer.AppVisualizerManager,
                    equalizerManager = equalizerManager as com.example.beatpulse.player.AppEqualizerManager,
                    playerViewModel = playerViewModel,
                    state = DesktopPlayerScreenState(
                        appPlayer = appPlayer as? com.example.beatpulse.player.AppPlayer,
                        currentTrack = currentTrack,
                        currentQueue = currentQueue,
                        paletteColors = paletteColors,
                        prefs = prefs,
                        bottomPadding = innerPadding.calculateBottomPadding()
                    ),
                    callbacks = DesktopPlayerScreenCallbacks(
                        onPlayTrack = { track, queue -> playerViewModel.playTrack(track, queue) }
                    )
                )
            }
        }
    }
}
