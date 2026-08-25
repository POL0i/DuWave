package com.example.beatpulse.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.beatpulse.ui.components.player.IPreferencesManager
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.player.AppEqualizerManager
import com.example.beatpulse.ui.components.BottomNavigationBar
import com.example.beatpulse.ui.components.StyleNotificationOverlay
import com.example.beatpulse.ui.components.backgrounds.*
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
    val paletteColors by playerViewModel.paletteColors.collectAsState()
    val repeatModeState by playerViewModel.repeatMode.collectAsState()
    val shuffleModeState by playerViewModel.shuffleModeEnabled.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()
    val playbackPitch by playerViewModel.playbackPitch.collectAsState()
    val reverbEnabled by playerViewModel.reverbEnabled.collectAsState()
    val effectsPreset by playerViewModel.effectsPreset.collectAsState()
    
    val isMicModeActive by playerViewModel.isMicModeActive.collectAsState()
    val streamConfigEffectsVisible by playerViewModel.streamConfigEffectsVisible.collectAsState()
    val bgStyle = 0
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

    var currentPage by remember { mutableIntStateOf(0) }


    var trackToAddToPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
    val playlists by libraryViewModel.playlists.collectAsState()

    var sleepTimerSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(sleepTimerSeconds) {
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

    val content: @Composable () -> Unit = {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val streamConfigUiVisible by playerViewModel.streamConfigUiVisible.collectAsState()
                val hideBottomBar = currentPage == 2 && isMicModeActive && !streamConfigUiVisible

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
                        accentColor = accentColor,
                        paletteColors = paletteColors,
                        bgStyle = bgStyle,
                        prefs = prefs,
                        onPlayPauseClick = { if (playerViewModel.isPlaying.value) playerViewModel.pause() else playerViewModel.play() }
                    )
                }
            }
        ) { innerPadding ->
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
                    0 -> Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        UnifiedLibraryScreen(
                            viewModel = libraryViewModel,
                            statsViewModel = statsViewModel,
                            paletteColors = paletteColors,
                            currentPlayingTrack = currentTrack,
                            isPlaying = isPlaying,
                            onTrackClick = { track, queue ->
                                playerViewModel.playTrack(track, queue)
                                currentPage = 2
                            }
                        )
                    }
                    1 -> Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            paletteColors = paletteColors,
                            currentPlayingTrack = currentTrack,
                            isPlaying = isPlaying,
                            onTrackClick = { track, queue ->
                                playerViewModel.playTrack(track, queue)
                                currentPage = 2
                            }
                        )
                    }
                    2 -> Box(modifier = Modifier.fillMaxSize()) {
                        PlayerScreen(
                            modifier = Modifier,
                            playerViewModel = playerViewModel,
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
            else -> Box(modifier = Modifier.fillMaxSize().then(bgModifier)) { content() }
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

    var showTutorial by remember { mutableStateOf(!false) }
    var showSwipeHint by remember { mutableStateOf(false) }

    if (showTutorial) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable {
                showTutorial = false
                showTutorial = false
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
                    Button(onClick = { ; SystemUtils.recreateApp() }, colors = ButtonDefaults.buttonColors(containerColor = if ("es" == "es") paletteColors.vibrant else Color.DarkGray)) { Text("🇲🇽 ES") }
                    Button(onClick = { ; SystemUtils.recreateApp() }, colors = ButtonDefaults.buttonColors(containerColor = if ("es" == "en") paletteColors.vibrant else Color.DarkGray)) { Text("🇬🇧 EN") }
                    Button(onClick = { ; SystemUtils.recreateApp() }, colors = ButtonDefaults.buttonColors(containerColor = if ("es" == "pt") paletteColors.vibrant else Color.DarkGray)) { Text("🇧🇷 PT") }
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
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.6f)).clickable { showSwipeHint = false },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 160.dp).padding(horizontal = 24.dp)) {
                Box(modifier = Modifier.offset(x = offsetX.dp).size(24.dp).background(Color.White, androidx.compose.foundation.shape.CircleShape))
                Spacer(modifier = Modifier.height(16.dp))
                Text("¡Desliza el minirreproductor a la derecha para elegir una canción!", color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            }
        }
    }
}
