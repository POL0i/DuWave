package com.example.beatpulse.ui.components.player

import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.service.PlaybackService
import com.example.beatpulse.data.PreferencesManager
import com.example.beatpulse.ui.components.PaletteCache
import com.example.beatpulse.visualizer.AudioVisualizerManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.palette.graphics.Palette
import com.example.beatpulse.theme.PaletteColors
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.content.Intent

import android.annotation.SuppressLint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository,
    val visualizerManager: AudioVisualizerManager
) : ViewModel() {

    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _playerState = MutableStateFlow<Player?>(null)
    val playerState: StateFlow<Player?> = _playerState

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    val abRepeatModeEnabled = MutableStateFlow(false)
    val abPointA = MutableStateFlow(0f)
    val abPointB = MutableStateFlow(0.5f)

    private val _currentQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val currentQueue: StateFlow<List<TrackEntity>> = _currentQueue

    private val _paletteColors = MutableStateFlow(PaletteColors())
    val paletteColors: StateFlow<PaletteColors> = _paletteColors

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private val _playbackPitch = MutableStateFlow(1.0f)
    val playbackPitch: StateFlow<Float> = _playbackPitch

    private val _reverbEnabled = MutableStateFlow(false)
    val reverbEnabled: StateFlow<Boolean> = _reverbEnabled

    private val _effectsPreset = MutableStateFlow("NORMAL")
    val effectsPreset: StateFlow<String> = _effectsPreset

    val isFetchingLyrics = MutableStateFlow(false)
    val searchFailed = MutableStateFlow(false)
    
    // --- Mic/Streamer Mode State ---
    val isMicModeActive = MutableStateFlow(false)
    val streamConfigUiVisible = MutableStateFlow(false) // Hide UI by default in Mic Mode
    val streamConfigEffectsVisible = MutableStateFlow(true) // Keep effects by default
    val streamConfigMiniPlayerVisible = MutableStateFlow(false) // Hide mini player by default
    val streamConfigAspectRatio = MutableStateFlow("default") // "default" or "16:9"
    val streamAvatarUri = MutableStateFlow<String?>(PreferencesManager.getInstance(context).streamAvatarUri)
    
    val isWifiStreamActive = MutableStateFlow(false)
    val wifiStreamFps = MutableStateFlow(60)
    val wifiStreamQuality = MutableStateFlow(100)
    val wifiStreamCustomWidth = MutableStateFlow(1920)
    val wifiStreamCustomHeight = MutableStateFlow(1080)

    fun updateStreamAvatar(uri: String?) {
        PreferencesManager.getInstance(context).streamAvatarUri = uri
        streamAvatarUri.value = uri
        if (isMicModeActive.value && uri != null) {
            viewModelScope.launch { extractColorsFromUri(uri) }
        } else if (isMicModeActive.value) {
            _currentTrack.value?.let { viewModelScope.launch { extractColors(it) } }
        }
    }

    fun toggleMicMode() {
        val newState = !isMicModeActive.value
        isMicModeActive.value = newState
        if (newState) {
            // Pause playback when entering Mic Mode
            _playerState.value?.pause()
            streamAvatarUri.value?.let { uri -> viewModelScope.launch { extractColorsFromUri(uri) } }
        } else {
            _currentTrack.value?.let { track -> viewModelScope.launch { extractColors(track) } }
        }
    }
    // -------------------------------
    
    val autoAnalyzeLyrics = MutableStateFlow(true)
    val availableLyricsResults = MutableStateFlow<List<LrcSearchResult>>(emptyList())
    
    fun toggleAutoAnalyze() {
        val newState = !autoAnalyzeLyrics.value
        autoAnalyzeLyrics.value = newState
        PreferencesManager.getInstance(context).autoAnalyzeLyrics = newState
        if (!newState) {
            availableLyricsResults.value = emptyList()
        } else {
            _currentTrack.value?.let { checkLyricsAvailable(it) }
        }
    }


    fun checkLyricsAvailable(track: TrackEntity) {
        if (!autoAnalyzeLyrics.value) {
            availableLyricsResults.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            isFetchingLyrics.value = true
            searchFailed.value = false
            try {
                val title = track.customTitle ?: track.title
                val artist = track.customArtist ?: track.artist
                val q = "$title $artist"
                val url = "https://lrclib.net/api/search?q=${java.net.URLEncoder.encode(q, "UTF-8")}"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = okhttp3.OkHttpClient().newCall(request).execute()
                val body = response.body?.string()
                val results = mutableListOf<LrcSearchResult>()
                if (response.isSuccessful && body != null) {
                    val jsonArray = JSONArray(body)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val name = obj.optString("trackName", obj.optString("name", ""))
                        val artistName = obj.optString("artistName", "")
                        val albumName = obj.optString("albumName", "")
                        val duration = obj.optLong("duration", 0L)
                        val syncedLyrics = obj.optString("syncedLyrics", "")
                        val plainLyrics = obj.optString("plainLyrics", "")
                        
                        val scoreName = similarityScore(title, name)
                        val scoreArtist = similarityScore(artist, artistName)
                        val combinedScore = (scoreName * 0.6) + (scoreArtist * 0.4)
                        
                        if (syncedLyrics.isNotEmpty() || plainLyrics.isNotEmpty()) {
                            results.add(LrcSearchResult(obj.optLong("id"), name, artistName, albumName, duration, syncedLyrics, plainLyrics, combinedScore))
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    availableLyricsResults.value = results.sortedByDescending { it.score }
                    isFetchingLyrics.value = false
                    if (results.isEmpty()) {
                        searchFailed.value = true
                        kotlinx.coroutines.delay(2000)
                        searchFailed.value = false
                    }
                }
            } catch (e: Exception) {
                // Ignore silent errors in background
                withContext(Dispatchers.Main) { 
                    availableLyricsResults.value = emptyList() 
                    searchFailed.value = true
                    kotlinx.coroutines.delay(1500)
                    searchFailed.value = false
                }
            } finally {
                isFetchingLyrics.value = false
            }
        }
    }

    fun saveLyricsAndNotify(track: TrackEntity, result: LrcSearchResult, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lyricsText = if (result.syncedLyrics.isNotEmpty()) result.syncedLyrics else result.plainLyrics
                val lrcFile = if (track.dataPath.startsWith("youtube://")) {
                    val videoId = track.dataPath.removePrefix("youtube://").substringBefore("|")
                    java.io.File(context.cacheDir, "$videoId.lrc")
                } else {
                    java.io.File(track.dataPath.substringBeforeLast(".") + ".lrc")
                }
                lrcFile.writeText(lyricsText)
                withContext(Dispatchers.Main) { 
                    onResult(true, "Letras descargadas con éxito")
                    availableLyricsResults.value = emptyList() // Hide button after applying
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Error al guardar letras") }
            }
        }
    }

    // Levenshtein-based similarity
    private fun similarityScore(s1: String, s2: String): Double {
        val a = s1.lowercase(Locale.getDefault())
        val b = s2.lowercase(Locale.getDefault())
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val maxLen = maxOf(a.length, b.length)
        
        var costs = IntArray(b.length + 1)
        for (j in costs.indices) costs[j] = j
        for (i in 1..a.length) {
            costs[0] = i
            var nw = i - 1
            for (j in 1..b.length) {
                val cj = minOf(1 + minOf(costs[j], costs[j - 1]), if (a[i - 1] == b[j - 1]) nw else nw + 1)
                nw = costs[j]
                costs[j] = cj
            }
        }
        val distance = costs[b.length]
        return 1.0 - (distance.toDouble() / maxLen)
    }

    init {
        val prefs = PreferencesManager.getInstance(context)
        _repeatMode.value = prefs.repeatMode
        _shuffleModeEnabled.value = prefs.shuffleModeEnabled
        _playbackSpeed.value = prefs.playbackSpeed
        _playbackPitch.value = prefs.playbackPitch
        _reverbEnabled.value = prefs.reverbEnabled
        _effectsPreset.value = prefs.effectsPreset
        autoAnalyzeLyrics.value = prefs.autoAnalyzeLyrics
        // Push saved values to service companion flows so service can restore them
        PlaybackService.playbackSpeedFlow.value = prefs.playbackSpeed
        PlaybackService.playbackPitchFlow.value = prefs.playbackPitch
        PlaybackService.reverbEnabledFlow.value = prefs.reverbEnabled
        setupPlayer()

        // Keep currentTrack synchronized with DB changes (e.g. when cover is updated from Library)
        viewModelScope.launch {
            repository.allTracksFlow.collect { allTracks ->
                val current = _currentTrack.value
                if (current != null) {
                    val updated = allTracks.find { it.id == current.id }
                    if (updated != null && updated != current) {
                        _currentTrack.value = updated
                        if (updated.customCoverPath != current.customCoverPath) {
                            extractColors(updated)
                        }
                    }
                }
            }
        }
    }

    private fun setupPlayer() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val controller = controllerFuture?.get()
            _playerState.value = controller
            
            controller?.let { player ->
                val prefs = PreferencesManager.getInstance(context)
                player.shuffleModeEnabled = prefs.shuffleModeEnabled
                player.repeatMode = prefs.repeatMode
            }
            
            // Restore current or last track
            viewModelScope.launch {
                val currentMediaItem = controller?.currentMediaItem
                if (currentMediaItem != null) {
                    val trackId = currentMediaItem.mediaId.toLongOrNull()
                    if (trackId != null) {
                        val allTracks = repository.allTracksFlow.first()
                        val track = allTracks.find { it.id == trackId }
                        if (track != null) {
                            _currentTrack.value = track
                            extractColors(track)
                        }
                    }
                } else {
                    val recents = repository.recentTracksFlow.first()
                    if (recents.isNotEmpty()) {
                        val lastTrack = recents.first()
                        _currentTrack.value = lastTrack
                        _currentQueue.value = recents
                        extractColors(lastTrack)
                        
                        val mediaItem = MediaItem.Builder()
                            .setUri(android.net.Uri.parse(lastTrack.dataPath))
                            .setRequestMetadata(
                                MediaItem.RequestMetadata.Builder()
                                    .setMediaUri(android.net.Uri.parse(lastTrack.dataPath))
                                    .build()
                            )
                            .setMediaId(lastTrack.id.toString())
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(lastTrack.title)
                                    .setArtist(lastTrack.artist)
                                    .setIsPlayable(true)
                                    .apply {
                                        if (!lastTrack.customCoverPath.isNullOrEmpty()) {
                                            val path = lastTrack.customCoverPath
                                            setArtworkUri(android.net.Uri.parse(if (path.startsWith("/")) "file://$path" else path))
                                        } else {
                                            val fingerprint = com.example.beatpulse.ui.components.ThumbnailCache.getTrackFingerprint(lastTrack)
                                            val fullFile = java.io.File(context.cacheDir, "full_${fingerprint}.jpg")
                                            if (fullFile.exists() && fullFile.length() > 0) {
                                                setArtworkUri(android.net.Uri.fromFile(fullFile))
                                            }
                                        }
                                    }
                                    .build()
                            )
                            .build()
                        controller?.setMediaItem(mediaItem)
                        controller?.prepare()
                    }
                }
            }

            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    // Visualizer lifecycle is managed by MainActivity.onStart/onStop
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val trackId = mediaItem?.mediaId?.toLongOrNull()
                    if (trackId != null) {
                        val track = _currentQueue.value.find { it.id == trackId }
                        if (track != null && _currentTrack.value?.id != track.id) {
                            _currentTrack.value = track
                            viewModelScope.launch {
                                repository.insertOrUpdateTrack(track)
                                repository.markAsPlayed(track.id)
                                extractColors(track)
                                checkLyricsAvailable(track)
                            }
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    super.onPlayerError(error)
                    android.widget.Toast.makeText(context, "Error al reproducir: Archivo no encontrado o dañado.", android.widget.Toast.LENGTH_LONG).show()
                    val player = _playerState.value
                    if (player != null) {
                        val index = player.currentMediaItemIndex
                        if (index != androidx.media3.common.C.INDEX_UNSET) {
                            player.removeMediaItem(index)
                            val newList = _currentQueue.value.toMutableList()
                            if (index in newList.indices) {
                                newList.removeAt(index)
                                _currentQueue.value = newList
                            }
                            player.prepare()
                            player.play()
                        }
                    }
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _shuffleModeEnabled.value = shuffleModeEnabled
                    PreferencesManager.getInstance(context).shuffleModeEnabled = shuffleModeEnabled
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = repeatMode
                    PreferencesManager.getInstance(context).repeatMode = repeatMode
                }
            })
        }, MoreExecutors.directExecutor())
    }

    var isUiVisible = true
        set(value) {
            field = value
            // Visualizer lifecycle is managed by MainActivity.onStart/onStop
        }

    fun playTrack(track: TrackEntity, queue: List<TrackEntity>) {
        val player = _playerState.value ?: return
        _currentTrack.value = track
        _currentQueue.value = queue
        
        viewModelScope.launch {
            repository.insertOrUpdateTrack(track)
            repository.markAsPlayed(track.id)
            extractColors(track)
            checkLyricsAvailable(track)
        }

        val startIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val mediaItems = queue.map { 
            MediaItem.Builder()
                .setUri(android.net.Uri.parse(it.dataPath))
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(android.net.Uri.parse(it.dataPath))
                        .build()
                )
                .setMediaId(it.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(it.customTitle ?: it.title)
                        .setArtist(it.customArtist ?: it.artist)
                        .setAlbumTitle(it.customAlbum ?: it.album)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .apply {
                            if (!it.customCoverPath.isNullOrEmpty()) {
                                val path = it.customCoverPath
                                setArtworkUri(android.net.Uri.parse(if (path.startsWith("/")) "file://$path" else path))
                            } else {
                                val fingerprint = com.example.beatpulse.ui.components.ThumbnailCache.getTrackFingerprint(it)
                                val fullFile = java.io.File(context.cacheDir, "full_${fingerprint}.jpg")
                                if (fullFile.exists() && fullFile.length() > 0) {
                                    setArtworkUri(android.net.Uri.fromFile(fullFile))
                                }
                                // Otherwise, let Media3 extract the ID3 tag natively during prepare()
                            }
                        }
                        .build()
                )
                .build()
        }
        
        player.setMediaItems(mediaItems, startIndex, androidx.media3.common.C.TIME_UNSET)
        player.prepare()
        player.playWhenReady = true
    }

    
    private suspend fun extractColors(track: TrackEntity) {
        // Check cache first — avoids re-reading the file if already processed
        PaletteCache.get(context, track.id)?.let {
            _paletteColors.value = it
            return
        }
        withContext(Dispatchers.IO) {
            try {
                var bitmap: android.graphics.Bitmap? = null
                if (!track.customCoverPath.isNullOrEmpty()) {
                    if (track.customCoverPath.startsWith("http")) {
                        try {
                            val request = okhttp3.Request.Builder().url(track.customCoverPath).build()
                            val response = okhttp3.OkHttpClient().newCall(request).execute()
                            response.body?.byteStream()?.use { inputStream ->
                                bitmap = BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            // fallback
                        }
                    } else {
                        val customFile = java.io.File(track.customCoverPath)
                        if (customFile.exists()) {
                            bitmap = BitmapFactory.decodeFile(customFile.absolutePath)
                        } else {
                            val uri = android.net.Uri.parse(track.customCoverPath)
                            try {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                bitmap = BitmapFactory.decodeStream(inputStream)
                                inputStream?.close()
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                    }
                }
                
                if (bitmap == null && !track.dataPath.startsWith("youtube://")) {
                    try {
                        val mmr = MediaMetadataRetriever()
                        mmr.setDataSource(track.dataPath)
                        val data = mmr.embeddedPicture
                        mmr.release()
                        if (data != null) {
                            bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                        }
                    } catch (e: Exception) {}
                }

                if (bitmap != null) {
                    val palette = Palette.from(bitmap!!).generate()
                    val dominantRaw = palette.getDominantColor(android.graphics.Color.DKGRAY)
                    val colors = PaletteColors(
                        dominant = Color(dominantRaw),
                        vibrant = Color(palette.getVibrantColor(dominantRaw)),
                        muted = Color(palette.getMutedColor(dominantRaw)),
                        darkVibrant = Color(palette.getDarkVibrantColor(dominantRaw)),
                        lightVibrant = Color(palette.getLightVibrantColor(dominantRaw)),
                        darkMuted = Color(palette.getDarkMutedColor(dominantRaw))
                    )
                    PaletteCache.put(context, track.id, colors)
                    _paletteColors.value = colors
                } else {
                    _paletteColors.value = PaletteColors()
                }
            } catch (e: Exception) {
                _paletteColors.value = PaletteColors()
            }
        }
    }
    
    private suspend fun extractColorsFromUri(uriString: String) {
        withContext(Dispatchers.IO) {
            try {
                var bitmap: android.graphics.Bitmap? = null
                val uri = android.net.Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val dominantRaw = palette.getDominantColor(android.graphics.Color.DKGRAY)
                    val colors = PaletteColors(
                        dominant = Color(dominantRaw),
                        vibrant = Color(palette.getVibrantColor(dominantRaw)),
                        muted = Color(palette.getMutedColor(dominantRaw)),
                        darkVibrant = Color(palette.getDarkVibrantColor(dominantRaw)),
                        lightVibrant = Color(palette.getLightVibrantColor(dominantRaw)),
                        darkMuted = Color(palette.getDarkMutedColor(dominantRaw))
                    )
                    _paletteColors.value = colors
                }
            } catch (e: Exception) {}
        }
    }

    fun updateTrackMetadata(id: Long, title: String?, artist: String?, album: String?, coverPath: String?) {
        viewModelScope.launch {
            repository.updateTrackMetadata(id, title, artist, album, coverPath)
            // Update current track if it's the one playing
            if (_currentTrack.value?.id == id) {
                _currentTrack.value = _currentTrack.value?.copy(
                    customTitle = title,
                    customArtist = artist,
                    customAlbum = album,
                    customCoverPath = coverPath
                )
                // Re-extract colors if cover changed
                _currentTrack.value?.let { extractColors(it) }
            }
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        _effectsPreset.value = "CUSTOM"
        val prefs = PreferencesManager.getInstance(context)
        prefs.playbackSpeed = speed
        prefs.effectsPreset = "CUSTOM"
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = "SET_SPEED"
            putExtra("speed", speed)
        }
        context.startService(intent)
    }

    fun setPitch(pitch: Float) {
        _playbackPitch.value = pitch
        _effectsPreset.value = "CUSTOM"
        val prefs = PreferencesManager.getInstance(context)
        prefs.playbackPitch = pitch
        prefs.effectsPreset = "CUSTOM"
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = "SET_PITCH"
            putExtra("pitch", pitch)
        }
        context.startService(intent)
    }

    fun setReverb(enabled: Boolean) {
        _reverbEnabled.value = enabled
        _effectsPreset.value = "CUSTOM"
        val prefs = PreferencesManager.getInstance(context)
        prefs.reverbEnabled = enabled
        prefs.effectsPreset = "CUSTOM"
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = "TOGGLE_REVERB"
            putExtra("enabled", enabled)
        }
        context.startService(intent)
    }

    fun applyPreset(preset: String) {
        val (speed, pitch, reverb) = when (preset) {
            "SLOWED" -> Triple(0.8f, 0.92f, true)
            "NIGHTCORE" -> Triple(1.25f, 1.15f, false)
            "CHIPMUNK" -> Triple(1.0f, 1.5f, false)
            "BASS" -> Triple(0.9f, 0.85f, true)
            else -> Triple(1.0f, 1.0f, false) // NORMAL
        }
        _playbackSpeed.value = speed
        _playbackPitch.value = pitch
        _reverbEnabled.value = reverb
        _effectsPreset.value = preset
        val prefs = PreferencesManager.getInstance(context)
        prefs.playbackSpeed = speed
        prefs.playbackPitch = pitch
        prefs.reverbEnabled = reverb
        prefs.effectsPreset = preset
        context.startService(Intent(context, PlaybackService::class.java).apply {
            action = "SET_SPEED"
            putExtra("speed", speed)
        })
        context.startService(Intent(context, PlaybackService::class.java).apply {
            action = "SET_PITCH"
            putExtra("pitch", pitch)
        })
        context.startService(Intent(context, PlaybackService::class.java).apply {
            action = "TOGGLE_REVERB"
            putExtra("enabled", reverb)
        })
    }

    override fun onCleared() {
        super.onCleared()
        visualizerManager.stop()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        _playerState.value = null
    }
}

data class LrcSearchResult(
    val id: Long,
    val name: String,
    val artistName: String,
    val albumName: String,
    val duration: Long,
    val syncedLyrics: String,
    val plainLyrics: String,
    val score: Double = 0.0
)
