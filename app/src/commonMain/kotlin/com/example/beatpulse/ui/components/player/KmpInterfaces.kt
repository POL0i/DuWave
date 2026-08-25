package com.example.beatpulse.ui.components.player

import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.theme.PaletteColors
import kotlinx.coroutines.flow.StateFlow

interface IPreferencesManager {
    var showGestureConfirmations: Boolean
    var albumArtCenterY: Float
    var visualizerStyle: String
    var thumbnailShape: Int
    var hasUsedPlaylistSwipeGesture: Boolean
    var hasUsedVinylSeekGesture: Boolean
    var hasUsedNextPrevGesture: Boolean
    var hasUsedSeek10sGesture: Boolean
    val thumbnailShapeFlow: StateFlow<Int>
}

interface IAudioVisualizerManager {
    val bassAmplitudes: StateFlow<FloatArray>
    val midAmplitudes: StateFlow<FloatArray>
    val highAmplitudes: StateFlow<FloatArray>
    val combinedAmplitudes: StateFlow<FloatArray>
    val isAdvancedMode: StateFlow<Boolean>
    val filterMode: StateFlow<Int>
    val sensitivity: StateFlow<Float>
    val reactivity: StateFlow<Float>
    val bassMultiplier: StateFlow<Float>
    val midMultiplier: StateFlow<Float>
    val trebleMultiplier: StateFlow<Float>
    val visualizerArchetype: StateFlow<Int>
    val fftMode: StateFlow<Int>
}

interface IPlayerViewModel {
    val abPointA: StateFlow<Float>
    val abPointB: StateFlow<Float>
    val abRepeatModeEnabled: StateFlow<Boolean>
    val autoAnalyzeLyrics: StateFlow<Boolean>
    val availableLyricsResults: StateFlow<List<Any>>
    val isFetchingLyrics: StateFlow<Boolean>
    val isMicModeActive: StateFlow<Boolean>
    val isWifiStreamActive: StateFlow<Boolean>
    val searchFailed: StateFlow<Boolean>
    val streamAvatarUri: StateFlow<String?>
    val streamConfigAspectRatio: StateFlow<String>
    val streamConfigEffectsVisible: StateFlow<Boolean>
    val streamConfigUiVisible: StateFlow<Boolean>
    val wifiStreamFps: StateFlow<Int>

    val playerState: StateFlow<Any?>
    val isPlaying: StateFlow<Boolean>
    val currentTrack: StateFlow<TrackEntity?>
    val currentQueue: StateFlow<List<TrackEntity>>
    val paletteColors: StateFlow<PaletteColors>
    val repeatMode: StateFlow<Int>
    val shuffleModeEnabled: StateFlow<Boolean>
    val playbackSpeed: StateFlow<Float>
    val playbackPitch: StateFlow<Float>
    val reverbEnabled: StateFlow<Boolean>
    val effectsPreset: StateFlow<String>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>

    fun seekTo(position: Long)
    fun seekToNext()
    fun seekToPrevious()
    fun setSpeed(speed: Float)
    fun setPitch(pitch: Float)
    fun toggleMicMode()
    fun togglePlayPause()
    fun play()
    fun pause()
    fun playTrack(track: TrackEntity, queue: List<TrackEntity>)
    fun setReverb(enabled: Boolean)
    fun applyPreset(preset: String)
    fun updateTrackMetadata(id: Long, title: String?, artist: String?, album: String?, coverPath: String?)
}

interface IEqualizerManager {
    val isEnabled: StateFlow<Boolean>
}
