package com.example.beatpulse.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.beatpulse.ui.components.player.IPreferencesManager
import com.example.beatpulse.data.AppPreferences
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.player.AppEqualizerManager
import com.example.beatpulse.ui.components.BottomNavigationBar
import com.example.beatpulse.ui.components.StyleNotificationOverlay
import com.example.beatpulse.ui.components.backgrounds.*
import com.example.beatpulse.ui.components.backgrounds.TerrariaWaterBackground
import com.example.beatpulse.ui.components.backgrounds.ZenClearBackground
import com.example.beatpulse.ui.components.backgrounds.SandsFlowBackground
import com.example.beatpulse.ui.components.player.PlayerScreen
import com.example.beatpulse.ui.components.player.PlayerScreenCallbacks
import com.example.beatpulse.ui.components.player.PlayerScreenState
import com.example.beatpulse.ui.screens.LibraryScreen
import com.example.beatpulse.ui.screens.UnifiedLibraryScreen
import com.example.beatpulse.ui.viewmodels.ILibraryViewModel
import com.example.beatpulse.ui.components.player.IPlayerViewModel
import com.example.beatpulse.utils.SystemUtils
import com.example.beatpulse.utils.getLocalizedString
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset

val LocalCoverOffset = staticCompositionLocalOf<Offset> { Offset.Zero }
val LocalAlbumArtCenterY = staticCompositionLocalOf<Float?> { null }

