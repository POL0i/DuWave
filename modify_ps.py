import os

def modify_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

# 1. Inject state
ps_reps = [
    ('    var lyrics by remember { mutableStateOf<List<com.example.beatpulse.utils.LyricLine>>(emptyList()) }',
     '''    var lyrics by remember { mutableStateOf<List<com.example.beatpulse.utils.LyricLine>>(emptyList()) }
    val autoAnalyzeLyrics by playerViewModel.autoAnalyzeLyrics.collectAsState()
    val availableLyricsResults by playerViewModel.availableLyricsResults.collectAsState()
    var showLyricsMatches by remember { mutableStateOf(false) }'''),

# 2. Modify sticker UI
    ('''                if (lyrics.isEmpty()) {
                    val isFetchingLyrics by playerViewModel.isFetchingLyrics.collectAsState()
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                currentTrack?.let { track ->
                                    playerViewModel.downloadLyrics(track) { success, msg ->
                                        prefs.showToast(msg)
                                        if (success) {
                                            val lrcFile = if (track.dataPath.startsWith("youtube://")) {
                                                val videoId = track.dataPath.removePrefix("youtube://").substringBefore("|")
                                                java.io.File(context.cacheDir, "$videoId.lrc")
                                            } else {
                                                java.io.File(track.dataPath.substringBeforeLast(".") + ".lrc")
                                            }
                                            if (lrcFile.exists()) {
                                                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                    val newLyrics = com.example.beatpulse.utils.LrcParser.parseLrcFile(lrcFile)
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { lyrics = newLyrics }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isFetchingLyrics) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.List, contentDescription = "Buscar Letras", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.search_lyrics), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }''',
     '''                if (lyrics.isEmpty() && autoAnalyzeLyrics && availableLyricsResults.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .size(40.dp) // Hitbox muy pequeño y céntrico
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable {
                                showLyricsMatches = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Buscar Letras", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                }'''),

# 3. Add Settings Toggle for autoAnalyzeLyrics
    ('''                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.shuffle_playback), color = Color.White)
                    var isShuffleEnabled by remember { mutableStateOf(exoPlayer?.shuffleModeEnabled == true) }
                    androidx.compose.material3.Switch(
                        checked = isShuffleEnabled,
                        onCheckedChange = { 
                            isShuffleEnabled = it
                            exoPlayer?.shuffleModeEnabled = it 
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                    )
                }''',
     '''                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.shuffle_playback), color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.IconToggleButton(
                            checked = autoAnalyzeLyrics,
                            onCheckedChange = { playerViewModel.toggleAutoAnalyze() }
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.FindInPage,
                                contentDescription = "Auto Analyze Lyrics",
                                tint = if (autoAnalyzeLyrics) colorVibrant else Color.Gray.copy(alpha = 0.5f)
                            )
                        }
                        var isShuffleEnabled by remember { mutableStateOf(exoPlayer?.shuffleModeEnabled == true) }
                        androidx.compose.material3.Switch(
                            checked = isShuffleEnabled,
                            onCheckedChange = { 
                                isShuffleEnabled = it
                                exoPlayer?.shuffleModeEnabled = it 
                            },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = colorVibrant, checkedTrackColor = colorVibrant.copy(alpha=0.5f))
                        )
                    }
                }'''),

# 4. Add AlertDialog for Lyrics Selection
    ('''        // Lyrics Overlay
        androidx.compose.animation.AnimatedVisibility(''',
     '''        if (showLyricsMatches) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showLyricsMatches = false },
                title = { Text("Resultados Encontrados") },
                text = {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(availableLyricsResults) { result ->
                            androidx.compose.material3.ListItem(
                                modifier = Modifier.clickable {
                                    currentTrack?.let { track ->
                                        playerViewModel.saveLyricsAndNotify(track, result) { success, msg ->
                                            prefs.showToast(msg)
                                            if (success) {
                                                val lrcFile = if (track.dataPath.startsWith("youtube://")) {
                                                    val videoId = track.dataPath.removePrefix("youtube://").substringBefore("|")
                                                    java.io.File(context.cacheDir, "$videoId.lrc")
                                                } else {
                                                    java.io.File(track.dataPath.substringBeforeLast(".") + ".lrc")
                                                }
                                                if (lrcFile.exists()) {
                                                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                        val newLyrics = com.example.beatpulse.utils.LrcParser.parseLrcFile(lrcFile)
                                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { lyrics = newLyrics }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    showLyricsMatches = false
                                },
                                headlineContent = { Text(result.name) },
                                supportingContent = { Text("${result.artistName} • ${result.albumName}") },
                                trailingContent = { Text("${(result.score * 100).toInt()}%") }
                            )
                            androidx.compose.material3.Divider()
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showLyricsMatches = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        // Lyrics Overlay
        androidx.compose.animation.AnimatedVisibility(''')
]

modify_file('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt', ps_reps)

print("PlayerScreen modified")
