package com.example.beatpulse.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.player.IPlayerViewModel
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.viewmodels.ILibraryViewModel
import com.example.beatpulse.ui.components.DesktopTrackItem
import kotlinx.coroutines.launch

@Composable
fun DesktopListaGeneralScreen(
    libraryViewModel: ILibraryViewModel,
    playerViewModel: IPlayerViewModel,
    paletteColors: PaletteColors,
    onPageChange: (Int) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    val allTracks by libraryViewModel.allTracks.collectAsState()
    val onlineSearchResults by libraryViewModel.onlineSearchResults.collectAsState()
    val isOnlineSearchLoading by libraryViewModel.isOnlineSearchLoading.collectAsState()
    val recommendations by libraryViewModel.recommendations.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search Bar (Buscador)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar...", color = Color.Gray) },
            trailingIcon = {
                IconButton(onClick = {
                    if (selectedTabIndex == 1 && searchQuery.isNotBlank()) {
                        scope.launch { 
                            libraryViewModel.isOnlineSearchLoading.value = true
                            libraryViewModel.onlineSearchResults.value = libraryViewModel.searchOnlineMusic(searchQuery)
                            libraryViewModel.isOnlineSearchLoading.value = false
                        }
                    }
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = paletteColors.vibrant)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (selectedTabIndex == 1 && searchQuery.isNotBlank()) {
                        scope.launch { 
                            libraryViewModel.isOnlineSearchLoading.value = true
                            libraryViewModel.onlineSearchResults.value = libraryViewModel.searchOnlineMusic(searchQuery)
                            libraryViewModel.isOnlineSearchLoading.value = false
                        }
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = paletteColors.vibrant,
                unfocusedBorderColor = Color.Gray,
                cursorColor = paletteColors.vibrant
            ),
            singleLine = true
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Tabs
        val tabs = listOf("Local", "Online (Buscador/Recomendaciones)")
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = paletteColors.vibrant,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = paletteColors.vibrant
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                Tab(
                    selected = isSelected,
                    onClick = { 
                        selectedTabIndex = index 
                    },
                    text = { 
                        Text(
                            text = title, 
                            color = if (isSelected) paletteColors.vibrant else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Content
        if (selectedTabIndex == 0) {
            // Local List
            val filteredLocal = remember(searchQuery, allTracks) {
                if (searchQuery.isBlank()) allTracks
                else allTracks.filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    it.artist.contains(searchQuery, ignoreCase = true) 
                }
            }
            
            if (filteredLocal.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (allTracks.isEmpty()) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = paletteColors.vibrant, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Tu biblioteca local está vacía.", color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { libraryViewModel.pickFolderAndScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = paletteColors.vibrant)
                            ) {
                                Text("Importar carpeta", color = Color.White)
                            }
                        } else {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = paletteColors.vibrant, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No se encontraron resultados para '$searchQuery'.", color = Color.White)
                        }
                    }
                }
            } else {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(filteredLocal, key = { it.id }) { track ->
                            DesktopTrackItem(
                                track = track,
                                paletteColors = paletteColors,
                                textColor = Color.White,
                                onClick = {
                                    playerViewModel.playTrack(track, filteredLocal)
                                    onPageChange(2)
                                },
                                onToggleFavorite = { libraryViewModel.toggleFavorite(track, !track.isFavorite) }
                            )
                        }
                    }
                }
            }
        } else {
            // Online Search / Recommendations
            if (isOnlineSearchLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = paletteColors.vibrant)
                }
            } else if (onlineSearchResults.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
                    item {
                        Text("Resultados Online", color = paletteColors.vibrant, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(onlineSearchResults, key = { it.id }) { track ->
                        DesktopTrackItem(
                            track = track,
                            paletteColors = paletteColors,
                            textColor = Color.White,
                            onClick = {
                                playerViewModel.playTrack(track, onlineSearchResults)
                                onPageChange(2)
                            },
                            onToggleFavorite = { libraryViewModel.toggleFavorite(track, !track.isFavorite) }
                        )
                    }
                }
            } else {
                // Show recommendations
                if (recommendations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Busca una canción online o espera las recomendaciones.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
                        item {
                            Text("Recomendaciones", color = paletteColors.vibrant, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        recommendations.forEach { (category, tracks) ->
                            item {
                                Text(category, color = paletteColors.vibrant, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            items(tracks, key = { it.id }) { track ->
                                DesktopTrackItem(
                                    track = track,
                                    paletteColors = paletteColors,
                                    textColor = Color.White,
                                    onClick = {
                                        playerViewModel.playTrack(track, tracks)
                                        onPageChange(2)
                                    },
                                    onToggleFavorite = { libraryViewModel.toggleFavorite(track, !track.isFavorite) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
