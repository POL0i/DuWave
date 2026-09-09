package com.example.beatpulse.ui.components.player

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

class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var alpha: Float, val color: Color)

enum class VisualizerStyle {
    WAVE, SLIME, BARS, DOTS, PARTICLES, RINGS, AURA, BANDS, TERRAIN, STAR, OSCILLOSCOPE, TRAP_NATION, SIDE_PERSPECTIVE_BANDS
}

enum class DragAction { NONE, DJ_SEEK, OPEN_QUEUE, DRAG_A, DRAG_B }

class OscilloscopeState(
    var accumulatedTime: Float = 0f,
    var dynamicPhase: Float = 0f
)


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

data class PlayerScreenState(
    val currentTrack: com.example.beatpulse.data.TrackEntity? = null,
    val currentQueue: List<com.example.beatpulse.data.TrackEntity>,
    val paletteColors: com.example.beatpulse.theme.PaletteColors,
    val bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    val prefs: IPreferencesManager,
    val repeatModeState: Int = 0,
    val shuffleModeState: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val reverbEnabled: Boolean = false,
    val effectsPreset: String = "NORMAL",
    val sleepTimerSeconds: Int = 0
)

data class PlayerScreenCallbacks(
    val onPlayTrack: (com.example.beatpulse.data.TrackEntity, List<com.example.beatpulse.data.TrackEntity>) -> Unit,
    val onSetSpeed: (Float) -> Unit = {},
    val onSetPitch: (Float) -> Unit = {},
    val onSetReverb: (Boolean) -> Unit = {},
    val onApplyPreset: (String) -> Unit = {},
    val onSetSleepTimer: (Int) -> Unit = {},
    val onUpdateTrackMetadata: (Long, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    val onAddToPlaylist: (com.example.beatpulse.data.TrackEntity) -> Unit = {}
)

val CathedralShape = GenericShape { size, _ ->
    moveTo(0f, size.height)
    lineTo(0f, size.height * 0.4f)
    quadraticTo(0f, 0f, size.width / 2f, 0f)
    quadraticTo(size.width, 0f, size.width, size.height * 0.4f)
    lineTo(size.width, size.height)
    close()
}

val DiamondShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width / 2f, size.height)
    lineTo(0f, size.height / 2f)
    close()
}

