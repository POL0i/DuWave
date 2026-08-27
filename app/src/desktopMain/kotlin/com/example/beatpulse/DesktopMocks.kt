package com.example.beatpulse

import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.IEqualizerManager
import com.example.beatpulse.ui.components.player.IPlayerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RealDesktopEqualizerManager : IEqualizerManager {
    override val isEnabled: StateFlow<Boolean> = MutableStateFlow(false)
    override val isAutoMode: StateFlow<Boolean> = MutableStateFlow(false)
    override val presets: StateFlow<List<Pair<Short, String>>> = MutableStateFlow(emptyList())
    override val currentPreset: StateFlow<Short> = MutableStateFlow(0)
    override val bands: StateFlow<List<Short>> = MutableStateFlow(emptyList())
    override val bandLevels: StateFlow<Map<Short, Short>> = MutableStateFlow(emptyMap())
    override val minLevel: StateFlow<Short> = MutableStateFlow(0)
    override val maxLevel: StateFlow<Short> = MutableStateFlow(0)

    override fun setEnabled(enabled: Boolean) {}
    override fun setAutoMode(enabled: Boolean) {}
    override fun setPreset(preset: Short) {}
    override fun getCenterFreq(band: Short): Int = 0
    override fun setBandLevel(band: Short, level: Short) {}
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
    
    override val coverVisibilityMode = MutableStateFlow("NORMAL")
    override val chromaKeyColor = MutableStateFlow("Green")
    override val coverDragEnabled = MutableStateFlow(false)
    override val cleanUiMode = MutableStateFlow(false)
    override val dynamicColorsPlus = MutableStateFlow(false)
    override val dynamicColorsInterval = MutableStateFlow(30)
    override var albumArtCenterY: Float? = null
    
    private val _supportDialogRequested = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    override val supportDialogRequested: kotlinx.coroutines.flow.SharedFlow<Unit> = _supportDialogRequested
    override fun triggerSupportDialog() {}
    
    override val coverOffsetX = MutableStateFlow(0f)
    override val coverOffsetY = MutableStateFlow(0f)
    override val wifiStreamFps: StateFlow<Int> = MutableStateFlow(30)
    
    override val playerState: StateFlow<Any?> = MutableStateFlow(null)
    override val isPlaying: StateFlow<Boolean> = MutableStateFlow(false)
    override val currentTrack: StateFlow<TrackEntity?> = MutableStateFlow(null)
    override val currentQueue: StateFlow<List<TrackEntity>> = MutableStateFlow(emptyList())
    override val paletteColors: StateFlow<PaletteColors> = MutableStateFlow(PaletteColors())
    override val repeatMode: MutableStateFlow<Int> = MutableStateFlow(0)
    override val shuffleModeEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val playbackSpeed: MutableStateFlow<Float> = MutableStateFlow(1f)
    override val playbackPitch: StateFlow<Float> = MutableStateFlow(1f)
    override val reverbEnabled: StateFlow<Boolean> = MutableStateFlow(false)
    override val effectsPreset: StateFlow<String> = MutableStateFlow("None")
    override val currentPosition: StateFlow<Long> = MutableStateFlow(0L)
    override val duration: StateFlow<Long> = MutableStateFlow(1L)
    
    override fun seekTo(position: Long) {}
    override fun seekToNext() {}
    override fun seekToPrevious() {}
    override fun fastForward() {}
    override fun rewind() {}
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
    
    override fun setCoverVisibilityMode(mode: String) {}
    override fun setChromaKeyColor(colorStr: String) {}
    override fun setCoverDragEnabled(enabled: Boolean) {}
    override fun setCleanUiMode(enabled: Boolean) {}
    override fun setDynamicColorsPlus(enabled: Boolean) {}
    override fun setDynamicColorsInterval(seconds: Int) {}
    override fun setCoverOffset(x: Float, y: Float) {}
    override val coverScale: StateFlow<Float> = MutableStateFlow(1f)
    override fun setCoverScale(scale: Float) {}

    private val _streamConfigDialogRequested = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    override val streamConfigDialogRequested: kotlinx.coroutines.flow.SharedFlow<Unit> = _streamConfigDialogRequested
    override fun triggerStreamConfigDialog() {}

    override val settingsMenuRequested = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    override fun triggerSettingsMenu() {}
}
