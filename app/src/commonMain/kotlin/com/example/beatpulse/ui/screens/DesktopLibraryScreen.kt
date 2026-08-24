package com.example.beatpulse.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.player.IPlayerViewModel
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.PixelIcons
import com.example.beatpulse.ui.viewmodels.ILibraryViewModel
import com.example.beatpulse.ui.viewmodels.PlaylistViewData
import com.example.beatpulse.ui.components.DesktopTrackItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopLibraryScreen(
    libraryViewModel: ILibraryViewModel,
    playerViewModel: IPlayerViewModel,
    paletteColors: PaletteColors,
    onPageChange: (Int) -> Unit
) {
    val prefs = libraryViewModel.prefs
    val allTracks by libraryViewModel.allTracks.collectAsState()
    val playlists by libraryViewModel.playlists.collectAsState()
    val bgStyle by prefs.backgroundStyleFlow.collectAsState(initial = 0)
    val shapeIdx by prefs.thumbnailShapeFlow.collectAsState(initial = 2)

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

    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedViewData by remember { mutableStateOf<PlaylistViewData?>(null) }
    val scope = rememberCoroutineScope()

    if (selectedViewData == null) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Biblioteca",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = dynamicTextColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    var sparkleTarget by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(1200)
                            sparkleTarget = !sparkleTarget
                        }
                    }
                    val sparkleScale by animateFloatAsState(
                        targetValue = if (sparkleTarget) 1.2f else 0.7f,
                        animationSpec = tween(1200, easing = FastOutSlowInEasing),
                        label = "sparkle_scale"
                    )
                    val sparkleAlpha by animateFloatAsState(
                        targetValue = if (sparkleTarget) 1.0f else 0.4f,
                        animationSpec = tween(1200, easing = FastOutSlowInEasing),
                        label = "sparkle_alpha"
                    )
                    Text(
                        text = "✨",
                        fontSize = 18.sp,
                        modifier = Modifier.graphicsLayer(
                            scaleX = sparkleScale,
                            scaleY = sparkleScale,
                            alpha = sparkleAlpha
                        )
                    )
                }

                Row {
                    IconButton(onClick = {
                        val newShape = (shapeIdx + 1) % 4
                        prefs.thumbnailShape = newShape
                    }) {
                        Icon(
                            imageVector = when(shapeIdx) {
                                0 -> Icons.Default.Circle
                                1 -> Icons.Default.CropSquare
                                2 -> Icons.Default.RoundedCorner
                                3 -> Icons.Default.Crop
                                else -> Icons.Default.Circle
                            },
                            contentDescription = "Forma",
                            tint = paletteColors.vibrant
                        )
                    }

                    IconButton(onClick = {
                        val newStyle = (bgStyle + 1) % 9
                        prefs.backgroundStyle = newStyle
                    }) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Estilo",
                            tint = paletteColors.vibrant
                        )
                    }

                    IconButton(onClick = {
                        libraryViewModel.scanMediaStore()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Actualizar",
                            tint = paletteColors.vibrant
                        )
                    }
                }
            }

            // Tabs
            val tabs = listOf("Playlists", "Artistas", "Álbumes", "Carpetas")
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = paletteColors.vibrant,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = paletteColors.vibrant,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTabIndex == index) paletteColors.vibrant else dynamicTextColor.copy(alpha = 0.6f),
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                when (selectedTabIndex) {
                    0 -> DesktopPlaylistsTab(playlists, paletteColors, dynamicTextColor, onCategorySelected = { selectedViewData = it })
                    1 -> DesktopCategoryTab(0, allTracks, paletteColors, dynamicTextColor, bgStyle, onCategorySelected = { selectedViewData = it })
                    2 -> DesktopCategoryTab(1, allTracks, paletteColors, dynamicTextColor, bgStyle, onCategorySelected = { selectedViewData = it })
                    3 -> DesktopCategoryTab(2, allTracks, paletteColors, dynamicTextColor, bgStyle, onCategorySelected = { selectedViewData = it })
                }
            }
        }
    } else {
        // Detalle de Categoría / Playlist
        DesktopCategoryDetailScreen(
            viewData = selectedViewData!!,
            libraryViewModel = libraryViewModel,
            playerViewModel = playerViewModel,
            paletteColors = paletteColors,
            dynamicTextColor = dynamicTextColor,
            onBack = { selectedViewData = null },
            onPageChange = onPageChange
        )
    }
}

