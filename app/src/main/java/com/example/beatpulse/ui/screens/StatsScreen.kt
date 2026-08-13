package com.example.beatpulse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.beatpulse.data.PreferencesManager
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.rememberAlbumArt
import com.example.beatpulse.data.TrackEntity
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StatsScreen(
    paletteColors: PaletteColors,
    prefs: PreferencesManager
) {
    val viewModel: StatsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = paletteColors.vibrant)
        }
        return
    }

    val hasData = state.totalPlayCount > 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header with refresh
        item {
            StaggeredEntry(visible = visible, delayMs = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 ${androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_title)}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    IconButton(onClick = { viewModel.loadStats() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Recargar",
                            tint = paletteColors.vibrant
                        )
                    }
                }
            }
        }

        if (!hasData) {
            item {
                StaggeredEntry(visible = visible, delayMs = 100) {
                    EmptyStatsCard(paletteColors)
                }
            }
            return@LazyColumn
        }

        // Hero listening time card
        item {
            StaggeredEntry(visible = visible, delayMs = 100) {
                HeroListeningCard(
                    totalMs = state.totalListeningTimeMs,
                    totalPlayCount = state.totalPlayCount,
                    paletteColors = paletteColors
                )
            }
        }

        // Quick stats row
        item {
            StaggeredEntry(visible = visible, delayMs = 200) {
                QuickStatsRow(
                    tracksPlayed = state.totalTracksPlayed,
                    uniqueArtists = state.uniqueArtists,
                    uniqueAlbums = state.uniqueAlbums,
                    paletteColors = paletteColors
                )
            }
        }

        // Top Artists
        if (state.topArtists.isNotEmpty()) {
            item {
                StaggeredEntry(visible = visible, delayMs = 300) {
                    SectionTitle(
                        icon = Icons.Default.Person,
                        title = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_top_artists),
                        paletteColors = paletteColors
                    )
                }
            }
            item {
                StaggeredEntry(visible = visible, delayMs = 350) {
                    TopArtistsCard(
                        artists = state.topArtists,
                        paletteColors = paletteColors
                    )
                }
            }
        }

        // Top Tracks
        if (state.topTracks.isNotEmpty()) {
            item {
                StaggeredEntry(visible = visible, delayMs = 400) {
                    SectionTitle(
                        icon = Icons.Default.MusicNote,
                        title = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_top_songs),
                        paletteColors = paletteColors
                    )
                }
            }
            itemsIndexed(state.topTracks) { index, track ->
                StaggeredEntry(visible = visible, delayMs = 450 + index * 50) {
                    TopTrackItem(
                        track = track,
                        rank = index + 1,
                        maxPlays = state.topTracks.firstOrNull()?.playCount ?: 1,
                        paletteColors = paletteColors
                    )
                }
            }
        }

        // Top Albums
        if (state.topAlbums.isNotEmpty()) {
            item {
                StaggeredEntry(visible = visible, delayMs = 700) {
                    SectionTitle(
                        icon = Icons.Default.Album,
                        title = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_top_albums),
                        paletteColors = paletteColors
                    )
                }
            }
            item {
                StaggeredEntry(visible = visible, delayMs = 750) {
                    TopAlbumsCard(
                        albums = state.topAlbums,
                        paletteColors = paletteColors
                    )
                }
            }
        }

        // Favorites summary
        if (state.favoriteTracks.isNotEmpty()) {
            item {
                StaggeredEntry(visible = visible, delayMs = 850) {
                    SectionTitle(
                        icon = Icons.Default.Favorite,
                        title = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_your_favorites),
                        paletteColors = paletteColors
                    )
                }
            }
            item {
                StaggeredEntry(visible = visible, delayMs = 900) {
                    FavoritesCard(
                        favorites = state.favoriteTracks.take(5),
                        paletteColors = paletteColors
                    )
                }
            }
        }
    }
}

@Composable
private fun StaggeredEntry(
    visible: Boolean,
    delayMs: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 500, delayMillis = delayMs, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 500, delayMillis = delayMs, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 3 }
        )
    ) {
        content()
    }
}

@Composable
private fun EmptyStatsCard(paletteColors: PaletteColors) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            paletteColors.darkMuted.copy(alpha = 0.8f),
                            paletteColors.dominant.copy(alpha = 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = paletteColors.vibrant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_no_data),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.start_listening_stats),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HeroListeningCard(
    totalMs: Long,
    totalPlayCount: Int,
    paletteColors: PaletteColors
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val timeText = formatDuration(totalMs, context)
    val formatter = NumberFormat.getNumberInstance(Locale("es", "ES"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            paletteColors.vibrant.copy(alpha = 0.85f),
                            paletteColors.darkVibrant.copy(alpha = 0.9f),
                            paletteColors.dominant.copy(alpha = 0.95f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(28.dp)
        ) {
            Column {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_you_listened),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_of_music),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_total_plays, formatter.format(totalPlayCount)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatsRow(
    tracksPlayed: Int,
    uniqueArtists: Int,
    uniqueAlbums: Int,
    paletteColors: PaletteColors
) {
    val formatter = NumberFormat.getNumberInstance(Locale("es", "ES"))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.LibraryMusic,
            value = formatter.format(tracksPlayed),
            label = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_songs),
            paletteColors = paletteColors
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Person,
            value = formatter.format(uniqueArtists),
            label = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_artists),
            paletteColors = paletteColors
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Album,
            value = formatter.format(uniqueAlbums),
            label = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_albums),
            paletteColors = paletteColors
        )
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    paletteColors: PaletteColors
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            paletteColors.darkMuted.copy(alpha = 0.7f),
                            paletteColors.dominant.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = paletteColors.lightVibrant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String,
    paletteColors: PaletteColors
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = paletteColors.vibrant
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
private fun TopArtistsCard(
    artists: List<com.example.beatpulse.data.ArtistStats>,
    paletteColors: PaletteColors
) {
    val maxPlays = artists.firstOrNull()?.totalPlays ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            paletteColors.darkMuted.copy(alpha = 0.7f),
                            paletteColors.dominant.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            artists.forEachIndexed { index, artist ->
                ArtistBarItem(
                    rank = index + 1,
                    name = artist.artist,
                    plays = artist.totalPlays,
                    maxPlays = maxPlays,
                    paletteColors = paletteColors
                )
            }
        }
    }
}