@Composable
fun AppScreen(
    visualizerManager: IAudioVisualizerManager,
    equalizerManager: com.example.beatpulse.ui.components.player.IEqualizerManager,
    prefs: IPreferencesManager,
    libraryViewModel: ILibraryViewModel,
    playerViewModel: IPlayerViewModel,
    statsViewModel: com.example.beatpulse.ui.screens.StatsViewModel
) {
    val exoPlayer by playerViewModel.playerState.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val currentQueue by playerViewModel.currentQueue.collectAsState()
    val paletteColorsFlow by playerViewModel.paletteColors.collectAsState()
    val cleanUiMode = playerViewModel.cleanUiMode.collectAsState().value
    val dynamicColorsPlus = playerViewModel.dynamicColorsPlus.collectAsState().value
    val dynamicColorsInterval = playerViewModel.dynamicColorsInterval.collectAsState().value
    
    var activeDynamicColor by remember { mutableStateOf<Color?>(null) }
    
    LaunchedEffect(Unit) {
        com.example.beatpulse.data.sync.EcosystemManager.startEcosystem()
    }
    
    LaunchedEffect(dynamicColorsPlus, dynamicColorsInterval, paletteColorsFlow) {
        if (!dynamicColorsPlus) {
            activeDynamicColor = null
            return@LaunchedEffect
        }
        val colors = listOf(
            paletteColorsFlow.dominant,
            paletteColorsFlow.vibrant,
            paletteColorsFlow.lightVibrant,
            paletteColorsFlow.darkVibrant,
            paletteColorsFlow.muted,
            paletteColorsFlow.darkMuted
        ).distinct().filter { it != Color.Black && it != Color.White && it != Color.Transparent }
        
        if (colors.isEmpty()) {
            activeDynamicColor = null
            return@LaunchedEffect
        }
        
        val recentColors = mutableListOf<Color>()
        while (true) {
            val availableColors = colors.filter { it !in recentColors }
            val nextColor = if (availableColors.isNotEmpty()) {
                availableColors.random()
            } else {
                colors.random()
            }
            
            activeDynamicColor = nextColor
            recentColors.add(nextColor)
            if (recentColors.size >= colors.size / 2 && recentColors.size > 0) {
                recentColors.removeAt(0)
            }
            
            kotlinx.coroutines.delay(dynamicColorsInterval * 1000L)
        }
    }
    
    val animatedDominantColor by animateColorAsState(
        targetValue = activeDynamicColor ?: paletteColorsFlow.dominant, 
        animationSpec = tween(3000)
    )
    
    val paletteColors = if (dynamicColorsPlus && activeDynamicColor != null) {
        paletteColorsFlow.copy(
            dominant = animatedDominantColor,
            vibrant = animatedDominantColor,
            lightVibrant = animatedDominantColor,
            darkVibrant = animatedDominantColor,
            muted = animatedDominantColor,
            darkMuted = animatedDominantColor
        )
    } else {
        paletteColorsFlow
    }
    val repeatModeState by playerViewModel.repeatMode.collectAsState()
    val shuffleModeState by playerViewModel.shuffleModeEnabled.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()
    val playbackPitch by playerViewModel.playbackPitch.collectAsState()
    val reverbEnabled by playerViewModel.reverbEnabled.collectAsState()
    val effectsPreset by playerViewModel.effectsPreset.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val coverDragEnabled by playerViewModel.coverDragEnabled.collectAsState()
    
    val isMicModeActive by playerViewModel.isMicModeActive.collectAsState()
    val streamConfigEffectsVisible by playerViewModel.streamConfigEffectsVisible.collectAsState()
    val bgStyle by libraryViewModel.prefs.backgroundStyleFlow.collectAsState()
    val scope = rememberCoroutineScope()

    var globalToastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        
    }

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

    var currentPage by remember { mutableIntStateOf(libraryViewModel.prefs.lastMainScreenPage) }
    
    val pageFlowValue by libraryViewModel.prefs.lastMainScreenPageFlow.collectAsState()
    val initialPage = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % 3) + libraryViewModel.prefs.lastMainScreenPage
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })

    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage % 3
    }

    LaunchedEffect(currentPage) {
        libraryViewModel.prefs.lastMainScreenPage = currentPage
        if (pagerState.currentPage % 3 != currentPage) {
            val diff = currentPage - (pagerState.currentPage % 3)
            val optimalDiff = when (diff) {
                2 -> -1
                -2 -> 1
                else -> diff
            }
            pagerState.animateScrollToPage(pagerState.currentPage + optimalDiff)
        }
    }

    LaunchedEffect(pageFlowValue) {
        if (currentPage != pageFlowValue) currentPage = pageFlowValue
    }


    var trackToAddToPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
    val playlists by libraryViewModel.playlists.collectAsState()

    var sleepTimerSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(sleepTimerSeconds > 0) {
        if (sleepTimerSeconds > 0) {
            while (sleepTimerSeconds > 0) {
                delay(1000)
                sleepTimerSeconds -= 1
            }
            if (playerViewModel.isPlaying.value) {
                playerViewModel.pause()
            }
        }
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    
    LaunchedEffect(prefs) {
        prefs.toastFlow.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val content: @Composable () -> Unit = {
        Scaffold(
            snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                com.example.beatpulse.ui.components.DownloadsFab(
                    modifier = Modifier.padding(bottom = 16.dp, end = 16.dp),
                    paletteColors = paletteColors
                )
            },
            modifier = Modifier.onPreviewKeyEvent { event ->
                if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                    val isShift = event.isShiftPressed
                    val isCtrl = event.isCtrlPressed
                    

                    if (event.key == androidx.compose.ui.input.key.Key.Tab) {
                        com.example.beatpulse.core.focus.AppFocusManager.cycleTabNavigation(currentPage, forward = !isShift)
                        return@onPreviewKeyEvent false // Let Native Compose Focus Manager handle Tab
                    }
                    if (com.example.beatpulse.core.focus.AppFocusManager.isTabNavigationActive) {
                        if (event.key == androidx.compose.ui.input.key.Key.DirectionLeft && isShift) {
                            com.example.beatpulse.core.focus.AppFocusManager.cancelTabNavigation()
                            focusManager.clearFocus()
                            return@onPreviewKeyEvent true
                        }
                        if (event.key == androidx.compose.ui.input.key.Key.Escape) {
                            com.example.beatpulse.core.focus.AppFocusManager.cancelTabNavigation()
                            focusManager.clearFocus()
                            return@onPreviewKeyEvent false
                        }
                    } else {
                        if (event.key == androidx.compose.ui.input.key.Key.Escape) {
                            if (com.example.beatpulse.utils.SystemUtils.dispatchSystemBack()) {
                                return@onPreviewKeyEvent true
                            }
                        }
                    }

                    val shiftStr = if (event.isShiftPressed) "Shift+" else ""
                    val altStr = if (event.isAltPressed) "Alt+" else ""
                    val ctrlStr = if (event.isCtrlPressed) "Ctrl+" else ""
                    val metaStr = if (event.isMetaPressed) "Meta+" else ""
                    
                    val baseKey = when(event.key) {
                        androidx.compose.ui.input.key.Key.DirectionRight -> "DirectionRight"
                        androidx.compose.ui.input.key.Key.DirectionLeft -> "DirectionLeft"
                        androidx.compose.ui.input.key.Key.DirectionUp -> "DirectionUp"
                        androidx.compose.ui.input.key.Key.DirectionDown -> "DirectionDown"
                        androidx.compose.ui.input.key.Key.Enter, androidx.compose.ui.input.key.Key.NumPadEnter -> "Enter"
                        androidx.compose.ui.input.key.Key.Spacebar -> "Spacebar"
                        androidx.compose.ui.input.key.Key.Escape -> "Escape"
                        androidx.compose.ui.input.key.Key.Tab -> "Tab"
                        else -> {
                            val name = event.key.toString()
                            var ext = name.substringAfterLast("Key: ").substringBefore(")")
                            if (!name.contains("Key:")) ext = event.key.keyCode.toString()
                            if (ext.contains("Unknown")) ext = "Unknown"
                            ext
                        }
                    }
                    val eventStr = "$ctrlStr$altStr$metaStr$shiftStr$baseKey"

                    when (eventStr) {
                        prefs.keyMapGlobalList -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_GLOBAL_LIST)
                            currentPage = 1
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapGlobalSearch -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_GLOBAL_SEARCH)
                            currentPage = 1
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapRecommendations -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_RECOMMENDATIONS)
                            currentPage = 1
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapLibraryPlaylists -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_LIBRARY_PLAYLISTS)
                            currentPage = 0
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapLibraryArtists -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_LIBRARY_ARTISTS)
                            currentPage = 0
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapLibraryAlbums -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_LIBRARY_ALBUMS)
                            currentPage = 0
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapLibraryFolders -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_LIBRARY_FOLDERS)
                            currentPage = 0
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapPlayerScreen -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_PLAYER)
                            currentPage = 2
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapOpenStats -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.OPEN_STATS)
                            currentPage = 0
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapOpenDesign -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.OPEN_DESIGN_SETTINGS)
                            currentPage = 0
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapOpenKeyboard -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.OPEN_KEYBOARD_SHORTCUTS)
                            currentPage = 0
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapOpenTimer -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.OPEN_TIMER)
                            currentPage = 2
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapOpenEqualizer -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.OPEN_EQUALIZER)
                            currentPage = 2
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapOpenAudioEffects -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.OPEN_AUDIO_EFFECTS)
                            currentPage = 2
                            return@onPreviewKeyEvent true
                        }
                        prefs.keyMapOpenPatreon -> {
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.OPEN_PATREON)
                            currentPage = 2
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                false
            },
            containerColor = Color.Transparent,
            bottomBar = {
                val streamConfigUiVisible by playerViewModel.streamConfigUiVisible.collectAsState()
                val hideBottomBar = currentPage == 2 && (cleanUiMode || (isMicModeActive && !streamConfigUiVisible))

                AnimatedVisibility(
                    visible = !hideBottomBar,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {

                        BottomNavigationBar(
                            currentPage = currentPage,
                            onPageChange = { currentPage = it },
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            accentColor = accentColor,
                            paletteColors = paletteColors,
                            bgStyle = bgStyle,
                            prefs = prefs,
                            onPlayPauseClick = { if (playerViewModel.isPlaying.value) playerViewModel.pause() else playerViewModel.play() }
                        )
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) { page ->
                when (page % 3) {
                    0 -> Box(modifier = Modifier.padding(innerPadding).fillMaxSize().clipToBounds()) {
                        UnifiedLibraryScreen(
                            viewModel = libraryViewModel,
                            statsViewModel = statsViewModel,
                            paletteColors = paletteColors,
                            currentPlayingTrack = currentTrack,
                            isPlaying = isPlaying,
                            onTrackClick = { track, queue ->
                                playerViewModel.playTrack(track, queue)
                                currentPage = 2
                            },
                            onPausePlayback = { if (playerViewModel.isPlaying.value) playerViewModel.pause() }
                        )
                    }
                    1 -> Box(modifier = Modifier.padding(innerPadding).fillMaxSize().clipToBounds()) {
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            paletteColors = paletteColors,
                            currentPlayingTrack = currentTrack,
                            isPlaying = isPlaying,
                            onTrackClick = { track, queue ->
                                playerViewModel.playTrack(track, queue)
                                currentPage = 2
                            },
                            onPausePlayback = { if (playerViewModel.isPlaying.value) playerViewModel.pause() }
                        )
                    }
                    2 -> Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                        com.example.beatpulse.utils.SystemStatusBarVisibility(visible = pagerState.currentPage != 2)
                        PlayerScreen(
                            isFocused = pagerState.currentPage == 2,
                            modifier = Modifier,
                            playerViewModel = playerViewModel,
                            dynamicColorsPlus = dynamicColorsPlus,
                            activeDynamicColor = activeDynamicColor,
                            dynamicColorsInterval = dynamicColorsInterval,
                            cleanUiMode = cleanUiMode,
                            coverDragEnabled = playerViewModel.coverDragEnabled.collectAsState().value,
                            coverVisibilityMode = playerViewModel.coverVisibilityMode.collectAsState().value,
                            chromaKeyColor = playerViewModel.chromaKeyColor.collectAsState().value,
                            coverScale = playerViewModel.coverScale.collectAsState().value,
                            coverOffsetX = playerViewModel.coverOffsetX.collectAsState().value,
                            coverOffsetY = playerViewModel.coverOffsetY.collectAsState().value,
                            visualizerManager = visualizerManager,
                            equalizerManager = equalizerManager,
                            state = PlayerScreenState(
                                currentTrack = currentTrack,
                                currentQueue = currentQueue,
                                paletteColors = paletteColors,
                                bottomPadding = innerPadding.calculateBottomPadding(),
                                prefs = prefs,
                                repeatModeState = repeatModeState,
                                shuffleModeState = shuffleModeState,
                                playbackSpeed = playbackSpeed,
                                playbackPitch = playbackPitch,
                                reverbEnabled = reverbEnabled,
                                effectsPreset = effectsPreset,
                                sleepTimerSeconds = sleepTimerSeconds
                            ),
                            callbacks = PlayerScreenCallbacks(
                                onPlayTrack = { track, queue -> playerViewModel.playTrack(track, queue) },
                                onSetSpeed = { speed -> playerViewModel.setSpeed(speed) },
                                onSetPitch = { pitch -> playerViewModel.setPitch(pitch) },
                                onSetReverb = { enabled -> playerViewModel.setReverb(enabled) },
                                onApplyPreset = { preset -> playerViewModel.applyPreset(preset) },
                                onSetSleepTimer = { seconds -> sleepTimerSeconds = seconds },
                                onUpdateTrackMetadata = { id, title, artist, album, coverPath ->
                                    playerViewModel.updateTrackMetadata(id, title, artist, album, coverPath)
                                },
                                onAddToPlaylist = { track -> trackToAddToPlaylist = track }
                            )
                        )
                    }
                }
            }
        }
    }

    val effectiveBgStyle = if (isMicModeActive && !streamConfigEffectsVisible) 0 else bgStyle
    
    val coverOffsetX by playerViewModel.coverOffsetX.collectAsState()
    val coverOffsetY by playerViewModel.coverOffsetY.collectAsState()
    val albumArtCenterY by derivedStateOf { playerViewModel.albumArtCenterY }
    CompositionLocalProvider(
        LocalCoverOffset provides androidx.compose.ui.geometry.Offset(coverOffsetX, coverOffsetY),
        LocalAlbumArtCenterY provides albumArtCenterY
    ) {
        AnimatedContent(
            targetState = effectiveBgStyle,
            transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) },
            label = "bg_transition"
        ) { style ->
            when (style) {
                1 -> CyberpunkBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                2 -> AnimeBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                3 -> LuminousBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                4 -> Y2KBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                5 -> DarkAmbientBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                6 -> GothicFantasyBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                7 -> CathedralFantasyBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2, ) { content() }
                8 -> TaleLegendBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                9 -> RetroWallBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                10 -> FountainBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                11 -> TerrariaWaterBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                12 -> ZenClearBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                13 -> SandsFlowBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                14 -> com.example.beatpulse.ui.components.backgrounds.LullabyEyesBackground(paletteColors = paletteColors, visualizerManager = visualizerManager, isPlayerScreen = currentPage == 2) { content() }
                15 -> {
                    val amps = visualizerManager.combinedAmplitudes.collectAsState().value
                    val energy = if (amps.isNotEmpty()) amps.average().toFloat() else 0f
                    com.example.beatpulse.ui.components.backgrounds.RetroCRTBackground(dominantColor = paletteColors.dominant, vibrantColor = paletteColors.vibrant, mutedColor = paletteColors.muted, dynamicEnergy = energy, dynamicOffsetY = 0f, dynamicOffsetX = 0f, isPlayerScreen = currentPage == 2) { content() }
                }
                16 -> {
                    val amps = visualizerManager.combinedAmplitudes.collectAsState().value
                    val energy = if (amps.isNotEmpty()) amps.average().toFloat() else 0f
                    com.example.beatpulse.ui.components.backgrounds.ProceduralCRTCdc3rxBackground(dominantColor = paletteColors.dominant, vibrantColor = paletteColors.vibrant, mutedColor = paletteColors.muted, dynamicEnergy = energy, dynamicOffsetY = 0f, dynamicOffsetX = 0f, isPlayerScreen = currentPage == 2) { content() }
                }
                else -> { Box(modifier = Modifier.fillMaxSize().then(bgModifier)) { content() } }
            }
        }
    }

    StyleNotificationOverlay(message = globalToastMessage) {
        globalToastMessage = null
    }

    trackToAddToPlaylist?.let { trackToAdd ->
        AlertDialog(
            onDismissRequest = { trackToAddToPlaylist = null },
            title = { Text(getLocalizedString("add_to_playlist"), color = paletteColors.vibrant) },
            text = {
                if (playlists.isEmpty()) {
                    Text(getLocalizedString("no_playlists_long"), color = Color.White)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(playlists) { pl ->
                            ListItem(
                                headlineContent = { Text(pl.name, color = Color.White) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    libraryViewModel.addTrackToPlaylist(pl.playlistId, trackToAdd)
                                    SystemUtils.showToast("Añadida a ${pl.name}")
                                    trackToAddToPlaylist = null
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { trackToAddToPlaylist = null }) {
                    Text(getLocalizedString("close"), color = paletteColors.vibrant)
                }
            },
            containerColor = paletteColors.dominant.copy(alpha = 0.9f)
        )
    }

    var showTutorial by remember { mutableStateOf(!prefs.hasSeenTutorial) }

    val showFps by playerViewModel.showFps.collectAsState()
    var currentFps by remember { mutableStateOf(0) }
    LaunchedEffect(showFps) {
        if (showFps) {
            var frameCount = 0
            var lastTime = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.let { 0L } ?: System.nanoTime()
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    frameCount++
                    val elapsed = frameTimeNanos - lastTime
                    if (elapsed >= 1_000_000_000L) {
                        currentFps = ((frameCount * 1_000_000_000.0) / elapsed).toInt()
                        frameCount = 0
                        lastTime = frameTimeNanos
                    }
                }
            }
        }
    }

    if (showFps) {
        Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp, end = 24.dp), contentAlignment = Alignment.TopEnd) {
            Text(
                text = "$currentFps FPS",
                color = Color.Green,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    var blurRadius by remember { mutableFloatStateOf(if (showTutorial) 30f else 0f) }
    var blurTarget by remember { mutableFloatStateOf(if (showTutorial) 30f else 0f) }
    var showSwipeHint by remember { mutableStateOf(false) }

    if (showTutorial) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable {
                showTutorial = false
                prefs.hasSeenTutorial = true
                showSwipeHint = true
            },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text(text = getLocalizedString("welcome_title"), color = Color.White, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = getLocalizedString("welcome_body"), color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { (prefs as AppPreferences).appLanguage = "es" }, colors = ButtonDefaults.buttonColors(containerColor = if ((prefs as AppPreferences).appLanguage == "es") paletteColors.vibrant else Color.DarkGray)) { Text("🇲🇽 ES") }
                    Button(onClick = { (prefs as AppPreferences).appLanguage = "en" }, colors = ButtonDefaults.buttonColors(containerColor = if ((prefs as AppPreferences).appLanguage == "en") paletteColors.vibrant else Color.DarkGray)) { Text("🇬🇧 EN") }
                    Button(onClick = { (prefs as AppPreferences).appLanguage = "pt" }, colors = ButtonDefaults.buttonColors(containerColor = if ((prefs as AppPreferences).appLanguage == "pt") paletteColors.vibrant else Color.DarkGray)) { Text("🇧🇷 PT") }
                }
            }
        }
    }

    if (showSwipeHint) {
        val infiniteTransition = rememberInfiniteTransition()
        val offsetX by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = -100f,
            animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart)
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(bottom = 160.dp)
                    .padding(horizontal = 24.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showSwipeHint = false }
                    .padding(16.dp)
            ) {
                Box(modifier = Modifier.offset(x = offsetX.dp).size(24.dp).background(Color.White, androidx.compose.foundation.shape.CircleShape))
                Spacer(modifier = Modifier.height(16.dp))
                Text(getLocalizedString("swipe_to_choose"), color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
        }
    }
}
