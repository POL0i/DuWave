package com.example.beatpulse.ui.components.player

import com.example.beatpulse.data.AppPreferences
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.data.LrcSearchResult
import com.example.beatpulse.player.AppPlayer
import com.example.beatpulse.player.AppPlayerListener
import com.example.beatpulse.ui.components.player.IPlayerViewModel
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.utils.LyricLine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap

class DesktopPlayerViewModel(
    private val appPlayer: AppPlayer,
    private val repository: MusicRepository,
    private val onlineRepository: com.example.beatpulse.data.IOnlineMusicRepository,
    private val prefs: AppPreferences
) : IPlayerViewModel {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override val currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val albumArt = MutableStateFlow<ImageBitmap?>(null)
    val streamAvatar = MutableStateFlow<ImageBitmap?>(null)
    override val currentQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    
    override val playerState: StateFlow<Any?> = MutableStateFlow(null)
    override var albumArtCenterY: Float? = null
    override val streamConfigAspectRatio: MutableStateFlow<String> = MutableStateFlow("16:9")
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPlaying
    override val isBuffering = MutableStateFlow(false)
    override val paletteColors = MutableStateFlow(PaletteColors(
        dominant = Color(0xFF1E1E1E),
        vibrant = Color(0xFF00E5FF),
        darkVibrant = Color(0xFF00B8D4),
        lightVibrant = Color(0xFF84FFFF),
        muted = Color(0xFF9E9E9E),
        darkMuted = Color(0xFF616161)
    ))
    override val repeatMode = MutableStateFlow(0)
    override val shuffleModeEnabled = MutableStateFlow(false)
    override val playbackSpeed = MutableStateFlow(1.0f)
    override val playbackPitch = MutableStateFlow(1.0f)
    override val reverbEnabled = MutableStateFlow(false)
    override val effectsPreset = MutableStateFlow("NONE")
    override val abRepeatModeEnabled = MutableStateFlow(false)
    override val abPointA = MutableStateFlow(0f)
    override val abPointB = MutableStateFlow(0f)
    override val isFetchingLyrics = MutableStateFlow(false)
    override val searchFailed = MutableStateFlow(false)
    override val availableLyricsResults = MutableStateFlow<List<Any>>(emptyList())
    override val autoAnalyzeLyrics = MutableStateFlow(false)
    override val isMicModeActive = MutableStateFlow(false)
    override val streamAvatarUri = MutableStateFlow<String?>(null)
    override val streamConfigUiVisible = MutableStateFlow(false)
    override fun setStreamConfigUiVisible(visible: Boolean) { streamConfigUiVisible.value = visible }
    override val streamConfigEffectsVisible = MutableStateFlow(true)
    override val isWifiStreamActive = MutableStateFlow(false)
    override val wifiStreamFps = MutableStateFlow(30)
    
    override val coverVisibilityMode = MutableStateFlow("NORMAL")
    override val chromaKeyColor = MutableStateFlow("Green")
    override val coverDragEnabled = MutableStateFlow(false)
    override val cleanUiMode = MutableStateFlow(false)
    override val dynamicColorsPlus = MutableStateFlow(false)
    override val dynamicColorsInterval = MutableStateFlow(30)
    
    override val coverOffsetX = MutableStateFlow(prefs.coverOffsetX)
    override val coverOffsetY = MutableStateFlow(prefs.coverOffsetY)
    override val coverScale = MutableStateFlow(prefs.coverScale)
    override val showFps = MutableStateFlow(prefs.showFps)

    override val availableAudioDevices = MutableStateFlow<List<String>>(emptyList())
    override val selectedAudioDevice = MutableStateFlow<String?>(null)

    override fun selectAudioDevice(name: String?) {
        selectedAudioDevice.value = name
    }

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

    init {
        try {
            val mixers = javax.sound.sampled.AudioSystem.getMixerInfo()
            val devices = mutableListOf<String>()
            for (info in mixers) {
                val mixer = javax.sound.sampled.AudioSystem.getMixer(info)
                if (mixer.targetLineInfo.isNotEmpty()) {
                    devices.add(info.name)
                }
            }
            availableAudioDevices.value = devices
            if (devices.isNotEmpty()) {
                selectedAudioDevice.value = devices.first()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        appPlayer.addListener(object : AppPlayerListener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                this@DesktopPlayerViewModel._isPlaying.value = isPlaying
                if (isPlaying) startPositionPolling()
            }
        })
        
        scope.launch {
            albumArt.collect { bitmap ->
                if (bitmap != null) {
                    val palette = extractPaletteFast(bitmap)
                    paletteColors.value = palette
                } else {
                    paletteColors.value = PaletteColors(
                        dominant = Color(0xFF1E1E1E),
                        vibrant = Color(0xFF00E5FF),
                        darkVibrant = Color(0xFF00B8D4),
                        lightVibrant = Color(0xFF84FFFF),
                        muted = Color(0xFF9E9E9E),
                        darkMuted = Color(0xFF616161)
                    )
                }
            }
        }
    }
    
    private fun extractPaletteFast(bitmap: ImageBitmap): PaletteColors {
        try {
            val pixelMap = bitmap.toPixelMap()
            val width = pixelMap.width
            val height = pixelMap.height
            
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0
            
            val step = maxOf(1, width / 30)
            
            var maxSat = -1f
            var vibR = 0
            var vibG = 0
            var vibB = 0
            
            for (x in 0 until width step step) {
                for (y in 0 until height step step) {
                    val pixel = pixelMap[x, y]
                    val r = (pixel.red * 255).toInt()
                    val g = (pixel.green * 255).toInt()
                    val b = (pixel.blue * 255).toInt()
                    
                    rSum += r
                    gSum += g
                    bSum += b
                    count++
                    
                    val maxC = maxOf(r, g, b)
                    val minC = minOf(r, g, b)
                    val sat = if (maxC == 0) 0f else (maxC - minC) / maxC.toFloat()
                    if (sat > maxSat && maxC > 50) { 
                        maxSat = sat
                        vibR = r
                        vibG = g
                        vibB = b
                    }
                }
            }
            
            if (count == 0) return paletteColors.value
            
            val avgR = (rSum / count).toInt()
            val avgG = (gSum / count).toInt()
            val avgB = (bSum / count).toInt()
            
            val dominant = Color(avgR, avgG, avgB)
            val vibrant = if (maxSat < 0) dominant else Color(vibR, vibG, vibB)
            
            fun mixColor(c: Color, mix: Color, ratio: Float): Color {
                return Color(
                    red = c.red * (1 - ratio) + mix.red * ratio,
                    green = c.green * (1 - ratio) + mix.green * ratio,
                    blue = c.blue * (1 - ratio) + mix.blue * ratio,
                    alpha = c.alpha
                )
            }
            
            return PaletteColors(
                dominant = dominant,
                vibrant = vibrant,
                muted = mixColor(dominant, Color.Gray, 0.4f),
                darkVibrant = mixColor(vibrant, Color.Black, 0.4f),
                lightVibrant = mixColor(vibrant, Color.White, 0.4f),
                darkMuted = mixColor(dominant, Color.Black, 0.6f)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return paletteColors.value
        }
    }
    
    private var pollingJob: Job? = null
    private fun startPositionPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive && isPlaying.value) {
                currentPosition.value = appPlayer.currentPosition
                duration.value = appPlayer.duration
                delay(250)
            }
        }
    }

    fun playNext() {
        val queue = currentQueue.value
        val track = currentTrack.value
        if (queue.isNotEmpty() && track != null) {
            val idx = queue.indexOfFirst { it.id == track.id }
            if (idx != -1 && idx < queue.size - 1) {
                playTrack(queue[idx + 1], queue)
            } else if (queue.isNotEmpty()) {
                playTrack(queue.first(), queue)
            }
        }
    }

    fun playPrevious() {
        val queue = currentQueue.value
        val track = currentTrack.value
        if (queue.isNotEmpty() && track != null) {
            val idx = queue.indexOfFirst { it.id == track.id }
            if (idx > 0) {
                playTrack(queue[idx - 1], queue)
            } else if (queue.isNotEmpty()) {
                playTrack(queue.last(), queue)
            }
        }
    }

    override fun togglePlayPause() {
        if (isPlaying.value) {
            appPlayer.pause()
        } else {
            appPlayer.play()
        }
    }
    
    override fun play() {
        appPlayer.play()
    }
    
    override fun pause() {
        appPlayer.pause()
    }

    override fun playTrack(track: TrackEntity, queue: List<TrackEntity>) {
        currentTrack.value = track
        currentQueue.value = queue
        
        scope.launch {
            repository.insertOrUpdateTrack(track)
            repository.markAsPlayed(track.id)
            
            val bitmap = com.example.beatpulse.ui.components.loadDesktopThumbnail(track)
            albumArt.value = bitmap
            
            val streamUrl = if (track.dataPath.startsWith("youtube://")) {
                val encryptedUrl = track.dataPath.removePrefix("youtube://")
                onlineRepository.getStreamUrl(encryptedUrl)
            } else {
                track.dataPath
            }

            if (streamUrl != null) {
                withContext(Dispatchers.Main) {
                    (appPlayer as? com.example.beatpulse.player.DesktopPlayerAdapter)?.setTrack(streamUrl, track.duration)
                        ?: appPlayer.setTrack(streamUrl)
                }
            } else {
                withContext(Dispatchers.Main) {
                    prefs.showToast("No se pudo obtener el audio de la canción online")
                }
            }
        }
    }

    override fun setSpeed(speed: Float) {
        playbackSpeed.value = speed
        appPlayer.setPlaybackSpeed(speed)
    }

    override fun setPitch(pitch: Float) {
        playbackPitch.value = pitch
        appPlayer.setPlaybackPitch(pitch)
    }

    fun setShuffleMode(enabled: Boolean) {
        shuffleModeEnabled.value = enabled
    }

    override fun setRepeatMode(mode: Int) {
        repeatMode.value = mode
    }

    override fun setReverb(enabled: Boolean) {
        reverbEnabled.value = enabled
    }


    override val currentPosition = MutableStateFlow(0L)
    override val duration = MutableStateFlow(0L)
    override fun seekTo(position: Long) {
        currentPosition.value = position
        appPlayer.seekTo(position)
    }
    override fun seekToNext() {
        playNext()
    }
    override fun seekToPrevious() {
        playPrevious()
    }
    override fun fastForward() {
        val newPos = currentPosition.value + 10000L
        seekTo(if (newPos > duration.value) duration.value else newPos)
    }
    override fun rewind() {
        val newPos = currentPosition.value - 10000L
        seekTo(if (newPos < 0L) 0L else newPos)
    }
    override fun applyPreset(preset: String) {
        effectsPreset.value = preset
    }

    override fun updateTrackMetadata(id: Long, title: String?, artist: String?, album: String?, coverPath: String?) {
        scope.launch {
            repository.updateTrackMetadata(id, title, artist, album, coverPath)
        }
    }

    override fun setCoverVisibilityMode(mode: String) { coverVisibilityMode.value = mode }
    override fun setChromaKeyColor(colorStr: String) { chromaKeyColor.value = colorStr }
    override fun setCoverDragEnabled(enabled: Boolean) { coverDragEnabled.value = enabled }
    override fun setCleanUiMode(enabled: Boolean) { cleanUiMode.value = enabled }
    override fun setDynamicColorsPlus(enabled: Boolean) { dynamicColorsPlus.value = enabled }
    override fun setDynamicColorsInterval(seconds: Int) { dynamicColorsInterval.value = seconds }
    override fun setCoverOffset(x: Float, y: Float) {
        coverOffsetX.value = x; prefs.coverOffsetX = x
        coverOffsetY.value = y; prefs.coverOffsetY = y
    }
    
    override fun setCoverScale(scale: Float) {
        coverScale.value = scale
        prefs.coverScale = scale
    }

    override fun toggleMicMode() {
        isMicModeActive.value = !isMicModeActive.value
    }

    fun toggleAutoAnalyze() {
        autoAnalyzeLyrics.value = !autoAnalyzeLyrics.value
    }

    override fun toggleStreamConfigEffects() {
        streamConfigEffectsVisible.value = !streamConfigEffectsVisible.value
    }

    fun setStreamAspectRatio(ratio: String) {
        streamConfigAspectRatio.value = ratio
    }

    override fun updateStreamAvatar(uri: String?) {
        streamAvatarUri.value = uri
    }
}
