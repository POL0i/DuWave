package com.example.beatpulse.ui.components.player
import kotlinx.coroutines.isActive

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.Feedback

import androidx.compose.animation.AnimatedVisibility
import com.example.beatpulse.utils.getLocalizedString
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateColor
import androidx.compose.material.icons.filled.Sync
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.AllOut
import androidx.compose.material.icons.filled.LensBlur
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.List

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.positionInRoot

import kotlin.math.abs
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beatpulse.ui.components.PixelIcons
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.visualizer.FilterMode
import kotlinx.coroutines.delay
import com.example.beatpulse.utils.SystemBackHandler

@Composable
fun PlayerScreen(
    playerViewModel: IPlayerViewModel,
    dynamicColorsPlus: Boolean,
    activeDynamicColor: Color?,
    dynamicColorsInterval: Int,
    cleanUiMode: Boolean,
    coverDragEnabled: Boolean,
    coverVisibilityMode: String,
    chromaKeyColor: String,
    coverScale: Float,
    coverOffsetX: Float,
    coverOffsetY: Float,
    visualizerManager: IAudioVisualizerManager,
    equalizerManager: IEqualizerManager,
    state: PlayerScreenState,
    callbacks: PlayerScreenCallbacks,
    isFocused: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Delegate to the internal implementation to keep the top-level function's register count low
    PlayerScreenContent(
        playerViewModel = playerViewModel,
        dynamicColorsPlus = dynamicColorsPlus,
        activeDynamicColor = activeDynamicColor,
        dynamicColorsInterval = dynamicColorsInterval,
        cleanUiMode = cleanUiMode,
        coverDragEnabled = coverDragEnabled,
        coverVisibilityMode = coverVisibilityMode,
        chromaKeyColor = chromaKeyColor,
        coverScale = coverScale,
        coverOffsetX = coverOffsetX,
        coverOffsetY = coverOffsetY,
        visualizerManager = visualizerManager,
        equalizerManager = equalizerManager,
        state = state,
        callbacks = callbacks,
        isFocused = isFocused,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreenContent(
    playerViewModel: IPlayerViewModel,
    dynamicColorsPlus: Boolean,
    activeDynamicColor: Color?,
    dynamicColorsInterval: Int,
    cleanUiMode: Boolean,
    coverDragEnabled: Boolean,
    coverVisibilityMode: String,
    chromaKeyColor: String,
    coverScale: Float,
    coverOffsetX: Float,
    coverOffsetY: Float,
    visualizerManager: IAudioVisualizerManager,
    equalizerManager: IEqualizerManager,
    state: PlayerScreenState,
    callbacks: PlayerScreenCallbacks,
    isFocused: Boolean = true,
    modifier: Modifier = Modifier
) {
    com.example.beatpulse.utils.SystemStatusBarVisibility(visible = false)
    val currentTrack = state.currentTrack
    val currentQueue = state.currentQueue
    val bottomPadding = state.bottomPadding
    val prefs = state.prefs
    val sleepTimerSeconds = state.sleepTimerSeconds
    val onPlayTrack = callbacks.onPlayTrack
    val onSetSpeed = callbacks.onSetSpeed
    val onSetPitch = callbacks.onSetPitch
    val onSetReverb = callbacks.onSetReverb
    val onApplyPreset = callbacks.onApplyPreset
    val onSetSleepTimer = callbacks.onSetSleepTimer
    val onUpdateTrackMetadata = callbacks.onUpdateTrackMetadata
    val onAddToPlaylist = callbacks.onAddToPlaylist
    val isBuffering = playerViewModel.isBuffering.collectAsState().value

    val bassAmplitudesState = visualizerManager.bassAmplitudes.collectAsState()
    val midAmplitudesState = visualizerManager.midAmplitudes.collectAsState()
    val highAmplitudesState = visualizerManager.highAmplitudes.collectAsState()
    val bgStyle = 0
    var currentStyle by remember {
        mutableStateOf(
            try {
                VisualizerStyle.valueOf(prefs.visualizerStyle.uppercase())
            } catch (e: Exception) {
                VisualizerStyle.BARS
            }
        )
    }
    LaunchedEffect(currentStyle) { prefs.visualizerStyle = currentStyle.name.lowercase() }

    LaunchedEffect(Unit) {
        com.example.beatpulse.core.focus.AppFocusManager.focusActions.collect { action ->
            if (prefs.lastMainScreenPage == 2) {
                val curPos = playerViewModel.currentPosition.value
                val maxDuration = playerViewModel.duration.value
                if (action == com.example.beatpulse.core.focus.FocusAction.NAVIGATE_LEFT) {
                    playerViewModel.seekTo((curPos - 10000).coerceAtLeast(0))
                } else if (action == com.example.beatpulse.core.focus.FocusAction.NAVIGATE_RIGHT) {
                    playerViewModel.seekTo((curPos + 10000).coerceAtMost(if (maxDuration > 0) maxDuration else Long.MAX_VALUE))
                }
            }
        }
    }

    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()

    val abRepeatModeEnabled by playerViewModel.abRepeatModeEnabled.collectAsState()
    val abPointA by playerViewModel.abPointA.collectAsState()
    val abPointB by playerViewModel.abPointB.collectAsState()
    var activeDraggingHandle by remember { mutableStateOf<String?>(null) }
    val isFetchingLyrics by playerViewModel.isFetchingLyrics.collectAsState()
    val searchFailed by playerViewModel.searchFailed.collectAsState()
    val availableLyricsResults by playerViewModel.availableLyricsResults.collectAsState()
    val autoAnalyzeLyrics by playerViewModel.autoAnalyzeLyrics.collectAsState()
    var showLyricsMatches by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf<List<com.example.beatpulse.utils.LyricLine>>(emptyList()) }

    LaunchedEffect(currentTrack) {
        if (currentTrack != null) {
            val lrcFile = java.io.File(currentTrack.dataPath.substringBeforeLast(".") + ".lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                lyrics = emptyList()
            } else { lyrics = emptyList() }
        } else { lyrics = emptyList() }
    }

    val isAdvanced by visualizerManager.isAdvancedMode.collectAsState()
    val filterMode by visualizerManager.filterMode.collectAsState()
    val sensitivity by visualizerManager.sensitivity.collectAsState()
    val reactivity by visualizerManager.reactivity.collectAsState()
    val isMicModeActive by playerViewModel.isMicModeActive.collectAsState()
    val streamAvatarUri by playerViewModel.streamAvatarUri.collectAsState()
    val albumArtBitmap = if (isMicModeActive && streamAvatarUri != null) {
        com.example.beatpulse.ui.components.rememberStreamAvatar(streamAvatarUri)
    } else {
        currentTrack?.let { com.example.beatpulse.ui.components.rememberFullAlbumArt(it) }
    }
    
    var localPalette by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.beatpulse.theme.PaletteColors?>(null) }
    androidx.compose.runtime.LaunchedEffect(albumArtBitmap) {
        if (albumArtBitmap != null) {
            localPalette = com.example.beatpulse.utils.extractPaletteFast(albumArtBitmap)
        } else {
            localPalette = null
        }
    }
    
    val paletteColors = localPalette ?: state.paletteColors
    
    val streamConfigUiVisible by playerViewModel.streamConfigUiVisible.collectAsState()
    val isWifiStreamActive by playerViewModel.isWifiStreamActive.collectAsState()
    val wifiStreamFps by playerViewModel.wifiStreamFps.collectAsState()
    
    // RTSP streaming is handled via Intent to RtspStreamService.
    

    val streamConfigEffectsVisible by playerViewModel.streamConfigEffectsVisible.collectAsState()
    val streamConfigAspectRatio by playerViewModel.streamConfigAspectRatio.collectAsState()
    var showStreamConfigDialog by remember { mutableStateOf(false) }
    
    
    var showMicButton by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(3000); showMicButton = !showMicButton } }

    val bassMult by visualizerManager.bassMultiplier.collectAsState()
    val midMult by visualizerManager.midMultiplier.collectAsState()
    val trebleMult by visualizerManager.trebleMultiplier.collectAsState()
    val visualizerArchetype by visualizerManager.visualizerArchetype.collectAsState()
    val fftMode by visualizerManager.fftMode.collectAsState()
    val combinedAmplitudesState = visualizerManager.combinedAmplitudes.collectAsState()
    var showAdvancedSettings by remember { mutableStateOf(false) }

    val targetColor = if (dynamicColorsPlus && activeDynamicColor != null) activeDynamicColor!! else paletteColors.dominant
    val colorDominant by animateColorAsState(targetColor, animationSpec = androidx.compose.animation.core.tween(3000), label = "color_dom")
    val colorVibrant by animateColorAsState(paletteColors.vibrant, label = "color_vib")
    val colorMuted by animateColorAsState(paletteColors.muted, label = "color_mut")

    val thumbnailShapeIdx by prefs.thumbnailShapeFlow.collectAsState()
    val shape = when (thumbnailShapeIdx) {
        1 -> RoundedCornerShape(0.dp)
        2 -> RoundedCornerShape(16.dp)
        3 -> RoundedCornerShape(32.dp)
        4 -> CathedralShape
        5 -> DiamondShape
        6 -> HexagonShape
        else -> CircleShape
    }

    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    var showQueue by remember { mutableStateOf(false) }
    var currentStyleName by remember { mutableStateOf<String?>(null) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showEqDialog by remember { mutableStateOf(false) }
    var showEffectsDialog by remember { mutableStateOf(false) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var isTrackInfoPopupVisible by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var showSupportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        com.example.beatpulse.core.focus.AppFocusManager.appShortcuts.collect { shortcut ->
            if (prefs.lastMainScreenPage == 2) {
                when (shortcut) {
                    com.example.beatpulse.core.focus.AppShortcut.OPEN_TIMER -> showTimerDialog = true
                    com.example.beatpulse.core.focus.AppShortcut.OPEN_EQUALIZER -> showEqDialog = true
                    com.example.beatpulse.core.focus.AppShortcut.OPEN_AUDIO_EFFECTS -> showEffectsDialog = true
                    com.example.beatpulse.core.focus.AppShortcut.OPEN_PATREON -> showSupportDialog = true
                    else -> {}
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        playerViewModel.settingsMenuRequested.collect {
            showSettingsMenu = true
        }
    }

    LaunchedEffect(Unit) {
        playerViewModel.supportDialogRequested.collect {
            showSupportDialog = true
        }
    }

    LaunchedEffect(Unit) {
        playerViewModel.streamConfigDialogRequested.collect {
            showStreamConfigDialog = true
            playerViewModel.setStreamConfigUiVisible(true)
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }

    var settingsButtonVisible by remember { mutableStateOf(true) }
    var settingsButtonBlink by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            settingsButtonVisible = true
            settingsButtonBlink = false
            // Blink for 3 seconds
            repeat(3) {
                settingsButtonBlink = true
                delay(500)
                settingsButtonBlink = false
                delay(500)
            }
            // Fade out
            settingsButtonVisible = false
        }
    }

    val settingsAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (settingsButtonVisible) { if (settingsButtonBlink) 0.3f else 1f } else 0f,
        animationSpec = tween(durationMillis = if (settingsButtonVisible) 300 else 2000),
        label = "settingsAlpha"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )
    val fastRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation2"
    )

    var lastAbSeekTime by remember { mutableStateOf(0L) }

    // Position polling
    LaunchedEffect(isPlaying) {
        while (true) {
            if (abRepeatModeEnabled) {
                val aPos = (abPointA * duration).toLong()
                val bPos = (abPointB * duration).toLong()
                val now = System.currentTimeMillis()
                if (now - lastAbSeekTime > 1000L) {
                    if (currentPosition >= bPos && bPos > aPos) { 
                        playerViewModel.seekTo(aPos)
                        lastAbSeekTime = now
                    } else if (currentPosition < aPos && bPos > aPos) { 
                        playerViewModel.seekTo(aPos)
                        lastAbSeekTime = now
                    }
                }
            }
            delay(if (isPlaying) 100L else 1000L)
        }
    }

    val isLandscape = false
    val scrollState = rememberScrollState()
    var showMicPermissionDialog by remember { mutableStateOf(false) }
    var requestMicPermission by remember { mutableStateOf(false) }
    
    var showNextPrevTutorial by remember { mutableStateOf(!prefs.hasUsedNextPrevGesture) }
    var showSeek10sTutorial by remember { mutableStateOf(!prefs.hasUsedSeek10sGesture) }
    var showVinylSeekTutorial by remember { mutableStateOf(!prefs.hasUsedVinylSeekGesture) }
    var showPlaylistSwipeTutorial by remember { mutableStateOf(!prefs.hasUsedPlaylistSwipeGesture) }
    var feedbackSeekLeft by remember { mutableStateOf(false) }
    var feedbackSeekRight by remember { mutableStateOf(false) }
    var feedbackPrevTrack by remember { mutableStateOf(false) }
    var feedbackNextTrack by remember { mutableStateOf(false) }

    val dynamicTextColor = if (paletteColors.dominant.luminance() < 0.5f) Color.White else Color.Black

    // Support dialog
    PlayerSupportDialog(
        showSupportDialog = showSupportDialog,
        onDismissRequest = { showSupportDialog = false },
        paletteColors = paletteColors,
        dynamicTextColor = dynamicTextColor,
        prefs = prefs
    )

    val activeAspectRatio = if (isMicModeActive) {
        when (streamConfigAspectRatio) { "16:9" -> 16f / 9f; "4:3" -> 4f / 3f; "1:1" -> 1f; else -> 0f }
    } else 0f
    
    LaunchedEffect(isMicModeActive, streamConfigAspectRatio) {
        // No-op for desktop
    }

    val aspectModifier = if (activeAspectRatio > 0f) {
        if (isLandscape) {
            Modifier.fillMaxHeight().aspectRatio(activeAspectRatio, matchHeightConstraintsFirst = true)
        } else {
            Modifier.fillMaxWidth().aspectRatio(activeAspectRatio, matchHeightConstraintsFirst = false)
        }
    } else Modifier.fillMaxSize()

    val terrainState = androidx.compose.runtime.remember { TerrainState() }
    val oscilloscopeState = androidx.compose.runtime.remember { OscilloscopeState() }

    // --- MAIN LAYOUT ---
    Box(
        modifier = modifier.fillMaxSize().drawBehind {
            if (currentStyle == VisualizerStyle.BANDS) {
                PlayerBandsBackground(this, bassAmplitudesState.value, midAmplitudesState.value, highAmplitudesState.value, combinedAmplitudesState.value, paletteColors, bassMult, midMult, trebleMult, reactivity, visualizerArchetype)
            } else if (currentStyle == VisualizerStyle.TERRAIN) {
                PlayerTerrainBackground(this, bassAmplitudesState.value, midAmplitudesState.value, highAmplitudesState.value, combinedAmplitudesState.value, paletteColors, bassMult, midMult, trebleMult, reactivity, visualizerArchetype, rotationAngle, terrainState)
            } else if (currentStyle == VisualizerStyle.SIDE_PERSPECTIVE_BANDS) {
                PlayerSidePerspectiveBandsBackground(this, bassAmplitudesState.value, midAmplitudesState.value, highAmplitudesState.value, combinedAmplitudesState.value, paletteColors, bassMult, midMult, trebleMult, reactivity, visualizerArchetype)
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = aspectModifier) {
    val cleanUiMode by playerViewModel.cleanUiMode.collectAsState()
    val isMicModeCleanUI = (isMicModeActive && !streamConfigUiVisible) || cleanUiMode
    
    if (cleanUiMode && !showSettingsMenu) {
        SystemBackHandler {
            showSettingsMenu = true
        }
    }
    val columnModifier = if (isMicModeCleanUI) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxSize().padding(bottom = bottomPadding).then(if (isLandscape) Modifier.verticalScroll(scrollState) else Modifier)
    }



    Column(modifier = columnModifier) {
        // Track Info Header
        if (!isMicModeCleanUI) {
            PlayerTrackInfoHeader(
            currentTrack = currentTrack,
            cleanUiMode = cleanUiMode,
            isMicModeActive = isMicModeActive,
            streamConfigUiVisible = streamConfigUiVisible,
            colorVibrant = colorVibrant,
            paletteColors = paletteColors,
            abRepeatModeEnabled = abRepeatModeEnabled,
            abPointA = abPointA,
            abPointB = abPointB,
            duration = duration,
            currentPosition = currentPosition,
            showMicButton = showMicButton,
            lyrics = lyrics,
            showLyrics = showLyrics,
            onToggleLyrics = { showLyrics = !showLyrics },
            onShowSupport = { showSupportDialog = true },
            onAddToPlaylist = { currentTrack?.let { onAddToPlaylist(it) } },
            onToggleMicMode = {
                if (com.example.beatpulse.utils.SystemUtils.isMobilePlatform) {
                    showMicPermissionDialog = true
                } else {
                    playerViewModel.toggleMicMode()
                }
            },
            onShowStreamConfig = { 
                showStreamConfigDialog = true
                playerViewModel.setStreamConfigUiVisible(true)
            },
            prefs = prefs
        )
        }

        // Visualizer area with gestures
        PlayerVisualizerArea(
            areaModifier = if (isMicModeCleanUI) Modifier.fillMaxSize() else if (isLandscape) Modifier.height(350.dp) else Modifier.weight(1f),
            isLandscape = isLandscape,
            isMicModeActive = isMicModeActive,
                        abRepeatModeEnabled = abRepeatModeEnabled,
            showPlaylistSwipeTutorial = showPlaylistSwipeTutorial,
            showVinylSeekTutorial = showVinylSeekTutorial,
            showNextPrevTutorial = showNextPrevTutorial,
            showSeek10sTutorial = showSeek10sTutorial,
            prefs = prefs,
            onShowQueue = { showQueue = true },
            onDismissPlaylistSwipeTutorial = { showPlaylistSwipeTutorial = false; prefs.hasUsedPlaylistSwipeGesture = true },
            onDismissVinylSeekTutorial = { showVinylSeekTutorial = false; prefs.hasUsedVinylSeekGesture = true },
            onDismissNextPrevTutorial = { showNextPrevTutorial = false; prefs.hasUsedNextPrevGesture = true },
            onDismissSeek10sTutorial = { showSeek10sTutorial = false; prefs.hasUsedSeek10sGesture = true },
            feedbackPrevTrack = feedbackPrevTrack,
            feedbackNextTrack = feedbackNextTrack,
            feedbackSeekLeft = feedbackSeekLeft,
            feedbackSeekRight = feedbackSeekRight,
            onFeedbackPrevTrack = { v -> feedbackPrevTrack = v },
            onFeedbackNextTrack = { v -> feedbackNextTrack = v },
            onFeedbackSeekLeft = { v -> feedbackSeekLeft = v },
            onFeedbackSeekRight = { v -> feedbackSeekRight = v },
            paletteColors = paletteColors,
            colorDominant = colorDominant,
            colorVibrant = colorVibrant,
            colorMuted = colorMuted,
            currentStyle = currentStyle,
            thumbnailShapeIdx = thumbnailShapeIdx,
            shape = shape,
            bassAmplitudesState = bassAmplitudesState,
            midAmplitudesState = midAmplitudesState,
            highAmplitudesState = highAmplitudesState,
            combinedAmplitudesState = combinedAmplitudesState,
            visualizerManager = visualizerManager,
            visualizerArchetype = visualizerArchetype,
            rotationAngle = rotationAngle,
            fastRotationAngle = fastRotationAngle,
            currentPosition = currentPosition,
            duration = duration,
            abPointA = abPointA,
            abPointB = abPointB,
            activeDraggingHandle = activeDraggingHandle,
            onActiveDraggingHandleChanged = { activeDraggingHandle = it },
            playerViewModel = playerViewModel,
            oscilloscopeState = oscilloscopeState,
            reactivity = reactivity,
            cleanUiMode = cleanUiMode,
            coverDragEnabled = playerViewModel.coverDragEnabled.collectAsState().value,
            coverVisibilityMode = playerViewModel.coverVisibilityMode.collectAsState().value,
            chromaKeyColor = playerViewModel.chromaKeyColor.collectAsState().value,
            coverScale = playerViewModel.coverScale.collectAsState().value,
            dynamicColorsPlus = dynamicColorsPlus,
            coverOffsetX = playerViewModel.coverOffsetX.collectAsState().value,
            coverOffsetY = playerViewModel.coverOffsetY.collectAsState().value,
            albumArtBitmap = albumArtBitmap,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            isFetchingLyrics = isFetchingLyrics,
            searchFailed = searchFailed,
            availableLyricsResults = availableLyricsResults,
            autoAnalyzeLyrics = autoAnalyzeLyrics,
            showLyricsMatches = showLyricsMatches,
            onShowLyricsMatches = { showLyricsMatches = true }
        )

        // Lyrics Overlay
        AnimatedVisibility(
            visible = showLyrics,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize().padding(top = 100.dp)
        ) {
            PlayerLyricsOverlay(lyrics = lyrics, currentPosition = currentPosition, colorVibrant = colorVibrant, onSeek = {})
        }

        // Mode notification
        AnimatedVisibility(
            visible = currentStyleName != null,
            enter = fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.8f),
            exit = fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 100.dp)
        ) {
            Box(modifier = Modifier.background(colorVibrant.copy(alpha = 0.8f), RoundedCornerShape(20.dp)).padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(text = "Modo: $currentStyleName", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            LaunchedEffect(currentStyleName) { if (currentStyleName != null) { delay(1200); currentStyleName = null } }
        }

        // Control Row
        AnimatedVisibility(visible = !isMicModeCleanUI, enter = fadeIn(), exit = fadeOut()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showTimerDialog = true }) {
                    Icon(Icons.Default.Timer, contentDescription = "Timer", tint = if (sleepTimerSeconds > 0) colorVibrant else Color.Gray, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { showEqDialog = true }) {
                    Icon(Icons.Default.GraphicEq, contentDescription = "Equalizer", tint = if (kotlinx.coroutines.flow.MutableStateFlow(false).collectAsState().value) colorVibrant else Color.Gray, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { showEffectsDialog = true }) {
                    Icon(Icons.Default.Star, contentDescription = "Audio Effects", tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { showEditorDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Track", tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { showSettingsMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
            }
        }
        }
    }

    // --- DIALOGS (each in its own composable = own register scope) ---
    PlayerTimerDialog(showTimerDialog = showTimerDialog, onDismissRequest = { showTimerDialog = false }, colorVibrant = colorVibrant, colorDominant = colorDominant, sleepTimerSeconds = sleepTimerSeconds, onSetSleepTimer = onSetSleepTimer)
    PlayerEqDialog(showEqDialog = showEqDialog, onDismissRequest = { showEqDialog = false }, colorVibrant = colorVibrant, colorDominant = colorDominant, equalizerManager = equalizerManager)
    PlayerEditorDialog(showEditorDialog = showEditorDialog, onDismissRequest = { showEditorDialog = false }, colorVibrant = colorVibrant, colorDominant = colorDominant, currentTrack = currentTrack, onUpdateTrackMetadata = onUpdateTrackMetadata)
    PlayerStreamConfigDialog(
        showStreamConfigDialog = showStreamConfigDialog,
        onDismissRequest = {
            showStreamConfigDialog = false
            playerViewModel.setStreamConfigUiVisible(false)
        },
        colorVibrant = colorVibrant, colorDominant = colorDominant, playerViewModel = playerViewModel
    )

    // Floating Stream Config Button for Clean UI Mode
    AnimatedVisibility(
        visible = isMicModeCleanUI && isMicModeActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 70.dp, end = 24.dp)
    ) {
        IconButton(
            onClick = {
                showStreamConfigDialog = true
                playerViewModel.setStreamConfigUiVisible(true)
            },
            modifier = Modifier.size(36.dp).clip(CircleShape).background(paletteColors.dominant.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Configuración de Stream",
                tint = colorVibrant,
                modifier = Modifier.size(20.dp)
            )
        }
    }


    val styleNames = mapOf(
        VisualizerStyle.WAVE to getLocalizedString("waves"),
        VisualizerStyle.SLIME to getLocalizedString("slime"),
        VisualizerStyle.BARS to getLocalizedString("bars"),
        VisualizerStyle.DOTS to getLocalizedString("dots"),
        VisualizerStyle.PARTICLES to getLocalizedString("particles"),
        VisualizerStyle.RINGS to getLocalizedString("rings"),
        VisualizerStyle.AURA to getLocalizedString("aura"),
        VisualizerStyle.BANDS to getLocalizedString("bands"),
        VisualizerStyle.TERRAIN to getLocalizedString("terrain_3d"),
        VisualizerStyle.STAR to getLocalizedString("star"),
        VisualizerStyle.OSCILLOSCOPE to getLocalizedString("oscilloscope"),
        VisualizerStyle.TRAP_NATION to getLocalizedString("trap_nation"),
        VisualizerStyle.SIDE_PERSPECTIVE_BANDS to getLocalizedString("perspective_bands")
    )

    PlayerSettingsSheet(
        showSettingsMenu = showSettingsMenu,
        onDismissRequest = { showSettingsMenu = false },
        colorDominant = colorDominant, colorVibrant = colorVibrant, colorMuted = colorMuted,
         playerViewModel = playerViewModel, visualizerManager = visualizerManager,
        prefs = prefs, currentStyle = currentStyle,
        onStyleChange = { style, name -> currentStyle = style; currentStyleName = name; prefs.visualizerStyle = style.name.lowercase() },
        styleNames = styleNames
    )

    com.example.beatpulse.utils.SystemMicPermissionHandler(
        requestTrigger = requestMicPermission,
        onResult = { granted ->
            requestMicPermission = false
            if (granted) {
                playerViewModel.toggleMicMode()
            }
        }
    )

    if (showMicPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showMicPermissionDialog = false },
            title = { Text(getLocalizedString("mic_test_title"), color = colorVibrant) },
            text = { Text(getLocalizedString("mic_test_desc"), color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    showMicPermissionDialog = false
                    requestMicPermission = true
                }) { Text("Confirmar", color = colorVibrant) }
            },
            dismissButton = {
                TextButton(onClick = { showMicPermissionDialog = false }) { Text(getLocalizedString("cancel"), color = Color.Gray) }
            },
            containerColor = colorDominant.copy(alpha = 0.95f)
        )
    }

    GestureFeedbackOverlay(show = feedbackPrevTrack, text = "⏮", alignLeft = true)
    GestureFeedbackOverlay(show = feedbackNextTrack, text = "⏭", alignLeft = false)
    GestureFeedbackOverlay(show = feedbackSeekLeft, text = "-10s", alignLeft = true)
    GestureFeedbackOverlay(show = feedbackSeekRight, text = "+10s", alignLeft = false)

    GestureTutorialOverlay(
        showNextPrev = showNextPrevTutorial,
        showSeek10s = showSeek10sTutorial,
        showVinylSeek = showVinylSeekTutorial,
        showPlaylistSwipe = showPlaylistSwipeTutorial
    )

    PlayerQueueSheet(
        showQueue = showQueue, onDismissRequest = { showQueue = false },
        colorDominant = colorDominant, colorVibrant = colorVibrant,
        currentQueue = currentQueue, currentTrack = currentTrack, onPlayTrack = onPlayTrack
    )
    PlayerEffectsDialog(
        showEffectsDialog = showEffectsDialog, onDismissRequest = { showEffectsDialog = false },
        colorVibrant = colorVibrant, colorDominant = colorDominant,
        reverbEnabled = state.reverbEnabled, onSetReverb = onSetReverb,
        playbackSpeed = state.playbackSpeed, onSetSpeed = onSetSpeed,
        playbackPitch = state.playbackPitch, onSetPitch = onSetPitch,
        effectsPreset = state.effectsPreset, onApplyPreset = onApplyPreset
    )
    

    
    } // End aspect Box
    } // End outer Box
}
