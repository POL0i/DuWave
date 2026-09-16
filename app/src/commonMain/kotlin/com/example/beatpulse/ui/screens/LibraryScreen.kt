package com.example.beatpulse.ui.screens

import androidx.compose.foundation.border
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.beatpulse.utils.getLocalizedString
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.animation.togetherWith

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.ui.viewmodels.PlaylistViewData
import com.example.beatpulse.ui.screens.LibraryViewModel
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.MoreVert
import com.example.beatpulse.ui.components.rememberAlbumArt
import com.example.beatpulse.ui.components.rememberAlbumArt
import com.example.beatpulse.ui.components.rememberTrackPalette
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import kotlin.math.abs
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import com.example.beatpulse.recognition.MusicRecognizer
import com.example.beatpulse.recognition.RecognizedTrack

@Composable
fun MicVisualizer(progress: Float, amplitude: Float, color: Color) {
    val barCount = 12
    val animatedAmplitudes = remember { List(barCount) { Animatable(0.1f) } }

    LaunchedEffect(amplitude) {
        animatedAmplitudes.forEachIndexed { index, animatable ->
            launch {
                val distanceFromCenter = abs(index - barCount / 2f)
                val scale = (1f - (distanceFromCenter / (barCount / 2f))).coerceAtLeast(0.3f)
                val targetValue = if (amplitude > 10f) {
                    val normalizedAmp = (amplitude / 1200f).coerceIn(0f, 1f)
                    val baseScale = 0.5f + Math.random().toFloat() * 1.5f
                    (normalizedAmp * scale * baseScale).coerceIn(0.15f, 1f)
                } else {
                    0.1f
                }
                animatable.animateTo(
                    targetValue = targetValue,
                    animationSpec = tween(durationMillis = 100)
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        animatedAmplitudes.forEachIndexed { index, animatable ->
            val isFilled = (index.toFloat() / barCount) <= progress
            val barColor = if (isFilled) color else Color.Gray.copy(alpha = 0.3f)
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(8.dp)
                    .fillMaxHeight(animatable.value)
                    .background(color = barColor, shape = RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun LibraryScreen(
    viewModel: com.example.beatpulse.ui.viewmodels.ILibraryViewModel,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    currentPlayingTrack: TrackEntity?,
    isPlaying: Boolean,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit,
    onPausePlayback: () -> Unit = {},
    onResumePlayback: () -> Unit = {}
) {
    val prefs = viewModel.prefs
    val shapeIdx by prefs.thumbnailShapeFlow.collectAsState(initial = 0)
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val onRescan = { viewModel.scanMediaStore() }
    var selectedTabIndex by remember { mutableIntStateOf(prefs.lastLibraryGeneralTab) }
    LaunchedEffect(selectedTabIndex) {
        prefs.lastLibraryGeneralTab = selectedTabIndex
    }

    LaunchedEffect(Unit) {
        com.example.beatpulse.core.focus.AppFocusManager.focusActions.collect { action ->
            if (action == com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_GLOBAL_TODOS) {
                selectedTabIndex = 0
            } else if (action == com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_GLOBAL_NAVEGADOR) {
                selectedTabIndex = 1
            } else if (action == com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_GLOBAL_RECOMENDACIONES) {
                selectedTabIndex = 2
            } else if (action == com.example.beatpulse.core.focus.FocusAction.NAVIGATE_LEFT) {
                if (!com.example.beatpulse.core.focus.AppFocusManager.isTabNavigationActive && prefs.lastMainScreenPage == 1) {
                    selectedTabIndex = if (selectedTabIndex == 0) 2 else selectedTabIndex - 1
                }
            } else if (action == com.example.beatpulse.core.focus.FocusAction.NAVIGATE_RIGHT) {
                if (!com.example.beatpulse.core.focus.AppFocusManager.isTabNavigationActive && prefs.lastMainScreenPage == 1) {
                    selectedTabIndex = if (selectedTabIndex == 2) 0 else selectedTabIndex + 1
                }
            }
        }
    }
    val tabs = listOf(getLocalizedString("tab_all"), getLocalizedString("tab_browser"), getLocalizedString("tab_recommendations"))

    val allTracks by viewModel.allTracks.collectAsState()
    val recentTracks by viewModel.recentTracks.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val resolvingTracks by viewModel.resolvingTracks.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    
    val colorDominant by animateColorAsState(paletteColors.dominant, label = "color_dom")
    val colorVibrant by animateColorAsState(paletteColors.vibrant, label = "color_vib")
    var localSearchQuery by remember { mutableStateOf("") }
    val onlineSearchQuery by viewModel.searchQuery.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var trackToAddToPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
    var trackPendingConfirmation by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToChangeCover by remember { mutableStateOf<TrackEntity?>(null) }
    var trackPendingDownload by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToDelete by remember { mutableStateOf<TrackEntity?>(null) }
    var trackPendingTrim by remember { mutableStateOf<TrackEntity?>(null) }

    var shazamPhase by remember { mutableStateOf(0) }
    var pauseMusicForShazam by remember { mutableStateOf(false) }
    var shazamJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var listeningProgress by remember { mutableFloatStateOf(0f) }
    var listeningAmplitude by remember { mutableFloatStateOf(0f) }
    val musicRecognizer = remember { MusicRecognizer() }
    var recognitionResult by remember { mutableStateOf<RecognizedTrack?>(null) }
    var recognitionError by remember { mutableStateOf<String?>(null) }

    val shazamApiUnavailableStr = getLocalizedString("shazam_api_unavailable")
    val shazamMicDeniedStr = getLocalizedString("shazam_mic_denied")
    val shazamIndieWarningStr = getLocalizedString("shazam_indie_warning")
    val shazamNoMatchStr = getLocalizedString("shazam_no_match")
    val shazamUnknownErrorStr = getLocalizedString("shazam_unknown_error")

    val trackDeletedStr = getLocalizedString("track_deleted")
    val deleteLauncher = com.example.beatpulse.ui.utils.rememberTrackDeleteHandler(onDeleted = {
        trackToDelete?.let { track ->
            viewModel.completeDeletion(track.id)
            prefs.showToast(trackDeletedStr)
            viewModel.scanMediaStore()
        }
        trackToDelete = null
    })

    val isSearchingOnline = selectedTabIndex == 1
    val onlineSearchResults by viewModel.onlineSearchResults.collectAsState()
    val isOnlineSearchLoading by viewModel.isOnlineSearchLoading.collectAsState()
    val isOnlineServiceDown by viewModel.isOnlineServiceDown.collectAsState()
    
    val recommendations by viewModel.recommendations.collectAsState()
    val isRecommendationsLoading by viewModel.isRecommendationsLoading.collectAsState()

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 2) {
            viewModel.loadRecommendations()
        }
    }



    LaunchedEffect(Unit) {
        com.example.beatpulse.core.focus.AppFocusManager.appShortcuts.collect { shortcut ->
            if (prefs.lastMainScreenPage == 1) {
                when (shortcut) {
                    com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_GLOBAL_LIST -> {
                        selectedTabIndex = 0
                        viewModel.searchQuery.value = ""
                    }
                    com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_GLOBAL_SEARCH -> {
                        selectedTabIndex = 1
                    }
                    com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_RECOMMENDATIONS -> {
                        selectedTabIndex = 2
                        viewModel.searchQuery.value = ""
                    }
                    else -> {}
                }
            }
        }
    }

    LaunchedEffect(onlineSearchQuery, selectedTabIndex) {
        if (selectedTabIndex == 1 && onlineSearchQuery.isNotBlank()) {
            kotlinx.coroutines.delay(500)
            viewModel.isOnlineSearchLoading.value = true
            try {
                viewModel.onlineSearchResults.value = viewModel.searchOnlineMusic(onlineSearchQuery)
            } catch (e: Exception) {
                viewModel.onlineSearchResults.value = emptyList()
            } finally {
                viewModel.isOnlineSearchLoading.value = false
            }
        } else if (selectedTabIndex == 1 && onlineSearchQuery.isBlank()) {
            viewModel.onlineSearchResults.value = emptyList()
        }
    }

    val bgStyle by prefs.backgroundStyleFlow.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()
    val dynamicTextColor = remember(bgStyle, paletteColors, isDarkTheme) {
                if (bgStyle == 3) {
            val colors = listOf(paletteColors.lightVibrant, paletteColors.vibrant, paletteColors.muted, paletteColors.dominant)
            val brightestColor = colors.filter { it != androidx.compose.ui.graphics.Color.Unspecified }.maxByOrNull { it.luminance() } ?: androidx.compose.ui.graphics.Color.White
            if (brightestColor.luminance() < 0.6f) {
                androidx.compose.ui.graphics.lerp(brightestColor, androidx.compose.ui.graphics.Color.White, 0.5f)
            } else {
                brightestColor
            }
        } else {
            val isBackgroundLight = when (bgStyle) {
                0 -> !isDarkTheme
                1 -> false
                2, 4 -> paletteColors.dominant.luminance() > 0.3f
                5, 6, 7, 8 -> false
                else -> paletteColors.dominant.luminance() > 0.5f
            }
            if (isBackgroundLight) androidx.compose.ui.graphics.Color(0xFF121212) else androidx.compose.ui.graphics.Color.White
        }
    }

    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = { accumulatedDrag = 0f },
                onDragCancel = { accumulatedDrag = 0f },
                onHorizontalDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float ->
                    accumulatedDrag += dragAmount
                    if (accumulatedDrag > 150f) {
                        // Swipe right -> Go to previous tab
                        selectedTabIndex = if (selectedTabIndex == 0) 2 else selectedTabIndex - 1
                        accumulatedDrag = 0f
                    } else if (accumulatedDrag < -150f) {
                        // Swipe left -> Go to next tab
                        selectedTabIndex = if (selectedTabIndex == 2) 0 else selectedTabIndex + 1
                        accumulatedDrag = 0f
                    }
                }
            )
        }
    ) {
        var sortOrder by remember { mutableStateOf(prefs.librarySortOrder) }
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = colorVibrant,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = colorVibrant,
                            height = 3.dp
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            Text(
                                text = title, 
                                color = if (isSelected) colorVibrant else dynamicTextColor.copy(alpha = 0.6f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            val currentList by produceState(initialValue = emptyList<TrackEntity>(), selectedTabIndex, allTracks, recentTracks, favoriteTracks, localSearchQuery, sortOrder) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    val baseList = when (selectedTabIndex) {
                        0 -> allTracks
                        1 -> recentTracks
                        2 -> emptyList() // handled separately
                        else -> allTracks
                    }
                    val filteredList = if (localSearchQuery.isEmpty()) baseList else baseList.filter { it.title.contains(localSearchQuery, ignoreCase = true) || it.artist.contains(localSearchQuery, ignoreCase = true) }
                    
                    value = when (sortOrder) {
                        "TITLE" -> filteredList.sortedBy { it.title.lowercase() }
                        "ARTIST" -> filteredList.sortedBy { it.artist.lowercase() }
                        "ALBUM" -> filteredList.sortedBy { it.album.lowercase() }
                        "DIRECTORY" -> filteredList.sortedWith(compareBy<TrackEntity> { if (it.dataPath.startsWith("youtube://")) "1_Online" else "0_Locales" }.thenBy { it.folderPath }.thenBy { it.title })
                        else -> filteredList
                    }
                }
            }

            val isScanning by viewModel.isScanning.collectAsState()
            val isOnlineServiceDown by viewModel.isOnlineServiceDown.collectAsState()

            if (selectedTabIndex == 1 && isOnlineServiceDown) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        colors = CardDefaults.cardColors(containerColor = paletteColors.dominant)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = paletteColors.vibrant, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = getLocalizedString("online_service_down"),
                                color = dynamicTextColor,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else if (isScanning || isOnlineSearchLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = paletteColors.vibrant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (isOnlineSearchLoading) getLocalizedString("searching_online") else getLocalizedString("searching_music"), color = dynamicTextColor)
                    }
                }
            } else if (!isSearchingOnline && selectedTabIndex != 2 && currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay canciones aquí.", color = dynamicTextColor)
                }
            } else if (isSearchingOnline && onlineSearchResults.isEmpty() && onlineSearchQuery.isNotBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(getLocalizedString("no_results_online"), color = dynamicTextColor)
                }
            } else {
                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = prefs.libraryScrollIndex,
                    initialFirstVisibleItemScrollOffset = prefs.libraryScrollOffset
                )


                
                DisposableEffect(listState) {
                    onDispose {
                        prefs.libraryScrollIndex = listState.firstVisibleItemIndex
                        prefs.libraryScrollOffset = listState.firstVisibleItemScrollOffset
                    }
                }
                
                val showFastScroll by remember { derivedStateOf { listState.firstVisibleItemIndex > 5 } }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (selectedTabIndex == 2) {
                        if (isRecommendationsLoading && recommendations.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = paletteColors.vibrant)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(getLocalizedString("searching_recommendations"), color = dynamicTextColor, modifier = Modifier.padding(16.dp))
                                }
                            }
                        } else {
                            val lastTime = prefs.lastRecommendationsTimestamp
                            val now = System.currentTimeMillis()
                            val nextUpdate = lastTime + (24 * 60 * 60 * 1000L)
                            val remainingMs = nextUpdate - now
                            val remainingHours = (remainingMs / (1000 * 60 * 60)).coerceAtLeast(0)
                            val remainingMinutes = ((remainingMs % (1000 * 60 * 60)) / (1000 * 60)).coerceAtLeast(0)
                            
                            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                                item {
                                    Text(
                                        text = if (remainingMs > 0) getLocalizedString("next_update_in_h_m", remainingHours.toInt(), remainingMinutes.toInt()) else getLocalizedString("updating_soon"),
                                        color = dynamicTextColor.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp)
                                    )
                                }
                                recommendations.forEach { (category, tracks) ->
                                    if (tracks.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = category,
                                                color = colorVibrant,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                                            )
                                        }
                                        itemsIndexed(tracks, key = { index, track -> "${category}_${track.id}_$index" }) { index, track ->
                                            val globalIndex = index
                                            TrackItem(
                                                track = track,
                                                paletteColors = paletteColors,
                                                thumbnailShapeIdx = shapeIdx,
                                                textColor = dynamicTextColor,
                                                onClick = { 
                                                    if (isOnlineServiceDown && track.dataPath.startsWith("youtube://")) {
                                                        // Do nothing
                                                    } else {
                                                        keyboardController?.hide()
                                                        onTrackClick(track, tracks) 
                                                    }
                                                },
                                                onToggleFavorite = { 
                                                    viewModel.toggleFavorite(track, !track.isFavorite)
                                                },
                                                onAddToPlaylist = null,
                                                onDeleteTrack = null,
                                                isResolving = resolvingTracks.contains(track.id),
                                                onDownloadTrack = if (track.dataPath.startsWith("youtube://")) {
                                                    { trackPendingDownload = track }
                                                } else null,
                                                onTrimTrack = null,
                                                isPlaying = currentPlayingTrack?.id == track.id,
                                                isActuallyPlaying = currentPlayingTrack?.id == track.id && isPlaying,
                                                isServiceDown = isOnlineServiceDown
                                            )
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(100.dp)) }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                            val itemsToShow = if (isSearchingOnline) onlineSearchResults else currentList
                            itemsIndexed(itemsToShow, key = { _, t -> t.id }) { index, track ->

                                TrackItem(
                                    track = track,
                                    paletteColors = paletteColors,
                                    thumbnailShapeIdx = shapeIdx,
                                    textColor = dynamicTextColor,
                                    onClick = { 
                                        if (isOnlineServiceDown && track.dataPath.startsWith("youtube://")) {
                                            // Do nothing if service is down and it's an online track
                                        } else {
                                            keyboardController?.hide()
                                            onTrackClick(track, itemsToShow) 
                                        }
                                    },
                                    onToggleFavorite = { 
                                        viewModel.toggleFavorite(track, !track.isFavorite)
                                    },
                                    onAddToPlaylist = if (!isSearchingOnline) { { trackToAddToPlaylist = track } } else null,
                                    onDeleteTrack = if (!isSearchingOnline) { { trackPendingConfirmation = track } } else null,
                                    isResolving = resolvingTracks.contains(track.id),
                                    onDownloadTrack = if (isSearchingOnline && track.dataPath.startsWith("youtube://")) {
                                        { trackPendingDownload = track }
                                    } else null,
                                    onTrimTrack = if (selectedTabIndex == 0) { { trackPendingTrim = track } } else null,
                                    onChangeCover = { trackToChangeCover = track },
                                    isPlaying = currentPlayingTrack?.id == track.id,
                                    isActuallyPlaying = currentPlayingTrack?.id == track.id && isPlaying,
                                    isServiceDown = isOnlineServiceDown
                                )
                            }
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .padding(bottom = 120.dp)
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showFastScroll,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            FloatingActionButton(
                                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                                containerColor = paletteColors.vibrant,
                                contentColor = Color.White
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Ir arriba")
                            }
                        }
                    }
                }
            }
        }
        
        // Floating search bar overlay
        var isLocalSearchExpanded by remember { mutableStateOf(false) }
        val isSearchExpanded = if (selectedTabIndex == 1 && !isOnlineServiceDown) true else isLocalSearchExpanded
        var isSortMenuExpanded by remember { mutableStateOf(false) }
        val searchOffset by animateDpAsState(
            targetValue = if (isSearchExpanded) 0.dp else 40.dp
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { androidx.compose.ui.unit.IntOffset(searchOffset.roundToPx(), 0) }
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                .background(Color.Black.copy(alpha=0.7f))
                .animateContentSize()
                .padding(end = if (!isSearchExpanded && selectedTabIndex != 1) 16.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
             Box {
                 IconButton(onClick = { isSortMenuExpanded = true }) {
                     Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = colorVibrant)
                 }
                 androidx.compose.material3.MaterialTheme(
                     colorScheme = androidx.compose.material3.MaterialTheme.colorScheme.copy(
                         surface = paletteColors.dominant,
                         onSurface = dynamicTextColor
                     )
                 ) {
                     DropdownMenu(
                         expanded = isSortMenuExpanded,
                         onDismissRequest = { isSortMenuExpanded = false }
                     ) {
                         DropdownMenuItem(text = { Text(getLocalizedString("sort_directory")) }, onClick = { sortOrder = "DIRECTORY"; prefs.librarySortOrder = "DIRECTORY"; isSortMenuExpanded = false })
                         DropdownMenuItem(text = { Text(getLocalizedString("sort_title")) }, onClick = { sortOrder = "TITLE"; prefs.librarySortOrder = "TITLE"; isSortMenuExpanded = false })
                         DropdownMenuItem(text = { Text(getLocalizedString("sort_artist")) }, onClick = { sortOrder = "ARTIST"; prefs.librarySortOrder = "ARTIST"; isSortMenuExpanded = false })
                         DropdownMenuItem(text = { Text(getLocalizedString("sort_album")) }, onClick = { sortOrder = "ALBUM"; prefs.librarySortOrder = "ALBUM"; isSortMenuExpanded = false })
                     }
                 }
             }
             if (selectedTabIndex != 1) {
                 IconButton(onClick = { isLocalSearchExpanded = !isLocalSearchExpanded }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = colorVibrant)
                 }
             }
             AnimatedVisibility(visible = isSearchExpanded) {
                 Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                     OutlinedTextField(
                         value = if (isSearchingOnline) onlineSearchQuery else localSearchQuery,
                         onValueChange = { if (isSearchingOnline) viewModel.searchQuery.value = it else localSearchQuery = it },
                         placeholder = { Text(if (isSearchingOnline) getLocalizedString("search_online_dots") else getLocalizedString("search_dots"), color = Color.LightGray) },
                         singleLine = true,
                         modifier = Modifier.width(200.dp),
                         keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                         keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                             onSearch = {
                                 keyboardController?.hide()
                                 if (isSearchingOnline && onlineSearchQuery.isNotBlank()) {
                                     scope.launch {
                                         viewModel.isOnlineSearchLoading.value = true
                                         try {
                                             viewModel.onlineSearchResults.value = viewModel.searchOnlineMusic(onlineSearchQuery)
                                         } catch (e: Exception) {
                                             viewModel.onlineSearchResults.value = emptyList()
                                         } finally {
                                             viewModel.isOnlineSearchLoading.value = false
                                         }
                                     }
                                 }
                             }
                         ),
                         colors = OutlinedTextFieldDefaults.colors(
                             focusedTextColor = Color.White,
                             unfocusedTextColor = Color.White,
                             cursorColor = colorVibrant,
                             focusedBorderColor = colorVibrant,
                             unfocusedBorderColor = Color.Transparent
                         )
                     )
                     if (isSearchingOnline) {
                         fun startRecognition() {
                             shazamJob = scope.launch {
                                 recognitionResult = null
                                 recognitionError = null
                                 if (pauseMusicForShazam) {
                                     onPausePlayback()
                                 }
                                 shazamPhase = 1
                                 listeningProgress = 0f
                                 
                                 kotlinx.coroutines.delay(1500)
                                 val isAvailable = musicRecognizer.checkAvailability()
                                 if (!isAvailable) {
                                     shazamPhase = 0
                                     recognitionError = shazamApiUnavailableStr
                                     return@launch
                                 }
                                 
                                 shazamPhase = 2
                                 kotlinx.coroutines.delay(1500)
                                 
                                 shazamPhase = 3
                                 val result = musicRecognizer.recognizeMusic { progress, amplitude ->
                                     listeningProgress = progress
                                     listeningAmplitude = amplitude
                                 }
                                 shazamPhase = 0
                                 
                                 result.fold(
                                     onSuccess = { track ->
                                         recognitionResult = track
                                     },
                                     onFailure = { error ->
                                         recognitionError = when {
                                             error.message?.contains("micrófono", ignoreCase = true) == true ||
                                             error.message?.contains("permission", ignoreCase = true) == true ||
                                             error.message?.contains("denegado", ignoreCase = true) == true ->
                                                 shazamMicDeniedStr
                                             error.message?.contains("No match", ignoreCase = true) == true ->
                                                 shazamNoMatchStr
                                             else -> error.message ?: shazamUnknownErrorStr
                                         }
                                     }
                                 )
                             }
                         }
                         
                         IconButton(onClick = {
                             startRecognition()
                         }) {
                             Icon(Icons.Default.Mic, contentDescription = "Reconocer Canción", tint = colorVibrant)
                         }
                     }
                     IconButton(onClick = onRescan) {
                         Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = colorVibrant)
                     }
                 }
             }
        }
        
        val addedToPlStr = getLocalizedString("added_to_playlist")
        trackToAddToPlaylist?.let { trackToAdd ->
            AlertDialog(
                onDismissRequest = { trackToAddToPlaylist = null },
                title = { Text(getLocalizedString("add_to_playlist")) },
                text = {
                    if (playlists.isEmpty()) {
                        Text(getLocalizedString("no_playlists_created"))
                    } else {
                        LazyColumn {
                            items(playlists) { pl ->
                                Text(
                                    text = pl.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addTrackToPlaylist(pl.playlistId, trackToAdd)
                                            prefs.showToast(addedToPlStr + " " + pl.name)
                                            trackToAddToPlaylist = null
                                        }
                                        .padding(16.dp),
                                    color = dynamicTextColor
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { trackToAddToPlaylist = null }) {
                        Text("Cerrar", color = colorVibrant)
                    }
                },
                containerColor = paletteColors.dominant
            )
        }

        trackPendingConfirmation?.let { track ->
            AlertDialog(
                onDismissRequest = { trackPendingConfirmation = null },
                title = { Text(getLocalizedString("delete_track_title"), color = dynamicTextColor) },
                text = { Text(getLocalizedString("delete_track_desc", track.title), color = dynamicTextColor) },
                confirmButton = {
                    TextButton(onClick = {
                        val t = track
                        trackPendingConfirmation = null
                        scope.launch {
                            val sender = viewModel.deleteTrack(t.id)
                            if (sender != null) {
                                trackToDelete = t
                                deleteLauncher(t.id, sender)
                            } else {
                                prefs.showToast(trackDeletedStr)
                                viewModel.scanMediaStore()
                            }
                        }
                    }) {
                        Text(getLocalizedString("delete_button_red"), color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { trackPendingConfirmation = null }) {
                        Text(getLocalizedString("cancel"), color = colorVibrant)
                    }
                },
                containerColor = paletteColors.dominant
            )
        }

        trackPendingTrim?.let { track ->
            com.example.beatpulse.ui.components.library.AudioTrimmerDialog(
                track = track,
                onDismiss = { trackPendingTrim = null },
                colorVibrant = paletteColors.vibrant,
                colorSurface = paletteColors.dominant,
                colorText = dynamicTextColor,
                onTrimSuccess = { newPath ->
                    viewModel.copyMetadataForTrimmedTrack(track, newPath)
                },
                onPausePlayback = onPausePlayback
            )
        }
        trackPendingDownload?.let { track ->
            val toastDownloadingMsg = getLocalizedString("toast_downloading", track.title)
            AlertDialog(
                onDismissRequest = { trackPendingDownload = null },
                title = { Text(getLocalizedString("confirm_download"), color = dynamicTextColor) },
                text = { Text(getLocalizedString("confirm_download_desc", track.title), color = dynamicTextColor) },
                confirmButton = {
                    TextButton(onClick = {
                        trackPendingDownload = null
                        viewModel.downloadOnlineTrack(track)
                        prefs.showToast(toastDownloadingMsg)
                    }) {
                        Text(getLocalizedString("download"), color = colorVibrant)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { trackPendingDownload = null }) {
                        Text(getLocalizedString("cancel"), color = dynamicTextColor)
                    }
                },
                containerColor = paletteColors.dominant
            )
        }

        trackToChangeCover?.let { track ->
            ChangeCoverDialog(
                track = track,
                viewModel = viewModel,
                paletteColors = paletteColors,
                thumbnailShapeIdx = shapeIdx,
                onDismiss = { trackToChangeCover = null },
                onCoverSelected = { newPath ->
                    viewModel.updateTrackCover(track, newPath)
                    trackToChangeCover = null
                }
            )
        }

        // Listening dialog
        if (shazamPhase > 0) {
            val sqDist = (colorVibrant.red - paletteColors.dominant.red) * (colorVibrant.red - paletteColors.dominant.red) +
                         (colorVibrant.green - paletteColors.dominant.green) * (colorVibrant.green - paletteColors.dominant.green) +
                         (colorVibrant.blue - paletteColors.dominant.blue) * (colorVibrant.blue - paletteColors.dominant.blue)
            val intelligentAccentColor = if (sqDist < 0.05f) dynamicTextColor else colorVibrant

            AlertDialog(
                onDismissRequest = { /* Modal, espera a que termine */ },
                title = { 
                    val titleText = when (shazamPhase) {
                        1 -> getLocalizedString("shazam_verifying_server")
                        2 -> getLocalizedString("shazam_starting_server")
                        else -> getLocalizedString("shazam_listening_title")
                    }
                    Text(titleText, color = dynamicTextColor) 
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = shazamPhase,
                            transitionSpec = { androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut() },
                            label = "shazamPhaseIcon"
                        ) { phase ->
                            when (phase) {
                                1 -> {
                                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "wifi")
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 0.3f, targetValue = 1f,
                                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                            animation = androidx.compose.animation.core.tween(500),
                                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                        ), label = "wifiAlpha"
                                    )
                                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(64.dp).alpha(alpha), tint = intelligentAccentColor)
                                }
                                2 -> {
                                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "power")
                                    val scale by infiniteTransition.animateFloat(
                                        initialValue = 0.8f, targetValue = 1.1f,
                                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                            animation = androidx.compose.animation.core.tween(400),
                                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                        ), label = "powerScale"
                                    )
                                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(64.dp).scale(scale), tint = intelligentAccentColor)
                                }
                                else -> {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(64.dp), tint = intelligentAccentColor)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        val descText = when (shazamPhase) {
                            1 -> getLocalizedString("shazam_checking_connection")
                            2 -> getLocalizedString("shazam_waking_engine")
                            else -> getLocalizedString("shazam_listening_desc")
                        }
                        Text(descText, color = dynamicTextColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        if (shazamPhase == 3) {
                            MicVisualizer(progress = listeningProgress, amplitude = listeningAmplitude, color = intelligentAccentColor)
                        } else {
                            androidx.compose.material3.LinearProgressIndicator(color = intelligentAccentColor, trackColor = paletteColors.dominant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth().height(4.dp))
                        }
                    }
                },
                confirmButton = { },
                dismissButton = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(
                                checked = pauseMusicForShazam,
                                onCheckedChange = { isChecked ->
                                    pauseMusicForShazam = isChecked
                                    if (isChecked) {
                                        onPausePlayback()
                                    } else {
                                        onResumePlayback()
                                    }
                                },
                                colors = androidx.compose.material3.CheckboxDefaults.colors(
                                    checkedColor = intelligentAccentColor,
                                    uncheckedColor = dynamicTextColor.copy(alpha = 0.6f),
                                    checkmarkColor = paletteColors.dominant
                                )
                            )
                            Text("Pausar", color = dynamicTextColor, fontSize = 13.sp)
                        }
                        TextButton(onClick = {
                            shazamJob?.cancel()
                            shazamPhase = 0
                        }) {
                            Text(getLocalizedString("cancel"), color = intelligentAccentColor)
                        }
                    }
                },
                containerColor = paletteColors.dominant
            )
        }

        // Recognition success dialog
        recognitionResult?.let { track ->
            AlertDialog(
                onDismissRequest = { recognitionResult = null },
                title = { Text(getLocalizedString("shazam_found_title"), color = dynamicTextColor) },
                icon = { Icon(Icons.Default.Mic, contentDescription = null, tint = colorVibrant) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(track.title, color = dynamicTextColor, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Text(track.artist, color = dynamicTextColor.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.searchQuery.value = "${track.title} ${track.artist} audio"
                        recognitionResult = null
                    }) {
                        Text(getLocalizedString("shazam_search_button"), color = colorVibrant)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recognitionResult = null }) {
                        Text(getLocalizedString("cancel"), color = dynamicTextColor.copy(alpha = 0.7f))
                    }
                },
                containerColor = paletteColors.dominant
            )
        }

        // Recognition error dialog
        recognitionError?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = { recognitionError = null },
                title = { Text(getLocalizedString("shazam_error_title"), color = dynamicTextColor) },
                icon = { Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f)) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(errorMsg, color = dynamicTextColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        if (errorMsg == shazamNoMatchStr) {
                            Text(shazamIndieWarningStr, color = dynamicTextColor.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        recognitionError = null
                        // Retry: trigger recognition again
                        shazamJob = scope.launch {
                            recognitionResult = null
                            recognitionError = null
                            if (pauseMusicForShazam) {
                                onPausePlayback()
                            }
                            shazamPhase = 1
                            listeningProgress = 0f
                            
                            kotlinx.coroutines.delay(1500)
                            val isAvailable = musicRecognizer.checkAvailability()
                            if (!isAvailable) {
                                shazamPhase = 0
                                recognitionError = shazamApiUnavailableStr
                                return@launch
                            }
                            
                            shazamPhase = 2
                            kotlinx.coroutines.delay(1500)
                            
                            shazamPhase = 3
                            val result = musicRecognizer.recognizeMusic { progress, amplitude ->
                                listeningProgress = progress
                                listeningAmplitude = amplitude
                            }
                            shazamPhase = 0
                            result.fold(
                                onSuccess = { track ->
                                    recognitionResult = track
                                },
                                onFailure = { error ->
                                    recognitionError = when {
                                        error.message?.contains("micrófono", ignoreCase = true) == true ||
                                        error.message?.contains("permission", ignoreCase = true) == true ||
                                        error.message?.contains("denegado", ignoreCase = true) == true ->
                                            shazamMicDeniedStr
                                        error.message?.contains("No match", ignoreCase = true) == true ->
                                            shazamNoMatchStr
                                        else -> error.message ?: shazamUnknownErrorStr
                                    }
                                }
                            )
                        }
                    }) {
                        Text(getLocalizedString("shazam_retry_button"), color = colorVibrant)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recognitionError = null }) {
                        Text(getLocalizedString("cancel"), color = dynamicTextColor.copy(alpha = 0.7f))
                    }
                },
                containerColor = paletteColors.dominant
            )
        }
    }
}

