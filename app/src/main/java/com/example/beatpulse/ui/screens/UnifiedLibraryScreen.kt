package com.example.beatpulse.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.PreferencesManager
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.PixelIcons
import com.example.beatpulse.ui.screens.LibraryViewModel

data class PlaylistViewData(val title: String, val tracks: List<TrackEntity>, val playlistId: Long? = null, val filterType: Int? = null, val filterValue: String? = null)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnifiedLibraryScreen(
    viewModel: LibraryViewModel,
    paletteColors: PaletteColors,
    currentPlayingTrack: TrackEntity?,
    isPlaying: Boolean,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit
) {
    val prefs = viewModel.prefs
    val onRescan = { viewModel.scanMediaStore() }
    val allTracks by viewModel.allTracks.collectAsState()
    val recentTracks by viewModel.recentTracks.collectAsState()
    val topTracks by viewModel.topTracks.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val bgStyle by prefs.backgroundStyleFlow.collectAsState()
    val shapeIdx by prefs.thumbnailShapeFlow.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    val isScanning by viewModel.isScanning.collectAsState()

    val isDarkTheme = isSystemInDarkTheme()
    val context = androidx.compose.ui.platform.LocalContext.current
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

    var selectedViewData by viewModel.selectedViewData
    var isCreatingPlaylist by rememberSaveable { mutableStateOf(false) }
    var addingTracksToPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var trackToAddToPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
    
    var trackToDelete by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToChangeCover by remember { mutableStateOf<TrackEntity?>(null) }
    var trackPendingConfirmation by remember { mutableStateOf<TrackEntity?>(null) }
    var trackPendingTrim by remember { mutableStateOf<TrackEntity?>(null) }
    var showStats by remember { mutableStateOf(false) }
    
    val deleteLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            trackToDelete?.let { track ->
                viewModel.completeDeletion(track.id)
                prefs.showToast(context.getString(com.example.beatpulse.R.string.track_deleted))
                viewModel.scanMediaStore()
            }
        }
        trackToDelete = null
    }

    // Global Search State
    var globalSearchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var onlineSearchResults by remember { mutableStateOf<List<TrackEntity>>(emptyList()) }
    var isOnlineSearchLoading by remember { mutableStateOf(false) }
    
    // Filter local tracks
    val localSearchResults = remember(globalSearchQuery, allTracks) {
        if (globalSearchQuery.isBlank()) emptyList()
        else allTracks.filter { 
            it.title.contains(globalSearchQuery, ignoreCase = true) || 
            it.artist.contains(globalSearchQuery, ignoreCase = true) 
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isScanning && allTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = paletteColors.vibrant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.searching_music), color = dynamicTextColor)
                }
            }
        } else if (showStats) {
            BackHandler { showStats = false }
            StatsScreen(
                paletteColors = paletteColors,
                prefs = viewModel.prefs
            )
        } else if (isCreatingPlaylist) {
            BackHandler { isCreatingPlaylist = false }
            CreatePlaylistScreen(
                viewModel = viewModel,
                allTracks = allTracks,
                dynamicTextColor = dynamicTextColor,
                paletteColors = paletteColors,
                onClose = { isCreatingPlaylist = false }
            )
        } else if (addingTracksToPlaylistId != null) {
            val plId = addingTracksToPlaylistId!!
            BackHandler { addingTracksToPlaylistId = null }
            AddTracksScreen(
                viewModel = viewModel,
                playlistId = plId,
                allTracks = allTracks,
                dynamicTextColor = dynamicTextColor,
                paletteColors = paletteColors,
                onClose = { addingTracksToPlaylistId = null }
            )
        } else if (selectedViewData == null) {
            // Main View with Horizontal Pager
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Biblioteca title with sparkle animation — tapping opens Stats
                    Row(
                        modifier = Modifier.clickable { showStats = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.library),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = dynamicTextColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Animated sparkle star — simple pulse
                        var sparkleTarget by remember { mutableStateOf(true) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                kotlinx.coroutines.delay(1200)
                                sparkleTarget = !sparkleTarget
                            }
                        }
                        val sparkleScale by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (sparkleTarget) 1.2f else 0.7f,
                            animationSpec = androidx.compose.animation.core.tween(1200, easing = FastOutSlowInEasing),
                            label = "sparkle_scale"
                        )
                        val sparkleAlpha by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (sparkleTarget) 1.0f else 0.4f,
                            animationSpec = androidx.compose.animation.core.tween(1200, easing = FastOutSlowInEasing),
                            label = "sparkle_alpha"
                        )
                        val sparkleRotation by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (sparkleTarget) 15f else -15f,
                            animationSpec = androidx.compose.animation.core.tween(1800, easing = FastOutSlowInEasing),
                            label = "sparkle_rotation"
                        )
                        Text(
                            text = "✨",
                            fontSize = 18.sp,
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleX = sparkleScale,
                                    scaleY = sparkleScale,
                                    alpha = sparkleAlpha,
                                    rotationZ = sparkleRotation
                                )
                        )
                    }
                    Row {
                        var showSettingsMenu by remember { mutableStateOf(false) }
                        var showLanguageDialog by remember { mutableStateOf(false) }

                        if (showLanguageDialog) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { showLanguageDialog = false },
                                title = { Text(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.select_language)) },
                                text = {
                                    Column {
                                        listOf("es" to "🇪🇸 Español", "en" to "🇺🇸 English", "pt" to "🇧🇷 Português").forEach { (code, name) ->
                                            Text(
                                                text = name,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        prefs.appLanguage = code
                                                        showLanguageDialog = false
                                                        if (context is android.app.Activity) {
                                                            context.recreate()
                                                        }
                                                    }
                                                    .padding(16.dp),
                                                fontSize = 18.sp,
                                                color = if (prefs.appLanguage == code) paletteColors.vibrant else dynamicTextColor
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showLanguageDialog = false }) {
                                        Text(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.cancel))
                                    }
                                },
                                containerColor = paletteColors.dominant,
                                titleContentColor = dynamicTextColor,
                                textContentColor = dynamicTextColor
                            )
                        }

                        Box {
                            IconButton(onClick = { showSettingsMenu = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = paletteColors.vibrant)
                            }
                            androidx.compose.material3.MaterialTheme(
                                colorScheme = androidx.compose.material3.MaterialTheme.colorScheme.copy(
                                    surface = paletteColors.dominant,
                                    onSurface = paletteColors.vibrant
                                )
                            ) {
                                DropdownMenu(
                                    expanded = showSettingsMenu,
                                    onDismissRequest = { showSettingsMenu = false }
                                ) {
                                    val filterWhatsApp = prefs.filterWhatsAppShorts
                                    DropdownMenuItem(
                                        text = { Text(if (filterWhatsApp) androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.show_whatsapp_audio) else androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.hide_whatsapp_audio)) },
                                        onClick = {
                                            prefs.filterWhatsAppShorts = !filterWhatsApp
                                            showSettingsMenu = false
                                            viewModel.scanMediaStore()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.language)) },
                                        onClick = {
                                            showSettingsMenu = false
                                            showLanguageDialog = true
                                        }
                                    )
                                    val showGestures = prefs.showGestureConfirmations
                                    DropdownMenuItem(
                                        text = { Text(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.gesture_confirmations_toggle) + ": " + if(showGestures) "ON" else "OFF") },
                                        onClick = {
                                            prefs.showGestureConfirmations = !showGestures
                                            showSettingsMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.scan_music_now)) },
                                        onClick = {
                                            showSettingsMenu = false
                                            onRescan()
                                        }
                                    )
                                }
                            }
                        }
                        
                        IconButton(onClick = {
                            val newShape = (shapeIdx + 1) % 4
                            prefs.thumbnailShape = newShape
                            prefs.showToast(when(newShape) {
                                0 -> context.getString(com.example.beatpulse.R.string.shape_circle)
                                1 -> context.getString(com.example.beatpulse.R.string.shape_square)
                                2 -> context.getString(com.example.beatpulse.R.string.shape_rounded)
                                3 -> context.getString(com.example.beatpulse.R.string.shape_squircle)
                                else -> "Forma"
                            })
                        }) {
                            Icon(
                                imageVector = when(shapeIdx) {
                                    0 -> Icons.Default.Circle
                                    1 -> Icons.Default.CropSquare
                                    2 -> Icons.Default.RoundedCorner
                                    3 -> Icons.Default.Crop
                                    else -> Icons.Default.Circle
                                },
                                contentDescription = "Toggle Shape",
                                tint = paletteColors.vibrant
                            )
                        }
                        
                        IconButton(onClick = {
                            val newStyle = (bgStyle + 1) % 9
                            prefs.backgroundStyle = newStyle
                            prefs.showToast(when(newStyle) {
                                0 -> context.getString(com.example.beatpulse.R.string.style_classic)
                                1 -> context.getString(com.example.beatpulse.R.string.style_cyberpunk)
                                2 -> context.getString(com.example.beatpulse.R.string.style_anime)
                                3 -> context.getString(com.example.beatpulse.R.string.style_luminous)
                                4 -> context.getString(com.example.beatpulse.R.string.style_kawaii)
                                5 -> context.getString(com.example.beatpulse.R.string.style_black_metal)
                                6 -> context.getString(com.example.beatpulse.R.string.style_dark_fantasy)
                                7 -> context.getString(com.example.beatpulse.R.string.style_cathedral)
                                8 -> context.getString(com.example.beatpulse.R.string.style_hearts)
                                else -> "Estilo Modificado"
                            })
                        }) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Toggle Background Style",
                                tint = paletteColors.vibrant
                            )
                        }
                    }
                }

                val prefs = remember { com.example.beatpulse.data.PreferencesManager.getInstance(context) }
                val initialPage = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % 4) + prefs.lastLibraryTab
                val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })
                LaunchedEffect(pagerState.currentPage) {
                    prefs.lastLibraryTab = pagerState.currentPage % 4
                }

                androidx.compose.material3.ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage % 4,
                    containerColor = Color.Transparent,
                    contentColor = paletteColors.vibrant,
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage % 4]),
                            color = paletteColors.vibrant,
                            height = 3.dp
                        )
                    }
                ) {
                    val tabTitles = listOf(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.tab_playlists), androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.tab_artists), androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.tab_albums), androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.tab_folders))
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = (pagerState.currentPage % 4) == index,
                            onClick = { 
                                coroutineScope.launch { 
                                    // animate to the nearest corresponding page
                                    val current = pagerState.currentPage
                                    val offset = index - (current % 4)
                                    val targetPage = current + offset
                                    pagerState.animateScrollToPage(targetPage)
                                } 
                            },
                            text = { 
                                Text(
                                    title, 
                                    color = if ((pagerState.currentPage % 4) == index) paletteColors.vibrant else dynamicTextColor.copy(alpha = 0.6f),
                                    fontWeight = if ((pagerState.currentPage % 4) == index) FontWeight.Bold else FontWeight.Normal
                                ) 
                            }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page % 4) {
                        0 -> ListsSubPage(
                            allTracks = allTracks,
                            recentTracks = recentTracks,
                            topTracks = topTracks,
                            recentlyAdded = recentlyAdded,
                            favoriteTracks = favoriteTracks,
                            playlists = playlists,
                            paletteColors = paletteColors,
                            dynamicTextColor = dynamicTextColor,
                            onPlaylistSelected = { selectedViewData = it },
                            onCreatePlaylist = { isCreatingPlaylist = true },
                            viewModel = viewModel
                        )
                        1 -> CategorySubPage(
                            categoryType = 0,
                            allTracks = allTracks,
                            paletteColors = paletteColors,
                            dynamicTextColor = dynamicTextColor,
                            bgStyle = bgStyle,
                            onCategorySelected = { selectedViewData = it }
                        )
                        2 -> CategorySubPage(
                            categoryType = 1,
                            allTracks = allTracks,
                            paletteColors = paletteColors,
                            dynamicTextColor = dynamicTextColor,
                            bgStyle = bgStyle,
                            onCategorySelected = { selectedViewData = it }
                        )
                        3 -> CategorySubPage(
                            categoryType = 2,
                            allTracks = allTracks,
                            paletteColors = paletteColors,
                            dynamicTextColor = dynamicTextColor,
                            bgStyle = bgStyle,
                            onCategorySelected = { selectedViewData = it }
                        )
                    }
                }
            }
        } else {
            // Detailed View for Selected Playlist/Category
            BackHandler { selectedViewData = null }
            val currentViewData = selectedViewData!!
            
            val dbTracks by if (currentViewData.playlistId != null) {
                viewModel.getTracksForPlaylist(currentViewData.playlistId).collectAsState(initial = emptyList())
            } else {
                remember { mutableStateOf(emptyList()) }
            }
            
            val tracksToDisplayRaw = if (currentViewData.playlistId != null) {
                dbTracks
            } else if (currentViewData.filterType != null && currentViewData.filterValue != null) {
                allTracks.filter { track ->
                    when (currentViewData.filterType) {
                        0 -> track.artist == currentViewData.filterValue
                        1 -> track.album == currentViewData.filterValue
                        2 -> track.folderPath == currentViewData.filterValue
                        else -> false
                    }
                }
            } else {
                currentViewData.tracks
            }
            
            // Search and sorting for detailed view
            var searchQuery by rememberSaveable { mutableStateOf("") }
            var sortOrder by rememberSaveable { mutableStateOf(prefs.librarySortOrder) }
            var isSortMenuExpanded by remember { mutableStateOf(false) }

            val tracksToDisplay = remember(tracksToDisplayRaw, searchQuery, sortOrder) {
                val list = tracksToDisplayRaw.filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    it.artist.contains(searchQuery, ignoreCase = true) 
                }
                if (currentViewData.playlistId != null) {
                    list // Maintain custom order for playlists
                } else {
                    when (sortOrder) {
                        "TITLE" -> list.sortedBy { it.title.lowercase() }
                        "ARTIST" -> list.sortedBy { it.artist.lowercase() }
                        "ALBUM" -> list.sortedBy { it.album.lowercase() }
                        else -> list
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedViewData = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = dynamicTextColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentViewData.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = dynamicTextColor,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = { viewModel.reloadMissingCoversForList(tracksToDisplay) }) {
                        Icon(Icons.Default.ImageSearch, contentDescription = context.getString(com.example.beatpulse.R.string.reload_covers), tint = paletteColors.vibrant)
                    }

                    if (currentViewData.playlistId == null) {
                        Box {
                             IconButton(onClick = { isSortMenuExpanded = true }) {
                                 Icon(Icons.Default.Sort, contentDescription = "Sort", tint = paletteColors.vibrant)
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
                    }

                    if (currentViewData.playlistId != null) {
                        IconButton(onClick = { addingTracksToPlaylistId = currentViewData.playlistId }) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir canciones", tint = paletteColors.vibrant)
                        }
                    }
                }
                
                // Optional Search Bar in detailed view
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.search_in_category, currentViewData.title), color = dynamicTextColor.copy(alpha=0.5f)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = dynamicTextColor.copy(alpha=0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = dynamicTextColor,
                        unfocusedTextColor = dynamicTextColor,
                        focusedBorderColor = paletteColors.vibrant,
                        unfocusedBorderColor = dynamicTextColor.copy(alpha = 0.2f)
                    )
                )

                val listState = rememberLazyListState()
                val showFastScroll by remember { derivedStateOf { listState.firstVisibleItemIndex > 5 } }


                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(tracksToDisplay.size, key = { tracksToDisplay[it].id }) { index ->
                            val track = tracksToDisplay[index]
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    TrackItem(
                                        track = track,
                                        paletteColors = paletteColors,
                                        thumbnailShapeIdx = shapeIdx,
                                        textColor = dynamicTextColor,
                                        onClick = { onTrackClick(track, tracksToDisplay) },
                                        onToggleFavorite = {
                                            viewModel.toggleFavorite(track, !track.isFavorite)
                                        },
                                        onChangeCover = { trackToChangeCover = track },
                                        onAddToPlaylist = { trackToAddToPlaylist = track },
                                        onDeleteTrack = if (currentViewData.playlistId == null) { { trackPendingConfirmation = track } } else null,
                                        onRemoveFromPlaylist = if (currentViewData.playlistId != null) { { viewModel.removeTrackFromPlaylist(currentViewData.playlistId, track.id) } } else null,
                                        onTrimTrack = if (!track.dataPath.startsWith("http")) { { trackPendingTrim = track } } else null,
                                        isPlaying = currentPlayingTrack?.id == track.id,
                                        isActuallyPlaying = currentPlayingTrack?.id == track.id && isPlaying
                                    )
                                }
                                if (currentViewData.playlistId != null) {
                                    Column {
                                        if (index > 0) {
                                            IconButton(onClick = {
                                                val prevTrack = tracksToDisplay[index - 1]
                                                viewModel.updatePlaylistOrder(
                                                    currentViewData.playlistId, 
                                                    listOf(Pair(track.id, index - 1), Pair(prevTrack.id, index))
                                                )
                                            }) {
                                                Icon(Icons.Default.KeyboardArrowUp, tint = dynamicTextColor, contentDescription = "Arriba")
                                            }
                                        }
                                        if (index < tracksToDisplay.size - 1) {
                                            IconButton(onClick = {
                                                val nextTrack = tracksToDisplay[index + 1]
                                                viewModel.updatePlaylistOrder(
                                                    currentViewData.playlistId, 
                                                    listOf(Pair(track.id, index + 1), Pair(nextTrack.id, index))
                                                )
                                            }) {
                                                Icon(Icons.Default.KeyboardArrowDown, tint = dynamicTextColor, contentDescription = "Abajo")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showFastScroll,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .padding(bottom = 120.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                            containerColor = paletteColors.vibrant,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Ir arriba")
                        }
                    }
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

                trackToChangeCover?.let { track ->
                    com.example.beatpulse.ui.screens.ChangeCoverDialog(
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

                trackToAddToPlaylist?.let { trackToAdd ->
                    AlertDialog(
                        onDismissRequest = { trackToAddToPlaylist = null },
                        title = { Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.add_to_playlist)) },
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
                                                    prefs.showToast(context.getString(com.example.beatpulse.R.string.added_to_playlist, pl.name))
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
                                Text("Cerrar", color = paletteColors.vibrant)
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
                                coroutineScope.launch {
                                    val sender = viewModel.deleteTrack(t.id)
                                    if (sender != null) {
                                        trackToDelete = t
                                        deleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(sender).build())
                                    } else {
                                        prefs.showToast(context.getString(com.example.beatpulse.R.string.track_deleted))
                                        viewModel.scanMediaStore()
                                    }
                                }
                            }) {
                                Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.delete_button_red), color = Color.Red)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { trackPendingConfirmation = null }) {
                                Text("Cancelar", color = paletteColors.vibrant)
                            }
                        },
                        containerColor = paletteColors.dominant
                    )
                }
            }
        }
    }
}

