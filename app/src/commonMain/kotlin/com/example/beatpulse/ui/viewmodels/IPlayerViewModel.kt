package com.example.beatpulse.ui.viewmodels

import kotlinx.coroutines.flow.StateFlow
import com.example.beatpulse.theme.PaletteColors
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.data.LrcSearchResult

interface IPlayerViewModel {
    val playerState: kotlinx.coroutines.flow.StateFlow<Any?>
    val currentTrack: kotlinx.coroutines.flow.StateFlow<TrackEntity?>
    val _isPlaying: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val isPlaying: kotlinx.coroutines.flow.StateFlow<Boolean>
    val abRepeatModeEnabled: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val abPointA: kotlinx.coroutines.flow.MutableStateFlow<Float>
    val abPointB: kotlinx.coroutines.flow.MutableStateFlow<Float>
    val currentQueue: kotlinx.coroutines.flow.StateFlow<List<TrackEntity>>
    val _paletteColors: kotlinx.coroutines.flow.MutableStateFlow<Any>
    val paletteColors: kotlinx.coroutines.flow.StateFlow<PaletteColors>
    val _repeatMode: kotlinx.coroutines.flow.MutableStateFlow<Any>
    val repeatMode: kotlinx.coroutines.flow.StateFlow<Int>
    val _shuffleModeEnabled: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val shuffleModeEnabled: kotlinx.coroutines.flow.StateFlow<Boolean>
    val _playbackSpeed: kotlinx.coroutines.flow.MutableStateFlow<Float>
    val playbackSpeed: kotlinx.coroutines.flow.StateFlow<Float>
    val _playbackPitch: kotlinx.coroutines.flow.MutableStateFlow<Float>
    val playbackPitch: kotlinx.coroutines.flow.StateFlow<Float>
    val _reverbEnabled: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val reverbEnabled: kotlinx.coroutines.flow.StateFlow<Boolean>
    val _effectsPreset: kotlinx.coroutines.flow.MutableStateFlow<Any>
    val effectsPreset: kotlinx.coroutines.flow.StateFlow<String>
    val isFetchingLyrics: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val searchFailed: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val isMicModeActive: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val streamConfigUiVisible: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val streamConfigEffectsVisible: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val streamConfigMiniPlayerVisible: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val streamConfigAspectRatio: kotlinx.coroutines.flow.MutableStateFlow<Any>
    val isWifiStreamActive: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val wifiStreamFps: kotlinx.coroutines.flow.MutableStateFlow<Int>
    val wifiStreamQuality: kotlinx.coroutines.flow.MutableStateFlow<Int>
    val wifiStreamCustomWidth: kotlinx.coroutines.flow.MutableStateFlow<Int>
    val wifiStreamCustomHeight: kotlinx.coroutines.flow.MutableStateFlow<Int>
    fun updateStreamAvatar(uri: String?)
    fun toggleMicMode()
    val autoAnalyzeLyrics: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    fun toggleAutoAnalyze()
    fun checkLyricsAvailable(track: TrackEntity)
    fun saveLyricsAndNotify(track: TrackEntity, result: LrcSearchResult, onResult: (Boolean, String) -> Unit)
    fun playTrack(track: TrackEntity, queue: List<TrackEntity>)
    fun updateTrackMetadata(id: Long, title: String?, artist: String?, album: String?, coverPath: String?)
    fun setSpeed(speed: Float)
    fun setPitch(pitch: Float)
    fun setReverb(enabled: Boolean)
    fun applyPreset(preset: String)
    val currentPosition: kotlinx.coroutines.flow.StateFlow<Long>
    val duration: kotlinx.coroutines.flow.StateFlow<Long>
    fun togglePlayPause()
    fun seekTo(position: Long)
    fun seekToNext()
    fun seekToPrevious()
    fun fastForward()
    fun rewind()

    val coverVisibilityMode: kotlinx.coroutines.flow.StateFlow<String>
    val chromaKeyColor: kotlinx.coroutines.flow.StateFlow<String>
    val coverDragEnabled: kotlinx.coroutines.flow.StateFlow<Boolean>
    val cleanUiMode: kotlinx.coroutines.flow.StateFlow<Boolean>
    val dynamicColorsPlus: kotlinx.coroutines.flow.StateFlow<Boolean>
    val dynamicColorsInterval: kotlinx.coroutines.flow.StateFlow<Int>
    val coverOffsetX: kotlinx.coroutines.flow.StateFlow<Float>
    val coverOffsetY: kotlinx.coroutines.flow.StateFlow<Float>
    val coverScale: kotlinx.coroutines.flow.StateFlow<Float>
    val albumArtCenterY: Float?
    var availableLyricsResults: kotlinx.coroutines.flow.StateFlow<List<LrcSearchResult>>

    fun setCoverVisibilityMode(mode: String)
    fun setChromaKeyColor(color: String)
    fun setCoverDragEnabled(enabled: Boolean)
    fun setCleanUiMode(enabled: Boolean)
    fun setDynamicColorsPlus(enabled: Boolean)
    fun setDynamicColorsInterval(seconds: Int)
    fun setCoverOffset(x: Float, y: Float)
    fun setCoverScale(scale: Float)

    val supportDialogRequested: kotlinx.coroutines.flow.SharedFlow<Unit>
    fun triggerSupportDialog()

    val streamConfigDialogRequested: kotlinx.coroutines.flow.SharedFlow<Unit>
    fun triggerStreamConfigDialog()

    val settingsMenuRequested: kotlinx.coroutines.flow.SharedFlow<Unit>
    fun triggerSettingsMenu()
}
