package com.example.beatpulse.ui.screens
import com.example.beatpulse.utils.getLocalizedString
import com.example.beatpulse.core.focus.animatedFocusBorder
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.input.key.*
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
import com.example.beatpulse.utils.SystemBackHandler
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.ui.viewmodels.PlaylistViewData
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.PixelIcons
import com.example.beatpulse.ui.screens.LibraryViewModel


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnifiedLibraryScreen(
    viewModel: com.example.beatpulse.ui.viewmodels.ILibraryViewModel,
    paletteColors: PaletteColors,
    currentPlayingTrack: TrackEntity?,
    isPlaying: Boolean,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit,
    statsViewModel: com.example.beatpulse.ui.screens.StatsViewModel
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
    val isOnlineServiceDown by viewModel.isOnlineServiceDown.collectAsState()

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

    var selectedViewData by viewModel.selectedViewData
    var isCreatingPlaylist by rememberSaveable { mutableStateOf(false) }
    var addingTracksToPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var trackToAddToPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
    
    var trackToDelete by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToChangeCover by remember { mutableStateOf<TrackEntity?>(null) }
    var trackPendingConfirmation by remember { mutableStateOf<TrackEntity?>(null) }
    var trackPendingTrim by remember { mutableStateOf<TrackEntity?>(null) }
    var showStats by remember { mutableStateOf(false) }
    var showDesignSettings by remember { mutableStateOf(false) }
    var showKeyboardSettings by remember { mutableStateOf(false) }
    var showEcosystemScreen by remember { mutableStateOf(false) }
    
    val trackDeletedMsg = getLocalizedString("track_deleted")

    val deleteLauncher = com.example.beatpulse.ui.utils.rememberTrackDeleteHandler(onDeleted = {
        trackToDelete?.let { track ->
            viewModel.completeDeletion(track.id)
            prefs.showToast(trackDeletedMsg)
            viewModel.scanMediaStore()
        }
        trackToDelete = null
    })

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
                    Text(getLocalizedString("searching_music"), color = dynamicTextColor)
                }
            }
        } else if (showEcosystemScreen) {
            SystemBackHandler { showEcosystemScreen = false }
            EcosystemScreen(onNavigateBack = { showEcosystemScreen = false })
        } else if (showStats) {
            SystemBackHandler { showStats = false }
            StatsScreen(
                paletteColors = paletteColors,
                prefs = viewModel.prefs,
                viewModel = statsViewModel
            )
        } else if (isCreatingPlaylist) {
            SystemBackHandler { isCreatingPlaylist = false }
            CreatePlaylistScreen(
                viewModel = viewModel,
                allTracks = allTracks,
                dynamicTextColor = dynamicTextColor,
                paletteColors = paletteColors,
                onClose = { isCreatingPlaylist = false }
            )
        } else if (addingTracksToPlaylistId != null) {
            val plId = addingTracksToPlaylistId!!
            SystemBackHandler { addingTracksToPlaylistId = null }
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
                            text = getLocalizedString("library"),
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
                                title = { Text(getLocalizedString("select_language")) },
                                text = {
                                    Column {
                                        listOf("es" to "🇪🇸 Español", "en" to "🇺🇸 English", "pt" to "🇧🇷 Português").forEach { (code, name) ->
                                            Text(
                                                text = name,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (prefs.appLanguage == code) return@clickable
                                                        prefs.appLanguage = code
                                                        showLanguageDialog = false
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
                                        Text(getLocalizedString("cancel"))
                                    }
                                },
                                containerColor = paletteColors.dominant,
                                titleContentColor = dynamicTextColor,
                                textContentColor = dynamicTextColor
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showSettingsMenu = true },
                                modifier = Modifier.background(color = paletteColors.dominant.copy(alpha = 0.6f), shape = CircleShape)
                            ) {
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
                                        text = { Text(if (filterWhatsApp) getLocalizedString("show_whatsapp_audio") else getLocalizedString("hide_whatsapp_audio")) },
                                        onClick = {
                                            prefs.filterWhatsAppShorts = !filterWhatsApp
                                            showSettingsMenu = false
                                            viewModel.scanMediaStore()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(getLocalizedString("language")) },
                                        onClick = {
                                            showSettingsMenu = false
                                            showLanguageDialog = true
                                        }
                                    )
                                    val showGestures = prefs.showGestureConfirmations
                                    DropdownMenuItem(
                                        text = { Text(getLocalizedString("gesture_confirmations_toggle") + ": " + if(showGestures) "ON" else "OFF") },
                                        onClick = {
                                            prefs.showGestureConfirmations = !showGestures
                                            showSettingsMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(getLocalizedString("scan_music_now")) },
                                        onClick = {
                                            showSettingsMenu = false
                                            onRescan()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Transferir música (Red Local/Online)") },
                                        onClick = {
                                            showSettingsMenu = false
                                            showEcosystemScreen = true
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showDesignSettings = true },
                            modifier = Modifier.background(color = paletteColors.dominant.copy(alpha = 0.6f), shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Ajustes de Diseño",
                                tint = paletteColors.vibrant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showKeyboardSettings = true },
                            modifier = Modifier.background(color = paletteColors.dominant.copy(alpha = 0.6f), shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Atajos de Teclado",
                                tint = paletteColors.vibrant
                            )
                        }

                        if (showKeyboardSettings) {
                            KeyboardSettingsDialog(
                                onDismiss = { showKeyboardSettings = false },
                                paletteColors = paletteColors,
                                prefs = prefs
                            )
                        }

                        if (showDesignSettings) {
                            DesignSettingsDialog(
                                prefs = prefs,
                                paletteColors = paletteColors,
                                dynamicTextColor = dynamicTextColor,
                                currentShapeIdx = shapeIdx,
                                currentBgStyle = bgStyle,
                                onDismiss = { showDesignSettings = false }
                            )
                        }
                    }
                }

                val pagerState = rememberPagerState(initialPage = prefs.lastLibraryTab.coerceIn(0, 3), pageCount = { 4 })
                LaunchedEffect(pagerState.currentPage) {
                    prefs.lastLibraryTab = pagerState.currentPage % 4
                }
                
                LaunchedEffect(Unit) {
                    com.example.beatpulse.core.focus.AppFocusManager.focusActions.collect { action ->
                        if (prefs.lastMainScreenPage == 0) { // Only if we are on the Library screen
                            when (action) {
                                com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_LIBRARY_PLAYLISTS -> pagerState.animateScrollToPage(0)
                                com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_LIBRARY_ARTISTS -> pagerState.animateScrollToPage(1)
                                com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_LIBRARY_ALBUMS -> pagerState.animateScrollToPage(2)
                                com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_LIBRARY_FOLDERS -> pagerState.animateScrollToPage(3)
                                com.example.beatpulse.core.focus.FocusAction.NAVIGATE_LEFT -> {
                                    if (!com.example.beatpulse.core.focus.AppFocusManager.isTabNavigationActive) {
                                        val prevPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                                        if (prevPage != pagerState.currentPage) pagerState.animateScrollToPage(prevPage)
                                    }
                                }
                                com.example.beatpulse.core.focus.FocusAction.NAVIGATE_RIGHT -> {
                                    if (!com.example.beatpulse.core.focus.AppFocusManager.isTabNavigationActive) {
                                        val nextPage = (pagerState.currentPage + 1).coerceAtMost(3)
                                        if (nextPage != pagerState.currentPage) pagerState.animateScrollToPage(nextPage)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    com.example.beatpulse.core.focus.AppFocusManager.appShortcuts.collect { shortcut ->
                        if (prefs.lastMainScreenPage == 0) {
                            when (shortcut) {
                                com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_LIBRARY_PLAYLISTS -> pagerState.scrollToPage(0)
                                com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_LIBRARY_ARTISTS -> pagerState.scrollToPage(1)
                                com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_LIBRARY_ALBUMS -> pagerState.scrollToPage(2)
                                com.example.beatpulse.core.focus.AppShortcut.NAVIGATE_LIBRARY_FOLDERS -> pagerState.scrollToPage(3)
                                com.example.beatpulse.core.focus.AppShortcut.OPEN_STATS -> showStats = true
                                com.example.beatpulse.core.focus.AppShortcut.OPEN_DESIGN_SETTINGS -> showDesignSettings = true
                                com.example.beatpulse.core.focus.AppShortcut.OPEN_KEYBOARD_SHORTCUTS -> showKeyboardSettings = true
                                else -> {}
                            }
                        }
                    }
                }

                androidx.compose.material3.ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage % 4,
                    containerColor = Color.Transparent,
                    contentColor = dynamicTextColor,
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage % 4]),
                            color = dynamicTextColor
                        )
                    }
                ) {
                    val tabTitles = listOf(getLocalizedString("tab_playlists"), getLocalizedString("tab_artists"), getLocalizedString("tab_albums"), getLocalizedString("tab_folders"))
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
                                    color = if ((pagerState.currentPage % 4) == index) dynamicTextColor else dynamicTextColor.copy(alpha = 0.6f),
                                    fontWeight = if ((pagerState.currentPage % 4) == index) FontWeight.Bold else FontWeight.Normal
                                ) 
                            }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                        .onKeyEvent { event ->
                            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                                if (event.key == androidx.compose.ui.input.key.Key.DirectionRight || event.key == androidx.compose.ui.input.key.Key.DirectionLeft) {
                                    return@onKeyEvent true // Guard against native HorizontalPager edge crash
                                }
                            }
                            false
                        }
                        .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while(true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                    event.changes.forEach { it.consume() }
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            }
                        }
                    }
                ) { page ->
                    when (page) {
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
            SystemBackHandler { selectedViewData = null }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = dynamicTextColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentViewData.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = dynamicTextColor,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = { viewModel.reloadMissingCoversForList(tracksToDisplay) }) {
                        Icon(Icons.Default.ImageSearch, contentDescription = getLocalizedString("reload_covers"), tint = paletteColors.vibrant)
                    }

                    if (currentViewData.playlistId == null) {
                        Box {
                             IconButton(onClick = { isSortMenuExpanded = true }) {
                                 Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = paletteColors.vibrant)
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
                                     DropdownMenuItem(text = { Text(getLocalizedString("sort_directory")) }, onClick = { sortOrder = "DIRECTORY"; prefs.librarySortOrder = "DIRECTORY"; isSortMenuExpanded = false })
                                     DropdownMenuItem(text = { Text(getLocalizedString("sort_title")) }, onClick = { sortOrder = "TITLE"; prefs.librarySortOrder = "TITLE"; isSortMenuExpanded = false })
                                     DropdownMenuItem(text = { Text(getLocalizedString("sort_artist")) }, onClick = { sortOrder = "ARTIST"; prefs.librarySortOrder = "ARTIST"; isSortMenuExpanded = false })
                                     DropdownMenuItem(text = { Text(getLocalizedString("sort_album")) }, onClick = { sortOrder = "ALBUM"; prefs.librarySortOrder = "ALBUM"; isSortMenuExpanded = false })
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
                    placeholder = { Text(getLocalizedString("search_in_category").replace("%s", currentViewData.title), color = dynamicTextColor.copy(alpha=0.5f)) },
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
                                        onClick = { 
                                            if (isOnlineServiceDown && track.dataPath.startsWith("youtube://")) {
                                                // Do nothing
                                            } else {
                                                onTrackClick(track, tracksToDisplay)
                                            }
                                        },
                                        onToggleFavorite = {
                                            viewModel.toggleFavorite(track, !track.isFavorite)
                                        },
                                        onChangeCover = { trackToChangeCover = track },
                                        onAddToPlaylist = { trackToAddToPlaylist = track },
                                        onDeleteTrack = if (currentViewData.playlistId == null) { { trackPendingConfirmation = track } } else null,
                                        onRemoveFromPlaylist = if (currentViewData.playlistId != null) { { viewModel.removeTrackFromPlaylist(currentViewData.playlistId, track.id) } } else null,
                                        onTrimTrack = if (!track.dataPath.startsWith("http")) { { trackPendingTrim = track } } else null,
                                        isPlaying = currentPlayingTrack?.id == track.id,
                                        isActuallyPlaying = currentPlayingTrack?.id == track.id && isPlaying,
                                        isServiceDown = isOnlineServiceDown
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
                        },
                        colorVibrant = paletteColors.vibrant,
                        colorSurface = paletteColors.dominant.copy(alpha = 0.95f),
                        colorText = dynamicTextColor
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
                                                    prefs.showToast("Añadida a playlist")
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
                        title = { Text(getLocalizedString("delete_track_title"), color = dynamicTextColor) },
                        text = { Text((getLocalizedString("delete_track_desc") + " " + track.title), color = dynamicTextColor) },
                        confirmButton = {
                            TextButton(onClick = {
                                val t = track
                                trackPendingConfirmation = null
                                coroutineScope.launch {
                                    val sender = viewModel.deleteTrack(t.id)
                                    if (sender != null) {
                                        trackToDelete = t
                                        deleteLauncher(t.id, sender)
                                    } else {
                                        prefs.showToast(trackDeletedMsg)
                                        viewModel.scanMediaStore()
                                    }
                                }
                            }) {
                                Text(getLocalizedString("delete_button_red"), color = Color.Red)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { trackPendingConfirmation = null }) {
                                Text(getLocalizedString("cancel"), color = paletteColors.vibrant)
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
    viewModel: com.example.beatpulse.ui.viewmodels.ILibraryViewModel,
) {
    val titleAllSongs = getLocalizedString("all_songs_title")
    val titleRecent = getLocalizedString("recent_plays_title")
    val titleFavorites = getLocalizedString("favorites_title")
    val titleTop = getLocalizedString("top_plays_title")
    val titleAdded = getLocalizedString("recently_added_title")
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
    ) {
        item {
            Text(
                text = getLocalizedString("general_lists"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = dynamicTextColor,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
        
        item {
            PlaylistFolderItem(
                title = titleAllSongs,
                count = allTracks.size,
                icon = Icons.Default.MusicNote,
                tint = paletteColors.vibrant,
                textColor = dynamicTextColor,
                onClick = { onPlaylistSelected(PlaylistViewData(titleAllSongs, allTracks)) }
            )
        }
        
        if (recentTracks.isNotEmpty()) {
            item {
                PlaylistFolderItem(
                    title = titleRecent,
                    count = recentTracks.size,
                    icon = Icons.Default.History,
                    tint = paletteColors.vibrant,
                    textColor = dynamicTextColor,
                    onClick = { onPlaylistSelected(PlaylistViewData(titleRecent, recentTracks)) }
                )
            }
        }
        
        if (favoriteTracks.isNotEmpty()) {
            item {
                PlaylistFolderItem(
                    title = titleFavorites,
                    count = favoriteTracks.size,
                    icon = Icons.Default.Favorite,
                    tint = paletteColors.vibrant,
                    textColor = dynamicTextColor,
                    onClick = { onPlaylistSelected(PlaylistViewData(titleFavorites, favoriteTracks)) }
                )
            }
        }
        
        if (topTracks.isNotEmpty()) {
            item {
                PlaylistFolderItem(
                    title = titleTop,
                    count = topTracks.size,
                    icon = Icons.Default.Star,
                    tint = paletteColors.vibrant,
                    textColor = dynamicTextColor,
                    onClick = { onPlaylistSelected(PlaylistViewData(titleTop, topTracks)) }
                )
            }
        }
        
        if (recentlyAdded.isNotEmpty()) {
            item {
                PlaylistFolderItem(
                    title = titleAdded,
                    count = recentlyAdded.size,
                    icon = Icons.Default.NewReleases,
                    tint = paletteColors.vibrant,
                    textColor = dynamicTextColor,
                    onClick = { onPlaylistSelected(PlaylistViewData(titleAdded, recentlyAdded)) }
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = getLocalizedString("my_lists"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = dynamicTextColor,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
        
        itemsIndexed(items = playlists, key = { _, it -> "pl_${it.playlistId}" }) { index, pl ->
            val trackCount by viewModel.getPlaylistTrackCountFlow(pl.playlistId).collectAsState(initial = 0)
            PlaylistFolderItem(
                title = pl.name,
                count = trackCount,
                icon = Icons.AutoMirrored.Filled.QueueMusic,
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
                Text(getLocalizedString("create_playlist"))
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