@Composable
private fun ArtistBarItem(
    rank: Int,
    name: String,
    plays: Int,
    maxPlays: Int,
    paletteColors: PaletteColors
) {
    val fraction = if (maxPlays > 0) plays.toFloat() / maxPlays else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "bar_$rank"
    )
    val medalEmoji = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }
    val formatter = NumberFormat.getNumberInstance(Locale("es", "ES"))

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (medalEmoji != null) {
                    Text(text = medalEmoji, fontSize = 18.sp)
                } else {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.width(28.dp)
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.stats_plays_short, formatter.format(plays)),
                style = MaterialTheme.typography.labelMedium,
                color = paletteColors.lightVibrant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                paletteColors.vibrant,
                                paletteColors.lightVibrant
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun TopTrackItem(
    track: TrackEntity,
    rank: Int,
    maxPlays: Int,
    paletteColors: PaletteColors
) {
    val albumArt = rememberAlbumArt(track)
    val fraction = if (maxPlays > 0) track.playCount.toFloat() / maxPlays else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "track_bar_$rank"
    )
    val medalEmoji = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }
    val formatter = NumberFormat.getNumberInstance(Locale("es", "ES"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            paletteColors.darkMuted.copy(alpha = 0.7f),
                            paletteColors.dominant.copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Rank
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (medalEmoji != null) {
                    Text(text = medalEmoji, fontSize = 22.sp)
                } else {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // Album art
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(paletteColors.darkMuted.copy(alpha = 0.5f))
            ) {
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center),
                        tint = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            // Track info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.customTitle ?: track.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.customArtist ?: track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedFraction)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        paletteColors.vibrant,
                                        paletteColors.lightVibrant
                                    )
                                )
                            )
                    )
                }
            }

            // Play count badge
            Box(
                modifier = Modifier
                    .background(
                        paletteColors.vibrant.copy(alpha = 0.3f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatter.format(track.playCount),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = paletteColors.lightVibrant
                )
            }
        }
    }
}

@Composable
private fun TopAlbumsCard(
    albums: List<com.example.beatpulse.data.AlbumStats>,
    paletteColors: PaletteColors
) {
    val maxPlays = albums.firstOrNull()?.totalPlays ?: 1
    val formatter = NumberFormat.getNumberInstance(Locale("es", "ES"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            paletteColors.darkMuted.copy(alpha = 0.7f),
                            paletteColors.dominant.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            albums.take(5).forEachIndexed { index, album ->
                val fraction = if (maxPlays > 0) album.totalPlays.toFloat() / maxPlays else 0f
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    label = "album_bar_$index"
                )
                val medalEmoji = when (index) {
                    0 -> "🥇"
                    1 -> "🥈"
                    2 -> "🥉"
                    else -> null
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (medalEmoji != null) {
                                Text(text = medalEmoji, fontSize = 18.sp)
                            } else {
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.width(28.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = album.album,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = album.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = "${formatter.format(album.totalPlays)} rep.",
                            style = MaterialTheme.typography.labelMedium,
                            color = paletteColors.lightVibrant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            paletteColors.muted,
                                            paletteColors.vibrant
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesCard(
    favorites: List<TrackEntity>,
    paletteColors: PaletteColors
) {
    val formatter = NumberFormat.getNumberInstance(Locale("es", "ES"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            paletteColors.darkVibrant.copy(alpha = 0.6f),
                            paletteColors.darkMuted.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            favorites.forEach { track ->
                val albumArt = rememberAlbumArt(track)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Album art
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(paletteColors.darkMuted.copy(alpha = 0.5f))
                    ) {
                        if (albumArt != null) {
                            Image(
                                bitmap = albumArt,
                                contentDescription = track.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.Center),
                                tint = paletteColors.lightVibrant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.customTitle ?: track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.customArtist ?: track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (track.playCount > 0) {
                        Text(
                            text = "${formatter.format(track.playCount)} rep.",
                            style = MaterialTheme.typography.labelSmall,
                            color = paletteColors.lightVibrant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long, context: android.content.Context): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return if (hours > 0) {
        context.getString(com.example.beatpulse.R.string.stats_time_format, hours, minutes)
    } else {
        context.getString(com.example.beatpulse.R.string.stats_time_format, 0, minutes).let {
            if (it.contains("0 horas ")) it.replace("0 horas ", "")
            else if (it.contains("0 hours ")) it.replace("0 hours ", "")
            else it
        }
    }
}
