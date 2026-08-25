package com.example.beatpulse

import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IEqualizerManager
import com.example.beatpulse.ui.components.player.IPlayerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RealDesktopEqualizerManager : IEqualizerManager {
    override val isEnabled: StateFlow<Boolean> = MutableStateFlow(false)
}

class DesktopPlayerViewModel : IPlayerViewModel {
    override val abPointA: StateFlow<Float> = MutableStateFlow(0f)
    override val abPointB: StateFlow<Float> = MutableStateFlow(0f)
    override val abRepeatModeEnabled: StateFlow<Boolean> = MutableStateFlow(false)
    override val autoAnalyzeLyrics: StateFlow<Boolean> = MutableStateFlow(false)
    override val availableLyricsResults: StateFlow<List<Any>> = MutableStateFlow(emptyList())
    override val isFetchingLyrics: StateFlow<Boolean> = MutableStateFlow(false)
    override val isMicModeActive: StateFlow<Boolean> = MutableStateFlow(false)
    override val isWifiStreamActive: StateFlow<Boolean> = MutableStateFlow(false)
    override val searchFailed: StateFlow<Boolean> = MutableStateFlow(false)
    override val streamAvatarUri: StateFlow<String?> = MutableStateFlow(null)
    override val streamConfigAspectRatio: StateFlow<String> = MutableStateFlow("16:9")
    override val streamConfigEffectsVisible: StateFlow<Boolean> = MutableStateFlow(false)
    override val streamConfigUiVisible: StateFlow<Boolean> = MutableStateFlow(false)
    override val wifiStreamFps: StateFlow<Int> = MutableStateFlow(30)
    
    override val playerState: StateFlow<Any?> = MutableStateFlow(null)
    override val isPlaying: StateFlow<Boolean> = MutableStateFlow(false)
    override val currentTrack: StateFlow<TrackEntity?> = MutableStateFlow(null)
    override val currentQueue: StateFlow<List<TrackEntity>> = MutableStateFlow(emptyList())
    override val paletteColors: StateFlow<PaletteColors> = MutableStateFlow(PaletteColors())
    override val repeatMode: StateFlow<Int> = MutableStateFlow(0)
    override val shuffleModeEnabled: StateFlow<Boolean> = MutableStateFlow(false)
    override val playbackSpeed: StateFlow<Float> = MutableStateFlow(1f)
    override val playbackPitch: StateFlow<Float> = MutableStateFlow(1f)
    override val reverbEnabled: StateFlow<Boolean> = MutableStateFlow(false)
    override val effectsPreset: StateFlow<String> = MutableStateFlow("None")
    override val currentPosition: StateFlow<Long> = MutableStateFlow(0L)
    override val duration: StateFlow<Long> = MutableStateFlow(1L)
    
    override fun seekTo(position: Long) {}
    override fun seekToNext() {}
    override fun seekToPrevious() {}
    override fun setSpeed(speed: Float) {}
    override fun setPitch(pitch: Float) {}
    override fun toggleMicMode() {}
    override fun togglePlayPause() {}
    override fun play() {}
    override fun pause() {}
    override fun playTrack(track: TrackEntity, queue: List<TrackEntity>) {}
    override fun setReverb(enabled: Boolean) {}
    override fun applyPreset(preset: String) {}
    override fun updateTrackMetadata(id: Long, title: String?, artist: String?, album: String?, coverPath: String?) {}
}