@Composable
fun DesktopPlaylistsTab(
    playlists: List<com.example.beatpulse.data.PlaylistEntity>,
    paletteColors: PaletteColors,
    dynamicTextColor: Color,
    onCategorySelected: (PlaylistViewData) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)) {
        if (playlists.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No tienes playlists creadas.", color = dynamicTextColor.copy(alpha = 0.6f))
                }
            }
        }
        items(playlists) { pl ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategorySelected(PlaylistViewData(pl.name, emptyList(), pl.playlistId)) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = paletteColors.vibrant, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(16.dp))
                Text(pl.name, color = dynamicTextColor, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DesktopCategoryTab(
    categoryType: Int,
    allTracks: List<TrackEntity>,
    paletteColors: PaletteColors,
    dynamicTextColor: Color,
    bgStyle: Int,
    onCategorySelected: (PlaylistViewData) -> Unit
) {
    val itemsList = remember(categoryType, allTracks) {
        when (categoryType) {
            0 -> allTracks.groupBy { it.artist }.toSortedMap()
            1 -> allTracks.groupBy { it.album }.toSortedMap()
            2 -> allTracks.groupBy { it.folderPath }.toSortedMap()
            else -> emptyMap()
        }
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)) {
        items(itemsList.keys.toList()) { key ->
            val categoryTracks = itemsList[key] ?: emptyList()
            val icon = when (categoryType) {
                0 -> Icons.Default.Person
                1 -> Icons.Default.Album
                2 -> if (bgStyle == 8) PixelIcons.Folder else Icons.Default.Folder
                else -> Icons.Default.Folder
            }
            val displayTitle = if (categoryType == 2) key.substringAfterLast("/") else key
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCategorySelected(PlaylistViewData(title = displayTitle.ifEmpty { "Desconocido" }, tracks = categoryTracks, filterType = categoryType, filterValue = key))
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = paletteColors.vibrant, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayTitle.ifEmpty { "Desconocido" }, color = dynamicTextColor, style = MaterialTheme.typography.titleMedium)
                    Text("${categoryTracks.size} canciones", color = dynamicTextColor.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun DesktopCategoryDetailScreen(
    viewData: PlaylistViewData,
    libraryViewModel: ILibraryViewModel,
    playerViewModel: IPlayerViewModel,
    paletteColors: PaletteColors,
    dynamicTextColor: Color,
    onBack: () -> Unit,
    onPageChange: (Int) -> Unit
) {
    val allTracks by libraryViewModel.allTracks.collectAsState()
    
    val dbTracks by if (viewData.playlistId != null) {
        libraryViewModel.getTracksForPlaylist(viewData.playlistId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<TrackEntity>()) }
    }

    val tracksToDisplay = remember(viewData, allTracks, dbTracks) {
        if (viewData.playlistId != null) {
            dbTracks
        } else if (viewData.filterType != null && viewData.filterValue != null) {
            allTracks.filter { track ->
                when (viewData.filterType) {
                    0 -> track.artist == viewData.filterValue
                    1 -> track.album == viewData.filterValue
                    2 -> track.folderPath == viewData.filterValue
                    else -> false
                }
            }
        } else {
            viewData.tracks
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = dynamicTextColor)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = viewData.title,
                style = MaterialTheme.typography.titleLarge,
                color = dynamicTextColor,
                modifier = Modifier.weight(1f)
            )
        }

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(tracksToDisplay, key = { it.id }) { track ->
                DesktopTrackItem(
                    track = track,
                    paletteColors = paletteColors,
                    textColor = dynamicTextColor,
                    onClick = {
                        playerViewModel.playTrack(track, tracksToDisplay)
                        onPageChange(2)
                    },
                    onToggleFavorite = { libraryViewModel.toggleFavorite(track, !track.isFavorite) }
                )
            }
        }
    }
}
