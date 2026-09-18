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
import com.example.beatpulse.utils.getLocalizedString
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
    thumbnailShapeIdx: Int,
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
                thumbnailShapeIdx = thumbnailShapeIdx,
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
                    thumbnailShapeIdx = thumbnailShapeIdx,
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
                    thumbnailShapeIdx = thumbnailShapeIdx,
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
                    thumbnailShapeIdx = thumbnailShapeIdx,
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
                    thumbnailShapeIdx = thumbnailShapeIdx,
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
                thumbnailShapeIdx = thumbnailShapeIdx,
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
    thumbnailShapeIdx: Int,
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
                    thumbnailShapeIdx = thumbnailShapeIdx,
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
