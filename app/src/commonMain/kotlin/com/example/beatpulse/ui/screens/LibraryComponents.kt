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