@Composable
fun ListsSubPage(
    allTracks: List<TrackEntity>,
    recentTracks: List<TrackEntity>,
    topTracks: List<TrackEntity>,
    recentlyAdded: List<TrackEntity>,
    favoriteTracks: List<TrackEntity>,
    playlists: List<com.example.beatpulse.data.PlaylistEntity>,
    paletteColors: PaletteColors,
    dynamicTextColor: Color,
    onPlaylistSelected: (PlaylistViewData) -> Unit,
    onCreatePlaylist: () -> Unit,
    viewModel: LibraryViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
    ) {
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.general_lists),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = dynamicTextColor,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
        
        item {
            PlaylistFolderItem(
                title = androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.all_songs_title),
                count = allTracks.size,
                icon = Icons.Default.MusicNote,
                tint = paletteColors.vibrant,
                textColor = dynamicTextColor,
                onClick = { onPlaylistSelected(PlaylistViewData(context.getString(com.example.beatpulse.R.string.all_songs_title), allTracks)) }
            )
        }
        
        if (recentTracks.isNotEmpty()) {
            item {
                PlaylistFolderItem(
                    title = androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.recent_plays_title),
                    count = recentTracks.size,
                    icon = Icons.Default.History,
                    tint = paletteColors.vibrant,
                    textColor = dynamicTextColor,
                    onClick = { onPlaylistSelected(PlaylistViewData(context.getString(com.example.beatpulse.R.string.recent_plays_title), recentTracks)) }
                )
            }
        }
        
        if (favoriteTracks.isNotEmpty()) {
            item {
                PlaylistFolderItem(
                    title = androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.favorites_title),
                    count = favoriteTracks.size,
                    icon = Icons.Default.Favorite,
                    tint = paletteColors.vibrant,
                    textColor = dynamicTextColor,
                    onClick = { onPlaylistSelected(PlaylistViewData(context.getString(com.example.beatpulse.R.string.favorites_title), favoriteTracks)) }
                )
            }
        }
        
        if (topTracks.isNotEmpty()) {
            item {
                PlaylistFolderItem(
                    title = androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.top_plays_title),
                    count = topTracks.size,
                    icon = Icons.Default.Star,
                    tint = paletteColors.vibrant,
                    textColor = dynamicTextColor,
                    onClick = { onPlaylistSelected(PlaylistViewData(context.getString(com.example.beatpulse.R.string.top_plays_title), topTracks)) }
                )
            }
        }
        
        if (recentlyAdded.isNotEmpty()) {
            item {
                PlaylistFolderItem(
                    title = androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.recently_added_title),
                    count = recentlyAdded.size,
                    icon = Icons.Default.NewReleases,
                    tint = paletteColors.vibrant,
                    textColor = dynamicTextColor,
                    onClick = { onPlaylistSelected(PlaylistViewData(context.getString(com.example.beatpulse.R.string.recently_added_title), recentlyAdded)) }
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.my_lists),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = dynamicTextColor,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
        
        items(playlists, key = { "pl_${it.playlistId}" }) { pl ->
            val trackCount by viewModel.getPlaylistTrackCountFlow(pl.playlistId).collectAsState(initial = 0)
            PlaylistFolderItem(
                title = pl.name,
                count = trackCount,
                icon = Icons.Default.QueueMusic,
                tint = paletteColors.vibrant,
                textColor = dynamicTextColor,
                onClick = { onPlaylistSelected(PlaylistViewData(pl.name, emptyList(), pl.playlistId)) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreatePlaylist,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = paletteColors.vibrant)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(androidx.compose.ui.res.stringResource(com.example.beatpulse.R.string.create_playlist))
            }
        }
    }
}

