import os
import re

def modify_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

# 1. PlayerScreen.kt
ps_reps = [
    # Strings replacements
    ('"Reproducción Aleatoria"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.shuffle_playback)'),
    ('"Método de Repetición"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.repeat_method)'),
    ('"Estilo de Visualización"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.visualizer_style)'),
    ('"Física del Visualizador"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.visualizer_physics)'),
    ('"Modo de bandas"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.bands_mode)'),
    ('"Velocidad y tono"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.speed_and_pitch)'),
    ('"Velocidad: ${String.format(\\"%.2f\\", playbackSpeed)}x"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.speed_format, String.format("%.2f", playbackSpeed))'),
    ('"Tono: ${String.format(\\"%.2f\\", playbackPitch)}x"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.pitch_format, String.format("%.2f", playbackPitch))'),
    ('"Título"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.title)'),
    ('"Artista"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.artist)'),
    ('"Álbum"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.album)'),
    ('"Guardar"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.save)'),
    ('"Cancelar"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.cancel)'),
    ('"Activar ecualizador"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.enable_equalizer)'),
    ('"Personalizado"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.custom)'),
    ('"NORMAL"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.eq_normal).uppercase()'),
    # Fix lyrics Loading to support online tracks
    (
        'val lrcFile = java.io.File(currentTrack.dataPath.substringBeforeLast(".") + ".lrc")',
        'val lrcFile = if (currentTrack.dataPath.startsWith("youtube://")) {\n                val videoId = currentTrack.dataPath.removePrefix("youtube://").substringBefore("|")\n                java.io.File(context.cacheDir, "$videoId.lrc")\n            } else {\n                java.io.File(currentTrack.dataPath.substringBeforeLast(".") + ".lrc")\n            }'
    ),
    # Add Lyrics button inside the album art Box
    (
        '''                androidx.compose.animation.AnimatedVisibility(
                    visible = !isPlayingState,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                            contentDescription = "Paused",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }''',
        '''                androidx.compose.animation.AnimatedVisibility(
                    visible = !isPlayingState,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                            contentDescription = "Paused",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                
                if (lyrics.isEmpty()) {
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
                                Icon(Icons.Default.Search, contentDescription = "Buscar Letras", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.search_lyrics), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }'''
    )
]

modify_file('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt', ps_reps)

# 2. PlayerViewModel.kt -> add downloadLyrics method and isFetchingLyrics flow
pvm_reps = [
    (
        'val effectsPreset: StateFlow<String> = _effectsPreset',
        'val effectsPreset: StateFlow<String> = _effectsPreset\n\n    val isFetchingLyrics = MutableStateFlow(false)\n\n    fun downloadLyrics(track: TrackEntity, onResult: (Boolean, String) -> Unit) {\n        viewModelScope.launch(Dispatchers.IO) {\n            isFetchingLyrics.value = true\n            try {\n                val title = track.customTitle ?: track.title\n                val artist = track.customArtist ?: track.artist\n                val url = "https://lrclib.net/api/get?track_name=${java.net.URLEncoder.encode(title, "UTF-8")}&artist_name=${java.net.URLEncoder.encode(artist, "UTF-8")}"\n                val request = okhttp3.Request.Builder().url(url).build()\n                val response = okhttp3.OkHttpClient().newCall(request).execute()\n                val body = response.body?.string()\n                if (response.isSuccessful && body != null) {\n                    val json = org.json.JSONObject(body)\n                    val syncedLyrics = json.optString("syncedLyrics", "")\n                    val plainLyrics = json.optString("plainLyrics", "")\n                    val lyricsText = if (syncedLyrics.isNotEmpty()) syncedLyrics else plainLyrics\n                    if (lyricsText.isNotEmpty()) {\n                        val lrcFile = if (track.dataPath.startsWith("youtube://")) {\n                            val videoId = track.dataPath.removePrefix("youtube://").substringBefore("|")\n                            java.io.File(context.cacheDir, "$videoId.lrc")\n                        } else {\n                            java.io.File(track.dataPath.substringBeforeLast(".") + ".lrc")\n                        }\n                        lrcFile.writeText(lyricsText)\n                        withContext(Dispatchers.Main) { onResult(true, "Letras descargadas con éxito") }\n                    } else {\n                        withContext(Dispatchers.Main) { onResult(false, "No se encontraron letras") }\n                    }\n                } else {\n                    withContext(Dispatchers.Main) { onResult(false, "No se encontraron letras") }\n                }\n            } catch (e: Exception) {\n                withContext(Dispatchers.Main) { onResult(false, "Error de red al buscar letras") }\n            } finally {\n                isFetchingLyrics.value = false\n            }\n        }\n    }'
    )
]
modify_file('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerViewModel.kt', pvm_reps)

