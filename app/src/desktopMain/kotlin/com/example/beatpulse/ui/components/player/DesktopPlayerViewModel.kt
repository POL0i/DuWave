package com.example.beatpulse.ui.components.player

import com.example.beatpulse.data.AppPreferences
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.data.LrcSearchResult
import com.example.beatpulse.player.AppPlayer
import com.example.beatpulse.player.AppPlayerListener
import com.example.beatpulse.player.IPlayerViewModel
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.utils.LyricLine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

class DesktopPlayerViewModel(
    private val appPlayer: AppPlayer,
    private val repository: MusicRepository,
    private val prefs: AppPreferences
) : IPlayerViewModel {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override val currentTrack = MutableStateFlow<TrackEntity?>(null)
    override val currentLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    override val albumArt = MutableStateFlow<ImageBitmap?>(null)
    override val streamAvatar = MutableStateFlow<ImageBitmap?>(null)
    override val currentQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    override val isPlaying = MutableStateFlow(false)
    override val paletteColors = MutableStateFlow(PaletteColors(Color.Black, Color.Black, Color.Black, Color.Black))
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
    override val availableLyricsResults = MutableStateFlow<List<LrcSearchResult>>(emptyList())
    override val autoAnalyzeLyrics = MutableStateFlow(false)
    override val isMicModeActive = MutableStateFlow(false)
    override val streamAvatarUri = MutableStateFlow<String?>(null)
    override val streamConfigUiVisible = MutableStateFlow(false)
    override val isWifiStreamActive = MutableStateFlow(false)
    override val wifiStreamFps = MutableStateFlow(30)
    override val wifiStreamCustomWidth = MutableStateFlow(1280)
    override val wifiStreamCustomHeight = MutableStateFlow(720)
    override val wifiStreamQuality = MutableStateFlow(80)
    override val streamConfigEffectsVisible = MutableStateFlow(false)
    override val streamConfigAspectRatio = MutableStateFlow("16:9")

    init {
        appPlayer.addListener(object : AppPlayerListener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                this@DesktopPlayerViewModel.isPlaying.value = isPlaying
            }
        })
    }

    override fun playTrack(track: TrackEntity, queue: List<TrackEntity>) {
        currentTrack.value = track
        currentQueue.value = queue
        
        scope.launch {
            repository.insertOrUpdateTrack(track)
            repository.markAsPlayed(track.id)
            // Desktop palette extraction could be added here in the future
        }
        
        appPlayer.setTrack(track.dataPath)
        appPlayer.play()
    }

    override fun setSpeed(speed: Float) {
        playbackSpeed.value = speed
        appPlayer.setPlaybackSpeed(speed)
    }

    override fun setPitch(pitch: Float) {
        playbackPitch.value = pitch
        appPlayer.setPlaybackPitch(pitch)
    }

    override fun setShuffleMode(enabled: Boolean) {
        shuffleModeEnabled.value = enabled
    }

    override fun setRepeatMode(mode: Int) {
        repeatMode.value = mode
    }

    override fun setReverb(enabled: Boolean) {
        reverbEnabled.value = enabled
    }

    override fun applyPreset(preset: String) {
        effectsPreset.value = preset
    }

    override fun updateTrackMetadata(id: Long, title: String?, artist: String?, album: String?, coverPath: String?) {
        scope.launch {
            repository.updateTrackMetadata(id, title, artist, album, coverPath)
        }
    }

    override fun toggleMicMode() {
        isMicModeActive.value = !isMicModeActive.value
    }

    override fun toggleAutoAnalyze() {
        autoAnalyzeLyrics.value = !autoAnalyzeLyrics.value
    }

    override fun toggleStreamConfigEffects() {
        streamConfigEffectsVisible.value = !streamConfigEffectsVisible.value
    }

    override fun setStreamAspectRatio(ratio: String) {
        streamConfigAspectRatio.value = ratio
    }

    override fun updateStreamAvatar(uri: String?) {
        streamAvatarUri.value = uri
    }
}
