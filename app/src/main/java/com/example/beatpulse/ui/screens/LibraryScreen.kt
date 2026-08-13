package com.example.beatpulse.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.res.stringResource
import com.example.beatpulse.R
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Cloud
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
import com.example.beatpulse.ui.screens.LibraryViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.MoreVert
import com.example.beatpulse.ui.components.rememberAlbumArt
import com.example.beatpulse.ui.components.rememberAlbumArt
import com.example.beatpulse.ui.components.rememberTrackPalette
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    currentPlayingTrack: TrackEntity?,
    isPlaying: Boolean,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit
) {
    val prefs = viewModel.prefs
    val onRescan = { viewModel.scanMediaStore() }
    var selectedTabIndex by remember { mutableIntStateOf(prefs.lastLibraryGeneralTab) }
    LaunchedEffect(selectedTabIndex) {
        prefs.lastLibraryGeneralTab = selectedTabIndex
    }
    val tabs = listOf(stringResource(R.string.tab_all), stringResource(R.string.tab_browser), stringResource(R.string.tab_recommendations))

    val allTracks by viewModel.allTracks.collectAsState()
    val recentTracks by viewModel.recentTracks.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val resolvingTracks by viewModel.resolvingTracks.collectAsState()
    
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
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

    val deleteLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            trackToDelete?.let { track ->
                viewModel.completeDeletion(track.id)
                prefs.showToast("Canción eliminada")
                viewModel.scanMediaStore()
            }
        }
        trackToDelete = null
    }

    val isSearchingOnline = selectedTabIndex == 1
    val onlineSearchResults by viewModel.onlineSearchResults.collectAsState()
    val isOnlineSearchLoading by viewModel.isOnlineSearchLoading.collectAsState()
    
    val recommendations by viewModel.recommendations.collectAsState()
    val isRecommendationsLoading by viewModel.isRecommendationsLoading.collectAsState()

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 2) {
            viewModel.loadRecommendations()
        }
    }

    // Remove automatic LaunchedEffect search for online to prevent unnecessary API calls

    val bgStyle by prefs.backgroundStyleFlow.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()
    val dynamicTextColor = remember(bgStyle, paletteColors.dominant, isDarkTheme) {
        val isBackgroundLight = when (bgStyle) {
            0 -> !isDarkTheme
            1 -> false
            2, 4 -> paletteColors.dominant.luminance() > 0.3f
            3 -> true
            5, 6, 7, 8 -> false
            else -> paletteColors.dominant.luminance() > 0.5f
        }
        if (isBackgroundLight) Color(0xFF121212) else Color.White
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
                        TabRowDefaults.Indicator(
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

            if (isScanning || isOnlineSearchLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = paletteColors.vibrant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (isOnlineSearchLoading) androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.searching_online) else androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.searching_music), color = dynamicTextColor)
                    }
                }
            } else if (!isSearchingOnline && selectedTabIndex != 2 && currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay canciones aquí.", color = dynamicTextColor)
                }
            } else if (isSearchingOnline && onlineSearchResults.isEmpty() && onlineSearchQuery.isNotBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.no_results_online), color = dynamicTextColor)
                }
            } else {
                val shapeIdx by prefs.thumbnailShapeFlow.collectAsState(initial = 0)
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
                                    Text(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.searching_recommendations), color = dynamicTextColor, modifier = Modifier.padding(16.dp))
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
                                        text = if (remainingMs > 0) androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.next_update_in_h_m, remainingHours, remainingMinutes) else androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.updating_soon),
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
                                        itemsIndexed(tracks, key = { index, track -> "${category}_${track.id}_$index" }) { _, track ->
                                            TrackItem(
                                                track = track,
                                                paletteColors = paletteColors,
                                                thumbnailShapeIdx = shapeIdx,
                                                textColor = dynamicTextColor,
                                                onClick = { onTrackClick(track, tracks) },
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
                                                isActuallyPlaying = currentPlayingTrack?.id == track.id && isPlaying
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
                            items(itemsToShow, key = { it.id }) { track ->
                                TrackItem(
                                    track = track,
                                    paletteColors = paletteColors,
                                    thumbnailShapeIdx = shapeIdx,
                                    textColor = dynamicTextColor,
                                    onClick = { onTrackClick(track, itemsToShow) },
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
                                    isPlaying = currentPlayingTrack?.id == track.id,
                                    isActuallyPlaying = currentPlayingTrack?.id == track.id && isPlaying
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
        val isSearchExpanded = if (selectedTabIndex == 1) true else isLocalSearchExpanded
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
                     Icon(Icons.Default.Sort, contentDescription = "Sort", tint = colorVibrant)
                 }
                 androidx.compose.material3.MaterialTheme(
                     colorScheme = androidx.compose.material3.MaterialTheme.colorScheme.copy(
                         surface = paletteColors.dominant,
                         onSurface = paletteColors.vibrant
                     )
                 ) {
                     DropdownMenu(
                         expanded = isSortMenuExpanded,
                         onDismissRequest = { isSortMenuExpanded = false }
                     ) {
                         DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.sort_directory)) }, onClick = { sortOrder = "DIRECTORY"; prefs.librarySortOrder = "DIRECTORY"; isSortMenuExpanded = false })
                         DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.sort_title)) }, onClick = { sortOrder = "TITLE"; prefs.librarySortOrder = "TITLE"; isSortMenuExpanded = false })
                         DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.sort_artist)) }, onClick = { sortOrder = "ARTIST"; prefs.librarySortOrder = "ARTIST"; isSortMenuExpanded = false })
                         DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.sort_album)) }, onClick = { sortOrder = "ALBUM"; prefs.librarySortOrder = "ALBUM"; isSortMenuExpanded = false })
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
                     val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                     OutlinedTextField(
                         value = if (isSearchingOnline) onlineSearchQuery else localSearchQuery,
                         onValueChange = { if (isSearchingOnline) viewModel.searchQuery.value = it else localSearchQuery = it },
                         placeholder = { Text(if (isSearchingOnline) androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.search_online_dots) else androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.search_dots), color = Color.LightGray) },
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
                     IconButton(onClick = onRescan) {
                         Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = colorVibrant)
                     }
                 }
             }
        }
        
        trackToAddToPlaylist?.let { trackToAdd ->
            AlertDialog(
                onDismissRequest = { trackToAddToPlaylist = null },
                title = { Text(context.getString(R.string.add_to_playlist)) },
                text = {
                    if (playlists.isEmpty()) {
                        Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.no_playlists_created))
                    } else {
                        LazyColumn {
                            items(playlists) { pl ->
                                Text(
                                    text = pl.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addTrackToPlaylist(pl.playlistId, trackToAdd)
                                            prefs.showToast("Añadido a ${pl.name}")
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
                title = { Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.delete_track_title), color = dynamicTextColor) },
                text = { Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.delete_track_desc, track.title), color = dynamicTextColor) },
                confirmButton = {
                    TextButton(onClick = {
                        val t = track
                        trackPendingConfirmation = null
                        scope.launch {
                            val sender = viewModel.deleteTrack(t.id)
                            if (sender != null) {
                                trackToDelete = t
                                deleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(sender).build())
                            } else {
                                prefs.showToast("Canción eliminada")
                                viewModel.scanMediaStore()
                            }
                        }
                    }) {
                        Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.delete_button_red), color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { trackPendingConfirmation = null }) {
                        Text("Cancelar", color = colorVibrant)
                    }
                },
                containerColor = paletteColors.dominant
            )
        }

        trackPendingTrim?.let { track ->
            com.example.beatpulse.ui.components.library.AudioTrimmerDialog(
                track = track,
                onDismiss = { trackPendingTrim = null },
                onTrimSuccess = { newPath ->
                    viewModel.copyMetadataForTrimmedTrack(track, newPath)
                }
            )
        }

        trackPendingDownload?.let { track ->
            AlertDialog(
                onDismissRequest = { trackPendingDownload = null },
                title = { Text(stringResource(R.string.confirm_download), color = dynamicTextColor) },
                text = { Text(stringResource(R.string.confirm_download_desc, track.title), color = dynamicTextColor) },
                confirmButton = {
                    TextButton(onClick = {
                        trackPendingDownload = null
                        viewModel.downloadOnlineTrack(context, track)
                        prefs.showToast("Descargando ${track.title}...")
                    }) {
                        Text(stringResource(R.string.download), color = colorVibrant)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { trackPendingDownload = null }) {
                        Text(stringResource(R.string.cancel), color = dynamicTextColor)
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
                onDismiss = { trackToChangeCover = null },
                onCoverSelected = { newPath ->
                    viewModel.updateTrackCover(track, newPath)
                    trackToChangeCover = null
                }
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
    isActuallyPlaying: Boolean = false
) {
    // Usar la paleta global para evitar recalcular colores por cada pista, lo cual traba la lista
    val accentColor = paletteColors.vibrant
    val bgColor = paletteColors.dominant
    
    val bgBrush = remember(bgColor, accentColor) {
        Brush.linearGradient(colors = listOf(bgColor, accentColor.copy(alpha = 0.5f)))
    }
    
    val shape = remember(thumbnailShapeIdx) {
        when (thumbnailShapeIdx) {
            1 -> RoundedCornerShape(0.dp)
            2 -> RoundedCornerShape(10.dp)
            3 -> RoundedCornerShape(24.dp)
            else -> androidx.compose.foundation.shape.CircleShape
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val albumArt = rememberAlbumArt(track = track)
        
        Box(
            modifier = Modifier
                .size(52.dp)
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
                    modifier = Modifier.size(24.dp)
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isPlaying) accentColor else textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val displayArtist = track.customArtist ?: track.artist
            val displayAlbum = track.customAlbum ?: track.album
            Text(
                text = "$displayArtist • $displayAlbum",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (track.isFavorite) accentColor else Color.Gray
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
            var isMenuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                }
                androidx.compose.material3.MaterialTheme(
                    colorScheme = androidx.compose.material3.MaterialTheme.colorScheme.copy(
                        surface = paletteColors.dominant,
                        onSurface = paletteColors.vibrant
                    )
                ) {
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        if (onAddToPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_to_playlist)) },
                                onClick = {
                                    isMenuExpanded = false
                                    onAddToPlaylist()
                                }
                            )
                        }
                        if (onDeleteTrack != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_from_device)) },
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
                                text = { Text(stringResource(R.string.download_music)) },
                                onClick = {
                                    isMenuExpanded = false
                                    onDownloadTrack()
                                }
                            )
                        }
                        if (onTrimTrack != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.trim_audio)) },
                                onClick = {
                                    isMenuExpanded = false
                                    onTrimTrack()
                                }
                            )
                        }
                        if (onChangeCover != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.change_cover_title)) },
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
    viewModel: LibraryViewModel,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
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
        title = { Text(stringResource(R.string.change_cover_title), color = paletteColors.vibrant) },
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onCoverSelected(path) }
                            ) {
                                coil.compose.AsyncImage(
                                    model = path,
                                    contentDescription = "Cover option",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
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
