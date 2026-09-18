package com.example.beatpulse.ui.components.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Central visualizer area with gesture handling, canvas, album art overlay */
@Composable
fun ColumnScope.PlayerVisualizerArea(
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
    shape: Shape,
    bassAmplitudesState: State<FloatArray>, midAmplitudesState: State<FloatArray>,
    highAmplitudesState: State<FloatArray>, combinedAmplitudesState: State<FloatArray>,
    visualizerManager: IAudioVisualizerManager, visualizerArchetype: Int,
    rotationAngle: Float, fastRotationAngle: Float,
    currentPosition: Long, duration: Long,
    abPointA: Float, abPointB: Float, activeDraggingHandle: String?,
    onActiveDraggingHandleChanged: (String?) -> Unit,
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
    var currentDragAction by remember { mutableStateOf(DragAction.NONE) }
    var lastAngle by remember { mutableStateOf<Float?>(null) }
    var dragSeekTimeMs by remember { mutableStateOf<Long?>(null) }

    var manualRotation by remember { mutableFloatStateOf(0f) }
    val sparks = remember { mutableListOf<Spark>() }
    var playheadPos by remember { mutableStateOf(Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()
    var accumulatedAngle by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    
    val bassAvgAnim = remember { Animatable(0f) }
    val midAvgAnim = remember { Animatable(0f) }
    val trebleAvgAnim = remember { Animatable(0f) }
    val animatedScaleAnim = remember { Animatable(1f) }

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

    val elementSize by visualizerManager.elementSize.collectAsState()

    // Use rememberUpdatedState so pointerInput always reads fresh values
    val updatedAbPointA by rememberUpdatedState(abPointA)
    val updatedAbPointB by rememberUpdatedState(abPointB)
    val updatedDuration by rememberUpdatedState(duration)
    val updatedCurrentPosition by rememberUpdatedState(currentPosition)
    var lastDragProgress by remember { mutableStateOf(0f) }
    var markerPosA by remember { mutableStateOf(Offset.Zero) }
    var markerPosB by remember { mutableStateOf(Offset.Zero) }
    val updatedMarkerPosA by rememberUpdatedState(markerPosA)
    val updatedMarkerPosB by rememberUpdatedState(markerPosB)
    val updatedScale by rememberUpdatedState(animatedScaleAnim.value * coverScale)
    val updatedOffsetX by rememberUpdatedState(coverOffsetX)
    val updatedOffsetY by rememberUpdatedState(coverOffsetY)

    val manualRotationAnim = remember { Animatable(0f) }
    val dragScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .then(areaModifier)
            .fillMaxWidth()
            .then(
                if (coverDragEnabled) Modifier
                else Modifier.pointerInput(abRepeatModeEnabled) {
                    val coverSizePx = 160f * density
                    val hitRadiusPx = 48f * density // 48dp touch radius
                    
                    detectDragGestures(
                        onDragStart = { offset -> 
                            var action = DragAction.NONE
                            if (abRepeatModeEnabled && updatedDuration > 0) {
                                val center = Offset(size.width.toFloat() / 2f, size.height.toFloat() / 2f)

                                val screenA = Offset(
                                    center.x + updatedOffsetX + updatedMarkerPosA.x * updatedScale,
                                    center.y + updatedOffsetY + updatedMarkerPosA.y * updatedScale
                                )
                                val screenB = Offset(
                                    center.x + updatedOffsetX + updatedMarkerPosB.x * updatedScale,
                                    center.y + updatedOffsetY + updatedMarkerPosB.y * updatedScale
                                )

                                val distA = kotlin.math.sqrt(
                                    (offset.x - screenA.x) * (offset.x - screenA.x) +
                                    (offset.y - screenA.y) * (offset.y - screenA.y)
                                )
                                val distB = kotlin.math.sqrt(
                                    (offset.x - screenB.x) * (offset.x - screenB.x) +
                                    (offset.y - screenB.y) * (offset.y - screenB.y)
                                )

                                if (distA < hitRadiusPx && distA <= distB) {
                                    action = DragAction.DRAG_A
                                    onActiveDraggingHandleChanged("A")
                                    lastDragProgress = updatedAbPointA
                                } else if (distB < hitRadiusPx) {
                                    action = DragAction.DRAG_B
                                    onActiveDraggingHandleChanged("B")
                                    lastDragProgress = updatedAbPointB
                                }
                            }
                            currentDragAction = action
                            accumulatedAngle = 0f
                            if (action != DragAction.NONE) {
                                val center = Offset(size.width.toFloat() / 2f, size.height.toFloat() / 2f)
                                val dx = offset.x - center.x; val dy = offset.y - center.y
                                lastAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                            } else {
                                lastAngle = null
                            }
                        },
                        onDragEnd = {
                            if (currentDragAction == DragAction.DJ_SEEK) { dragSeekTimeMs?.let { playerViewModel.seekTo(it) } }
                            currentDragAction = DragAction.NONE; lastAngle = null; dragSeekTimeMs = null
                            dragScope.launch { manualRotationAnim.animateTo(0f, tween(700)) }
                            onActiveDraggingHandleChanged(null)
                        },
                        onDragCancel = {
                            currentDragAction = DragAction.NONE; lastAngle = null; dragSeekTimeMs = null
                            dragScope.launch { manualRotationAnim.animateTo(0f, tween(700)) }
                            onActiveDraggingHandleChanged(null)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val center = Offset(size.width.toFloat() / 2f, size.height.toFloat() / 2f)
                        val touchPos = change.position

                        if (currentDragAction == DragAction.NONE) {
                            if (dragAmount.y < -10f && abs(dragAmount.x) < 20f) {
                                currentDragAction = DragAction.OPEN_QUEUE
                                onShowQueue()
                                if (showPlaylistSwipeTutorial) onDismissPlaylistSwipeTutorial()
                            } else if (abs(dragAmount.x) > 5f || abs(dragAmount.y) > 5f) {
                                currentDragAction = DragAction.DJ_SEEK
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val dx = touchPos.x - center.x; val dy = touchPos.y - center.y
                                lastAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                                dragSeekTimeMs = updatedCurrentPosition
                                accumulatedAngle = 0f
                            }
                        }

                        if (currentDragAction == DragAction.DJ_SEEK) {
                            val dx = touchPos.x - center.x; val dy = touchPos.y - center.y
                            val currentAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                            val prevAngle = lastAngle
                            if (prevAngle != null) {
                                var deltaAngle = currentAngle - prevAngle; if (deltaAngle > 180f) deltaAngle -= 360f; if (deltaAngle < -180f) deltaAngle += 360f
                                accumulatedAngle += deltaAngle
                                if (abs(accumulatedAngle) >= 10f) { accumulatedAngle = 0f }
                                val seekMs = (deltaAngle / 360f) * 120000f
                                val current = dragSeekTimeMs ?: updatedCurrentPosition
                                val maxDuration = if (updatedDuration > 0) updatedDuration else Long.MAX_VALUE
                                dragSeekTimeMs = (current + seekMs.toLong()).coerceIn(0L, maxDuration)
                                if (showVinylSeekTutorial) onDismissVinylSeekTutorial()
                                dragScope.launch { manualRotationAnim.snapTo(manualRotationAnim.value + deltaAngle) }
                            }
                            lastAngle = currentAngle
                        } else if (currentDragAction == DragAction.DRAG_A || currentDragAction == DragAction.DRAG_B) {
                            val dx = touchPos.x - center.x; val dy = touchPos.y - center.y
                            val currentAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                            val prevAngle = lastAngle
                            if (prevAngle != null) {
                                var deltaAngle = currentAngle - prevAngle
                                if (deltaAngle > 180f) deltaAngle -= 360f
                                if (deltaAngle < -180f) deltaAngle += 360f
                                
                                val deltaProgress = deltaAngle / 360f
                                if (currentDragAction == DragAction.DRAG_A) {
                                    val newProgress = (updatedAbPointA + deltaProgress).coerceIn(0f, updatedAbPointB)
                                    (playerViewModel.abPointA as? kotlinx.coroutines.flow.MutableStateFlow)?.value = newProgress
                                } else {
                                    val newProgress = (updatedAbPointB + deltaProgress).coerceIn(updatedAbPointA, 1f)
                                    (playerViewModel.abPointB as? kotlinx.coroutines.flow.MutableStateFlow)?.value = newProgress
                                }
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
            Canvas(
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
            bassAmplitudes = { bassAmplitudesState.value },
            midAmplitudes = { midAmplitudesState.value },
            highAmplitudes = { highAmplitudesState.value },
            combinedAmplitudes = { combinedAmplitudesState.value },
            bassAvg = { bassAvg }, midAvg = { midAvg }, trebleAvg = { trebleAvg },
            bassOpacity = { bassOpacity }, midOpacity = { midOpacity }, highOpacity = { highOpacity },
            visualizerArchetype = visualizerArchetype,
                        colorDominant = colorDominant, colorVibrant = colorVibrant, colorMuted = colorMuted,
            paletteColors = paletteColors,
            rotationAngle = { rotationAngle }, fastRotationAngle = { fastRotationAngle },
            currentPosition = currentPosition, duration = duration,
            dragSeekTimeMs = dragSeekTimeMs,
            abRepeatModeEnabled = abRepeatModeEnabled,
            abPointA = abPointA, abPointB = abPointB,
            activeDraggingHandle = activeDraggingHandle,
            animatedScale = { animatedScaleAnim.value },
            coverScale = coverScale,
            cleanUiMode = cleanUiMode,
            elementSize = elementSize,
            onPlayheadPosChanged = { playheadPos = it },
            onMarkerAPosChanged = { markerPosA = it },
            onMarkerBPosChanged = { markerPosB = it }
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
                        rotationZ = manualRotationAnim.value
                    }
                    .then(
                        if (coverDragEnabled) {
                            Modifier.pointerInput(Unit) {
                                var localDragOffset = Offset.Zero
                                detectDragGestures(
                                    onDragStart = { localDragOffset = Offset(playerViewModel.coverOffsetX.value, playerViewModel.coverOffsetY.value) },
                                    onDragEnd = { playerViewModel.setCoverOffset(0f, 0f) },
                                    onDragCancel = { playerViewModel.setCoverOffset(0f, 0f) },
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
                            Image(bitmap = bmp, contentDescription = "Album Art", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            // Se removieron las líneas de la vidriera a petición del usuario.
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
            Canvas(
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