@Composable
fun CategorySubPage(
    categoryType: Int, // 0 = Artistas, 1 = Albumes, 2 = Carpetas
    allTracks: List<TrackEntity>,
    paletteColors: PaletteColors,
    dynamicTextColor: Color,
    bgStyle: Int,
    onCategorySelected: (PlaylistViewData) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val itemsList = remember(categoryType, allTracks) {
            when (categoryType) {
                0 -> allTracks.groupBy { it.artist }.toSortedMap()
                1 -> allTracks.groupBy { it.album }.toSortedMap()
                2 -> allTracks.groupBy { it.folderPath }.toSortedMap()
                else -> emptyMap()
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
        ) {
            items(itemsList.keys.toList(), key = { "cat_$it" }) { key ->
                val categoryTracks = itemsList[key] ?: emptyList()
                val icon = when (categoryType) {
                    0 -> Icons.Default.Person
                    1 -> Icons.Default.Album
                    2 -> if (bgStyle == 8) PixelIcons.Folder else Icons.Default.Folder
                    else -> Icons.Default.Folder
                }
                val displayTitle = if (categoryType == 2) key.substringAfterLast("/") else key
                
                PlaylistFolderItem(
                    title = displayTitle.ifEmpty { "Desconocido" },
                    count = categoryTracks.size,
                    icon = icon,
                    tint = paletteColors.vibrant,
                    textColor = dynamicTextColor,
                    onClick = { 
                        onCategorySelected(
                            PlaylistViewData(
                                title = displayTitle.ifEmpty { "Desconocido" }, 
                                tracks = categoryTracks,
                                filterType = categoryType,
                                filterValue = key
                            )
                        ) 
                    }
                )
            }
        }
    }
}