val HexagonShape = GenericShape { size, _ ->
    moveTo(size.width * 0.5f, 0f)
    lineTo(size.width, size.height * 0.25f)
    lineTo(size.width, size.height * 0.75f)
    lineTo(size.width * 0.5f, size.height)
    lineTo(0f, size.height * 0.75f)
    lineTo(0f, size.height * 0.25f)
    close()
}

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

    // Position polling
    LaunchedEffect(isPlaying) {
        while (true) {
            // isPlaying = isPlaying
            // duration = duration
            // currentPosition = currentPosition
            if (abRepeatModeEnabled) {
                val aPos = (abPointA * duration).toLong()
                val bPos = (abPointB * duration).toLong()
                if (currentPosition >= bPos && bPos > aPos) { playerViewModel.seekTo(aPos) }
                else if (currentPosition < aPos && bPos > aPos) { playerViewModel.seekTo(aPos) }
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

    // Floating Settings Button
    if (!cleanUiMode) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            IconButton(
                onClick = { showSettingsMenu = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ajustes de reproducción",
                    tint = colorVibrant.copy(alpha = settingsAlpha),
                    modifier = Modifier.size(28.dp)
                )
            }
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

// --- SUB-COMPOSABLES ---

/** Draws BANDS visualizer in the background (drawBehind scope) */
fun PlayerBandsBackground(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    bassAmps: FloatArray, midAmps: FloatArray, highAmps: FloatArray, combinedAmps: FloatArray,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    bassMult: Float, midMult: Float, trebleMult: Float, reactivity: Float, visualizerArchetype: Int
) {
    with(scope) {
        val w = size.width; val h = size.height
        val bassOpacity = (0.4f + bassMult * 0.4f + reactivity * 0.2f).coerceIn(0f, 1f)
        val midOpacity = (0.5f + midMult * 0.3f + reactivity * 0.2f).coerceIn(0f, 1f)
        val highOpacity = (0.6f + trebleMult * 0.2f + reactivity * 0.2f).coerceIn(0f, 1f)

        fun drawBandsEdge(amps: FloatArray, color: Color, widthMult: Float, isLeft: Boolean) {
            val count = amps.size; if (count == 0) return
            val stepY = h / count.coerceAtLeast(1).toFloat()
            for (i in 0 until count) {
                val amp = amps[i]
                val bandWidth = amp * w * 0.45f * widthMult
                if (bandWidth <= 1f) continue
                val startX = if (isLeft) 0f else w - bandWidth
                drawRect(color = color, topLeft = Offset(startX, i * stepY), size = Size(bandWidth, stepY), style = Stroke(width = 4f))
            }
        }

        val drawBandLayer = { amps: FloatArray, color: Color, widthMult: Float ->
            drawBandsEdge(amps, color, widthMult, true)
            drawBandsEdge(amps, color, widthMult, false)
        }

        if (visualizerArchetype == 1) {
            drawBandLayer(combinedAmps, paletteColors.vibrant.copy(alpha = maxOf(bassOpacity, midOpacity, highOpacity)), 1.5f)
        } else {
            drawBandLayer(bassAmps, paletteColors.dominant.copy(alpha = bassOpacity * 0.7f), 0.9f)
            drawBandLayer(midAmps, paletteColors.vibrant.copy(alpha = midOpacity), 1.3f)
            drawBandLayer(highAmps, paletteColors.muted.copy(alpha = highOpacity), 0.7f)
        }
    }
}

class TerrainState(
    var history: FloatArray = FloatArray(24 * 40),
    var lastZOffsetInt: Int = 0,
    var accumulatedAngle: Float = 0f,
    var previousAngle: Float = -1f
)

/** Draws TERRAIN (Synthwave 3D Grid) in the background */
fun PlayerTerrainBackground(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    bassAmps: FloatArray, midAmps: FloatArray, highAmps: FloatArray, combinedAmps: FloatArray,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    bassMult: Float, midMult: Float, trebleMult: Float, reactivity: Float, visualizerArchetype: Int,
    rotationAngle: Float,
    terrainState: TerrainState
) {
    with(scope) {
        val w = size.width; val h = size.height
        val bassOpacity = (0.3f + bassMult * 0.4f + reactivity * 0.2f).coerceIn(0f, 1f)
        val midOpacity = (0.4f + midMult * 0.3f + reactivity * 0.2f).coerceIn(0f, 1f)
        val highOpacity = (0.5f + trebleMult * 0.2f + reactivity * 0.2f).coerceIn(0f, 1f)
        
        val isMobile = com.example.beatpulse.utils.SystemUtils.isMobilePlatform
        // EXTREME OPTIMIZATION: Grid restored to 14x20 to keep peaks visible, but we remove the fill entirely on mobile for max FPS
        val numZ = if (isMobile) 14 else 24
        val numX = if (isMobile) 20 else 40
        
        if (terrainState.history.size != numZ * numX) {
            terrainState.history = FloatArray(numZ * numX)
        }
        
        val speed = 20f
        
        if (terrainState.previousAngle == -1f) terrainState.previousAngle = rotationAngle
        var delta = rotationAngle - terrainState.previousAngle
        if (delta < -180f) delta += 360f
        if (delta > 180f) delta -= 360f
        terrainState.accumulatedAngle += delta
        terrainState.previousAngle = rotationAngle
        
        val zOffset = (terrainState.accumulatedAngle / 360f) * speed
        val currentZInt = zOffset.toInt()
        val scrollZ = zOffset % 1f
        
        // Helper for smoothed sampling
        fun sampleSmoothed(array: FloatArray, index: Int): Float {
            val dataCount = array.size
            if (dataCount == 0) return 0f
            var sum = 0f
            var weightSum = 0f
            for (j in -2..2) {
                val idx = (index + j).coerceIn(0, dataCount - 1)
                val weight = 1f / (1f + kotlin.math.abs(j))
                sum += array[idx] * weight
                weightSum += weight
            }
            return sum / weightSum
        }
        
        // Update history
        if (currentZInt != terrainState.lastZOffsetInt) {
            val diff = currentZInt - terrainState.lastZOffsetInt
            if (diff > 0 && diff < numZ) {
                // Shift array
                val shift = diff * numX
                terrainState.history.copyInto(terrainState.history, shift, 0, terrainState.history.size - shift)
            } else if (diff >= numZ) {
                terrainState.history.fill(0f)
            }
            terrainState.lastZOffsetInt = currentZInt
            
            // Insert new data at row 0 (which will be drawn at Z = far)
            if (combinedAmps.isNotEmpty()) {
                val dataCount = combinedAmps.size
                for (xi in 0 until numX) {
                    val xNormalized = xi.toFloat() / (numX - 1) // 0 to 1
                    val distanceFromCenter = kotlin.math.abs(xNormalized - 0.5f) * 2f // 0 at center, 1 at edges
                    
                    var elevation = 0f
                    if (distanceFromCenter > 0.15f) { // Leave a flat road in the middle
                        val mountainPos = ((distanceFromCenter - 0.15f) / 0.85f).coerceIn(0f, 1f)
                        val ampIndex = (mountainPos * (dataCount - 1)).toInt().coerceIn(0, dataCount - 1)
                        
                        val blended = if (visualizerArchetype == 1) {
                            sampleSmoothed(combinedAmps, ampIndex)
                        } else {
                            // Three wave mode: Bass -> Outer, Mid -> Middle, High -> Inner
                            val highWeight = (1f - mountainPos * 2f).coerceIn(0f, 1f) // 1 at inner, 0 at mid
                            val midWeight = (1f - kotlin.math.abs(mountainPos - 0.5f) * 2f).coerceIn(0f, 1f) // peak at 0.5
                            val bassWeight = ((mountainPos - 0.5f) * 2f).coerceIn(0f, 1f) // 0 at mid, 1 at outer
                            
                            val highVal = sampleSmoothed(highAmps, ampIndex)
                            val midVal = sampleSmoothed(midAmps, ampIndex)
                            val bassVal = sampleSmoothed(bassAmps, ampIndex)
                            
                            highVal * highWeight + midVal * midWeight + bassVal * bassWeight
                        }
                        
                        elevation = blended * (distanceFromCenter * distanceFromCenter)
                        if (elevation < 0.05f) elevation = 0f // Threshold noise
                    }
                    terrainState.history[xi] = elevation
                }
            } else {
                for (xi in 0 until numX) terrainState.history[xi] = 0f
            }
        }

           fun drawTerrainLayer(color: androidx.compose.ui.graphics.Color, isTop: Boolean, opacityMult: Float, pulseMult: Float) {
            val ampMult = if (isTop) 2.5f else 5.0f

            fun project(x: Float, y: Float, z: Float): androidx.compose.ui.geometry.Offset {
                val scale = h * 1.4f / z // Perspectiva incrementada (antes 0.9f)
                val px = w / 2f + x * scale
                val horizonOffset = h * 0.12f // Separación del horizonte
                val cameraY = 1.0f // Cámara un poco más baja para acentuar profundidad
                
                // Pulse the Y height dynamically with the music! 
                // Increased global bounce multiplier (1.5x) for more reaction
                val pulsedY = y * pulseMult * 1.5f
                
                // Floor (+Y goes down on screen). Ceiling (-Y goes up).
                val screenY = if (isTop) {
                    val baseScreenY = h / 2f - horizonOffset
                    baseScreenY - cameraY * scale + pulsedY * ampMult * scale
                } else {
                    val baseScreenY = h / 2f + horizonOffset
                    baseScreenY + cameraY * scale - pulsedY * ampMult * scale
                }
                return androidx.compose.ui.geometry.Offset(px, screenY)
            }

            val strokeColor = color.copy(alpha = opacityMult)
            
            // Helper for 3-wave gradient coloring
            fun getNeonColor(xi: Int): androidx.compose.ui.graphics.Color {
                if (visualizerArchetype == 1) return color
                
                val xNormalized = xi.toFloat() / (numX - 1)
                val distCenter = kotlin.math.abs(xNormalized - 0.5f) * 2f
                val mountainPos = ((distCenter - 0.15f) / 0.85f).coerceIn(0f, 1f)
                
                val highW = (1f - mountainPos * 2f).coerceIn(0f, 1f)
                val midW = (1f - kotlin.math.abs(mountainPos - 0.5f) * 2f).coerceIn(0f, 1f)
                val bassW = ((mountainPos - 0.5f) * 2f).coerceIn(0f, 1f)
                
                val total = (highW + midW + bassW).coerceAtLeast(0.01f)
                val hW = highW / total
                val mW = midW / total
                val bW = bassW / total
                
                // Use distinct colors from the cover art palette directly!
                val bassColor = paletteColors.vibrant
                val midColor = color
                val highColor = paletteColors.lightVibrant
                
                val r = highColor.red * hW + midColor.red * mW + bassColor.red * bW
                val g = highColor.green * hW + midColor.green * mW + bassColor.green * bW
                val b = highColor.blue * hW + midColor.blue * mW + bassColor.blue * bW
                return androidx.compose.ui.graphics.Color(r, g, b, 1f)
            }
            
            // ULTRA OPTIMIZATION: Combine entire wireframe into a single Path and draw ONCE with a solid color.
            // Eliminates 14+ JNI drawPath calls per frame and removes heavy Skia gradient shaders entirely.
            terrainSharedLinePath.reset()

            // Draw back-to-front for proper painter's algorithm occlusion.
            for (zi in 0 until numZ - 1) {
                val zBack = (numZ - zi).toFloat() - scrollZ
                val zFront = (numZ - zi - 1).toFloat() - scrollZ
                
                // Fade out near the camera and fade IN at the horizon (fog effect)
                val fadeBack = (zBack / 2f).coerceIn(0f, 1f) * (1f - (zBack / numZ)).coerceIn(0f, 1f)
                val fadeFront = (zFront / 2f).coerceIn(0f, 1f) * (1f - (zFront / numZ)).coerceIn(0f, 1f)
                
                // Grow from 0 elevation to full elevation over the first 4 chunks
                val growBack = ((numZ - zBack) / 4f).coerceIn(0f, 1f)
                val growFront = ((numZ - zFront) / 4f).coerceIn(0f, 1f)
                
                if (fadeBack <= 0.01f && fadeFront <= 0.01f) continue

                // 1. Draw filled geometry
                if (isMobile) {
                    // ULTRA OPTIMIZATION FOR MOBILE: 
                    // To guarantee 60 FPS on weak hardware, we skip drawing the filled quad entirely
                    // and just draw the wireframe neon lines (holographic grid effect).
                } else {
                    for (xi in 0 until numX - 1) {
                        val elBL = terrainState.history[zi * numX + xi] * growBack
                        val elBR = terrainState.history[zi * numX + xi + 1] * growBack
                        val elFL = terrainState.history[(zi + 1) * numX + xi] * growFront
                        val elFR = terrainState.history[(zi + 1) * numX + xi + 1] * growFront
                        
                        val xL = (xi.toFloat() / (numX - 1)) * 4f - 2f
                        val xR = ((xi + 1).toFloat() / (numX - 1)) * 4f - 2f

                        val ptBL = project(xL, elBL, zBack)
                        val ptBR = project(xR, elBR, zBack)
                        val ptFL = project(xL, elFL, zFront)
                        val ptFR = project(xR, elFR, zFront)

                        terrainSharedQuadPath.reset()
                        terrainSharedQuadPath.moveTo(ptBL.x, ptBL.y)
                        terrainSharedQuadPath.lineTo(ptBR.x, ptBR.y)
                        terrainSharedQuadPath.lineTo(ptFR.x, ptFR.y)
                        terrainSharedQuadPath.lineTo(ptFL.x, ptFL.y)
                        terrainSharedQuadPath.close()

                        // Vary color based on elevation for 3D depth effect
                        val avgY = (elBL + elBR + elFL + elFR) / 4f
                        val intensity = (avgY / 2.5f).coerceIn(0f, 1f)
                        
                        val avgNeon = getNeonColor(xi)
                        
                        // Blend quad color with neon based on elevation
                        val baseFill = androidx.compose.ui.graphics.lerp(
                            paletteColors.darkMuted,
                            avgNeon,
                            intensity * 0.4f
                        )
                        // Blend quad color with background based on fadeBack (creates horizon fog effect)
                        val quadFill = androidx.compose.ui.graphics.lerp(
                            paletteColors.dominant,
                            baseFill,
                            fadeBack
                        ).copy(alpha = 1.0f)

                        drawPath(terrainSharedQuadPath, color = quadFill, style = androidx.compose.ui.graphics.drawscope.Fill)
                    }
                }

                // 2. Add to master wireframe path
                // Draw all horizontal lines first
                var first = true
                for (xi in 0 until numX) {
                    // Multiplicamos la elevación x1.5 para picos más pronunciados y visibles
                    val elB = terrainState.history[zi * numX + xi] * growBack * 1.5f
                    // Comprimimos el ancho total de 6 a 4 unidades (-2 a 2)
                    val x = (xi.toFloat() / (numX - 1)) * 4f - 2f
                    val ptB = project(x, elB, zBack)
                    if (first) {
                        terrainSharedLinePath.moveTo(ptB.x, ptB.y)
                        first = false
                    } else {
                        terrainSharedLinePath.lineTo(ptB.x, ptB.y)
                    }
                }

                // Draw all vertical lines
                for (xi in 0 until numX) {
                    val elB = terrainState.history[zi * numX + xi] * growBack * 1.5f
                    val elF = terrainState.history[(zi + 1) * numX + xi] * growFront * 1.5f
                    val x = (xi.toFloat() / (numX - 1)) * 4f - 2f
                    val ptB = project(x, elB, zBack)
                    val ptF = project(x, elF, zFront)
                    terrainSharedLinePath.moveTo(ptB.x, ptB.y)
                    terrainSharedLinePath.lineTo(ptF.x, ptF.y)
                }
            }
            
            // Draw the very front horizontal line
            val lastZ = numZ - 1
            val zFront = lastZ.toFloat() - scrollZ
            var firstFront = true
            for (xi in 0 until numX) {
                val elF = terrainState.history[lastZ * numX + xi] * 1.5f // Fully grown, taller
                val x = (xi.toFloat() / (numX - 1)) * 4f - 2f
                val ptF = project(x, elF, zFront)
                if (firstFront) {
                    terrainSharedLinePath.moveTo(ptF.x, ptF.y)
                    firstFront = false
                } else {
                    terrainSharedLinePath.lineTo(ptF.x, ptF.y)
                }
            }
            
            // Draw the master wireframe with a single blazing-fast native call.
            // Used a thicker stroke (4f on mobile) and solid bright color to make the lines pop as requested.
            drawPath(
                terrainSharedLinePath,
                color = color,
                alpha = (opacityMult * 1.5f).coerceIn(0.5f, 1f), // Siempre visible, nunca muy oscuro
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = if (isMobile) 4f else 2f, 
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }

        if (visualizerArchetype == 1) {
            val neonColor = paletteColors.vibrant
            val bassPulse = 1f + (bassMult * 1.5f)
            val midPulse = 1f + (midMult * 1.5f)
            
            drawTerrainLayer(neonColor, isTop = true, opacityMult = midOpacity, pulseMult = midPulse)
            drawTerrainLayer(neonColor, isTop = false, opacityMult = bassOpacity, pulseMult = bassPulse)
        } else {
            // Siempre forzar colores vibrantes para la malla de modo ahorro de recursos
            drawTerrainLayer(paletteColors.vibrant, false, bassOpacity, 1f)
            drawTerrainLayer(paletteColors.vibrant, true, highOpacity, 1f)
        }
    }
}

/** Track info header (title, artist, time, buttons) */
@Composable
private fun PlayerTrackInfoHeader(
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
                    Crossfade(
                        targetState = if (isMicModeActive) 2 else if (showMicButton) 1 else 0,
                        animationSpec = tween(500)
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

/** Central visualizer area with gesture handling, canvas, album art overlay */
@Composable
private fun ColumnScope.PlayerVisualizerArea(
    areaModifier: Modifier = Modifier,
    isLandscape: Boolean,
    isMicModeActive: Boolean,
        abRepeatModeEnabled: Boolean,
    showPlaylistSwipeTutorial: Boolean,
    showVinylSeekTutorial: Boolean,
    showNextPrevTutorial: Boolean,
    showSeek10sTutorial: Boolean,
    prefs: IPreferencesManager,
    onShowQueue: () -> Unit,
    onDismissPlaylistSwipeTutorial: () -> Unit,
    onDismissVinylSeekTutorial: () -> Unit,
    onDismissNextPrevTutorial: () -> Unit,
    onDismissSeek10sTutorial: () -> Unit,
    feedbackPrevTrack: Boolean, feedbackNextTrack: Boolean,
    feedbackSeekLeft: Boolean, feedbackSeekRight: Boolean,
    onFeedbackPrevTrack: (Boolean) -> Unit, onFeedbackNextTrack: (Boolean) -> Unit,
    onFeedbackSeekLeft: (Boolean) -> Unit, onFeedbackSeekRight: (Boolean) -> Unit,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    colorDominant: Color, colorVibrant: Color, colorMuted: Color,
    currentStyle: VisualizerStyle, thumbnailShapeIdx: Int,
    shape: androidx.compose.ui.graphics.Shape,
    bassAmplitudesState: State<FloatArray>, midAmplitudesState: State<FloatArray>,
    highAmplitudesState: State<FloatArray>, combinedAmplitudesState: State<FloatArray>,
    visualizerManager: IAudioVisualizerManager, visualizerArchetype: Int,
    rotationAngle: Float, fastRotationAngle: Float,
    currentPosition: Long, duration: Long,
    abPointA: Float, abPointB: Float, activeDraggingHandle: String?,
    playerViewModel: IPlayerViewModel,
    oscilloscopeState: OscilloscopeState,
    reactivity: Float,
    cleanUiMode: Boolean,
    coverDragEnabled: Boolean,
    coverVisibilityMode: String,
    chromaKeyColor: String,
    coverScale: Float,
    dynamicColorsPlus: Boolean,
    coverOffsetX: Float,
    coverOffsetY: Float,
    albumArtBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    isPlaying: Boolean, isBuffering: Boolean,
    isFetchingLyrics: Boolean, searchFailed: Boolean,
    availableLyricsResults: List<Any>, autoAnalyzeLyrics: Boolean,
    showLyricsMatches: Boolean, onShowLyricsMatches: () -> Unit
) {
    var currentDragAction by remember { mutableStateOf(com.example.beatpulse.ui.components.player.DragAction.NONE) }
    var lastAngle by remember { mutableStateOf<Float?>(null) }
    var dragSeekTimeMs by remember { mutableStateOf<Long?>(null) }

    val coverRotationAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val sparks = remember { mutableListOf<Spark>() }
    var playheadPos by remember { mutableStateOf(Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()
    var accumulatedAngle by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    
    val bassAvgAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val midAvgAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val trebleAvgAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val animatedScaleAnim = remember { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(visualizerManager.bassAmplitudes, visualizerManager.midAmplitudes, visualizerManager.highAmplitudes) {
        launch { visualizerManager.bassAmplitudes.collect { amps -> if (amps.isNotEmpty()) { val r = amps.average().toFloat().let { if (it.isNaN()) 0f else it }; launch { bassAvgAnim.animateTo(r, tween(150, easing = FastOutSlowInEasing)) }; launch { animatedScaleAnim.animateTo(1f + (r.coerceIn(0f, 1f) * 0.45f), spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)) } } } }
        launch { visualizerManager.midAmplitudes.collect { amps -> if (amps.isNotEmpty()) { val r = amps.average().toFloat().let { if (it.isNaN()) 0f else it }; launch { midAvgAnim.animateTo(r, tween(150, easing = FastOutSlowInEasing)) } } } }
        launch { visualizerManager.highAmplitudes.collect { amps -> if (amps.isNotEmpty()) { val r = amps.average().toFloat().let { if (it.isNaN()) 0f else it }; launch { trebleAvgAnim.animateTo(r, tween(150, easing = FastOutSlowInEasing)) } } } }
    }

    val bassAvg = bassAvgAnim.value
    val midAvg = midAvgAnim.value
    val trebleAvg = trebleAvgAnim.value
    val maxAnim = maxOf(bassAvg, midAvg, trebleAvg, 0.001f)
    val bassOpacity = (bassAvg / maxAnim).coerceIn(0.2f, 1.0f)
    val midOpacity = (midAvg / maxAnim).coerceIn(0.2f, 1.0f)
    val highOpacity = (trebleAvg / maxAnim).coerceIn(0.2f, 1.0f)

    Box(
        modifier = Modifier
            .then(areaModifier)
            .fillMaxWidth()
            .then(
                if (coverDragEnabled) Modifier
                else Modifier.pointerInput(abRepeatModeEnabled) {
                    detectDragGestures(
                        onDragStart = { offset -> 
                            var action = DragAction.NONE
                            if (abRepeatModeEnabled && duration > 0) {
                                val center = Offset(size.width.toFloat() / 2f, size.height.toFloat() / 2f)
                                val radius = minOf(size.width, size.height).toFloat() * 0.4f
                                val aAngle = (abPointA / duration.toFloat()) * 2f * kotlin.math.PI.toFloat() - kotlin.math.PI.toFloat() / 2f
                                val aX = center.x + radius * kotlin.math.cos(aAngle)
                                val aY = center.y + radius * kotlin.math.sin(aAngle)
                                val distA = kotlin.math.hypot(offset.x - aX, offset.y - aY)

                                val bAngle = (abPointB / duration.toFloat()) * 2f * kotlin.math.PI.toFloat() - kotlin.math.PI.toFloat() / 2f
                                val bX = center.x + radius * kotlin.math.cos(bAngle)
                                val bY = center.y + radius * kotlin.math.sin(bAngle)
                                val distB = kotlin.math.hypot(offset.x - bX, offset.y - bY)

                                if (distA < 80f) {
                                    action = DragAction.DRAG_A
                                } else if (distB < 80f) {
                                    action = DragAction.DRAG_B
                                }
                            }
                            currentDragAction = action
                            lastAngle = null; accumulatedAngle = 0f; dragSeekTimeMs = currentPosition
                        },
                        onDragEnd = {
                            if (currentDragAction == DragAction.DJ_SEEK) { dragSeekTimeMs?.let { playerViewModel.seekTo(it) } }
                            coroutineScope.launch { coverRotationAnim.animateTo(0f, spring(stiffness = Spring.StiffnessLow)) }
                            currentDragAction = DragAction.NONE; lastAngle = null; dragSeekTimeMs = null
                        },
                        onDragCancel = { currentDragAction = DragAction.NONE; coroutineScope.launch { coverRotationAnim.animateTo(0f, spring(stiffness = Spring.StiffnessLow)) }; lastAngle = null; dragSeekTimeMs = null }
                    ) { change, dragAmount ->
                        change.consume()
                        val center = Offset(size.width.toFloat() / 2f, size.height.toFloat() / 2f)
                        val touchPos = change.position
                        if (currentDragAction == DragAction.NONE) {
                            if (dragAmount.y < -15f && abs(dragAmount.x) < 20f && touchPos.y > center.y) { currentDragAction = DragAction.OPEN_QUEUE; onShowQueue(); if (showPlaylistSwipeTutorial) onDismissPlaylistSwipeTutorial() }
                            else if (abs(dragAmount.x) > 5f || abs(dragAmount.y) > 5f) { currentDragAction = DragAction.DJ_SEEK; val dx = touchPos.x - center.x; val dy = touchPos.y - center.y; lastAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f }
                        }
                        if (currentDragAction == DragAction.DRAG_A || currentDragAction == DragAction.DRAG_B) {
                            val dx = touchPos.x - center.x; val dy = touchPos.y - center.y
                            val currentAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                            val rawTime = ((currentAngle + 90f) % 360f / 360f) * duration
                            if (currentDragAction == DragAction.DRAG_A) {
                                (playerViewModel.abPointA as? kotlinx.coroutines.flow.MutableStateFlow)?.value = rawTime.toFloat()
                            } else {
                                (playerViewModel.abPointB as? kotlinx.coroutines.flow.MutableStateFlow)?.value = rawTime.toFloat()
                            }
                        } else if (currentDragAction == DragAction.DJ_SEEK) {
                            val dx = touchPos.x - center.x; val dy = touchPos.y - center.y
                            val currentAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                            val prevAngle = lastAngle
                            if (prevAngle != null) {
                                var deltaAngle = currentAngle - prevAngle; if (deltaAngle > 180f) deltaAngle -= 360f; if (deltaAngle < -180f) deltaAngle += 360f
                                accumulatedAngle += deltaAngle
                                if (abs(accumulatedAngle) >= 10f) { /* audioManager click */ accumulatedAngle = 0f }
                                val seekMs = (deltaAngle / 360f) * 120000f
                                val current = dragSeekTimeMs ?: currentPosition; val maxDuration = if (duration > 0) duration else Long.MAX_VALUE; dragSeekTimeMs = (current + seekMs.toLong()).coerceIn(0L, maxDuration); if (showVinylSeekTutorial) onDismissVinylSeekTutorial() 
                                coroutineScope.launch { coverRotationAnim.snapTo(coverRotationAnim.value + deltaAngle) }
                                if (Math.random() < 0.5) { val vx = (Math.random().toFloat() - 0.5f) * 15f; val vy = (Math.random().toFloat() - 0.5f) * 15f; sparks.add(Spark(playheadPos.x, playheadPos.y, vx, vy, 1f, if (Math.random() < 0.5) paletteColors.vibrant else paletteColors.dominant)) }
                            }
                            lastAngle = currentAngle
                        }
                    }
                }
            )
            .then(
                if (coverDragEnabled) Modifier
                else Modifier.pointerInput(abRepeatModeEnabled) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (showNextPrevTutorial) onDismissNextPrevTutorial()
                            if (offset.x < size.width / 2) { playerViewModel.seekToPrevious(); coroutineScope.launch { onFeedbackPrevTrack(true); delay(400); onFeedbackPrevTrack(false) } }
                            else { playerViewModel.seekToNext(); coroutineScope.launch { onFeedbackNextTrack(true); delay(400); onFeedbackNextTrack(false) } }
                        },
                        onPress = {
                            val job = coroutineScope.launch { delay(300); playerViewModel.setSpeed(2f) }
                            tryAwaitRelease(); job.cancel()
                            playerViewModel.setSpeed(1f)
                        }
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // OSCILLOSCOPE BACKGROUND (Behind cover)
        if (currentStyle == VisualizerStyle.OSCILLOSCOPE) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
                    .offset { androidx.compose.ui.unit.IntOffset(coverOffsetX.toInt(), coverOffsetY.toInt()) }
                    .graphicsLayer {
                        scaleX = animatedScaleAnim.value
                        scaleY = animatedScaleAnim.value
                    }
            ) {
                PlayerOscilloscope(this, bassAmplitudesState.value, midAmplitudesState.value, highAmplitudesState.value, combinedAmplitudesState.value, paletteColors, visualizerArchetype, rotationAngle, reactivity, oscilloscopeState, isForeground = false)
            }
        }

        // Visualizer Canvas (extracted to its own composable)
        PlayerVisualizerCanvas(
            coverOffsetX = coverOffsetX,
            coverOffsetY = coverOffsetY,
            currentStyle = currentStyle,
            thumbnailShapeIdx = thumbnailShapeIdx,
            bassAmplitudes = bassAmplitudesState.value,
            midAmplitudes = midAmplitudesState.value,
            highAmplitudes = highAmplitudesState.value,
            combinedAmplitudes = combinedAmplitudesState.value,
            bassAvg = bassAvg, midAvg = midAvg, trebleAvg = trebleAvg,
            bassOpacity = bassOpacity, midOpacity = midOpacity, highOpacity = highOpacity,
            visualizerArchetype = visualizerArchetype,
                        colorDominant = colorDominant, colorVibrant = colorVibrant, colorMuted = colorMuted,
            paletteColors = paletteColors,
            rotationAngle = rotationAngle, fastRotationAngle = fastRotationAngle,
            currentPosition = currentPosition, duration = duration,
            dragSeekTimeMs = dragSeekTimeMs,
            abRepeatModeEnabled = abRepeatModeEnabled,
            abPointA = abPointA, abPointB = abPointB,
            activeDraggingHandle = activeDraggingHandle,
            animatedScale = animatedScaleAnim.value,
            coverScale = coverScale,
            cleanUiMode = cleanUiMode,
            onPlayheadPosChanged = { playheadPos = it }
        )
          // Central Album Art
        if (coverVisibilityMode != "HIDDEN") {
            val chromaColor = when (chromaKeyColor) {
                "MAGENTA" -> Color(0xFFFF00FF)
                "BLUE" -> Color(0xFF0000FF)
                else -> Color(0xFF00FF00)
            }
            
            Box(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(coverOffsetX.toInt(), coverOffsetY.toInt()) }
                    .size(160.dp)
                    .graphicsLayer {
                        val baseScale = animatedScaleAnim.value * coverScale
                        if (dynamicColorsPlus) {
                            val midPeak = if (midAvg > 0.50f) (midAvg - 0.50f) * 3.0f else 0f
                            val treblePeak = if (trebleAvg > 0.25f) (trebleAvg - 0.25f) * 4.0f else 0f
                            
                            scaleX = baseScale * (1f + treblePeak).coerceAtMost(1.7f)
                            scaleY = baseScale * (1f + midPeak).coerceAtMost(1.7f)
                        } else {
                            scaleX = baseScale
                            scaleY = baseScale
                        }
                        if (thumbnailShapeIdx == 0) rotationZ = coverRotationAnim.value
                    }
                    .then(
                        if (coverDragEnabled) {
                            Modifier.pointerInput(Unit) {
                                var localDragOffset = Offset.Zero
                                detectDragGestures(
                                    onDragStart = { localDragOffset = Offset(playerViewModel.coverOffsetX.value, playerViewModel.coverOffsetY.value) },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        localDragOffset = Offset(localDragOffset.x + dragAmount.x, localDragOffset.y + dragAmount.y)
                                        playerViewModel.setCoverOffset(localDragOffset.x, localDragOffset.y)
                                    }
                                )
                            }
                        } else Modifier
                    )
                    .then(
                        if (coverDragEnabled) Modifier
                        else Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { playerViewModel.togglePlayPause() },
                                onDoubleTap = { offset ->
                                    if (showSeek10sTutorial) onDismissSeek10sTutorial()
                                    if (offset.x < size.width / 2) {
                                        playerViewModel.seekTo((currentPosition - 10000).coerceAtLeast(0))
                                        coroutineScope.launch { onFeedbackSeekLeft(true); delay(400); onFeedbackSeekLeft(false) }
                                    } else {
                                        playerViewModel.seekTo((currentPosition + 10000).coerceAtMost(duration))
                                        coroutineScope.launch { onFeedbackSeekRight(true); delay(400); onFeedbackSeekRight(false) }
                                    }
                                }
                            )
                        }
                    )
                    .clip(shape)
                    .background(colorDominant.copy(alpha = 0.5f))
                    .onGloballyPositioned { coordinates ->
                    val yOffset = coordinates.positionInRoot().y
                    val height = coordinates.size.height
                    val centerY = yOffset + (height / 2f)
                    /* 
                    if (abs((Any.albumArtCenterY ?: 0f) - centerY) > 5f) {
                        Any.albumArtCenterY = centerY
                    }
                    */
                },
            contentAlignment = Alignment.Center
        ) {
            if (coverVisibilityMode == "CHROMA_KEY") {
                Box(modifier = Modifier.fillMaxSize().background(chromaColor))
            } else {
                AnimatedContent(targetState = albumArtBitmap, label = "album_art") { bmp ->
                    if (bmp != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(bitmap = bmp, contentDescription = "Album Art", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                            if (thumbnailShapeIdx == 4) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val lineColor = colorVibrant.copy(alpha = 0.6f)
                                    drawLine(color = lineColor, start = androidx.compose.ui.geometry.Offset(w / 2f, 0f), end = androidx.compose.ui.geometry.Offset(w / 2f, h), strokeWidth = 4f)
                                    drawLine(color = lineColor, start = androidx.compose.ui.geometry.Offset(0f, h * 0.4f), end = androidx.compose.ui.geometry.Offset(w, h * 0.4f), strokeWidth = 4f)
                                    drawLine(color = lineColor, start = androidx.compose.ui.geometry.Offset(w / 4f, h * 0.4f), end = androidx.compose.ui.geometry.Offset(w / 4f, h), strokeWidth = 2f)
                                    drawLine(color = lineColor, start = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.4f), end = androidx.compose.ui.geometry.Offset(w * 0.75f, h), strokeWidth = 2f)
                                }
                            }
                        }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(visible = isBuffering, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = paletteColors.vibrant, modifier = Modifier.size(64.dp))
                }
            }
            androidx.compose.animation.AnimatedVisibility(visible = !isPlaying && !isMicModeActive && !cleanUiMode && coverVisibilityMode != "CHROMA_KEY", enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Paused", tint = Color.White, modifier = Modifier.size(64.dp))
                }
            }
        }
        }
        
        // OSCILLOSCOPE FOREGROUND (In front of cover)
        if (currentStyle == VisualizerStyle.OSCILLOSCOPE) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
                    .offset { androidx.compose.ui.unit.IntOffset(coverOffsetX.toInt(), coverOffsetY.toInt()) }
                    .graphicsLayer {
                        scaleX = animatedScaleAnim.value
                        scaleY = animatedScaleAnim.value
                    }
            ) {
                PlayerOscilloscope(this, bassAmplitudesState.value, midAmplitudesState.value, highAmplitudesState.value, combinedAmplitudesState.value, paletteColors, visualizerArchetype, rotationAngle, reactivity, oscilloscopeState, isForeground = true)
            }
        }
        
        // Lyrics status
        if (autoAnalyzeLyrics) {
            PlayerLyricsStatusIndicator(isFetchingLyrics = isFetchingLyrics, searchFailed = searchFailed, availableLyricsResults = availableLyricsResults, onShowLyricsMatches = onShowLyricsMatches)
        }
    }
}

