package com.example.beatpulse.ui.components.player

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
    visualizerManager: AudioVisualizerManager,
    equalizerManager: com.example.beatpulse.service.EqualizerManager,
    exoPlayer: androidx.media3.common.Player?,
    currentTrack: TrackEntity? = null,
    currentQueue: List<TrackEntity>,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    modifier: Modifier = Modifier,
    prefs: com.example.beatpulse.data.PreferencesManager,
    sleepTimerSeconds: Int = 0,
    onSetSleepTimer: (Int) -> Unit = {},
    onUpdateTrackMetadata: (Long, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onAddToPlaylist: ((TrackEntity) -> Unit)? = null
) {
    val albumArtBitmap = currentTrack?.let { com.example.beatpulse.ui.components.rememberFullAlbumArt(it) }
    val amplitudesState = visualizerManager.amplitudes.collectAsState()
    val ghostsState = visualizerManager.ghostsFlow.collectAsState()
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
            title = { Text("Sugerir mejoras", color = colorVibrant) },
            text = { Text("¿Deseas abrir el navegador para sugerir mejoras o reportar problemas en GitHub?", color = dynamicTextColor) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/polonio/duwave/issues"))
                    context.startActivity(intent)
                    showFeedbackDialog = false
                }) {
                    Text("Abrir", color = colorVibrant)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Cancelar", color = dynamicTextColor.copy(alpha=0.7f))
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
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorVibrant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                .pointerInput(Unit) {
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
                .pointerInput(Unit) {
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

            LaunchedEffect(visualizerManager.amplitudes) {
                visualizerManager.amplitudes.collect { amps ->
                    if (amps.isNotEmpty()) {
                        val rawBass = amps.take(amps.size / 3).average().toFloat().let { if(it.isNaN()) 0f else it }
                        val rawMid = amps.drop(amps.size / 3).take(amps.size / 3).average().toFloat().let { if(it.isNaN()) 0f else it }
                        val rawTreble = amps.takeLast(amps.size / 3).average().toFloat().let { if(it.isNaN()) 0f else it }
                        
                        launch { bassAvgAnim.animateTo(rawBass, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        launch { midAvgAnim.animateTo(rawMid, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        launch { trebleAvgAnim.animateTo(rawTreble, androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        
                        val bassIntensity = rawBass.coerceIn(0f, 1f)
                        launch { animatedScaleAnim.animateTo(1f + (bassIntensity * 0.45f), androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)) }
                    }
                }
            }
            
            val bassAvg = bassAvgAnim.value
            val midAvg = midAvgAnim.value
            val trebleAvg = trebleAvgAnim.value
            val animatedScale = animatedScaleAnim.value

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
            val progressPath = remember { androidx.compose.ui.graphics.Path() }
            val progressMeasure = remember { androidx.compose.ui.graphics.PathMeasure() }
            val androidPathMeasure = remember { android.graphics.PathMeasure() }
            val slimeX = remember { FloatArray(150) }
            val slimeY = remember { FloatArray(150) }
            
            var lastSize = remember { androidx.compose.ui.geometry.Size.Zero }
            var lastShape = remember { -1 }
            var pathLength = remember { 0f }

            Canvas(modifier = Modifier.size(320.dp).scale(animatedScale)) {
                val amplitudes = amplitudesState.value
                val ghosts = ghostsState.value
                val radius = size.minDimension / 4f
                val center = Offset(size.width / 2, size.height / 2)

                
                // --- Dynamic Path for Shape ---
                val coverSize = 160.dp.toPx()
                val rPx = coverSize / 2f
                val m = when (thumbnailShapeIdx) {
                    0 -> 2.0 // Circle
                    1 -> 16.0 // Square
                    2 -> 4.0 // Squircle 16dp
                    3 -> 3.0 // Squircle 32dp
                    else -> 2.0
                }

                if (size != lastSize || thumbnailShapeIdx != lastShape) {
                    lastSize = size
                    lastShape = thumbnailShapeIdx
                    basePath.reset()
                    val rect = androidx.compose.ui.geometry.Rect(center.x - rPx, center.y - rPx, center.x + rPx, center.y + rPx)
                    when (thumbnailShapeIdx) {
                        0 -> basePath.addOval(rect)
                        1 -> basePath.addRect(rect)
                        2 -> basePath.addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())))
                        3 -> basePath.addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, androidx.compose.ui.geometry.CornerRadius(32.dp.toPx())))
                        else -> basePath.addOval(rect)
                    }
                    
                    androidPathMeasure.setPath(basePath.asAndroidPath(), false)
                    pathLength = androidPathMeasure.length
                }


                // Out variables for zero-allocation math
                var outPx = 0f
                var outPy = 0f
                var outNx = 0f
                var outNy = 0f
                
                fun computePointAndNormal(dist: Float) {
                    val d = (dist % pathLength + pathLength) % pathLength
                    
                    if (thumbnailShapeIdx == 0) { // Circle
                        val angle = (d / pathLength) * Math.PI * 2.0 - Math.PI / 2.0
                        outPx = center.x + rPx * kotlin.math.cos(angle).toFloat()
                        outPy = center.y + rPx * kotlin.math.sin(angle).toFloat()
                        outNx = kotlin.math.cos(angle).toFloat()
                        outNy = kotlin.math.sin(angle).toFloat()
                        return
                    }
                    
                    if (thumbnailShapeIdx == 1) { // Square
                        var x = 0f
                        var y = 0f
                        var nx = 0f
                        var ny = 0f
                        if (d < rPx) { 
                            x = d; y = -rPx; nx = 0f; ny = -1f
                        } else if (d < 3f * rPx) { 
                            x = rPx; y = d - 2f * rPx; nx = 1f; ny = 0f
                        } else if (d < 5f * rPx) { 
                            x = 4f * rPx - d; y = rPx; nx = 0f; ny = 1f
                        } else if (d < 7f * rPx) { 
                            x = -rPx; y = 6f * rPx - d; nx = -1f; ny = 0f
                        } else { 
                            x = d - 8f * rPx; y = -rPx; nx = 0f; ny = -1f
                        }
                        outPx = center.x + x
                        outPy = center.y + y
                        outNx = nx
                        outNy = ny
                        return
                    }
                    
                    // Squircle (rounded rect)
                    val cornerRadius = if (thumbnailShapeIdx == 2) 16.dp.toPx() else 32.dp.toPx()
                    val straightEdge = 2f * (rPx - cornerRadius)
                    val cornerLen = (Math.PI.toFloat() * cornerRadius) / 2f
                    
                    var x = 0f; var y = 0f; var nx = 0f; var ny = 0f
                    var remaining = d
                    
                    val halfTop = straightEdge / 2f
                    if (remaining <= halfTop) {
                        x = remaining; y = -rPx; nx = 0f; ny = -1f
                    } else {
                        remaining -= halfTop
                        if (remaining <= cornerLen) {
                            val angle = -Math.PI / 2.0 + (remaining / cornerLen) * (Math.PI / 2.0)
                            x = rPx - cornerRadius + cornerRadius * kotlin.math.cos(angle).toFloat()
                            y = -rPx + cornerRadius + cornerRadius * kotlin.math.sin(angle).toFloat()
                            nx = kotlin.math.cos(angle).toFloat(); ny = kotlin.math.sin(angle).toFloat()
                        } else {
                            remaining -= cornerLen
                            if (remaining <= straightEdge) {
                                x = rPx; y = -rPx + cornerRadius + remaining; nx = 1f; ny = 0f
                            } else {
                                remaining -= straightEdge
                                if (remaining <= cornerLen) {
                                    val angle = 0.0 + (remaining / cornerLen) * (Math.PI / 2.0)
                                    x = rPx - cornerRadius + cornerRadius * kotlin.math.cos(angle).toFloat()
                                    y = rPx - cornerRadius + cornerRadius * kotlin.math.sin(angle).toFloat()
                                    nx = kotlin.math.cos(angle).toFloat(); ny = kotlin.math.sin(angle).toFloat()
                                } else {
                                    remaining -= cornerLen
                                    if (remaining <= straightEdge) {
                                        x = rPx - cornerRadius - remaining; y = rPx; nx = 0f; ny = 1f
                                    } else {
                                        remaining -= cornerLen
                                        if (remaining <= cornerLen) {
                                            val angle = Math.PI / 2.0 + (remaining / cornerLen) * (Math.PI / 2.0)
                                            x = -rPx + cornerRadius + cornerRadius * kotlin.math.cos(angle).toFloat()
                                            y = rPx - cornerRadius + cornerRadius * kotlin.math.sin(angle).toFloat()
                                            nx = kotlin.math.cos(angle).toFloat(); ny = kotlin.math.sin(angle).toFloat()
                                        } else {
                                            remaining -= cornerLen
                                            if (remaining <= straightEdge) {
                                                x = -rPx; y = rPx - cornerRadius - remaining; nx = -1f; ny = 0f
                                            } else {
                                                remaining -= straightEdge
                                                if (remaining <= cornerLen) {
                                                    val angle = Math.PI + (remaining / cornerLen) * (Math.PI / 2.0)
                                                    x = -rPx + cornerRadius + cornerRadius * kotlin.math.cos(angle).toFloat()
                                                    y = -rPx + cornerRadius + cornerRadius * kotlin.math.sin(angle).toFloat()
                                                    nx = kotlin.math.cos(angle).toFloat(); ny = kotlin.math.sin(angle).toFloat()
                                                } else {
                                                    remaining -= cornerLen
                                                    x = -rPx + cornerRadius + remaining; y = -rPx; nx = 0f; ny = -1f
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    outPx = center.x + x
                    outPy = center.y + y
                    outNx = nx
                    outNy = ny
                }

                val numBars = amplitudes.size
                if (numBars > 0) {
                    val distStep = (pathLength / 2f) / (numBars - 1).coerceAtLeast(1).toFloat()
                    
                    when (currentStyle) {
                        VisualizerStyle.SLIME -> {
                            val totalPoints = numBars * 2
                            for (i in 0 until totalPoints) {
                                val isRightSide = i < numBars
                                val ampIndex = if (isRightSide) i else (totalPoints - 1 - i)
                                val amplitude = amplitudes[ampIndex]
                                
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

                            sharedPath.reset()
                            if (totalPoints > 0) {
                                var prevMidX = (slimeX[0] + slimeX[totalPoints - 1]) / 2f
                                var prevMidY = (slimeY[0] + slimeY[totalPoints - 1]) / 2f
                                sharedPath.moveTo(prevMidX, prevMidY)
                                for (i in 0 until totalPoints) {
                                    val nextIndex = (i + 1) % totalPoints
                                    val midX = (slimeX[i] + slimeX[nextIndex]) / 2f
                                    val midY = (slimeY[i] + slimeY[nextIndex]) / 2f
                                    sharedPath.quadraticTo(slimeX[i], slimeY[i], midX, midY)
                                }
                                sharedPath.close()
                                
                                drawPath(
                                    path = sharedPath,
                                    brush = Brush.radialGradient(
                                        colors = listOf(colorDominant.copy(alpha = 1f), colorVibrant.copy(alpha = 0.8f)),
                                        center = center,
                                        radius = radius + 250f
                                    )
                                )
                                drawPath(path = sharedPath, color = colorVibrant, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }
                        VisualizerStyle.PARTICLES -> {
                            for (i in 0 until numBars) {
                                val amplitude = amplitudes[i]
                                val multiplicador = 1f + (i.toFloat() / numBars) * 1.5f
                                val boostedAmplitude = amplitude * multiplicador

                                val distFromEdge = 10f + (boostedAmplitude * 90f)
                                
                                val dRight = 0f + i * distStep
                                val dLeft = pathLength - i * distStep

                                computePointAndNormal(dRight)
                                val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                                computePointAndNormal(dLeft)
                                val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy

                                val mainRadius = 4f + (amplitude * 6f)

                                // Main particle
                                drawCircle(color = colorVibrant, radius = mainRadius, center = Offset(pxR + nxR * distFromEdge, pyR + nyR * distFromEdge))
                                drawCircle(color = colorVibrant, radius = mainRadius, center = Offset(pxL + nxL * distFromEdge, pyL + nyL * distFromEdge))

                                // Trail 1
                                val trail1Dist = distFromEdge * 0.6f
                                drawCircle(color = colorDominant.copy(alpha = 0.6f), radius = mainRadius * 0.7f, center = Offset(pxR + nxR * trail1Dist, pyR + nyR * trail1Dist))
                                drawCircle(color = colorDominant.copy(alpha = 0.6f), radius = mainRadius * 0.7f, center = Offset(pxL + nxL * trail1Dist, pyL + nyL * trail1Dist))

                                // Trail 2
                                val trail2Dist = distFromEdge * 0.3f
                                drawCircle(color = colorMuted.copy(alpha = 0.3f), radius = mainRadius * 0.4f, center = Offset(pxR + nxR * trail2Dist, pyR + nyR * trail2Dist))
                                drawCircle(color = colorMuted.copy(alpha = 0.3f), radius = mainRadius * 0.4f, center = Offset(pxL + nxL * trail2Dist, pyL + nyL * trail2Dist))
                            }
                        }
                        VisualizerStyle.RINGS -> {
                            // RINGS son círculos abstractos independientemente de la forma, así que los mantendré concéntricos.
                            val bassRadius = radius + 30f + (bassAvg * 80f)
                            val midRadius = radius + 60f + (midAvg * 70f)
                            val trebleRadius = radius + 90f + (trebleAvg * 60f)
                            
                            val dynamicRotation = rotationAngle + (bassAvg * 90f)
                            val dynamicFastRotation = fastRotationAngle - (midAvg * 90f)
                            
                            fun drawGlitchRing(r: Float, thickness: Float, gapAngle: Float, startOffset: Float, brushColor: Color) {
                                val sweep = 360f / 4f - gapAngle
                                for (i in 0 until 4) {
                                    drawArc(
                                        color = brushColor, startAngle = startOffset + (i * 90f), sweepAngle = sweep,
                                        useCenter = false, topLeft = Offset(center.x - r, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                                        style = Stroke(width = thickness, cap = StrokeCap.Square)
                                    )
                                }
                            }
                            drawGlitchRing(bassRadius, 8f + (bassAvg * 15f), 20f - (bassAvg * 10f), dynamicRotation, colorDominant.copy(alpha = 0.8f))
                            drawGlitchRing(midRadius, 4f + (midAvg * 10f), 30f, dynamicFastRotation, colorVibrant.copy(alpha = 0.6f))
                            drawGlitchRing(trebleRadius, 2f + (trebleAvg * 5f), 45f, dynamicRotation * 0.5f, colorMuted.copy(alpha = 0.5f))
                        }
                        VisualizerStyle.AURA -> {
                            val bassPulse = (bassAvg * 1.5f).coerceIn(0f, 1f)
                            val midPulse = (midAvg * 1.5f).coerceIn(0f, 1f)
                            val treblePulse = (trebleAvg * 1.5f).coerceIn(0f, 1f)

                            // Bass (Low) Aura
                            drawPath(
                                path = basePath,
                                color = colorDominant.copy(alpha = 0.1f + 0.1f * bassPulse),
                                style = Stroke(width = 80f + bassAvg * 200f, join = StrokeJoin.Round)
                            )
                            // Mid Aura
                            drawPath(
                                path = basePath,
                                color = colorVibrant.copy(alpha = 0.15f + 0.15f * midPulse),
                                style = Stroke(width = 40f + midAvg * 100f, join = StrokeJoin.Round)
                            )
                            // Treble (High) Aura
                            drawPath(
                                path = basePath,
                                color = colorMuted.copy(alpha = 0.25f + 0.25f * treblePulse),
                                style = Stroke(width = 15f + trebleAvg * 50f, join = StrokeJoin.Round)
                            )
                        }
                        VisualizerStyle.WAVE -> {
                            wavePath.reset()
                            
                            for (i in 0 until numBars) {
                                val amplitude = amplitudes[i]
                                val dRight = 0f + i * distStep
                                computePointAndNormal(dRight)
                                val extrude = 20f + (amplitude * 150f)
                                val px = outPx + outNx * extrude
                                val py = outPy + outNy * extrude
                                if (i == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
                            }
                            
                            for (i in numBars - 1 downTo 0) {
                                val amplitude = amplitudes[i]
                                val dLeft = pathLength - i * distStep
                                computePointAndNormal(dLeft)
                                val extrude = 20f + (amplitude * 150f)
                                wavePath.lineTo(outPx + outNx * extrude, outPy + outNy * extrude)
                            }
                            wavePath.close()
                            
                            drawPath(path = wavePath, brush = Brush.radialGradient(listOf(colorVibrant.copy(alpha = 0.3f), Color.Transparent), center, radius + 150f))
                            drawPath(path = wavePath, brush = sweepGradient, style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        VisualizerStyle.BARS -> {
                            for (i in 0 until numBars) {
                                val amplitude = amplitudes[i]
                                val barLength = 10f + (amplitude * 200f)
                                
                                val dRight = 0f + i * distStep
                                val dLeft = pathLength - i * distStep
                                
                                computePointAndNormal(dRight)
                                val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                                computePointAndNormal(dLeft)
                                val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy
                                
                                drawLine(brush = sweepGradient, start = Offset(pxR + nxR * 20f, pyR + nyR * 20f), 
                                         end = Offset(pxR + nxR * (20f + barLength), pyR + nyR * (20f + barLength)), strokeWidth = 8f, cap = StrokeCap.Round)
                                drawLine(brush = sweepGradient, start = Offset(pxL + nxL * 20f, pyL + nyL * 20f), 
                                         end = Offset(pxL + nxL * (20f + barLength), pyL + nyL * (20f + barLength)), strokeWidth = 8f, cap = StrokeCap.Round)
                            }
                        }
                        VisualizerStyle.DOTS -> {
                            for (i in 0 until numBars) {
                                val amplitude = amplitudes[i]
                                val dist = 30f + (amplitude * 180f)
                                val capLen = 8f + (amplitude * 30f)
                                val ghostAmp = if (i < ghosts.size) ghosts[i] else 0f
                                val ghostDist = 30f + (ghostAmp * 180f)
                                
                                val dRight = 0f + i * distStep
                                val dLeft = pathLength - i * distStep
                                
                                computePointAndNormal(dRight)
                                val pxR = outPx; val pyR = outPy; val nxR = outNx; val nyR = outNy
                                computePointAndNormal(dLeft)
                                val pxL = outPx; val pyL = outPy; val nxL = outNx; val nyL = outNy
                                
                                drawLine(color = colorVibrant, start = Offset(pxR + nxR * dist, pyR + nyR * dist), 
                                         end = Offset(pxR + nxR * (dist + capLen), pyR + nyR * (dist + capLen)), strokeWidth = 6f + amplitude*4f, cap = StrokeCap.Round)
                                drawLine(color = colorVibrant, start = Offset(pxL + nxL * dist, pyL + nyL * dist), 
                                         end = Offset(pxL + nxL * (dist + capLen), pyL + nyL * (dist + capLen)), strokeWidth = 6f + amplitude*4f, cap = StrokeCap.Round)
                                
                                if (ghostAmp > amplitude + 0.05f) {
                                    drawLine(color = colorVibrant.copy(alpha=0.3f), start = Offset(pxR + nxR * ghostDist, pyR + nyR * ghostDist), 
                                             end = Offset(pxR + nxR * (ghostDist + 8f), pyR + nyR * (ghostDist + 8f)), strokeWidth = 6f + amplitude*4f, cap = StrokeCap.Round)
                                    drawLine(color = colorVibrant.copy(alpha=0.3f), start = Offset(pxL + nxL * ghostDist, pyL + nyL * ghostDist), 
                                             end = Offset(pxL + nxL * (ghostDist + 8f), pyL + nyL * (ghostDist + 8f)), strokeWidth = 6f + amplitude*4f, cap = StrokeCap.Round)
                                }
                            }
                        }
                    }
                }
                
                // Draw Song Progress Ring on the boundary
                val activePosition = dragSeekTimeMs ?: currentPosition
                val progressFraction = if (duration > 0) activePosition.toFloat() / duration else 0f
                
                // Fondo de la ruta
                drawPath(
                    path = basePath,
                    color = colorDominant.copy(alpha = 0.3f),
                    style = Stroke(width = 4f)
                )

                                // Progreso parcial animado en el path
                progressMeasure.setPath(basePath, forceClosed = false)
                val pLen = progressMeasure.length
                progressPath.reset()
                val targetLength = pLen * progressFraction
                
                if (targetLength > 0f) {
                    // For square/squircle drawn via addRect/addRoundRect, it starts at Top-Left (0.0). Top-Center is ~ 0.125 * pLen
                    // For circle (addOval), it starts at Right-Center (0.0). Top-Center is 0.75 * pLen
                    val startD = if (thumbnailShapeIdx == 0) pLen * 0.75f else pLen * 0.125f
                    val endD = startD + targetLength
                    
                    if (endD <= pLen) {
                        progressMeasure.getSegment(startD, endD, progressPath, true)
                    } else {
                        progressMeasure.getSegment(startD, pLen, progressPath, true)
                        progressMeasure.getSegment(0f, endD % pLen, progressPath, true)
                    }
                    
                    drawPath(
                        path = progressPath,
                        brush = sweepGradient,
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                    
                    val thumbDist = endD % pLen
                    val thumbPos = progressMeasure.getPosition(thumbDist)
                    if (thumbPos != androidx.compose.ui.geometry.Offset.Unspecified) {
                        playheadPos = thumbPos
                        drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 8f, center = thumbPos)
                    }
                }

                // Draw sparks
                val iterator = sparks.iterator()
                while (iterator.hasNext()) {
                    val spark = iterator.next()
                    spark.x += spark.vx
                    spark.y += spark.vy
                    spark.alpha -= 0.03f
                    if (spark.alpha <= 0f) {
                        iterator.remove()
                    } else {
                        drawCircle(color = spark.color.copy(alpha = spark.alpha), radius = 4f, center = Offset(spark.x, spark.y))
                    }
                }
            }

            // Central Album Art
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
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
            title = { Text("Temporizador", color = colorVibrant) },
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
                TextButton(onClick = { showTimerDialog = false }) { Text("Aceptar", color = colorVibrant) }
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
            title = { Text("Ecualizador", color = colorVibrant, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Activar Ecualizador", color = Color.White)
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
                            Text("Modo Auto (Loudness)", color = Color.White)
                            androidx.compose.material3.Switch(
                                checked = isAutoMode,
                                onCheckedChange = { equalizerManager.setAutoMode(it) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorDominant)
                            )
                        }

                        if (!isAutoMode) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Preset:", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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
                                        text = { Text("Personalizado", color = Color.White) },
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
            confirmButton = { TextButton(onClick = { showEqDialog = false }) { Text("Cerrar", color = colorVibrant) } },
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
            title = { Text("Editar Etiqueta", color = colorVibrant) },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Título", color = Color.Gray) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = editArtist,
                        onValueChange = { editArtist = it },
                        label = { Text("Artista", color = Color.Gray) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = editAlbum,
                        onValueChange = { editAlbum = it },
                        label = { Text("Álbum", color = Color.Gray) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = colorVibrant, cursorColor = colorVibrant)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Button(
                        onClick = { launcher.launch(arrayOf("image/*")) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colorVibrant)
                    ) {
                        Text("Elegir Portada")
                    }
                    if (editCoverPath != null) {
                        Text("Portada personalizada seleccionada", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    onUpdateTrackMetadata(currentTrack.id, editTitle, editArtist, editAlbum, editCoverPath)
                    showEditorDialog = false 
                }) { Text("Guardar", color = colorVibrant) }
            },
            dismissButton = {
                TextButton(onClick = { showEditorDialog = false }) { Text("Cancelar", color = Color.Gray) }
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
                Text("Ajustes de Reproducción", color = colorVibrant, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Reproducción Aleatoria", color = Color.White)
                    var isShuffleEnabled by remember { mutableStateOf(exoPlayer?.shuffleModeEnabled == true) }
                    androidx.compose.material3.Switch(
                        checked = isShuffleEnabled,
                        onCheckedChange = { 
                            isShuffleEnabled = it
                            exoPlayer?.shuffleModeEnabled = it 
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Modo de Repetición", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val currentMode = exoPlayer?.repeatMode ?: androidx.media3.common.Player.REPEAT_MODE_OFF
                    TextButton(onClick = { exoPlayer?.repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF }) {
                        Text("Desactivado", color = if (currentMode == androidx.media3.common.Player.REPEAT_MODE_OFF) colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = { exoPlayer?.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL }) {
                        Text("Lista", color = if (currentMode == androidx.media3.common.Player.REPEAT_MODE_ALL) colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = { exoPlayer?.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE }) {
                        Text("Una", color = if (currentMode == androidx.media3.common.Player.REPEAT_MODE_ONE) colorVibrant else Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Estilo Visual", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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
                
                Text("Física del Visualizador", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { 
                        visualizerManager.isAdvancedMode.value = false
                        prefs.isAdvancedMode = false
                    }) {
                        Text("Clásico (Suave)", color = if (!isAdvanced) colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = { 
                        visualizerManager.isAdvancedMode.value = true
                        prefs.isAdvancedMode = true
                    }) {
                        Text("Gravedad (Monstercat)", color = if (isAdvanced) colorVibrant else Color.Gray)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Filtro de Frecuencias", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { 
                        visualizerManager.filterMode.value = FilterMode.ALL
                        prefs.filterMode = "ALL"
                    }) {
                        Text("Todos", color = if (filterMode == FilterMode.ALL) colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = { 
                        visualizerManager.filterMode.value = FilterMode.BASS
                        prefs.filterMode = "BASS"
                    }) {
                        Text("Bajos", color = if (filterMode == FilterMode.BASS) colorVibrant else Color.Gray)
                    }
                    TextButton(onClick = { 
                        visualizerManager.filterMode.value = FilterMode.MIDS
                        prefs.filterMode = "MIDS"
                    }) {
                        Text("Medios", color = if (filterMode == FilterMode.MIDS) colorVibrant else Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Sensibilidad (Multiplicador): ${String.format("%.1f", sensitivity)}x", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = sensitivity,
                    onValueChange = { 
                        visualizerManager.sensitivity.value = it 
                        prefs.sensitivity = it
                    },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = colorVibrant, activeTrackColor = colorDominant)
                )

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
                Text("Cola de Reproducción", color = colorVibrant, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
}