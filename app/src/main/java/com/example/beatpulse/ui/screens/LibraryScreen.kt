package com.example.beatpulse.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.TrackEntity
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import com.example.beatpulse.ui.components.rememberAlbumArt
import com.example.beatpulse.ui.components.rememberAlbumArt
import com.example.beatpulse.ui.components.rememberTrackPalette
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun LibraryScreen(
    repository: MusicRepository,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    prefs: com.example.beatpulse.data.PreferencesManager,
    onRescan: () -> Unit,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Todos", "Recientes", "Favoritos")

    val allTracks by repository.allTracksFlow.collectAsState(initial = emptyList())
    val recentTracks by repository.recentTracksFlow.collectAsState(initial = emptyList())
    val favoriteTracks by repository.favoritesFlow.collectAsState(initial = emptyList())
    
    val scope = rememberCoroutineScope()
    
    val colorDominant by animateColorAsState(paletteColors.dominant, label = "color_dom")
    val colorVibrant by animateColorAsState(paletteColors.vibrant, label = "color_vib")
    var searchQuery by remember { mutableStateOf("") }

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

            val currentList = remember(selectedTabIndex, allTracks, recentTracks, favoriteTracks, searchQuery) {
                when (selectedTabIndex) {
                    0 -> allTracks
                    1 -> recentTracks
                    2 -> favoriteTracks
                    else -> allTracks
                }.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
            }

            val isScanning by repository.isScanning.collectAsState()

            if (isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = paletteColors.vibrant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Buscando música...", color = dynamicTextColor)
                    }
                }
            } else if (currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay canciones aquí.", color = dynamicTextColor)
                }
            } else {
                val shapeIdx by prefs.thumbnailShapeFlow.collectAsState(initial = 0)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(currentList, key = { it.id }) { track ->
                        TrackItem(
                            track = track,
                            paletteColors = paletteColors,
                            thumbnailShapeIdx = shapeIdx,
                            textColor = dynamicTextColor,
                            onClick = { onTrackClick(track, currentList) },
                            onToggleFavorite = { 
                                scope.launch { repository.toggleFavorite(track.id, !track.isFavorite) }
                            }
                        )
                    }
                }
            }
        }
        
        // Floating search bar overlay
        var isSearchExpanded by remember { mutableStateOf(false) }
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
                .padding(end = if (!isSearchExpanded) 16.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
             IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = colorVibrant)
             }
             AnimatedVisibility(visible = isSearchExpanded) {
                 Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                     OutlinedTextField(
                         value = searchQuery,
                         onValueChange = { searchQuery = it },
                         placeholder = { Text("Buscar...", color = Color.LightGray) },
                         singleLine = true,
                         modifier = Modifier.width(200.dp),
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
    }
}

@Composable
fun TrackItem(
    track: TrackEntity,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    thumbnailShapeIdx: Int = 0,
    textColor: Color = LocalContentColor.current,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
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
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist} • ${track.album}",
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
    }
}
