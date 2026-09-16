package com.example.beatpulse.player

import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.graphics.ImageBitmap
import com.example.beatpulse.utils.LyricLine
import androidx.compose.runtime.MutableState
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.data.LrcSearchResult
import com.example.beatpulse.theme.PaletteColors

interface IPlayerViewModel {
    val currentTrack: StateFlow<TrackEntity?>
    val currentLyrics: StateFlow<List<LyricLine>>
    val albumArt: StateFlow<ImageBitmap?>
    val streamAvatar: StateFlow<ImageBitmap?>
    val currentQueue: StateFlow<List<TrackEntity>>
    val isPlaying: StateFlow<Boolean>
    val paletteColors: StateFlow<PaletteColors>
    val repeatMode: kotlinx.coroutines.flow.MutableStateFlow<Int>
    val shuffleModeEnabled: StateFlow<Boolean>
    val playbackSpeed: StateFlow<Float>
    val playbackPitch: StateFlow<Float>
    val reverbEnabled: StateFlow<Boolean>
    val effectsPreset: StateFlow<String>
    
    val abRepeatModeEnabled: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val abPointA: kotlinx.coroutines.flow.MutableStateFlow<Float>
    val abPointB: kotlinx.coroutines.flow.MutableStateFlow<Float>
    val isFetchingLyrics: StateFlow<Boolean>
    val searchFailed: StateFlow<Boolean>
    val availableLyricsResults: StateFlow<List<LrcSearchResult>>
    val autoAnalyzeLyrics: StateFlow<Boolean>
    
    val isMicModeActive: StateFlow<Boolean>
    val streamAvatarUri: kotlinx.coroutines.flow.MutableStateFlow<String?>
    val streamConfigUiVisible: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val isWifiStreamActive: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    
    val wifiStreamFps: kotlinx.coroutines.flow.MutableStateFlow<Int>
    val wifiStreamCustomWidth: kotlinx.coroutines.flow.MutableStateFlow<Int>
    val wifiStreamCustomHeight: kotlinx.coroutines.flow.MutableStateFlow<Int>
    val wifiStreamQuality: kotlinx.coroutines.flow.MutableStateFlow<Int>
    
    val streamConfigEffectsVisible: kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val streamConfigAspectRatio: kotlinx.coroutines.flow.MutableStateFlow<String>

    fun playTrack(track: TrackEntity, queue: List<TrackEntity>)
    fun setSpeed(speed: Float)
    fun setPitch(pitch: Float)
    fun setShuffleMode(enabled: Boolean)
    fun setRepeatMode(mode: Int)
    fun setReverb(enabled: Boolean)
    fun applyPreset(preset: String)
    fun updateTrackMetadata(id: Long, title: String?, artist: String?, album: String?, coverPath: String?)
    
    fun playNext()
    fun playPrevious()
    fun togglePlayPause()

    fun toggleMicMode()
    fun toggleAutoAnalyze()
    fun toggleStreamConfigEffects()
    fun setStreamAspectRatio(ratio: String)
    fun updateStreamAvatar(uri: String?)
}