@Composable
fun TrackItem(
    track: TrackEntity,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    thumbnailShapeIdx: Int = 0,
    textColor: Color = LocalContentColor.current,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onDeleteTrack: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onDownloadTrack: (() -> Unit)? = null,
    onTrimTrack: (() -> Unit)? = null,
    onChangeCover: (() -> Unit)? = null,
    isResolving: Boolean = false,
    isPlaying: Boolean = false,
    isActuallyPlaying: Boolean = false,
    isServiceDown: Boolean = false,
    isDisabled: Boolean = false,
    hasMenuOptions: Boolean = true
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isRowFocused by remember { mutableStateOf(false) }
    var isFavoriteFocused by remember { mutableStateOf(false) }
    var isMenuFocused by remember { mutableStateOf(false) }

    val accentColor = paletteColors.vibrant
    val bgColor = paletteColors.dominant
    val isOnline = track.dataPath.startsWith("youtube://")
    val isDisabled = isOnline && isServiceDown
    
    val bgBrush = remember(bgColor, accentColor) {
        Brush.linearGradient(colors = listOf(bgColor, accentColor.copy(alpha = 0.5f)))
    }
    
    val shape = remember(thumbnailShapeIdx) {
        com.example.beatpulse.ui.utils.getShapeForIndex(thumbnailShapeIdx)
    }

    val isDesktop = !com.example.beatpulse.utils.SystemUtils.isMobilePlatform
    val imageSize = if (isDesktop) 72.dp else 52.dp
    val titleStyle = if (isDesktop) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
    val subtitleStyle = if (isDesktop) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall
    val iconSize = if (isDesktop) 32.dp else 24.dp
    val paddingVert = if (isDesktop) 16.dp else 10.dp

    val focusedBg = if (isRowFocused) accentColor.copy(alpha = 0.1f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDisabled) Modifier else Modifier.clickable(onClick = onClick))
            .onFocusChanged { isRowFocused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = paddingVert)
            .background(if (isDisabled) Color.Black.copy(alpha = 0.2f) else focusedBg)
            .then(if (isRowFocused) Modifier.border(2.dp, accentColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val albumArt = rememberAlbumArt(track = track)
        
        Box(
            modifier = Modifier
                .size(imageSize)
                .clip(shape)
                .background(bgBrush),
            contentAlignment = Alignment.Center
        ) {
            if (albumArt != null) {
                Image(
                    bitmap = albumArt,
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPlaying) {
                    AnimatedEqualizer(isActuallyPlaying = isActuallyPlaying, color = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = track.customTitle ?: track.title,
                    style = titleStyle,
                    color = if (isPlaying) accentColor else textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val displayArtist = track.customArtist ?: track.artist
            val displayAlbum = track.customAlbum ?: track.album
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDisabled) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "$displayArtist • $displayAlbum",
                    style = subtitleStyle,
                    color = if (isDisabled) Color.Red.copy(alpha = 0.7f) else textColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.onFocusChanged { isFavoriteFocused = it.isFocused }
                .then(if (isFavoriteFocused) Modifier.border(2.dp, accentColor, androidx.compose.foundation.shape.CircleShape).background(accentColor.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape) else Modifier)
        ) {
            Icon(
                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (track.isFavorite) accentColor else Color.Gray,
                modifier = Modifier.size(iconSize)
            )
        }
        val hasMenuOptions = onAddToPlaylist != null || onDeleteTrack != null || onDownloadTrack != null || onRemoveFromPlaylist != null || onTrimTrack != null || onChangeCover != null
        if (isResolving) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(24.dp).padding(4.dp),
                color = accentColor,
                strokeWidth = 2.dp
            )
        } else if (hasMenuOptions) {
            Box {
                IconButton(
                    onClick = { isMenuExpanded = true },
                    modifier = Modifier.onFocusChanged { isMenuFocused = it.isFocused }
                        .then(if (isMenuFocused) Modifier.border(2.dp, accentColor, androidx.compose.foundation.shape.CircleShape).background(accentColor.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape) else Modifier)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray, modifier = Modifier.size(iconSize))
                }
                androidx.compose.material3.MaterialTheme(
                    colorScheme = androidx.compose.material3.MaterialTheme.colorScheme.copy(
                        surface = paletteColors.dominant,
                        onSurface = textColor
                    )
                ) {
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        if (onAddToPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text(getLocalizedString("add_to_playlist")) },
                                onClick = {
                                    isMenuExpanded = false
                                    onAddToPlaylist()
                                }
                            )
                        }
                        if (onDeleteTrack != null) {
                            DropdownMenuItem(
                                text = { Text(getLocalizedString("delete_from_device")) },
                                onClick = {
                                    isMenuExpanded = false
                                    onDeleteTrack()
                                }
                            )
                        }
                        if (onRemoveFromPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text("Quitar de la Playlist") },
                                onClick = {
                                    isMenuExpanded = false
                                    onRemoveFromPlaylist()
                                }
                            )
                        }
                        if (onDownloadTrack != null) {
                            DropdownMenuItem(
                                text = { Text(getLocalizedString("download_music")) },
                                onClick = {
                                    isMenuExpanded = false
                                    onDownloadTrack()
                                }
                            )
                        }
                        if (onTrimTrack != null) {
                            DropdownMenuItem(
                                text = { Text(getLocalizedString("trim_audio")) },
                                onClick = {
                                    isMenuExpanded = false
                                    onTrimTrack()
                                }
                            )
                        }
                        if (onChangeCover != null) {
                            DropdownMenuItem(
                                text = { Text(getLocalizedString("change_cover_title")) },
                                onClick = {
                                    isMenuExpanded = false
                                    onChangeCover()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChangeCoverDialog(
    track: TrackEntity,
    viewModel: com.example.beatpulse.ui.viewmodels.ILibraryViewModel,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    thumbnailShapeIdx: Int,
    onDismiss: () -> Unit,
    onCoverSelected: (String) -> Unit
) {
    val searchResults by viewModel.changeCoverSearchResults.collectAsState()
    val isLoading by viewModel.isChangeCoverLoading.collectAsState()

    LaunchedEffect(track) {
        viewModel.searchCoversForTrack(track)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(getLocalizedString("change_cover_title"), color = paletteColors.vibrant) },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = paletteColors.vibrant)
                }
            } else if (searchResults.isEmpty()) {
                Text("No se encontraron portadas.", color = paletteColors.dominant)
            } else {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 300.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults.size) { index ->
                        val result = searchResults[index]
                        val path = result.customCoverPath
                        if (path != null) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(com.example.beatpulse.ui.utils.getShapeForIndex(thumbnailShapeIdx))
                                    .clickable { onCoverSelected(path) }
                            ) {
                                val bitmap = com.example.beatpulse.ui.components.rememberStreamAvatar(path)
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Cover",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                                        androidx.compose.material3.CircularProgressIndicator(color = paletteColors.vibrant)
                                    }
                                    }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = paletteColors.vibrant)
            }
        },
        containerColor = paletteColors.dominant
    )
}

@Composable
fun AnimatedEqualizer(isActuallyPlaying: Boolean, color: Color) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "eq")
    
    val bar1 = if (isActuallyPlaying) infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(animation = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "b1"
    ) else remember { mutableStateOf(0.3f) }
    
    val bar2 = if (isActuallyPlaying) infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(animation = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "b2"
    ) else remember { mutableStateOf(0.6f) }
    
    val bar3 = if (isActuallyPlaying) infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(animation = androidx.compose.animation.core.tween(500, easing = androidx.compose.animation.core.LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "b3"
    ) else remember { mutableStateOf(0.4f) }
    
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(14.dp)
    ) {
        listOf(bar1, bar2, bar3).forEach { anim ->
            Box(modifier = Modifier.width(3.dp).fillMaxHeight(anim.value).clip(RoundedCornerShape(1.dp)).background(color))
        }
    }
}
