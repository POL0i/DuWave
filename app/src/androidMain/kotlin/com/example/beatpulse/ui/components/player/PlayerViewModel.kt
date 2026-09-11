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
import com.kmpalette.palette.graphics.Palette
import androidx.compose.ui.graphics.asImageBitmap
import com.example.beatpulse.theme.PaletteColors
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.content.Intent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.annotation.SuppressLint
import com.example.beatpulse.ui.components.player.IPlayerViewModel

@SuppressLint("StaticFieldLeak")
class PlayerViewModel(
    private val context: Context,
    private val repository: MusicRepository,
    val visualizerManager: AudioVisualizerManager
) : ViewModel(), IPlayerViewModel {

    override val currentPosition = MutableStateFlow(0L)
    override val duration = MutableStateFlow(0L)
    

    override fun play() {
        _playerState.value?.play()
    }
    override fun pause() {
        _playerState.value?.pause()
    }
    override fun seekTo(position: Long) {
        playerState.value?.seekTo(position)
        currentPosition.value = position
        lastSeekTimeMs = System.currentTimeMillis()
    }

    override fun fastForward() {
        playerState.value?.let { it.seekTo(it.currentPosition + 10000) }
    }
    override fun rewind() {
        playerState.value?.let { it.seekTo(it.currentPosition - 10000) }
    }


    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _playerState = MutableStateFlow<Player?>(null)
    override val playerState: StateFlow<Player?> = _playerState
    override var albumArtCenterY: Float? by mutableStateOf(null)

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    override val currentTrack: StateFlow<TrackEntity?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying
    override val isBuffering: StateFlow<Boolean> = PlaybackService.isBufferingFlow

    override val abRepeatModeEnabled = MutableStateFlow(false)
    override val abPointA = MutableStateFlow(0f)
    override val abPointB = MutableStateFlow(0.5f)

    private val _currentQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    override val currentQueue: StateFlow<List<TrackEntity>> = _currentQueue

    private val _paletteColors = MutableStateFlow(PaletteColors())
    override val paletteColors: StateFlow<PaletteColors> = _paletteColors

    override val repeatMode: MutableStateFlow<Int> = MutableStateFlow(Player.REPEAT_MODE_OFF)
    override val shuffleModeEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val playbackSpeed: MutableStateFlow<Float> = MutableStateFlow(1.0f)

    override fun setRepeatMode(mode: Int) {
        repeatMode.value = mode
        PreferencesManager.getInstance(context).repeatMode = mode
        _playerState.value?.repeatMode = mode
    }

    private val _playbackPitch = MutableStateFlow(1.0f)
    override val playbackPitch: StateFlow<Float> = _playbackPitch

    private val _reverbEnabled = MutableStateFlow(false)
    override val reverbEnabled: StateFlow<Boolean> = _reverbEnabled

    private val _effectsPreset = MutableStateFlow("NORMAL")
    override val effectsPreset: StateFlow<String> = _effectsPreset

    private val _systemVolume = MutableStateFlow(
        // Use system volume up to 1.0f. If preferences had amplification (>1.0), append it.
        com.example.beatpulse.utils.SystemUtils.getSystemVolumeLevel().let { sysVol ->
            val prefVol = PreferencesManager.getInstance(context).systemVolume
            if (prefVol > 1.0f && sysVol >= 0.99f) prefVol else sysVol
        }
    )
    override val systemVolume: StateFlow<Float> = _systemVolume

    override val isFetchingLyrics = MutableStateFlow(false)
    override val searchFailed = MutableStateFlow(false)
    
    // --- Mic/Streamer Mode State ---
    override val isMicModeActive = MutableStateFlow(false)
    override val streamConfigUiVisible = MutableStateFlow(false)
    override fun setStreamConfigUiVisible(visible: Boolean) { streamConfigUiVisible.value = visible }
    override val streamConfigEffectsVisible = MutableStateFlow(true) // Keep effects by default
    override fun toggleStreamConfigEffects() { streamConfigEffectsVisible.value = !streamConfigEffectsVisible.value }
    val streamConfigMiniPlayerVisible = MutableStateFlow(false) // Hide mini player by default
    override val streamConfigAspectRatio = MutableStateFlow("default") // "default" or "16:9"
    override val streamAvatarUri = MutableStateFlow<String?>(PreferencesManager.getInstance(context).streamAvatarUri)
    
    override val isWifiStreamActive = MutableStateFlow(false)
    override val wifiStreamFps = MutableStateFlow(60)
    
    override val coverVisibilityMode = MutableStateFlow("NORMAL")
    override val chromaKeyColor = MutableStateFlow("Green")
    override val coverDragEnabled = MutableStateFlow(false)
    override val cleanUiMode = MutableStateFlow(PreferencesManager.getInstance(context).cleanUiMode)
    override val dynamicColorsPlus = MutableStateFlow(PreferencesManager.getInstance(context).dynamicColorsPlus)
    override val dynamicColorsInterval = MutableStateFlow(PreferencesManager.getInstance(context).dynamicColorsInterval)
    override val coverOffsetX = MutableStateFlow(0f)
    override val coverOffsetY = MutableStateFlow(0f)
    override val coverScale = MutableStateFlow(1f)
    
    override val showFps = MutableStateFlow(PreferencesManager.getInstance(context).showFps)

    private val _supportDialogRequested = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val supportDialogRequested: kotlinx.coroutines.flow.SharedFlow<Unit> = _supportDialogRequested

    override fun triggerSupportDialog() {
        _supportDialogRequested.tryEmit(Unit)
    }

    private val _streamConfigDialogRequested = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val streamConfigDialogRequested: kotlinx.coroutines.flow.SharedFlow<Unit> = _streamConfigDialogRequested

    override fun triggerStreamConfigDialog() {
        _streamConfigDialogRequested.tryEmit(Unit)
    }

    private val _settingsMenuRequested = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val settingsMenuRequested: kotlinx.coroutines.flow.SharedFlow<Unit> = _settingsMenuRequested

    override fun triggerSettingsMenu() {
        _settingsMenuRequested.tryEmit(Unit)
    }

    private var lastSeekTimeMs = 0L

    init {
        // Poll playerState.value for currentPosition and duration
        viewModelScope.launch {
            while(true) {
                if (playerState.value?.isPlaying == true) {
                    if (System.currentTimeMillis() - lastSeekTimeMs > 500) {
                        currentPosition.value = playerState.value?.currentPosition ?: 0L
                    }
                    duration.value = (playerState.value?.duration ?: 1L).coerceAtLeast(1L)
                }
                kotlinx.coroutines.delay(50)
            }
        }
        
        // Poll system volume to sync with hardware button changes
        viewModelScope.launch {
            while(true) {
                val sysVol = com.example.beatpulse.utils.SystemUtils.getSystemVolumeLevel()
                val currentVol = _systemVolume.value
                // If not boosted and system volume changed by > 1%, update our state
                if (currentVol <= 1.0f && kotlin.math.abs(sysVol - currentVol) > 0.01f) {
                    _systemVolume.value = sysVol
                    _playerState.value?.volume = sysVol
                }
                kotlinx.coroutines.delay(500)
            }
        }
        
        val wifiStreamQuality = MutableStateFlow(100)
        val wifiStreamCustomWidth = MutableStateFlow(1920)
        val wifiStreamCustomHeight = MutableStateFlow(1080)
    }

    override val availableAudioDevices = MutableStateFlow<List<String>>(emptyList())
    override val selectedAudioDevice = MutableStateFlow<String?>(null)
    override fun selectAudioDevice(name: String?) {
        selectedAudioDevice.value = name
    }

    override fun updateStreamAvatar(uri: String?) {
        PreferencesManager.getInstance(context).streamAvatarUri = uri
        streamAvatarUri.value = uri
        if (isMicModeActive.value && uri != null) {
            viewModelScope.launch { extractColorsFromUri(uri) }
        } else if (isMicModeActive.value) {
            _currentTrack.value?.let { viewModelScope.launch { extractColors(it) } }
        }
    }

    override fun toggleMicMode() {
        val newState = !isMicModeActive.value
        isMicModeActive.value = newState
        if (newState) {
            // Pause playback when entering Mic Mode
            _playerState.value?.pause()
            streamAvatarUri.value?.let { uri -> viewModelScope.launch { extractColorsFromUri(uri) } }
            visualizerManager.startMicMode(context)
        } else {
            _currentTrack.value?.let { track -> viewModelScope.launch { extractColors(track) } }
            visualizerManager.stopMicMode()
        }
    }
    // -------------------------------
    
    override val autoAnalyzeLyrics = MutableStateFlow(true)
    override val availableLyricsResults = MutableStateFlow<List<LrcSearchResult>>(emptyList())
    
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
        repeatMode.value = prefs.repeatMode
        shuffleModeEnabled.value = prefs.shuffleModeEnabled
        playbackSpeed.value = prefs.playbackSpeed
        _playbackPitch.value = prefs.playbackPitch
        _reverbEnabled.value = prefs.reverbEnabled
        _effectsPreset.value = prefs.effectsPreset
        autoAnalyzeLyrics.value = prefs.autoAnalyzeLyrics
        // Push saved values to service companion flows so service can restore them
        coverOffsetX.value = PreferencesManager.getInstance(context).coverOffsetX
        coverOffsetY.value = PreferencesManager.getInstance(context).coverOffsetY
        coverScale.value = PreferencesManager.getInstance(context).coverScale
        PlaybackService.playbackSpeedFlow.value = prefs.playbackSpeed
        PlaybackService.playbackPitchFlow.value = prefs.playbackPitch
        PlaybackService.reverbEnabledFlow.value = prefs.reverbEnabled
        
        if (prefs.systemVolume > 1.0f) {
            prefs.systemVolume = 1.0f
        }

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
                player.volume = _systemVolume.value
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
                        val lastQueueIdsString = PreferencesManager.getInstance(context).lastQueueIds
                        val allTracks = repository.allTracksFlow.first()
                        var queue = emptyList<TrackEntity>()
                        if (lastQueueIdsString.isNotEmpty()) {
                            val idToTrack = allTracks.associateBy { it.id }
                            queue = lastQueueIdsString.split(",").mapNotNull { idToTrack[it.toLongOrNull()] }
                        }
                        if (queue.isEmpty()) queue = allTracks

                        _currentQueue.value = queue
                        extractColors(lastTrack)
                        
                        val startIndex = queue.indexOfFirst { it.id == lastTrack.id }.coerceAtLeast(0)
                        
                        val mediaItems = queue.map { track ->
                            val rawUri = if (track.dataPath.startsWith("youtube://")) {
                                val videoId = track.dataPath.removePrefix("youtube://").substringBefore("|")
                                "youtube://$videoId"
                            } else {
                                track.dataPath
                            }
                            MediaItem.Builder()
                                .setUri(android.net.Uri.parse(rawUri))
                                .setRequestMetadata(
                                    MediaItem.RequestMetadata.Builder()
                                        .setMediaUri(android.net.Uri.parse(rawUri))
                                        .build()
                                )
                                .setMediaId(track.id.toString())
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(track.customTitle ?: track.title)
                                        .setArtist(track.customArtist ?: track.artist)
                                        .setAlbumTitle(track.customAlbum ?: track.album)
                                        .setIsPlayable(true)
                                        .apply {
                                            if (!track.customCoverPath.isNullOrEmpty()) {
                                                val path = track.customCoverPath
                                                setArtworkUri(android.net.Uri.parse(if (path.startsWith("/")) "file://$path" else path))
                                            }
                                            // Do NOT set artworkUri for internal cache files, it causes SystemUI crash (ENOENT/EACCES)
                                            // Media3 automatically extracts metadata from the file itself for the notification.
                                        }
                                        .build()
                                )
                                .build()
                        }
                        controller?.setMediaItems(mediaItems, startIndex, 0L)
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
                    val lang = PreferencesManager.getInstance(context).appLanguage
                    val locale = java.util.Locale(lang)
                    val conf = context.resources.configuration
                    conf.setLocale(locale)
                    val localizedContext = context.createConfigurationContext(conf)
                    val errorMsg = localizedContext.getString(com.example.beatpulse.R.string.playback_error)
                    android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                    val player = _playerState.value
                    if (player != null) {
                        player.pause()
                    }
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    this@PlayerViewModel.shuffleModeEnabled.value = shuffleModeEnabled
                    PreferencesManager.getInstance(context).shuffleModeEnabled = shuffleModeEnabled
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    this@PlayerViewModel.repeatMode.value = repeatMode
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

    override fun playTrack(track: TrackEntity, queue: List<TrackEntity>) {
        val player = _playerState.value ?: return
        _currentTrack.value = track
        _currentQueue.value = queue
        currentPosition.value = 0L
        
        PreferencesManager.getInstance(context).lastQueueIds = queue.joinToString(",") { it.id.toString() }
        
        viewModelScope.launch {
            repository.insertOrUpdateTrack(track)
            repository.markAsPlayed(track.id)
            extractColors(track)
            checkLyricsAvailable(track)
        }

        val startIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val mediaItems = queue.map { 
            val rawUri = if (it.dataPath.startsWith("youtube://")) {
                val videoId = it.dataPath.removePrefix("youtube://").substringBefore("|")
                "youtube://$videoId"
            } else {
                it.dataPath
            }
            MediaItem.Builder()
                .setUri(android.net.Uri.parse(rawUri))
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(android.net.Uri.parse(rawUri))
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
                            }
                            // Otherwise, let Media3 extract the ID3 tag natively during prepare()
                        }
                        .build()
                )
                .build()
        }
        
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.playWhenReady = true
    }

    override fun seekToNext() {
        _playerState.value?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            } else {
                player.seekToNext()
            }
        }
    }

    override fun seekToPrevious() {
        _playerState.value?.let { player ->
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else {
                player.seekToPrevious()
            }
        }
    }

    override fun togglePlayPause() {
        _playerState.value?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    override fun setSystemVolume(volume: Float) {
        _systemVolume.value = volume
        PreferencesManager.getInstance(context).systemVolume = volume
        
        // Update actual OS volume or internal app volume
        if (volume <= 1.0f) {
            com.example.beatpulse.utils.SystemUtils.setSystemVolumeLevel(volume)
        } else {
            // Keep system volume at max if amplifying
            com.example.beatpulse.utils.SystemUtils.setSystemVolumeLevel(1.0f)
        }
        
        _playerState.value?.volume = volume.coerceAtMost(1.0f)
        
        // Broadcast the volume > 1.0 to PlaybackService for software amplification
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = "SET_VOLUME_AMPLIFICATION"
            putExtra("volume", volume)
        }
        context.startService(intent)
    }    
    private suspend fun extractColors(track: TrackEntity) {
        val fingerprint = com.example.beatpulse.ui.components.ThumbnailCache.getTrackFingerprint(track)
        // Check cache first — avoids re-reading the file if already processed
        PaletteCache.get(fingerprint)?.let {
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
                    val palette = Palette.from(bitmap!!.asImageBitmap()).generate()
                    val dominantRaw = (palette.dominantSwatch?.rgb ?: android.graphics.Color.DKGRAY)
                    val colors = PaletteColors(
                        dominant = Color(dominantRaw),
                        vibrant = Color((palette.vibrantSwatch?.rgb ?: dominantRaw)),
                        muted = Color((palette.mutedSwatch?.rgb ?: dominantRaw)),
                        darkVibrant = Color((palette.darkVibrantSwatch?.rgb ?: dominantRaw)),
                        lightVibrant = Color((palette.lightVibrantSwatch?.rgb ?: dominantRaw)),
                        darkMuted = Color((palette.darkMutedSwatch?.rgb ?: dominantRaw))
                    )
                    PaletteCache.put(fingerprint, colors)
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
                    val palette = Palette.from(bitmap.asImageBitmap()).generate()
                    val dominantRaw = (palette.dominantSwatch?.rgb ?: android.graphics.Color.DKGRAY)
                    val colors = PaletteColors(
                        dominant = Color(dominantRaw),
                        vibrant = Color((palette.vibrantSwatch?.rgb ?: dominantRaw)),
                        muted = Color((palette.mutedSwatch?.rgb ?: dominantRaw)),
                        darkVibrant = Color((palette.darkVibrantSwatch?.rgb ?: dominantRaw)),
                        lightVibrant = Color((palette.lightVibrantSwatch?.rgb ?: dominantRaw)),
                        darkMuted = Color((palette.darkMutedSwatch?.rgb ?: dominantRaw))
                    )
                    _paletteColors.value = colors
                }
            } catch (e: Exception) {}
        }
    }

    override fun setCoverVisibilityMode(mode: String) { coverVisibilityMode.value = mode }
    override fun setChromaKeyColor(colorStr: String) { chromaKeyColor.value = colorStr }
    override fun setCoverDragEnabled(enabled: Boolean) { coverDragEnabled.value = enabled }
    override fun setCleanUiMode(enabled: Boolean) { 
        cleanUiMode.value = enabled
        PreferencesManager.getInstance(context).cleanUiMode = enabled
    }
    override fun setDynamicColorsPlus(enabled: Boolean) { 
        dynamicColorsPlus.value = enabled
        PreferencesManager.getInstance(context).dynamicColorsPlus = enabled
    }
    override fun setDynamicColorsInterval(seconds: Int) { 
        dynamicColorsInterval.value = seconds
        PreferencesManager.getInstance(context).dynamicColorsInterval = seconds
    }
    override fun setCoverOffset(x: Float, y: Float) { 
        coverOffsetX.value = x
        coverOffsetY.value = y
        val prefs = PreferencesManager.getInstance(context)
        prefs.coverOffsetX = x
        prefs.coverOffsetY = y 
    }

    override fun setCoverScale(scale: Float) {
        coverScale.value = scale
        PreferencesManager.getInstance(context).coverScale = scale
    }

    override fun updateTrackMetadata(id: Long, title: String?, artist: String?, album: String?, coverPath: String?) {
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

    override fun setSpeed(speed: Float) {
        playbackSpeed.value = speed
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

    override fun setPitch(pitch: Float) {
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

    override fun setReverb(enabled: Boolean) {
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

    override fun applyPreset(preset: String) {
        val (speed, pitch, reverb) = when (preset) {
            "SLOWED" -> Triple(0.8f, 0.92f, true)
            "NIGHTCORE" -> Triple(1.25f, 1.15f, false)
            "CHIPMUNK" -> Triple(1.0f, 1.5f, false)
            "BASS" -> Triple(0.9f, 0.85f, true)
            else -> Triple(1.0f, 1.0f, false) // NORMAL
        }
        playbackSpeed.value = speed
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
