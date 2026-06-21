package com.example.beatpulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.PreferencesManager
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.PixelIcons

@Composable
fun AlbumsScreen(
    repository: MusicRepository,
    paletteColors: PaletteColors,
    prefs: PreferencesManager,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit
) {
    val recentTracks by repository.recentTracksFlow.collectAsState(initial = emptyList())
    val topTracks by repository.topPlayedFlow.collectAsState(initial = emptyList())
    val recentlyAdded by repository.recentlyAddedFlow.collectAsState(initial = emptyList())
    val allTracks by repository.allTracksFlow.collectAsState(initial = emptyList())
    val bgStyle by prefs.backgroundStyleFlow.collectAsState()
    val shapeIdx by prefs.thumbnailShapeFlow.collectAsState()

    // Group tracks by folder path
    val tracksByFolder = remember(allTracks) {
        allTracks.groupBy { it.folderPath }.toSortedMap()
    }

    data class PlaylistViewData(val title: String, val tracks: List<TrackEntity>, val playlistId: Long? = null)
    var selectedPlaylist by remember { mutableStateOf<PlaylistViewData?>(null) }
    var isCreatingPlaylist by remember { mutableStateOf(false) }
    val playlists by repository.playlistsFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

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

    val isScanning by repository.isScanning.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isScanning && allTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = paletteColors.vibrant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Buscando música...", color = dynamicTextColor)
                }
            }
        } else if (isCreatingPlaylist) {
            BackHandler { isCreatingPlaylist = false }
            CreatePlaylistScreen(
                repository = repository,
                allTracks = allTracks,
                dynamicTextColor = dynamicTextColor,
                paletteColors = paletteColors,
                onClose = { isCreatingPlaylist = false }
            )
        } else if (selectedPlaylist == null) {
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
                    Text(
                        text = "Inicio",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = dynamicTextColor
                    )
                    
                    Row {
                        IconButton(onClick = {
                            val newShape = (shapeIdx + 1) % 4
                            prefs.thumbnailShape = newShape
                            prefs.showToast(when(newShape) {
                                0 -> "Forma: Círculo"
                                1 -> "Forma: Cuadrado"
                                2 -> "Forma: Redondeado"
                                3 -> "Forma: Squircle"
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
                                0 -> "Estilo: Clásico"
                                1 -> "Estilo: Cyberpunk"
                                2 -> "Estilo: Anime Pastel"
                                3 -> "Estilo: Luminoso"
                                4 -> "Estilo: Y2K Kawaii"
                                5 -> "Estilo: Black Metal"
                                6 -> "Estilo: Dark Fantasy"
                                7 -> "Estilo: Catedral"
                                8 -> "Estilo: Corazones"
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    item {
                        Text(
                            text = "Listas de Reproducción",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = dynamicTextColor,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    
                    if (recentTracks.isNotEmpty()) {
                        item {
                            PlaylistFolderItem(
                                title = "20 Últimas Reproducciones",
                                count = recentTracks.take(20).size,
                                icon = Icons.Default.History,
                                tint = paletteColors.vibrant,
                                textColor = dynamicTextColor,
                                onClick = { selectedPlaylist = PlaylistViewData("20 Últimas Reproducciones", recentTracks.take(20)) }
                            )
                        }
                    }
                    
                    if (topTracks.isNotEmpty()) {
                        item {
                            PlaylistFolderItem(
                                title = "Mejores Reproducciones",
                                count = topTracks.size,
                                icon = Icons.Default.Star,
                                tint = paletteColors.vibrant,
                                textColor = dynamicTextColor,
                                onClick = { selectedPlaylist = PlaylistViewData("Mejores Reproducciones", topTracks) }
                            )
                        }
                    }
                    
                    if (recentlyAdded.isNotEmpty()) {
                        item {
                            PlaylistFolderItem(
                                title = "Últimos Agregados",
                                count = recentlyAdded.size,
                                icon = Icons.Default.NewReleases,
                                tint = paletteColors.vibrant,
                                textColor = dynamicTextColor,
                                onClick = { selectedPlaylist = PlaylistViewData("Últimos Agregados", recentlyAdded) }
                            )
                        }
                    }
                    
                    if (playlists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Mis Listas",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = dynamicTextColor,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                        items(playlists) { pl ->
                            val trackCount by repository.getPlaylistTrackCountFlow(pl.playlistId).collectAsState(initial = 0)
                            PlaylistFolderItem(
                                title = pl.name,
                                count = trackCount,
                                icon = Icons.Default.QueueMusic,
                                tint = paletteColors.vibrant,
                                textColor = dynamicTextColor,
                                onClick = { selectedPlaylist = PlaylistViewData(pl.name, emptyList(), pl.playlistId) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { isCreatingPlaylist = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = paletteColors.vibrant)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Crear Playlist")
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Carpetas",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = dynamicTextColor,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(tracksByFolder.keys.toList()) { folder ->
                        val folderTracks = tracksByFolder[folder] ?: emptyList()
                        PlaylistFolderItem(
                            title = folder.substringAfterLast("/"),
                            count = folderTracks.size,
                            icon = if (bgStyle == 8) PixelIcons.Folder else Icons.Default.Folder,
                            tint = paletteColors.vibrant,
                            textColor = dynamicTextColor,
                            onClick = { selectedPlaylist = PlaylistViewData(folder.substringAfterLast("/"), folderTracks) }
                        )
                    }
                }
            }
        } else {
            // Detailed View for Selected Playlist/Folder
            BackHandler { selectedPlaylist = null }
            val currentViewData = selectedPlaylist!!
            val dbTracks by if (currentViewData.playlistId != null) {
                repository.getTracksForPlaylist(currentViewData.playlistId).collectAsState(initial = emptyList())
            } else {
                remember { mutableStateOf(emptyList()) }
            }
            
            val tracksToDisplay = if (currentViewData.playlistId != null) dbTracks else currentViewData.tracks

            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedPlaylist = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = dynamicTextColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentViewData.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = dynamicTextColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(tracksToDisplay.size, key = { tracksToDisplay[it].id }) { index ->
                        val track = tracksToDisplay[index]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                com.example.beatpulse.ui.screens.TrackItem(
                                    track = track,
                                    paletteColors = paletteColors,
                                    thumbnailShapeIdx = shapeIdx,
                                    textColor = dynamicTextColor,
                                    onClick = { onTrackClick(track, tracksToDisplay) },
                                    onToggleFavorite = {
                                        coroutineScope.launch { repository.toggleFavorite(track.id, !track.isFavorite) }
                                    } 
                                )
                            }
                            if (currentViewData.playlistId != null) {
                                Column {
                                    if (index > 0) {
                                        IconButton(onClick = {
                                            coroutineScope.launch {
                                                val prevTrack = tracksToDisplay[index - 1]
                                                repository.updatePlaylistOrder(
                                                    currentViewData.playlistId, 
                                                    listOf(Pair(track.id, index - 1), Pair(prevTrack.id, index))
                                                )
                                            }
                                        }) {
                                            Icon(Icons.Default.KeyboardArrowUp, tint = dynamicTextColor, contentDescription = "Arriba")
                                        }
                                    }
                                    if (index < tracksToDisplay.size - 1) {
                                        IconButton(onClick = {
                                            coroutineScope.launch {
                                                val nextTrack = tracksToDisplay[index + 1]
                                                repository.updatePlaylistOrder(
                                                    currentViewData.playlistId, 
                                                    listOf(Pair(track.id, index + 1), Pair(nextTrack.id, index))
                                                )
                                            }
                                        }) {
                                            Icon(Icons.Default.KeyboardArrowDown, tint = dynamicTextColor, contentDescription = "Abajo")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistFolderItem(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = textColor)
            Text(text = "$count canciones", style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.7f))
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun CreatePlaylistScreen(
    repository: MusicRepository,
    allTracks: List<TrackEntity>,
    dynamicTextColor: androidx.compose.ui.graphics.Color,
    paletteColors: PaletteColors,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedTracks = remember { androidx.compose.runtime.mutableStateMapOf<Long, Boolean>() }
    val coroutineScope = rememberCoroutineScope()

    val filteredTracks = remember(searchQuery, allTracks) {
        if (searchQuery.isEmpty()) allTracks
        else allTracks.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.artist.contains(searchQuery, ignoreCase = true) 
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = dynamicTextColor)
            }
            Text("Nueva Playlist", style = MaterialTheme.typography.titleLarge, color = dynamicTextColor, modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        coroutineScope.launch {
                            val id = repository.createPlaylist(name)
                            selectedTracks.filterValues { it }.keys.forEach { trackId ->
                                repository.addTrackToPlaylist(id, trackId)
                            }
                            onClose()
                        }
                    }
                },
                enabled = name.isNotBlank() && selectedTracks.values.any { it },
                colors = ButtonDefaults.buttonColors(containerColor = paletteColors.vibrant)
            ) {
                Text("Guardar")
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre de la playlist", color = dynamicTextColor.copy(alpha=0.7f)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = dynamicTextColor,
                unfocusedTextColor = dynamicTextColor,
                focusedBorderColor = paletteColors.vibrant,
                unfocusedBorderColor = dynamicTextColor.copy(alpha = 0.5f)
            )
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar canciones...", color = dynamicTextColor.copy(alpha=0.5f)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = dynamicTextColor.copy(alpha=0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = dynamicTextColor,
                unfocusedTextColor = dynamicTextColor,
                focusedBorderColor = paletteColors.vibrant,
                unfocusedBorderColor = dynamicTextColor.copy(alpha = 0.5f)
            )
        )

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
            items(filteredTracks.size, key = { filteredTracks[it].id }) { index ->
                val track = filteredTracks[index]
                val isSelected = selectedTracks[track.id] == true
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedTracks[track.id] = !isSelected }.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { selectedTracks[track.id] = it },
                        colors = CheckboxDefaults.colors(checkedColor = paletteColors.vibrant)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, color = dynamicTextColor, style = MaterialTheme.typography.bodyLarge)
                        Text(track.artist, color = dynamicTextColor.copy(alpha=0.7f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