fun PlayerOscilloscope(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    bassAmps: FloatArray, midAmps: FloatArray, highAmps: FloatArray, combinedAmps: FloatArray,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    visualizerArchetype: Int,
    rotationAngle: Float,
    reactivity: Float,
    state: OscilloscopeState,
    isForeground: Boolean
) {
    val w = scope.size.width
    val h = scope.size.height
    val cx = w / 2f
    val cy = h / 2f
    val baseR = minOf(w, h) * 0.18f
    
    // Only update state once per frame (when rendering background)
    if (!isForeground) {
        var avgCombined = 0f
        if (combinedAmps.isNotEmpty()) avgCombined = combinedAmps.average().toFloat()
        
        var bassAvg = 0f
        if (bassAmps.isNotEmpty()) bassAvg = bassAmps.average().toFloat()
        
        val deltaAngle = rotationAngle - (state.accumulatedTime % 360f) // roughly 0.5 per frame
        state.accumulatedTime += (0.5f) * 0.1f + (avgCombined * 0.3f)
        state.dynamicPhase += bassAvg * 0.2f
    }
    
    fun getInterpolatedAmp(amps: FloatArray, tNormalized: Float): Float {
        if (amps.isEmpty()) return 0f
        val exactIdx = tNormalized * (amps.size - 1)
        val idx0 = exactIdx.toInt().coerceIn(0, amps.size - 1)
        val idx1 = (idx0 + 1).coerceIn(0, amps.size - 1)
        val fraction = exactIdx - idx0
        return amps[idx0] + (amps[idx1] - amps[idx0]) * fraction
    }

    val rotSpeedY = 0.005f
    val rotSpeedZ = 0.003f
    val ringAngle = rotationAngle * (kotlin.math.PI / 180.0).toFloat()

    fun drawSphere(amps: FloatArray, color: androidx.compose.ui.graphics.Color, numRings: Int, radiusMod: Float, rotSpeedYMult: Float, rotSpeedZMult: Float, lineW: Float, isHorizontal: Boolean) {
        if (amps.isEmpty()) return
        val timeDelta = state.accumulatedTime * 0.015f * rotSpeedZMult // Faster overall speed
        
        // Wobble on X and Y to keep the hole mostly facing the camera
        val currentXRot = kotlin.math.sin(timeDelta * 0.5f) * 0.4f
        val currentYRot = kotlin.math.cos(timeDelta * 0.7f) * 0.4f
        val currentZRot = ringAngle + timeDelta * 2.0f
        val numPoints = if (com.example.beatpulse.utils.SystemUtils.isMobilePlatform) 60 else 120
        
        for (ring in 0 until numRings) {
            val ringT = ring.toFloat() / numRings
            val ringOffset = ringT * kotlin.math.PI.toFloat() * 2f
            
            var prevX = 0f
            var prevY = 0f
            var prevZ = 0f
            var hasPrev = false
            
            for (i in 0..numPoints) {
                val t = (i.toFloat() / numPoints) * kotlin.math.PI.toFloat() * 2f
                val normalizedT = i.toFloat() / numPoints
                val mirroredT = if (normalizedT < 0.5f) normalizedT * 2f else (1f - normalizedT) * 2f
                
                val amp = getInterpolatedAmp(amps, mirroredT) * reactivity
                val stretch = 1f + (amp * 4.0f)
                val r = baseR * radiusMod * stretch
                
                val pX = kotlin.math.cos(t) * r
                val pY = kotlin.math.sin(t) * r
                
                var sX = pX
                var sY = pY * kotlin.math.cos(ringOffset)
                var sZ = pY * kotlin.math.sin(ringOffset)
                
                if (!isHorizontal) {
                    sY = pX * kotlin.math.cos(ringOffset)
                    sX = pY
                    sZ = pX * kotlin.math.sin(ringOffset)
                }
                
                // 1. Z-axis rotation (spin around the cover)
                val x1 = sX * kotlin.math.cos(currentZRot) - sY * kotlin.math.sin(currentZRot)
                val y1 = sX * kotlin.math.sin(currentZRot) + sY * kotlin.math.cos(currentZRot)
                val z1 = sZ
                
                // 2. X-axis rotation (tilt up/down)
                val x2 = x1
                val y2 = y1 * kotlin.math.cos(currentXRot) - z1 * kotlin.math.sin(currentXRot)
                val z2 = y1 * kotlin.math.sin(currentXRot) + z1 * kotlin.math.cos(currentXRot)
                
                // 3. Y-axis rotation (tilt left/right)
                val x3 = x2 * kotlin.math.cos(currentYRot) - z2 * kotlin.math.sin(currentYRot)
                val y3 = y2
                val z3 = x2 * kotlin.math.sin(currentYRot) + z2 * kotlin.math.cos(currentYRot)
                
                val px = cx + x3.toFloat()
                val py = cy + y3.toFloat()
                
                if (hasPrev) {
                    val avgZ = (prevZ + z3) / 2f
                    if ((isForeground && avgZ >= 0) || (!isForeground && avgZ < 0)) {
                        scope.drawLine(
                            color = color,
                            start = androidx.compose.ui.geometry.Offset(prevX, prevY),
                            end = androidx.compose.ui.geometry.Offset(px, py),
                            strokeWidth = lineW,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
                prevX = px
                prevY = py
                prevZ = z3.toFloat()
                hasPrev = true
            }
        }
    }

    fun drawTorusKnot(amps: FloatArray, color: androidx.compose.ui.graphics.Color, p: Float, q: Float, lineW: Float, rotSpeedMult: Float) {
        if (amps.isEmpty()) return
        val timeDelta = state.accumulatedTime * 0.015f * rotSpeedMult // Faster flowing
        
        // Wobble on X and Y to keep the hole facing us, spin on Z
        val currentXRot = kotlin.math.sin(timeDelta * 0.5f) * 0.4f
        val currentYRot = kotlin.math.cos(timeDelta * 0.7f) * 0.4f
        val currentZRot = ringAngle + timeDelta * 1.5f
        val numPoints = if (com.example.beatpulse.utils.SystemUtils.isMobilePlatform) 90 else 250 // Reduced for mobile
        
        var prevX = 0f
        var prevY = 0f
        var prevZ = 0f
        var hasPrev = false
        
        for (i in 0..numPoints) {
            val normalizedT = i.toFloat() / numPoints
            val t = normalizedT * kotlin.math.PI.toFloat() * 2f
            val mirroredT = if (normalizedT < 0.5f) normalizedT * 2f else (1f - normalizedT) * 2f
            
            val amp = getInterpolatedAmp(amps, mirroredT) * reactivity
            val stretch = 1f + (amp * 1.5f)
            
            // Torus knot equations
            val r1 = baseR * 1.4f // Main radius (wraps around the album art)
            val r2 = baseR * 0.4f * stretch // Tube radius (reacts to audio)
            
            val rTorus = r1 + r2 * kotlin.math.cos(q * t + timeDelta * 1.5f)
            val lX = rTorus * kotlin.math.cos(p * t + timeDelta)
            val lY = rTorus * kotlin.math.sin(p * t + timeDelta)
            val lZ = r2 * kotlin.math.sin(q * t + timeDelta * 1.5f)
            
            // 1. Z-axis rotation (spin around the cover)
            val x1 = lX * kotlin.math.cos(currentZRot) - lY * kotlin.math.sin(currentZRot)
            val y1 = lX * kotlin.math.sin(currentZRot) + lY * kotlin.math.cos(currentZRot)
            val z1 = lZ
            
            // 2. X-axis rotation (tilt up/down)
            val x2 = x1
            val y2 = y1 * kotlin.math.cos(currentXRot) - z1 * kotlin.math.sin(currentXRot)
            val z2 = y1 * kotlin.math.sin(currentXRot) + z1 * kotlin.math.cos(currentXRot)
            
            // 3. Y-axis rotation (tilt left/right)
            val x3 = x2 * kotlin.math.cos(currentYRot) - z2 * kotlin.math.sin(currentYRot)
            val y3 = y2
            val z3 = x2 * kotlin.math.sin(currentYRot) + z2 * kotlin.math.cos(currentYRot)
            
            val px = cx + x3.toFloat()
            val py = cy + y3.toFloat()
            
            if (hasPrev) {
                val avgZ = (prevZ + z3) / 2f
                if ((isForeground && avgZ >= 0) || (!isForeground && avgZ < 0)) {
                    scope.drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(prevX, prevY),
                        end = androidx.compose.ui.geometry.Offset(px, py),
                        strokeWidth = lineW,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            prevX = px
            prevY = py
            prevZ = z3.toFloat()
            hasPrev = true
        }
    }

    if (visualizerArchetype == 1) { // 3 waves (Torus Knots)
        drawTorusKnot(bassAmps, paletteColors.dominant, 3f, 8f, 6f, 0.8f) // 3 lobes, 8 twists
        drawTorusKnot(midAmps, paletteColors.vibrant, 5f, 3f, 4f, 1.2f)   // 5 lobes, 3 twists
        drawTorusKnot(highAmps, paletteColors.muted.copy(alpha = 0.7f), 7f, 4f, 2.5f, 1.6f) // 7 lobes, 4 twists
    } else { // 1 wave
        drawSphere(bassAmps, paletteColors.dominant.copy(alpha=0.6f), 8, 1.2f, 0.8f, 0.5f, 3f, true)
        drawSphere(midAmps, paletteColors.vibrant.copy(alpha=0.8f), 6, 1.0f, 1.2f, 0.7f, 4f, false)
        drawSphere(highAmps, paletteColors.muted, 4, 0.8f, 1.5f, 1.0f, 6f, true)
    }
}









// Pre-allocated buffers for PlayerSidePerspectiveBandsBackground to avoid GC pauses at 60 FPS
private val sidePerspectiveSmoothedAmpsBuf = FloatArray(256)
private val sidePerspectiveXLArr = FloatArray(256)
private val sidePerspectiveYLArr = FloatArray(256)
private val sidePerspectiveXRArr = FloatArray(256)
private val sidePerspectiveYRArr = FloatArray(256)

// Pre-allocated paths for Terrain 3D to avoid massive object allocation (100k+/sec)
private val terrainSharedQuadPath = androidx.compose.ui.graphics.Path()
private val terrainSharedLinePath = androidx.compose.ui.graphics.Path()
private val sidePerspectivePathLeft = androidx.compose.ui.graphics.Path()
private val sidePerspectivePathRight = androidx.compose.ui.graphics.Path()
private val sidePerspectivePathFill = androidx.compose.ui.graphics.Path()

/** Draws SIDE_PERSPECTIVE_BANDS visualizer in the background (drawBehind scope) */
fun PlayerSidePerspectiveBandsBackground(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    bassAmps: FloatArray, midAmps: FloatArray, highAmps: FloatArray, combinedAmps: FloatArray,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    bassMult: Float, midMult: Float, trebleMult: Float, reactivity: Float, visualizerArchetype: Int
) {
    with(scope) {
        val w = size.width
        val h = size.height
        val bassOpacity = (0.5f + bassMult * 0.4f + reactivity * 0.2f).coerceIn(0f, 1f)
        val midOpacity = (0.6f + midMult * 0.3f + reactivity * 0.2f).coerceIn(0f, 1f)
        val highOpacity = (0.7f + trebleMult * 0.2f + reactivity * 0.2f).coerceIn(0f, 1f)
        val vpX = w * 0.5f
        val vpY = h * 0.5f

        fun drawPerspectivePillar(amps: FloatArray, color: androidx.compose.ui.graphics.Color, widthMult: Float, isLeft: Boolean) {
            val count = amps.size; if (count == 0) return
            val sAmps = if (sidePerspectiveSmoothedAmpsBuf.size >= count) sidePerspectiveSmoothedAmpsBuf else FloatArray(count)
            val sXL = if (sidePerspectiveXLArr.size >= count) sidePerspectiveXLArr else FloatArray(count)
            val sYL = if (sidePerspectiveYLArr.size >= count) sidePerspectiveYLArr else FloatArray(count)
            val sXR = if (sidePerspectiveXRArr.size >= count) sidePerspectiveXRArr else FloatArray(count)
            val sYR = if (sidePerspectiveYRArr.size >= count) sidePerspectiveYRArr else FloatArray(count)
            
            for (i in 0 until count) {
                var sum = 0f
                var weightSum = 0f
                // Wider smoothing radius (-8 to 8) for much more fluid organic curves
                for (j in -8..8) {
                    val idx = i + j
                    if (idx in 0 until count) {
                        val weight = 1f / (1f + kotlin.math.abs(j))
                        sum += amps[idx] * weight
                        weightSum += weight
                    }
                }
                sAmps[i] = sum / weightSum
            }
            
            // Move pillars a bit away from the edges
            val cX = if (isLeft) w * 0.11f else w * 0.89f
            val baseWidth = w * 0.03f
            
            for (i in 0 until count) {
                val t = -0.4f + 1.8f * (i.toFloat() / (count - 1).coerceAtLeast(1))
                val yEdge = h * (1f - t)
                val amp = sAmps[i]
                
                // Deadzone to mute base noise, then a 2.8x linear multiplier.
                val processedAmp = kotlin.math.max(0f, amp - 0.05f) * 2.8f
                
                val bandWidth = baseWidth + (processedAmp * w * 0.22f * widthMult)
                val xL = cX - bandWidth / 2f
                val xR = cX + bandWidth / 2f
                val slope = (vpY - yEdge) / (vpX - cX)
                sXL[i] = xL
                sYL[i] = yEdge + (xL - cX) * slope
                sXR[i] = xR
                sYR[i] = yEdge + (xR - cX) * slope
            }
            
            for (i in 0 until count) {
                drawLine(
                    color = color.copy(alpha = color.alpha * 0.5f),
                    start = androidx.compose.ui.geometry.Offset(sXL[i], sYL[i]),
                    end = androidx.compose.ui.geometry.Offset(sXR[i], sYR[i]),
                    strokeWidth = 2f
                )
            }
            
            if (count > 0) {
                sidePerspectivePathLeft.reset()
                sidePerspectivePathLeft.moveTo(sXL[0], sYL[0])
                for (i in 0 until count - 1) {
                    sidePerspectivePathLeft.lineTo(sXL[i], sYL[i+1])
                    sidePerspectivePathLeft.lineTo(sXL[i+1], sYL[i+1])
                }
                sidePerspectivePathRight.reset()
                sidePerspectivePathRight.moveTo(sXR[0], sYR[0])
                for (i in 0 until count - 1) {
                    sidePerspectivePathRight.lineTo(sXR[i], sYR[i+1])
                    sidePerspectivePathRight.lineTo(sXR[i+1], sYR[i+1])
                }
                sidePerspectivePathFill.reset()
                sidePerspectivePathFill.moveTo(sXL[0], sYL[0])
                for (i in 0 until count - 1) {
                    sidePerspectivePathFill.lineTo(sXL[i], sYL[i+1])
                    sidePerspectivePathFill.lineTo(sXL[i+1], sYL[i+1])
                }
                sidePerspectivePathFill.lineTo(sXR[count-1], sYR[count-1])
                for (i in count - 1 downTo 1) {
                    sidePerspectivePathFill.lineTo(sXR[i], sYR[i-1])
                    sidePerspectivePathFill.lineTo(sXR[i-1], sYR[i-1])
                }
                sidePerspectivePathFill.close()
                drawPath(sidePerspectivePathFill, color.copy(alpha = color.alpha * 0.2f), style = androidx.compose.ui.graphics.drawscope.Fill)
                drawPath(sidePerspectivePathLeft, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f, join = androidx.compose.ui.graphics.StrokeJoin.Miter))
                drawPath(sidePerspectivePathRight, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f, join = androidx.compose.ui.graphics.StrokeJoin.Miter))
            }
        }

        val drawBandLayer = { amps: FloatArray, color: androidx.compose.ui.graphics.Color, widthMult: Float ->
            drawPerspectivePillar(amps, color, widthMult, true)
            drawPerspectivePillar(amps, color, widthMult, false)
        }

        if (visualizerArchetype == 1) {
            drawBandLayer(combinedAmps, paletteColors.vibrant.copy(alpha = maxOf(bassOpacity, midOpacity, highOpacity)), 1.5f)
        } else {
            drawBandLayer(bassAmps, paletteColors.dominant.copy(alpha = bassOpacity * 0.8f), 0.9f)
            drawBandLayer(midAmps, paletteColors.vibrant.copy(alpha = midOpacity), 1.3f)
            drawBandLayer(highAmps, paletteColors.muted.copy(alpha = highOpacity), 0.7f)
        }
    }
}