# 3. StatsScreen.kt and LibraryScreen.kt
ss_reps = [
    ('"\\U0001F4CA Tus Estadísticas"', '"\\U0001F4CA " + androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.your_statistics)'),
    ('"Empieza a escuchar música para ver tus estadísticas aquí."', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.start_listening_stats)')
]
modify_file('app/src/main/java/com/example/beatpulse/ui/screens/StatsScreen.kt', ss_reps)

ls_reps = [
    ('"Buscando recomendaciones basadas en tus estadísticas..."', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.searching_recommendations)'),
    ('"Buscando online..."', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.searching_online)')
]
modify_file('app/src/main/java/com/example/beatpulse/ui/screens/LibraryScreen.kt', ls_reps)

# 4. Strings XML
es_strings = """
    <string name="shuffle_playback">Reproducción Aleatoria</string>
    <string name="repeat_method">Método de Repetición</string>
    <string name="visualizer_style">Estilo de Visualización</string>
    <string name="visualizer_physics">Física del Visualizador</string>
    <string name="bands_mode">Modo de bandas</string>
    <string name="speed_and_pitch">Velocidad y tono</string>
    <string name="speed_format">Velocidad: %1$sx</string>
    <string name="pitch_format">Tono: %1$sx</string>
    <string name="title">Título</string>
    <string name="artist">Artista</string>
    <string name="album">Álbum</string>
    <string name="save">Guardar</string>
    <string name="cancel">Cancelar</string>
    <string name="enable_equalizer">Activar ecualizador</string>
    <string name="custom">Personalizado</string>
    <string name="your_statistics">Tus Estadísticas</string>
    <string name="start_listening_stats">Empieza a escuchar música para ver tus estadísticas aquí.</string>
    <string name="searching_recommendations">Buscando recomendaciones basadas en tus estadísticas...</string>
    <string name="searching_online">Buscando online...</string>
    <string name="search_lyrics">Letras</string>
"""

en_strings = """
    <string name="shuffle_playback">Shuffle Playback</string>
    <string name="repeat_method">Repeat Method</string>
    <string name="visualizer_style">Visualizer Style</string>
    <string name="visualizer_physics">Visualizer Physics</string>
    <string name="bands_mode">Bands Mode</string>
    <string name="speed_and_pitch">Speed and Pitch</string>
    <string name="speed_format">Speed: %1$sx</string>
    <string name="pitch_format">Pitch: %1$sx</string>
    <string name="title">Title</string>
    <string name="artist">Artist</string>
    <string name="album">Album</string>
    <string name="save">Save</string>
    <string name="cancel">Cancel</string>
    <string name="enable_equalizer">Enable Equalizer</string>
    <string name="custom">Custom</string>
    <string name="your_statistics">Your Statistics</string>
    <string name="start_listening_stats">Start listening to music to see your stats here.</string>
    <string name="searching_recommendations">Searching recommendations based on your stats...</string>
    <string name="searching_online">Searching online...</string>
    <string name="search_lyrics">Lyrics</string>
"""

pt_strings = """
    <string name="shuffle_playback">Reprodução Aleatória</string>
    <string name="repeat_method">Método de Repetição</string>
    <string name="visualizer_style">Estilo de Visualização</string>
    <string name="visualizer_physics">Física do Visualizador</string>
    <string name="bands_mode">Modo de bandas</string>
    <string name="speed_and_pitch">Velocidade e Tom</string>
    <string name="speed_format">Velocidade: %1$sx</string>
    <string name="pitch_format">Tom: %1$sx</string>
    <string name="title">Título</string>
    <string name="artist">Artista</string>
    <string name="album">Álbum</string>
    <string name="save">Salvar</string>
    <string name="cancel">Cancelar</string>
    <string name="enable_equalizer">Ativar Equalizador</string>
    <string name="custom">Personalizado</string>
    <string name="your_statistics">Suas Estatísticas</string>
    <string name="start_listening_stats">Comece a ouvir música para ver suas estatísticas aqui.</string>
    <string name="searching_recommendations">Procurando recomendações com base nas suas estatísticas...</string>
    <string name="searching_online">Buscando online...</string>
    <string name="search_lyrics">Letras</string>
"""

def append_to_xml(filepath, strings_to_append):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    if "visualizer_style" not in content:
        content = content.replace("</resources>", strings_to_append + "\n</resources>")
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)

append_to_xml('app/src/main/res/values/strings.xml', es_strings)
append_to_xml('app/src/main/res/values-en/strings.xml', en_strings)
append_to_xml('app/src/main/res/values-pt/strings.xml', pt_strings)

print("Patching complete.")
