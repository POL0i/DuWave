package com.example.beatpulse.ui.components.player

import androidx.compose.material.icons.filled.Star
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.ui.graphics.asAndroidPath
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.media.AudioManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import kotlinx.coroutines.launch
import androidx.media3.common.PlaybackParameters
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Feedback
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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.automirrored.filled.List

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.beatpulse.R
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beatpulse.ui.components.PixelIcons
import androidx.media3.exoplayer.ExoPlayer
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.visualizer.AudioVisualizerManager
import com.example.beatpulse.visualizer.FilterMode
import kotlinx.coroutines.delay

class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var alpha: Float, val color: Color)

enum class VisualizerStyle {
    WAVE, SLIME, BARS, DOTS, PARTICLES, RINGS, AURA
}

enum class DragAction { NONE, DJ_SEEK, OPEN_QUEUE }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerViewModel: com.example.beatpulse.ui.components.player.PlayerViewModel,
    visualizerManager: AudioVisualizerManager,
    equalizerManager: com.example.beatpulse.service.EqualizerManager,
    exoPlayer: androidx.media3.common.Player?,
    currentTrack: TrackEntity? = null,
    currentQueue: List<TrackEntity>,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    modifier: Modifier = Modifier,
    prefs: com.example.beatpulse.data.PreferencesManager,
    repeatModeState: Int = 0,
    shuffleModeState: Boolean = false,
    playbackSpeed: Float = 1.0f,
    playbackPitch: Float = 1.0f,
    reverbEnabled: Boolean = false,
    effectsPreset: String = "NORMAL",
    onSetSpeed: (Float) -> Unit = {},
    onSetPitch: (Float) -> Unit = {},
    onSetReverb: (Boolean) -> Unit = {},
    onApplyPreset: (String) -> Unit = {},
    sleepTimerSeconds: Int = 0,
    onSetSleepTimer: (Int) -> Unit = {},
    onUpdateTrackMetadata: (Long, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onAddToPlaylist: ((TrackEntity) -> Unit)? = null
) {
    val albumArtBitmap = currentTrack?.let { com.example.beatpulse.ui.components.rememberFullAlbumArt(it) }
    val bassAmplitudesState = visualizerManager.bassAmplitudes.collectAsState()
    val midAmplitudesState = visualizerManager.midAmplitudes.collectAsState()
    val highAmplitudesState = visualizerManager.highAmplitudes.collectAsState()
    val bgStyle by prefs.backgroundStyleFlow.collectAsState(initial = 0)
    var currentStyle by remember { 
        mutableStateOf(
            try {
                VisualizerStyle.valueOf(prefs.visualizerStyle)
            } catch (e: IllegalArgumentException) {
                if (prefs.visualizerStyle == "SYMMETRY") VisualizerStyle.SLIME else VisualizerStyle.BARS
            }
        ) 
    }
    LaunchedEffect(currentStyle) {
        prefs.visualizerStyle = currentStyle.name
    }
    
    // Player state
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var duration by remember { androidx.compose.runtime.mutableLongStateOf(1L) }
    
    // A-B Repeat state
    val abRepeatModeEnabled by playerViewModel.abRepeatModeEnabled.collectAsState()
    val abPointA by playerViewModel.abPointA.collectAsState()
    val abPointB by playerViewModel.abPointB.collectAsState()
    var activeDraggingHandle by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var showRemainingTime by remember { androidx.compose.runtime.mutableStateOf(false) }
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
                val parsedLyrics = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.beatpulse.utils.LrcParser.parseLrcFile(lrcFile)
                }
                lyrics = parsedLyrics
            } else {
                lyrics = emptyList()
            }
        } else {
            lyrics = emptyList()
        }
    }

    // Advanced Audio Settings State
    val isAdvanced by visualizerManager.isAdvancedMode.collectAsState()
    val filterMode by visualizerManager.filterMode.collectAsState()
    val sensitivity by visualizerManager.sensitivity.collectAsState()
    val reactivity by visualizerManager.reactivity.collectAsState()
    val bassMult by visualizerManager.bassMultiplier.collectAsState()
    val midMult by visualizerManager.midMultiplier.collectAsState()
    val trebleMult by visualizerManager.trebleMultiplier.collectAsState()
    val visualizerArchetype by visualizerManager.visualizerArchetype.collectAsState()
    val fftMode by visualizerManager.fftMode.collectAsState()
    val combinedAmplitudesState = visualizerManager.combinedAmplitudes.collectAsState()
    var showAdvancedSettings by remember { mutableStateOf(false) }

    val colorDominant by animateColorAsState(paletteColors.dominant, label = "color_dom")
    val colorVibrant by animateColorAsState(paletteColors.vibrant, label = "color_vib")
    val colorMuted by animateColorAsState(paletteColors.muted, label = "color_mut")
    
    val thumbnailShapeIdx by prefs.thumbnailShapeFlow.collectAsState()
    val shape = when (thumbnailShapeIdx) {
        1 -> androidx.compose.foundation.shape.RoundedCornerShape(0.dp) // Square
        2 -> androidx.compose.foundation.shape.RoundedCornerShape(16.dp) // Rounded Square
        3 -> androidx.compose.foundation.shape.RoundedCornerShape(32.dp) // Squircle
        else -> androidx.compose.foundation.shape.CircleShape
    }
    
    val sharedPath = remember { Path() }
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    var showQueue by remember { mutableStateOf(false) }
    var currentStyleName by remember { mutableStateOf<String?>(null) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showEqDialog by remember { mutableStateOf(false) }
    var showEffectsDialog by remember { mutableStateOf(false) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isPlayingState by remember { mutableStateOf(exoPlayer?.isPlaying ?: false) }

    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayingState = isPlaying
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
        }
    }

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val fastRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation2"
    )

    // Update player state periodically
    LaunchedEffect(exoPlayer, isPlayingState) {
        while (true) {
            if (exoPlayer != null) {
                isPlaying = exoPlayer.isPlaying
                duration = exoPlayer.duration.coerceAtLeast(1L)
                currentPosition = exoPlayer.currentPosition
                
                if (abRepeatModeEnabled) {
                    val aPos = (abPointA * duration).toLong()
                    val bPos = (abPointB * duration).toLong()
                    if (currentPosition >= bPos && bPos > aPos) {
                        exoPlayer.seekTo(aPos)
                        currentPosition = aPos
                    } else if (currentPosition < aPos && bPos > aPos) {
                        exoPlayer.seekTo(aPos)
                        currentPosition = aPos
                    }
                }
            }
            if (isPlayingState) {
                delay(100L) // Poll at 10fps for smooth progress update
            } else {
                delay(1000L) // Idle poll when paused
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    var showFeedbackDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dynamicTextColor = if (paletteColors.dominant.luminance() < 0.5f) Color.White else Color.Black

    if (showFeedbackDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text(stringResource(R.string.suggest_improvements), color = colorVibrant) },
            text = { Text(stringResource(R.string.suggest_improvements_desc), color = dynamicTextColor) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/POL0i/DuWave/issues"))
                    context.startActivity(intent)
                    showFeedbackDialog = false
                }) {
                    Text(stringResource(R.string.open), color = colorVibrant)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text(stringResource(R.string.cancel), color = dynamicTextColor.copy(alpha=0.7f))
                }
            },
            containerColor = paletteColors.dominant
        )
    }

    Column(modifier = modifier
        .fillMaxSize()
        .then(if (isLandscape) Modifier.verticalScroll(scrollState) else Modifier)
    ) {
        

        // Track Info Header
        AnimatedContent(targetState = currentTrack, label = "track_info") { track ->
            if (track != null) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 24.dp, end = 24.dp)) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.6f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        var showRemainingTime by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = androidx.compose.ui.Modifier.clickable(
                                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { showRemainingTime = !showRemainingTime }
                        ) {
                            Text(
                                text = track.artist,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = colorVibrant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                            val dur = duration
                            val pos = currentPosition
                            val formatTime = { ms: Long -> 
                                val totalSeconds = ms / 1000
                                val m = totalSeconds / 60
                                val s = totalSeconds % 60
                                String.format("%02d:%02d", m, s)
                            }
                            val timeText = if (abRepeatModeEnabled) {
                                val aTime = (abPointA * dur).toLong()
                                val bTime = (abPointB * dur).toLong()
                                val posStr = if (showRemainingTime) "-${formatTime(dur - pos)}" else formatTime(pos)
                                "$posStr / A:${formatTime(aTime)} - B:${formatTime(bTime)}"
                            } else {
                                if (showRemainingTime) "-${formatTime(dur - pos)} / ${formatTime(dur)}" else "${formatTime(pos)} / ${formatTime(dur)}"
                            }
                            Text(
                                text = timeText,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = colorVibrant
                            )
                        }
                    }
                    Row(modifier = Modifier.align(Alignment.CenterStart)) {
                        IconButton(
                            onClick = { showFeedbackDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(paletteColors.dominant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Feedback,
                                contentDescription = "Sugerir mejoras",
                                tint = colorVibrant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                        IconButton(
                            onClick = { onAddToPlaylist?.invoke(track) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(paletteColors.dominant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Añadir a Playlist",
                                tint = colorVibrant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (lyrics.isNotEmpty()) {
                            IconButton(
                                onClick = { showLyrics = !showLyrics },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(paletteColors.dominant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Letras",
                                    tint = if (showLyrics) colorVibrant else colorVibrant.copy(alpha=0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        var currentDragAction by remember { mutableStateOf(DragAction.NONE) }
        var lastAngle by remember { mutableStateOf<Float?>(null) }
        var dragSeekTimeMs by remember { mutableStateOf<Long?>(null) }
        val coverRotationAnim = remember { androidx.compose.animation.core.Animatable(0f) }
        val sparks = remember { mutableListOf<Spark>() }
        var playheadPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        val coroutineScope = rememberCoroutineScope()
        var accumulatedAngle by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        val haptic = LocalHapticFeedback.current
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager

        Box(
            modifier = Modifier
                .then(if (isLandscape) Modifier.height(350.dp) else Modifier.weight(1f))
                .fillMaxWidth()
                .pointerInput(abRepeatModeEnabled) {
                    detectDragGestures(
                        onDragStart = { 
                            currentDragAction = DragAction.NONE
                            lastAngle = null 
                            accumulatedAngle = 0f
                            dragSeekTimeMs = exoPlayer?.currentPosition
                        },
                        onDragEnd = { 
                            if (currentDragAction == DragAction.DJ_SEEK) {
                                dragSeekTimeMs?.let { exoPlayer?.seekTo(it) }
                            }
                            coroutineScope.launch { coverRotationAnim.animateTo(0f, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)) }
                            currentDragAction = DragAction.NONE
                            coroutineScope.launch { coverRotationAnim.animateTo(0f, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)) }
                            lastAngle = null 
                            dragSeekTimeMs = null
                        },
                        onDragCancel = { 
                            currentDragAction = DragAction.NONE
                            coroutineScope.launch { coverRotationAnim.animateTo(0f, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)) }
                            lastAngle = null 
                            dragSeekTimeMs = null
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val center = androidx.compose.ui.geometry.Offset(size.width.toFloat() / 2f, size.height.toFloat() / 2f)
                        val touchPos = change.position
                        
                        if (currentDragAction == DragAction.NONE) {
                            if (dragAmount.y < -15f && kotlin.math.abs(dragAmount.x) < 20f && touchPos.y > center.y) {
                                currentDragAction = DragAction.OPEN_QUEUE
                                showQueue = true
                            } else if (kotlin.math.abs(dragAmount.x) > 5f || kotlin.math.abs(dragAmount.y) > 5f) {
                                currentDragAction = DragAction.DJ_SEEK
                                val dx = touchPos.x - center.x
                                val dy = touchPos.y - center.y
                                lastAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                            }
                        }
                        
                        if (currentDragAction == DragAction.DJ_SEEK) {
                            val dx = touchPos.x - center.x
                            val dy = touchPos.y - center.y
                            val currentAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                            
                            val prevAngle = lastAngle
                            if (prevAngle != null) {
                                var deltaAngle = currentAngle - prevAngle
                                if (deltaAngle > 180f) deltaAngle -= 360f
                                if (deltaAngle < -180f) deltaAngle += 360f
                                
                                accumulatedAngle += deltaAngle
                                
                                if (kotlin.math.abs(accumulatedAngle) >= 10f) {
                                    audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    accumulatedAngle = 0f
                                }
                                
                                val seekMs = (deltaAngle / 360f) * 120000f // 2 minutos por vuelta
                                exoPlayer?.let { player ->
                                    val current = dragSeekTimeMs ?: player.currentPosition
                                    dragSeekTimeMs = (current + seekMs.toLong()).coerceIn(0, player.duration)
                                }
                                coroutineScope.launch { coverRotationAnim.snapTo(coverRotationAnim.value + deltaAngle) }
                                
                                // Spawn sparks
                                if (Math.random() < 0.5) { // No muchas chispas
                                    val vx = (Math.random().toFloat() - 0.5f) * 15f
                                    val vy = (Math.random().toFloat() - 0.5f) * 15f
                                    val sparkColor = if (Math.random() < 0.5) paletteColors.vibrant else paletteColors.dominant
                                    sparks.add(Spark(playheadPos.x, playheadPos.y, vx, vy, 1f, sparkColor))
                                }
                            }
                            lastAngle = currentAngle
                        }
                    }
                }
                .pointerInput(abRepeatModeEnabled) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2) {
                                exoPlayer?.seekToPrevious()
                            } else {
                                exoPlayer?.seekToNext()
                            }
                        },
                        onPress = {
                            val params = exoPlayer?.playbackParameters ?: androidx.media3.common.PlaybackParameters.DEFAULT
                            val job = coroutineScope.launch {
                                delay(300)
                                exoPlayer?.playbackParameters = params.withSpeed(2f)
                            }
                            tryAwaitRelease()
                            job.cancel()
                            if (exoPlayer?.playbackParameters?.speed == 2f) {
                                exoPlayer?.playbackParameters = params.withSpeed(1f)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            

            val bassAvgAnim = remember { androidx.compose.animation.core.Animatable(0f) }
            val midAvgAnim = remember { androidx.compose.animation.core.Animatable(0f) }
            val trebleAvgAnim = remember { androidx.compose.animation.core.Animatable(0f) }
            val animatedScaleAnim = remember { androidx.compose.animation.core.Animatable(1f) }

            LaunchedEffect(visualizerManager.bassAmplitudes, visualizerManager.midAmplitudes, visualizerManager.highAmplitudes) {
                launch {
                    visualizerManager.bassAmplitudes.collect { amps ->
                        if (amps.isNotEmpty()) {
                            val rawBass = amps.average().toFloat().let { if(it.isNaN()) 0f else it }
                            launch { bassAvgAnim.animateTo(rawBass, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                            val bassIntensity = rawBass.coerceIn(0f, 1f)
                            launch { animatedScaleAnim.animateTo(1f + (bassIntensity * 0.45f), androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)) }
                        }
                    }
                }
                launch {
                    visualizerManager.midAmplitudes.collect { amps ->
                        if (amps.isNotEmpty()) {
                            val rawMid = amps.average().toFloat().let { if(it.isNaN()) 0f else it }
                            launch { midAvgAnim.animateTo(rawMid, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        }
                    }
                }
                launch {
                    visualizerManager.highAmplitudes.collect { amps ->
                        if (amps.isNotEmpty()) {
                            val rawTreble = amps.average().toFloat().let { if(it.isNaN()) 0f else it }
                            launch { trebleAvgAnim.animateTo(rawTreble, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        }
                    }
                }
            }
            
            // Removing direct Animatable.value reads at composition scope to prevent infinite recomposition loops.

            // Gradiente ultra saturado
            val sweepGradient = remember(colorDominant, colorVibrant, colorMuted) {
                Brush.sweepGradient(
                    colors = listOf(
                        colorDominant,
                        colorVibrant,
                        colorMuted,
                        colorVibrant.copy(alpha = 0.8f),
                        colorMuted,
                        colorVibrant,
                        colorDominant
                    )
                )
            }

            val basePath = remember { androidx.compose.ui.graphics.Path() }
            val wavePath = remember { androidx.compose.ui.graphics.Path() }
            val wavePathL = remember { androidx.compose.ui.graphics.Path() }
            val wavePathR = remember { androidx.compose.ui.graphics.Path() }
            val progressPath = remember { androidx.compose.ui.graphics.Path() }
            val progressMeasure = remember { androidx.compose.ui.graphics.PathMeasure() }
            val androidPathMeasure = remember { android.graphics.PathMeasure() }
            val slimeX = remember { FloatArray(150) }
            val slimeY = remember { FloatArray(150) }
            
            var lastSize = remember { androidx.compose.ui.geometry.Size.Zero }
            var lastShape = remember { -1 }
            var pathLength = remember { 0f }

                        Canvas(modifier = Modifier.size(320.dp).graphicsLayer {
                            scaleX = animatedScaleAnim.value
                            scaleY = animatedScaleAnim.value
                        }.pointerInput(abRepeatModeEnabled, thumbnailShapeIdx) {
                            if (!abRepeatModeEnabled) return@pointerInput
                            
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val rPx = 160.dp.toPx() / 2f
                            
                            val getProgressFromOffset = { offset: androidx.compose.ui.geometry.Offset ->
                                val pLen = progressMeasure.length
                                if (pLen <= 0f) {
                                    0f
                                } else {
                                    var minD = Float.MAX_VALUE
                                    var bestP = 0f
                                    val startD = if (thumbnailShapeIdx == 0) pLen * 0.75f else pLen * 0.125f
                                    
                                    for(i in 0..360) {
                                        val p = i / 360f
                                        val dMod = (startD + p * pLen) % pLen
                                        val pos = progressMeasure.getPosition(dMod)
                                        if (pos != androidx.compose.ui.geometry.Offset.Unspecified) {
                                            val dx = pos.x - offset.x
                                            val dy = pos.y - offset.y
                                            val dSq = dx*dx + dy*dy
                                            if (dSq < minD) {
                                                minD = dSq
                                                bestP = p
                                            }
                                        }
                                    }
                                    bestP
                                }
                            }
                            
                            val getPosFromProgress = { progress: Float ->
                                val pLen = progressMeasure.length
                                if (pLen <= 0f) {
                                    androidx.compose.ui.geometry.Offset(cx, cy)
                                } else {
                                    val startD = if (thumbnailShapeIdx == 0) pLen * 0.75f else pLen * 0.125f
                                    val dMod = (startD + progress * pLen) % pLen
                                    val pos = progressMeasure.getPosition(dMod)
                                    if (pos != androidx.compose.ui.geometry.Offset.Unspecified) pos else androidx.compose.ui.geometry.Offset(cx, cy)
                                }
                            }


                            var draggingHandle: String? = null
                            val hitRadius = 32.dp.toPx()
                            
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: continue
                                    
                                    if (change.pressed) {
                                        if (draggingHandle == null) {
                                            val posA = getPosFromProgress(abPointA)
                                            val posB = getPosFromProgress(abPointB)
                                            
                                            val distA = kotlin.math.hypot((change.position.x - posA.x).toDouble(), (change.position.y - posA.y).toDouble()).toFloat()
                                            val distB = kotlin.math.hypot((change.position.x - posB.x).toDouble(), (change.position.y - posB.y).toDouble()).toFloat()

                                            if (distA < hitRadius && distA <= distB) {
                                                draggingHandle = "A"
                                                activeDraggingHandle = "A"
                                                change.consume()
                                            } else if (distB < hitRadius) {
                                                draggingHandle = "B"
                                                activeDraggingHandle = "B"
                                                change.consume()
                                            }
                                        } else {
                                            val newProgress = getProgressFromOffset(change.position)
                                            if (draggingHandle == "A") {
                                                playerViewModel.abPointA.value = newProgress.coerceAtMost(abPointB - 0.01f).coerceAtLeast(0f)
                                            } else {
                                                playerViewModel.abPointB.value = newProgress.coerceAtLeast(abPointA + 0.01f).coerceAtMost(1f)
                                            }
                                            change.consume()
                                        }
                                    } else {
                                        draggingHandle = null
                                        activeDraggingHandle = null
                                    }
                                }
                            }
                        }) {
                val bassAmplitudes = bassAmplitudesState.value
                val midAmplitudes = midAmplitudesState.value
                val highAmplitudes = highAmplitudesState.value

                val bassAvg = bassAvgAnim.value
                val midAvg = midAvgAnim.value
                val trebleAvg = trebleAvgAnim.value
                val maxAnim = maxOf(bassAvg, midAvg, trebleAvg, 0.001f)
                val bassOpacity = (bassAvg / maxAnim).coerceIn(0.2f, 1.0f)
                val midOpacity = (midAvg / maxAnim).coerceIn(0.2f, 1.0f)
                val highOpacity = (trebleAvg / maxAnim).coerceIn(0.2f, 1.0f)

                val radius = size.minDimension / 4f
                val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

                val coverSize = 160.dp.toPx()
                val rPx = coverSize / 2f

                if (size != lastSize || thumbnailShapeIdx != lastShape) {
                    basePath.reset()
                    if (thumbnailShapeIdx == 0) {
                        basePath.addOval(androidx.compose.ui.geometry.Rect(center.x - rPx, center.y - rPx, center.x + rPx, center.y + rPx))
                    } else if (thumbnailShapeIdx == 1) {
                        basePath.addRect(androidx.compose.ui.geometry.Rect(center.x - rPx, center.y - rPx, center.x + rPx, center.y + rPx))
                    } else if (thumbnailShapeIdx == 2) {
                        basePath.addRoundRect(androidx.compose.ui.geometry.RoundRect(
                            left = center.x - rPx, top = center.y - rPx, right = center.x + rPx, bottom = center.y + rPx,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx(), 32.dp.toPx())
                        ))
                    } else if (thumbnailShapeIdx == 3) {
                        val squircleRadius = 64.dp.toPx()
                        basePath.addRoundRect(androidx.compose.ui.geometry.RoundRect(
                            left = center.x - rPx, top = center.y - rPx, right = center.x + rPx, bottom = center.y + rPx,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(squircleRadius, squircleRadius)
                        ))
                    }
                    basePath.close()
                    progressMeasure.setPath(basePath, forceClosed = false)
                    androidPathMeasure.setPath(basePath.asAndroidPath(), false)
                    lastSize = size
                    lastShape = thumbnailShapeIdx
                }

                val pathLength = if (thumbnailShapeIdx == 0) rPx * 2f * Math.PI.toFloat() else androidPathMeasure.length

                fun drawLayer(amps: FloatArray, layerColor: Color, opacity: Float, isBassLayer: Boolean) {
                    val numBars = amps.size
                    if (numBars == 0 || currentStyle == VisualizerStyle.RINGS || currentStyle == VisualizerStyle.AURA) return
                    val distStep = (pathLength / 2f) / (numBars - 1).coerceAtLeast(1).toFloat()
                    var outPx = 0f; var outPy = 0f; var outNx = 0f; var outNy = 0f
                    
                    fun computePointAndNormal(d: Float) {
                        if (thumbnailShapeIdx == 0) {
                            val angle = (d / pathLength) * 2 * Math.PI - Math.PI / 2
                            outNx = kotlin.math.cos(angle).toFloat()
                            outNy = kotlin.math.sin(angle).toFloat()
                            outPx = center.x + rPx * outNx
                            outPy = center.y + rPx * outNy
                        } else {
                            val dMod = d % pathLength
                            val localPosTan = FloatArray(2)
                            val localPosTanTan = FloatArray(2)
                            val success = androidPathMeasure.getPosTan(dMod, localPosTan, localPosTanTan)
                            if (success) {
                                outPx = localPosTan[0]
                                outPy = localPosTan[1]
                                outNx = -localPosTanTan[1]
                                outNy = localPosTanTan[0]
                                val len = kotlin.math.hypot(outNx, outNy)
                                if (len > 0) { outNx /= len; outNy /= len }
                            } else {
                                outPx = center.x; outPy = center.y; outNx = 0f; outNy = -1f
                            }
                        }
                    }

                    val colorVibrantLayer = layerColor.copy(alpha = opacity)

                    when (currentStyle) {
                        VisualizerStyle.BARS, VisualizerStyle.WAVE -> {
                            val isWave = currentStyle == VisualizerStyle.WAVE
                            if (isWave) {
                                wavePathR.reset()
                                wavePathL.reset()
                            }

                            for (i in 0 until numBars) {
                                val amplitude = amps[i]
                                val dist = 30f + (amplitude * 180f)
                                val barLength = 8f + (amplitude * 60f)

                                val dRight = 0f + i * distStep
                                val dLeft = pathLength - i * distStep

                                computePointAndNormal(dRight)
                                val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                                computePointAndNormal(dLeft)
                                val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy
                                
                                if (isWave) {
                                    val rX = pxR + nxR * dist; val rY = pyR + nyR * dist
                                    val lX = pxL + nxL * dist; val lY = pyL + nyL * dist
                                    if (i == 0) {
                                        wavePathR.moveTo(rX, rY); wavePathL.moveTo(lX, lY)
                                    } else {
                                        wavePathR.lineTo(rX, rY); wavePathL.lineTo(lX, lY)
                                    }
                                } else {
                                    drawLine(color = colorVibrantLayer, start = androidx.compose.ui.geometry.Offset(pxR + nxR * dist, pyR + nyR * dist),
                                             end = androidx.compose.ui.geometry.Offset(pxR + nxR * (dist + barLength), pyR + nyR * (dist + barLength)), strokeWidth = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    drawLine(color = colorVibrantLayer, start = androidx.compose.ui.geometry.Offset(pxL + nxL * dist, pyL + nyL * dist),
                                             end = androidx.compose.ui.geometry.Offset(pxL + nxL * (dist + barLength), pyL + nyL * (dist + barLength)), strokeWidth = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                }
                            }

                            if (isWave) {
                                drawPath(wavePathR, color = colorVibrantLayer, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                                drawPath(wavePathL, color = colorVibrantLayer, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                        }
                        VisualizerStyle.DOTS -> {
                            for (i in 0 until numBars) {
                                val amplitude = amps[i]
                                val dist = 30f + (amplitude * 180f)
                                val capLen = 8f + (amplitude * 30f)

                                val dRight = 0f + i * distStep
                                val dLeft = pathLength - i * distStep

                                computePointAndNormal(dRight)
                                val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                                computePointAndNormal(dLeft)
                                val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy

                                drawLine(color = colorVibrantLayer, start = androidx.compose.ui.geometry.Offset(pxR + nxR * dist, pyR + nyR * dist),
                                         end = androidx.compose.ui.geometry.Offset(pxR + nxR * (dist + capLen), pyR + nyR * (dist + capLen)), strokeWidth = 6f + amplitude*4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                drawLine(color = colorVibrantLayer, start = androidx.compose.ui.geometry.Offset(pxL + nxL * dist, pyL + nyL * dist),
                                         end = androidx.compose.ui.geometry.Offset(pxL + nxL * (dist + capLen), pyL + nyL * (dist + capLen)), strokeWidth = 6f + amplitude*4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)

                                drawCircle(color = colorVibrantLayer, radius = 3f + amplitude*2f, center = androidx.compose.ui.geometry.Offset(pxR + nxR * (dist + capLen), pyR + nyR * (dist + capLen)))
                                drawCircle(color = colorVibrantLayer, radius = 3f + amplitude*2f, center = androidx.compose.ui.geometry.Offset(pxL + nxL * (dist + capLen), pyL + nyL * (dist + capLen)))
                            }
                        }
                        VisualizerStyle.SLIME -> {
                            val totalPoints = numBars * 2
                            val slimeX = FloatArray(totalPoints)
                            val slimeY = FloatArray(totalPoints)
                            
                            for (i in 0 until totalPoints) {
                                val isRightSide = i < numBars
                                val ampIndex = if (isRightSide) i else (totalPoints - 1 - i)
                                val amplitude = amps[ampIndex]

                                val offsetDist = if (isRightSide) {
                                    0f + ampIndex * distStep
                                } else {
                                    pathLength - ampIndex * distStep
                                }

                                computePointAndNormal(offsetDist)
                                val extrude = 5f + (amplitude * 150f)
                                slimeX[i] = outPx + outNx * extrude
                                slimeY[i] = outPy + outNy * extrude
                            }

                            wavePathR.reset()
                            if (totalPoints > 0) {
                                var prevMidX = (slimeX[0] + slimeX[totalPoints - 1]) / 2f
                                var prevMidY = (slimeY[0] + slimeY[totalPoints - 1]) / 2f
                                wavePathR.moveTo(prevMidX, prevMidY)
                                for (i in 0 until totalPoints) {
                                    val nextIndex = (i + 1) % totalPoints
                                    val midX = (slimeX[i] + slimeX[nextIndex]) / 2f
                                    val midY = (slimeY[i] + slimeY[nextIndex]) / 2f
                                    wavePathR.quadraticTo(slimeX[i], slimeY[i], midX, midY)
                                }
                                wavePathR.close()

                                drawPath(
                                    path = wavePathR,
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(layerColor.copy(alpha = opacity), layerColor.copy(alpha = opacity * 0.5f)),
                                        center = center,
                                        radius = radius + 250f
                                    )
                                )
                                drawPath(path = wavePathR, color = layerColor.copy(alpha = opacity), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                        }
                        VisualizerStyle.PARTICLES -> {
                            for (i in 0 until numBars) {
                                val amplitude = amps[i]
                                val multiplicador = 1f + (i.toFloat() / numBars) * 1.5f
                                val boostedAmplitude = amplitude * multiplicador

                                val extrude = 20f + (boostedAmplitude * 300f)
                                val size = 2f + (boostedAmplitude * 15f)

                                val dRight = 0f + i * distStep
                                val dLeft = pathLength - i * distStep

                                computePointAndNormal(dRight)
                                drawCircle(color = colorVibrantLayer, radius = size, center = androidx.compose.ui.geometry.Offset(outPx + outNx * extrude, outPy + outNy * extrude))
                                
                                computePointAndNormal(dLeft)
                                drawCircle(color = colorVibrantLayer, radius = size, center = androidx.compose.ui.geometry.Offset(outPx + outNx * extrude, outPy + outNy * extrude))
                            }
                        }
                        else -> {}
                    }
                }

                if (currentStyle == VisualizerStyle.RINGS || currentStyle == VisualizerStyle.AURA) {
                    when (currentStyle) {
                        VisualizerStyle.RINGS -> {
                            val dynamicRotation = rotationAngle + (bassAvg * 90f)
                            val dynamicFastRotation = fastRotationAngle - (midAvg * 90f)

                            fun drawGlitchRing(r: Float, thickness: Float, gapAngle: Float, startOffset: Float, brushColor: Color) {
                                val sweep = 360f / 4f - gapAngle
                                for (i in 0 until 4) {
                                    drawArc(
                                        color = brushColor, startAngle = startOffset + (i * 90f), sweepAngle = sweep,
                                        useCenter = false, topLeft = androidx.compose.ui.geometry.Offset(center.x - r, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = thickness, cap = androidx.compose.ui.graphics.StrokeCap.Square)
                                    )
                                }
                            }

                            if (visualizerArchetype == 1) {
                                val maxPulse = maxOf(bassAvg, midAvg, trebleAvg)
                                val combinedRadius = radius + 60f + (maxPulse * 80f)
                                drawGlitchRing(combinedRadius, 8f + (maxPulse * 15f), 20f, dynamicRotation, colorVibrant.copy(alpha = 0.8f))
                            } else {
                                val bassRadius = radius + 30f + (bassAvg * 80f)
                                val midRadius = radius + 60f + (midAvg * 70f)
                                val trebleRadius = radius + 90f + (trebleAvg * 60f)

                                drawGlitchRing(bassRadius, 8f + (bassAvg * 15f), 20f - (bassAvg * 10f), dynamicRotation, colorDominant.copy(alpha = 0.8f))
                                drawGlitchRing(midRadius, 4f + (midAvg * 10f), 30f, dynamicFastRotation, colorVibrant.copy(alpha = 0.6f))
                                drawGlitchRing(trebleRadius, 2f + (trebleAvg * 5f), 45f, dynamicRotation * 0.5f, colorMuted.copy(alpha = 0.5f))
                            }
                        }
                        VisualizerStyle.AURA -> {
                            if (visualizerArchetype == 1) {
                                val maxPulse = maxOf(bassAvg, midAvg, trebleAvg)
                                drawPath(
                                    path = basePath,
                                    color = colorVibrant.copy(alpha = 0.2f + 0.2f * maxPulse.coerceIn(0f, 1f)),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 60f + maxPulse * 150f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                )
                            } else {
                                val bassPulse = (bassAvg * 1.5f).coerceIn(0f, 1f)
                                val midPulse = (midAvg * 1.5f).coerceIn(0f, 1f)
                                val treblePulse = (trebleAvg * 1.5f).coerceIn(0f, 1f)

                                drawPath(
                                    path = basePath,
                                    color = colorDominant.copy(alpha = 0.1f + 0.1f * bassPulse),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 80f + bassAvg * 200f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                )
                                drawPath(
                                    path = basePath,
                                    color = colorVibrant.copy(alpha = 0.15f + 0.15f * midPulse),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 40f + midAvg * 100f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                )
                                drawPath(
                                    path = basePath,
                                    color = colorMuted.copy(alpha = 0.25f + 0.25f * treblePulse),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 15f + trebleAvg * 50f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                )
                            }
                        }
                        else -> {}
                    }
                } else {
                    if (visualizerArchetype == 1) {
                        // 1 Onda Combinada Segmentada
                        drawLayer(combinedAmplitudesState.value, paletteColors.vibrant, maxOf(bassOpacity, midOpacity, highOpacity), true)
                    } else {
                        // 3 Ondas Superpuestas
                        drawLayer(bassAmplitudes, paletteColors.dominant, bassOpacity, true)
                        drawLayer(midAmplitudes, paletteColors.vibrant, midOpacity, false)
                        drawLayer(highAmplitudes, paletteColors.muted, highOpacity, false)
                    }
                }

                val activePosition = dragSeekTimeMs ?: currentPosition
                val progressFraction = if (duration > 0) activePosition.toFloat() / duration else 0f
                drawPath(path = basePath, color = colorDominant.copy(alpha = 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

                progressMeasure.setPath(basePath, forceClosed = false)
                val pLen = progressMeasure.length
                progressPath.reset()
                val targetLength = pLen * progressFraction

                if (targetLength > 0f) {
                    val startD = if (thumbnailShapeIdx == 0) pLen * 0.75f else pLen * 0.125f
                    val endD = startD + targetLength
                    if (endD <= pLen) {
                        progressMeasure.getSegment(startD, endD, progressPath, true)
                    } else {
                        progressMeasure.getSegment(startD, pLen, progressPath, true)
                        progressMeasure.getSegment(0f, endD % pLen, progressPath, true)
                    }
                    drawPath(path = progressPath, brush = sweepGradient, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

                    val thumbDist = endD % pLen
                    var thumbPos = progressMeasure.getPosition(thumbDist)
                    if (thumbnailShapeIdx != 0 && (thumbPos == androidx.compose.ui.geometry.Offset.Unspecified || thumbPos == androidx.compose.ui.geometry.Offset.Zero)) {
                        val localPosTan = FloatArray(2)
                        if (androidPathMeasure.getPosTan(thumbDist, localPosTan, null)) {
                            thumbPos = androidx.compose.ui.geometry.Offset(localPosTan[0], localPosTan[1])
                        }
                    }
                    if (thumbPos != androidx.compose.ui.geometry.Offset.Unspecified && thumbPos != androidx.compose.ui.geometry.Offset.Zero) {
                        playheadPos = thumbPos
                        drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 8f, center = thumbPos)
                    }
                }
                
                if (abRepeatModeEnabled) {
                    val getPosFromProgress = { progress: Float ->
                        val pLen = progressMeasure.length
                        if (pLen <= 0f) {
                            center
                        } else {
                            val startD = if (thumbnailShapeIdx == 0) pLen * 0.75f else pLen * 0.125f
                            val dMod = (startD + progress * pLen) % pLen
                            val pos = progressMeasure.getPosition(dMod)
                            if (pos != androidx.compose.ui.geometry.Offset.Unspecified) pos else center
                        }
                    }
                    val posA = getPosFromProgress(abPointA)
                    val posB = getPosFromProgress(abPointB)
                    val markerSizeA = if (activeDraggingHandle == "A") 16.dp.toPx() else 8.dp.toPx()
                    val markerSizeB = if (activeDraggingHandle == "B") 16.dp.toPx() else 8.dp.toPx()
                    
                    if (thumbnailShapeIdx == 0) {
                        drawCircle(color = colorVibrant, radius = markerSizeA, center = posA)
                        drawCircle(color = colorVibrant.copy(alpha=0.3f), radius = markerSizeA * 2, center = posA)
                        
                        drawCircle(color = colorMuted, radius = markerSizeB, center = posB)
                        drawCircle(color = colorMuted.copy(alpha=0.3f), radius = markerSizeB * 2, center = posB)
                    } else {
                        val cornerRadius = if (thumbnailShapeIdx == 2) 4.dp.toPx() else if (thumbnailShapeIdx == 3) 8.dp.toPx() else 0f
                        
                        drawRoundRect(color = colorVibrant, topLeft = androidx.compose.ui.geometry.Offset(posA.x - markerSizeA, posA.y - markerSizeA), size = androidx.compose.ui.geometry.Size(markerSizeA*2, markerSizeA*2), cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius))
                        drawRoundRect(color = colorVibrant.copy(alpha=0.3f), topLeft = androidx.compose.ui.geometry.Offset(posA.x - markerSizeA*2, posA.y - markerSizeA*2), size = androidx.compose.ui.geometry.Size(markerSizeA*4, markerSizeA*4), cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius*2, cornerRadius*2))
                        
                        drawRoundRect(color = colorMuted, topLeft = androidx.compose.ui.geometry.Offset(posB.x - markerSizeB, posB.y - markerSizeB), size = androidx.compose.ui.geometry.Size(markerSizeB*2, markerSizeB*2), cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius))
                        drawRoundRect(color = colorMuted.copy(alpha=0.3f), topLeft = androidx.compose.ui.geometry.Offset(posB.x - markerSizeB*2, posB.y - markerSizeB*2), size = androidx.compose.ui.geometry.Size(markerSizeB*4, markerSizeB*4), cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius*2, cornerRadius*2))
                    }
                }
            }

            // Central Album Art
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = animatedScaleAnim.value
                        scaleY = animatedScaleAnim.value
                        if (thumbnailShapeIdx == 0) rotationZ = coverRotationAnim.value
                    }
                    .clip(shape)
                    .background(colorDominant.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                exoPlayer?.let { player ->
                                    if (player.isPlaying) {
                                        player.pause()
                                    } else {
                                        player.play()
                                    }
                                }
                            },
                            onDoubleTap = { offset ->
                                exoPlayer?.let { player ->
                                    if (offset.x < size.width / 2) {
                                        player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                                    } else {
                                        player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration))
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(targetState = albumArtBitmap, label = "album_art") { bmp ->
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Album Art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isPlayingState,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                            contentDescription = "Paused",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
        // Lyrics status block
        if (lyrics.isEmpty() && autoAnalyzeLyrics) {
            if (isFetchingLyrics) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 110.dp)
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White.copy(alpha = 0.8f),
                        strokeWidth = 2.dp
                    )
                }
            } else if (availableLyricsResults.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 110.dp)
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Color(0xFF333333))
                        .clickable { showLyricsMatches = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Filled.List,
                        contentDescription = "Buscar Letras",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (searchFailed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 110.dp)
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = "No hay letras",
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }


            }
        }

        // Lyrics Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = showLyrics,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.fillMaxSize().padding(top = 100.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f))) {
                val listState = rememberLazyListState()
                val activeLyricIndex = lyrics.indexOfLast { it.timeMs <= currentPosition }.coerceAtLeast(0)
                
                LaunchedEffect(activeLyricIndex) {
                    if (activeLyricIndex >= 0 && lyrics.isNotEmpty()) {
                        listState.animateScrollToItem(activeLyricIndex, scrollOffset = -200)
                    }
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(lyrics) { index, line ->
                        val isActive = index == activeLyricIndex
                        val alpha by androidx.compose.animation.core.animateFloatAsState(if (isActive) 1f else 0.4f)
                        val scale by androidx.compose.animation.core.animateFloatAsState(if (isActive) 1.1f else 1f)
                        val color = if (isActive) colorVibrant else Color.White
                        
                        Text(
                            text = line.text,
                            color = color.copy(alpha = alpha),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clickable {
                                    exoPlayer?.seekTo(line.timeMs)
                                }
                        )
                    }
                }
            }
        }

        
        // Mode Notification Overlay
        AnimatedVisibility(
            visible = currentStyleName != null,
            enter = fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.8f),
            exit = fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 100.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(colorVibrant.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Modo: $currentStyleName",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            LaunchedEffect(currentStyleName) {
                if (currentStyleName != null) {
                    delay(1200)
                    currentStyleName = null
                }
            }
        }

        
        // Control Row

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expandable Timer
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showTimerDialog = true }) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Timer, 
                        contentDescription = "Timer", 
                        tint = if (sleepTimerSeconds > 0) colorVibrant else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = { showEqDialog = true }) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.GraphicEq, 
                        contentDescription = "Equalizer", 
                        tint = if (equalizerManager.isEnabled.collectAsState().value) colorVibrant else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
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

    if (showTimerDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text(stringResource(R.string.sleep_timer), color = colorVibrant) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(200.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val x = change.position.x - size.width / 2
                                        val y = change.position.y - size.height / 2
                                        var newAngle = Math.toDegrees(kotlin.math.atan2(y.toDouble(), x.toDouble())).toFloat()
                                        newAngle = (newAngle + 90f) % 360f
                                        if (newAngle < 0) newAngle += 360f
                                        val newMinutes = ((newAngle / 360f) * 120f).toInt()
                                        onSetSleepTimer(newMinutes * 60)
                                    }
                                }
                        ) {
                            val strokeWidth = 20f
                            drawArc(
                                color = Color.DarkGray,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            val currentAngle = ((sleepTimerSeconds / 60).toFloat() / 120f) * 360f
                            drawArc(
                                color = colorVibrant,
                                startAngle = -90f,
                                sweepAngle = currentAngle,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                        Text("${sleepTimerSeconds / 60}m", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimerDialog = false }) { Text(stringResource(R.string.accept), color = colorVibrant) }
            },
            containerColor = colorDominant.copy(alpha = 0.95f)
        )
    }

    if (showEqDialog) {
        val isEqEnabled by equalizerManager.isEnabled.collectAsState()
        val isAutoMode by equalizerManager.isAutoMode.collectAsState()
        val presets by equalizerManager.presets.collectAsState()
        val currentPreset by equalizerManager.currentPreset.collectAsState()
        val bands by equalizerManager.bands.collectAsState()
        val bandLevels by equalizerManager.bandLevels.collectAsState()
        val minLevel by equalizerManager.minLevel.collectAsState()
        val maxLevel by equalizerManager.maxLevel.collectAsState()

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEqDialog = false },
            title = { Text(stringResource(R.string.equalizer), color = colorVibrant, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.enable_equalizer), color = Color.White)
                        androidx.compose.material3.Switch(
                            checked = isEqEnabled,
                            onCheckedChange = { equalizerManager.setEnabled(it) },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorDominant)
                        )
                    }
                    
                    if (isEqEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.auto_mode_loudness), color = Color.White)
                            androidx.compose.material3.Switch(
                                checked = isAutoMode,
                                onCheckedChange = { equalizerManager.setAutoMode(it) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorDominant)
                            )
                        }

                        if (!isAutoMode) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.preset_label), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val currentName = if (currentPreset.toInt() == -1) "Personalizado" else presets.find { it.first == currentPreset }?.second ?: "Normal"
                                    Text(currentName, color = Color.White)
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(Color.DarkGray)
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.custom), color = Color.White) },
                                        onClick = { expanded = false }
                                    )
                                    presets.forEach { preset ->
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(preset.second, color = Color.White) },
                                            onClick = { 
                                                equalizerManager.setPreset(preset.first)
                                                expanded = false 
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            val range = (maxLevel - minLevel).coerceAtLeast(1)
                            Row(modifier = Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                bands.forEach { band ->
                                    val level = bandLevels[band] ?: 0.toShort()
                                    val freqHz = equalizerManager.getCenterFreq(band) / 1000
                                    val freqStr = if (freqHz >= 1000) "${freqHz / 1000}k" else "$freqHz"
                                    
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        androidx.compose.material3.Slider(
                                            value = level.toFloat(),
                                            onValueChange = { equalizerManager.setBandLevel(band, it.toInt().toShort()) },
                                            valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                                            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorVibrant),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(freqStr, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showEqDialog = false }) { Text(stringResource(R.string.close), color = colorVibrant) } },
            containerColor = colorDominant.copy(alpha = 0.95f)
        )
    }

    if (showEditorDialog && currentTrack != null) {
        var editTitle by remember { mutableStateOf(currentTrack.customTitle ?: currentTrack.title) }
        var editArtist by remember { mutableStateOf(currentTrack.customArtist ?: currentTrack.artist) }
        var editAlbum by remember { mutableStateOf(currentTrack.customAlbum ?: currentTrack.album) }
        var editCoverPath by remember { mutableStateOf(currentTrack.customCoverPath) }
        
        val context = androidx.compose.ui.platform.LocalContext.current
        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                // Copy the selected image to internal storage to avoid losing it
                // if the user deletes the original or the URI permission expires.
                try {
                    val coversDir = java.io.File(context.filesDir, "custom_covers")
                    if (!coversDir.exists()) coversDir.mkdirs()
                    val destFile = java.io.File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(it)?.use { input ->
                        java.io.FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    editCoverPath = destFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback to original URI if copy fails
                    try {
                        context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (e2: Exception) {}
                    editCoverPath = it.toString()
                }
            }
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEditorDialog = false },
            title = { Text(stringResource(R.string.edit_tag), color = colorVibrant) },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text(stringResource(R.string.title), color = Color.Gray) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = editArtist,
                        onValueChange = { editArtist = it },
                        label = { Text(stringResource(R.string.artist), color = Color.Gray) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = editAlbum,
                        onValueChange = { editAlbum = it },
                        label = { Text(stringResource(R.string.album), color = Color.Gray) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Button(
                        onClick = { launcher.launch(arrayOf("image/*")) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colorVibrant)
                    ) {
                        Text(stringResource(R.string.choose_cover))
                    }
                    if (editCoverPath != null) {
                        Text(stringResource(R.string.custom_cover_selected), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    onUpdateTrackMetadata(currentTrack.id, editTitle, editArtist, editAlbum, editCoverPath)
                    showEditorDialog = false 
                }) { Text(stringResource(R.string.save), color = colorVibrant) }
            },
            dismissButton = {
                TextButton(onClick = { showEditorDialog = false }) { Text(stringResource(R.string.cancel), color = Color.Gray) }
            },
            containerColor = colorDominant.copy(alpha = 0.95f)
        )
    }

    val styleNames = mapOf(
        VisualizerStyle.WAVE to "Onda",
        VisualizerStyle.SLIME to "Slime",
        VisualizerStyle.BARS to "Barras",
        VisualizerStyle.DOTS to "Puntos",
        VisualizerStyle.PARTICLES to "Partículas",
        VisualizerStyle.RINGS to "Anillos",
        VisualizerStyle.AURA to "Aura"
    )

    if (showSettingsMenu) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showSettingsMenu = false },
            containerColor = colorDominant.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.playback_settings), color = colorVibrant, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.7f)) {
                        var isShuffleEnabled by remember { androidx.compose.runtime.mutableStateOf(exoPlayer?.shuffleModeEnabled == true) }
                        androidx.compose.material3.Switch(
                            checked = isShuffleEnabled,
                            onCheckedChange = {
                                isShuffleEnabled = it
                                exoPlayer?.shuffleModeEnabled = it 
                            },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.shuffle_playback), color = Color.White)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.weight(0.3f)) {
                        androidx.compose.material3.IconToggleButton(
                            checked = autoAnalyzeLyrics,
                            onCheckedChange = { playerViewModel.toggleAutoAnalyze() }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "Auto-Analyze Lyrics",
                                tint = if (autoAnalyzeLyrics) colorVibrant else Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.visualizer_style_opt), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val styles = VisualizerStyle.values()
                    items(styles.size) { i ->
                        val style = styles[i]
                        val isSelected = currentStyle == style
                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = { 
                                currentStyle = style
                                currentStyleName = styleNames[style]
                            },
                            label = { Text(style.name) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(selectedContainerColor = colorVibrant, selectedLabelColor = Color.White)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.repeat_method), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val currentMode = exoPlayer?.repeatMode ?: androidx.media3.common.Player.REPEAT_MODE_OFF
                    TextButton(onClick = { 
                        exoPlayer?.repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
                        playerViewModel.abRepeatModeEnabled.value = false
                    }) {
                        Text(stringResource(R.string.off), color = if (!abRepeatModeEnabled && currentMode == androidx.media3.common.Player.REPEAT_MODE_OFF) colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = { 
                        exoPlayer?.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                        playerViewModel.abRepeatModeEnabled.value = false
                    }) {
                        Text(stringResource(R.string.repeat_list), color = if (!abRepeatModeEnabled && currentMode == androidx.media3.common.Player.REPEAT_MODE_ALL) colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = { 
                        exoPlayer?.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                        playerViewModel.abRepeatModeEnabled.value = false
                    }) {
                        Text(stringResource(R.string.repeat_one), color = if (!abRepeatModeEnabled && currentMode == androidx.media3.common.Player.REPEAT_MODE_ONE) colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = { 
                        exoPlayer?.repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
                        playerViewModel.abRepeatModeEnabled.value = true
                        playerViewModel.abPointA.value = 0f
                        playerViewModel.abPointB.value = 0.5f
                        showSettingsMenu = false
                    }) {
                        Text(stringResource(R.string.repeat_ab), color = if (abRepeatModeEnabled) colorVibrant else Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.wave_archetype), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = {
                        visualizerManager.visualizerArchetype.value = 0
                        prefs.visualizerArchetype = 0
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Waves, contentDescription = null, tint = if (visualizerArchetype == 0) colorVibrant else Color.Gray)
                            Text(stringResource(R.string.waves_3_overlapping), color = if (visualizerArchetype == 0) colorVibrant else Color.Gray, fontSize = 12.sp)
                        }
                    }
                    TextButton(onClick = {
                        visualizerManager.visualizerArchetype.value = 1
                        prefs.visualizerArchetype = 1
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = if (visualizerArchetype == 1) colorVibrant else Color.Gray)
                            Text(stringResource(R.string.wave_1_combined), color = if (visualizerArchetype == 1) colorVibrant else Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.calculation_mode_fft), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = {
                        visualizerManager.fftMode.value = "AVERAGE"
                        prefs.visualizerFftMode = "AVERAGE"
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Equalizer, contentDescription = null, tint = if (fftMode == "AVERAGE") colorVibrant else Color.Gray)
                            Text(stringResource(R.string.fft_average), color = if (fftMode == "AVERAGE") colorVibrant else Color.Gray, fontSize = 12.sp)
                        }
                    }
                    TextButton(onClick = {
                        visualizerManager.fftMode.value = "MAX"
                        prefs.visualizerFftMode = "MAX"
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, tint = if (fftMode == "MAX") colorVibrant else Color.Gray)
                            Text(stringResource(R.string.fft_maximum), color = if (fftMode == "MAX") colorVibrant else Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.chaos_reactivity_format, String.format("%.2f", reactivity)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = reactivity,
                    onValueChange = {
                        visualizerManager.reactivity.value = it
                        prefs.reactivity = it
                    },
                    valueRange = 0.1f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                )
                Spacer(modifier = Modifier.height(16.dp))

                var isAdvancedMode by remember { androidx.compose.runtime.mutableStateOf(prefs.isAdvancedMode) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { 
                    isAdvancedMode = !isAdvancedMode
                    prefs.isAdvancedMode = isAdvancedMode
                    visualizerManager.isAdvancedMode.value = isAdvancedMode
                }) {
                    Text(if (isAdvancedMode) "Sensibilidad Avanzada" else "Sensibilidad General", color = Color.Gray, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(
                        checked = isAdvancedMode,
                        onCheckedChange = { 
                            isAdvancedMode = it
                            prefs.isAdvancedMode = it
                            visualizerManager.isAdvancedMode.value = it
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                    )
                }
                
                if (isAdvancedMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.size(12.dp).background(colorDominant, androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.bass_sensitivity_format, String.format("%.1f", bassMult)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = bassMult,
                        onValueChange = {
                            visualizerManager.bassMultiplier.value = it
                            prefs.bassMultiplier = it
                        },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = colorDominant, activeTrackColor = colorDominant)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.size(12.dp).background(colorVibrant, androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.mid_sensitivity_format, String.format("%.1f", midMult)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = midMult,
                        onValueChange = {
                            visualizerManager.midMultiplier.value = it
                            prefs.midMultiplier = it
                        },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorVibrant)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.size(12.dp).background(colorMuted, androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.treble_sensitivity_format, String.format("%.1f", trebleMult)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = trebleMult,
                        onValueChange = {
                            visualizerManager.trebleMultiplier.value = it
                            prefs.trebleMultiplier = it
                        },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = colorMuted, activeTrackColor = colorMuted)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.general_sensitivity_format, String.format("%.1f", sensitivity)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = sensitivity,
                        onValueChange = {
                            visualizerManager.sensitivity.value = it
                            prefs.sensitivity = it
                            // Keep advanced sync
                            visualizerManager.bassMultiplier.value = it
                            prefs.bassMultiplier = it
                            visualizerManager.midMultiplier.value = it
                            prefs.midMultiplier = it
                            visualizerManager.trebleMultiplier.value = it
                            prefs.trebleMultiplier = it
                        },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showQueue) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            containerColor = colorDominant.copy(alpha = 0.95f)
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(currentTrack) {
                val index = currentQueue.indexOfFirst { it.id == currentTrack?.id }
                if (index != -1) {
                    listState.animateScrollToItem(index)
                }
            }
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(stringResource(R.string.playback_queue), color = colorVibrant, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(state = listState) {

                    items(currentQueue, key = { it.id }) { track ->
                        val isCurrent = track.id == currentTrack?.id
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onPlayTrack(track, currentQueue)
                                showQueue = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCurrent) Icons.Default.PlayArrow else Icons.Default.List,
                                contentDescription = null,
                                tint = if (isCurrent) colorVibrant else Color.LightGray
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(track.title, color = if (isCurrent) colorVibrant else Color.White, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                                Text(track.artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showEffectsDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEffectsDialog = false },
            title = { Text(stringResource(R.string.audio_effects), color = colorVibrant) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.slow_reverb), color = Color.White)
                        androidx.compose.material3.Switch(
                            checked = reverbEnabled,
                            onCheckedChange = { onSetReverb(it) },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                        )
                    }

                    Column {
                        Text(stringResource(R.string.speed_format, String.format("%.2f", playbackSpeed)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = playbackSpeed,
                            onValueChange = { onSetSpeed(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                        )
                    }

                    Column {
                        Text(stringResource(R.string.pitch_format, String.format("%.2f", playbackPitch)), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = playbackPitch,
                            onValueChange = { onSetPitch(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                        )
                    }

                    Column {
                        Text(stringResource(R.string.preset_effects), color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val presets = listOf(
                                "NORMAL" to "Normal",
                                "NIGHTCORE" to "Nightcore",
                                "SLOWED" to "Slowed + Reverb",
                                "CHIPMUNK" to "Chipmunk",
                                "BASS" to "Bass Boost"
                            )
                            items(presets.size) { i ->
                                val (key, label) = presets[i]
                                androidx.compose.material3.FilterChip(
                                    selected = effectsPreset == key,
                                    onClick = { onApplyPreset(key) },
                                    label = { Text(label) },
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(selectedContainerColor = colorVibrant, selectedLabelColor = Color.White)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEffectsDialog = false }) {
                    Text(stringResource(R.string.close), color = colorVibrant)
                }
            },
            containerColor = colorDominant
        )
    }
}
