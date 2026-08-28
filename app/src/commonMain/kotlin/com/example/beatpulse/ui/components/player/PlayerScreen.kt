package com.example.beatpulse.ui.components.player

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.Feedback

import androidx.compose.animation.AnimatedVisibility
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
    WAVE, SLIME, BARS, DOTS, PARTICLES, RINGS, AURA, BANDS
}

enum class DragAction { NONE, DJ_SEEK, OPEN_QUEUE }


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

@Composable
fun PlayerScreen(
    playerViewModel: IPlayerViewModel,
    dynamicColorsPlus: Boolean,
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
    modifier: Modifier = Modifier
) {
    // Delegate to the internal implementation to keep the top-level function's register count low
    PlayerScreenContent(
        playerViewModel = playerViewModel,
        dynamicColorsPlus = dynamicColorsPlus,
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
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreenContent(
    playerViewModel: IPlayerViewModel,
    dynamicColorsPlus: Boolean,
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
    modifier: Modifier = Modifier
) {
        val currentTrack = state.currentTrack
    val currentQueue = state.currentQueue
    val paletteColors = state.paletteColors
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
    val isBuffering = false

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

    var activeDynamicColor by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(dynamicColorsPlus, dynamicColorsInterval, paletteColors) {
        if (!dynamicColorsPlus) {
            activeDynamicColor = null
            return@LaunchedEffect
        }
        val colors = listOf(
            paletteColors.dominant,
            paletteColors.vibrant,
            paletteColors.muted
        )
        if (colors.isEmpty()) {
            activeDynamicColor = null
            return@LaunchedEffect
        }
        var index = 0
        while (true) {
            activeDynamicColor = colors[index % colors.size]
            index++
            kotlinx.coroutines.delay(dynamicColorsInterval * 1000L)
        }
    }

    val targetColor = if (dynamicColorsPlus && activeDynamicColor != null) activeDynamicColor!! else paletteColors.dominant
    val colorDominant by animateColorAsState(targetColor, animationSpec = androidx.compose.animation.core.tween(3000), label = "color_dom")
    val colorVibrant by animateColorAsState(paletteColors.vibrant, label = "color_vib")
    val colorMuted by animateColorAsState(paletteColors.muted, label = "color_mut")

    val thumbnailShapeIdx by prefs.thumbnailShapeFlow.collectAsState()
    val shape = when (thumbnailShapeIdx) {
        1 -> RoundedCornerShape(0.dp)
        2 -> RoundedCornerShape(16.dp)
        3 -> RoundedCornerShape(32.dp)
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
    val coroutineScope = rememberCoroutineScope()
    var showSupportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        playerViewModel.settingsMenuRequested.collect {
            showSettingsMenu = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }

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

    // --- MAIN LAYOUT ---
    Box(
        modifier = modifier.fillMaxSize().drawBehind {
            if (currentStyle == VisualizerStyle.BANDS) {
                PlayerBandsBackground(this, bassAmplitudesState.value, midAmplitudesState.value, highAmplitudesState.value, combinedAmplitudesState.value, paletteColors, bassMult, midMult, trebleMult, reactivity, visualizerArchetype)
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
                playerViewModel.toggleMicMode()
            },
            onShowStreamConfig = { showStreamConfigDialog = true }
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
            cleanUiMode = cleanUiMode,
            coverDragEnabled = playerViewModel.coverDragEnabled.collectAsState().value,
            coverVisibilityMode = playerViewModel.coverVisibilityMode.collectAsState().value,
            chromaKeyColor = playerViewModel.chromaKeyColor.collectAsState().value,
            coverScale = playerViewModel.coverScale.collectAsState().value,
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
    PlayerStreamConfigDialog(showStreamConfigDialog = showStreamConfigDialog, onDismissRequest = { showStreamConfigDialog = false }, colorVibrant = colorVibrant, colorDominant = colorDominant, playerViewModel = playerViewModel)

    val styleNames = mapOf(
        VisualizerStyle.WAVE to "Ondas",
        VisualizerStyle.SLIME to "Slime",
        VisualizerStyle.BARS to "Barras",
        VisualizerStyle.DOTS to "Puntos",
        VisualizerStyle.PARTICLES to "Partículas",
        VisualizerStyle.RINGS to "Anillos",
        VisualizerStyle.AURA to "Aura",
        VisualizerStyle.BANDS to "Bandas"
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

    if (showMicPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showMicPermissionDialog = false },
            title = { Text("Modo Grabación", color = colorVibrant) },
            text = { Text("DuWave requiere permiso para grabar audio y así sincronizar los visualizadores. No guardaremos ningún audio.", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    showMicPermissionDialog = false
                    playerViewModel.toggleMicMode()
                }) { Text("Confirmar", color = colorVibrant) }
            },
            dismissButton = {
                TextButton(onClick = { showMicPermissionDialog = false }) { Text("Cancelar", color = Color.Gray) }
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

/** Track info header (title, artist, time, buttons) */
@Composable
private fun PlayerTrackInfoHeader(
    currentTrack: TrackEntity?,
    isMicModeActive: Boolean, streamConfigUiVisible: Boolean,
    colorVibrant: Color, paletteColors: com.example.beatpulse.theme.PaletteColors,
    abRepeatModeEnabled: Boolean, abPointA: Float, abPointB: Float,
    duration: Long, currentPosition: Long,
    showMicButton: Boolean, lyrics: List<com.example.beatpulse.utils.LyricLine>,
    showLyrics: Boolean, onToggleLyrics: () -> Unit,
    onShowSupport: () -> Unit, onAddToPlaylist: () -> Unit,
    onToggleMicMode: () -> Unit, onShowStreamConfig: () -> Unit
) {
    AnimatedContent(targetState = currentTrack, label = "track_info") { track ->
        if (track != null) {
            Box(modifier = Modifier.alpha(if (!isMicModeActive || streamConfigUiVisible) 1f else 0f).fillMaxWidth().padding(top = 24.dp, start = 24.dp, end = 24.dp)) {
                Column(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.6f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = track.title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, modifier = Modifier.basicMarquee())
                    var showRemainingTime by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { showRemainingTime = !showRemainingTime }
                    ) {
                        Text(text = track.artist, style = MaterialTheme.typography.bodyMedium, color = colorVibrant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
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
                Row(modifier = Modifier.align(Alignment.CenterStart)) {
                    IconButton(onClick = onShowSupport, modifier = Modifier.size(36.dp).clip(CircleShape).background(paletteColors.dominant.copy(alpha = 0.5f))) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = "Apoyo y Sugerencias", tint = colorVibrant, modifier = Modifier.size(20.dp))
                    }
                }
                Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                    AnimatedContent(
                        targetState = if (isMicModeActive) 2 else if (showMicButton) 1 else 0,
                        transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) }
                    ) { state ->
                        when (state) {
                            0 -> IconButton(onClick = onAddToPlaylist, modifier = Modifier.padding(end = 8.dp).size(36.dp).clip(CircleShape).background(paletteColors.dominant.copy(alpha = 0.5f))) {
                                Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = "Añadir a Playlist", tint = colorVibrant, modifier = Modifier.size(20.dp))
                            }
                            1 -> IconButton(onClick = onToggleMicMode, modifier = Modifier.padding(end = 8.dp).size(36.dp).clip(CircleShape).background(paletteColors.dominant.copy(alpha = 0.5f))) {
                                Icon(imageVector = Icons.Default.Mic, contentDescription = "Modo Streamer", tint = colorVibrant, modifier = Modifier.size(20.dp))
                            }
                            2 -> IconButton(onClick = onShowStreamConfig, modifier = Modifier.padding(end = 8.dp).size(36.dp).clip(CircleShape).background(paletteColors.dominant.copy(alpha = 0.5f))) {
                                Icon(imageVector = Icons.Default.CastConnected, contentDescription = "Configuración de Stream", tint = colorVibrant, modifier = Modifier.size(20.dp))
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
    cleanUiMode: Boolean,
    coverDragEnabled: Boolean,
    coverVisibilityMode: String,
    chromaKeyColor: String,
    coverScale: Float,
    coverOffsetX: Float,
    coverOffsetY: Float,
    albumArtBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    isPlaying: Boolean, isBuffering: Boolean,
    isFetchingLyrics: Boolean, searchFailed: Boolean,
    availableLyricsResults: List<Any>, autoAnalyzeLyrics: Boolean,
    showLyricsMatches: Boolean, onShowLyricsMatches: () -> Unit
) {
    var currentDragAction by remember { mutableStateOf(DragAction.NONE) }
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
            .pointerInput(abRepeatModeEnabled) {
                detectDragGestures(
                    onDragStart = { currentDragAction = DragAction.NONE; lastAngle = null; accumulatedAngle = 0f; dragSeekTimeMs = currentPosition },
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
                    if (currentDragAction == DragAction.DJ_SEEK) {
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
            .pointerInput(abRepeatModeEnabled) {
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
            },
        contentAlignment = Alignment.Center
    ) {
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
                        scaleX = animatedScaleAnim.value * coverScale; 
                        scaleY = animatedScaleAnim.value * coverScale; 
                        if (thumbnailShapeIdx == 0) rotationZ = coverRotationAnim.value 
                    }
                    .clip(shape)
                    .background(colorDominant.copy(alpha = 0.5f))
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
                    .pointerInput(Unit) {
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
                    if (bmp != null) Image(bitmap = bmp, contentDescription = "Album Art", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
        // Lyrics status
        if (autoAnalyzeLyrics) {
            PlayerLyricsStatusIndicator(isFetchingLyrics = isFetchingLyrics, searchFailed = searchFailed, availableLyricsResults = availableLyricsResults, onShowLyricsMatches = onShowLyricsMatches)
        }
    }
}








