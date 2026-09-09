package com.example.beatpulse.ui.components.player

import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.theme.PaletteColors
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

interface IPreferencesManager {
    var showGestureConfirmations: Boolean
    var albumArtCenterY: Float
    var visualizerStyle: String
    var thumbnailShape: Int
    var lastMainScreenPage: Int
    var showRemainingTime: Boolean
    val lastMainScreenPageFlow: kotlinx.coroutines.flow.StateFlow<Int>
    var hasUsedPlaylistSwipeGesture: Boolean
    var hasUsedVinylSeekGesture: Boolean
    var hasUsedNextPrevGesture: Boolean
    var hasUsedSeek10sGesture: Boolean
    var hasSeenTutorial: Boolean
    val thumbnailShapeFlow: StateFlow<Int>
    var visualizerElementSize: Float
    val visualizerElementSizeFlow: StateFlow<Float>
    var coverOffsetX: Float
    var coverOffsetY: Float
    var coverScale: Float
    var isPatreonUnlocked: Boolean
    val isPatreonUnlockedFlow: StateFlow<Boolean>
    var patreonFailedAttempts: Int
    var patreonLockoutTime: Long
    var showFps: Boolean

    // Player Memory
    var isAdvancedMode: Boolean
    var visualizerArchetype: Int
    var sensitivity: Float
    var bassMultiplier: Float
    var midMultiplier: Float
    var trebleMultiplier: Float
    var reactivity: Float
    var damping: Float
    var cleanUiMode: Boolean
    var dynamicColorsPlus: Boolean
    var dynamicColorsInterval: Int
    var repeatMode: Int

    // Keyboard Shortcuts
    var keyMapNextPage: String
    var keyMapPrevPage: String
    var keyMapSeekForward: String
    var keyMapSeekBackward: String
    var keyMapNavigateUp: String
    var keyMapNavigateDown: String
    var keyMapNavigateLeft: String
    var keyMapNavigateRight: String
    var keyMapTabNext: String
    var keyMapTabPrev: String
    var keyMapAction: String
    var keyMapPlayPause: String
    
    // New global and section shortcuts
    var keyMapGlobalList: String
    var keyMapGlobalSearch: String
    var keyMapRecommendations: String
    var keyMapLibraryPlaylists: String
    var keyMapLibraryArtists: String
    var keyMapLibraryAlbums: String
    var keyMapLibraryFolders: String
    var keyMapPlayerScreen: String
    
    var keyMapOpenStats: String
    var keyMapOpenDesign: String
    var keyMapOpenKeyboard: String
    var keyMapOpenTimer: String
    var keyMapOpenEqualizer: String
    var keyMapOpenAudioEffects: String
    var keyMapOpenPatreon: String

    val favoriteBackgroundStylesFlow: StateFlow<Set<Int>>
    var favoriteBackgroundStyles: Set<Int>
    val favoriteVisualizerStylesFlow: StateFlow<Set<String>>
    var favoriteVisualizerStyles: Set<String>
    
    val toastFlow: kotlinx.coroutines.flow.SharedFlow<String>
    fun showToast(message: String)
}

interface IAudioVisualizerManager {
    val bassAmplitudes: StateFlow<FloatArray>
    val midAmplitudes: StateFlow<FloatArray>
    val highAmplitudes: StateFlow<FloatArray>
    val combinedAmplitudes: StateFlow<FloatArray>
    val isAdvancedMode: MutableStateFlow<Boolean>
    val filterMode: MutableStateFlow<Any>
    val sensitivity: MutableStateFlow<Float>
    val reactivity: MutableStateFlow<Float>
    val damping: MutableStateFlow<Float>
    val bassMultiplier: MutableStateFlow<Float>
    val midMultiplier: MutableStateFlow<Float>
    val trebleMultiplier: MutableStateFlow<Float>
    val visualizerArchetype: MutableStateFlow<Int>
    val fftMode: MutableStateFlow<String>
    val elementSize: MutableStateFlow<Float>
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
    fun updateStreamAvatar(uri: String?)
    val streamConfigAspectRatio: StateFlow<String>
    val streamConfigUiVisible: StateFlow<Boolean>
    fun setStreamConfigUiVisible(visible: Boolean)
    val streamConfigEffectsVisible: StateFlow<Boolean>
    fun toggleStreamConfigEffects()
    val wifiStreamFps: StateFlow<Int>
    val coverVisibilityMode: StateFlow<String>
    val chromaKeyColor: StateFlow<String>
    val coverDragEnabled: StateFlow<Boolean>
    val cleanUiMode: StateFlow<Boolean>
    val dynamicColorsPlus: StateFlow<Boolean>
    val dynamicColorsInterval: StateFlow<Int>
    val showFps: StateFlow<Boolean>
    
    val availableAudioDevices: StateFlow<List<String>>
    val selectedAudioDevice: StateFlow<String?>
    fun selectAudioDevice(name: String?)

    val playerState: StateFlow<Any?>
    val isPlaying: StateFlow<Boolean>
    val isBuffering: StateFlow<Boolean>
    var albumArtCenterY: Float?
    val currentTrack: StateFlow<TrackEntity?>
    val currentQueue: StateFlow<List<TrackEntity>>
    val paletteColors: StateFlow<PaletteColors>
    val repeatMode: MutableStateFlow<Int>
    val shuffleModeEnabled: MutableStateFlow<Boolean>
    val playbackSpeed: MutableStateFlow<Float>
    val playbackPitch: StateFlow<Float>
    val reverbEnabled: StateFlow<Boolean>
    val effectsPreset: StateFlow<String>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val systemVolume: StateFlow<Float>

    fun setRepeatMode(mode: Int)

    fun seekTo(position: Long)
    fun seekToNext()
    fun seekToPrevious()
    fun fastForward()
    fun rewind()
    fun setSpeed(speed: Float)
    fun setPitch(pitch: Float)
    fun toggleMicMode()
    fun togglePlayPause()
    fun play()
    fun pause()
    fun setSystemVolume(volume: Float)
    fun playTrack(track: TrackEntity, queue: List<TrackEntity>)
    fun setReverb(enabled: Boolean)
    fun applyPreset(preset: String)
    fun updateTrackMetadata(id: Long, title: String?, artist: String?, album: String?, coverPath: String?)
    
    fun setCoverVisibilityMode(mode: String)
    fun setChromaKeyColor(colorStr: String)
    fun setCoverDragEnabled(enabled: Boolean)
    fun setCleanUiMode(enabled: Boolean)
    fun setDynamicColorsPlus(enabled: Boolean)
    fun setDynamicColorsInterval(seconds: Int)
    
    val coverOffsetX: StateFlow<Float>
    val coverOffsetY: StateFlow<Float>
    val coverScale: StateFlow<Float>
    fun setCoverOffset(x: Float, y: Float)
    fun setCoverScale(scale: Float)
    
    val supportDialogRequested: kotlinx.coroutines.flow.SharedFlow<Unit>
    fun triggerSupportDialog()
    
    val streamConfigDialogRequested: kotlinx.coroutines.flow.SharedFlow<Unit>
    fun triggerStreamConfigDialog()

    val settingsMenuRequested: kotlinx.coroutines.flow.SharedFlow<Unit>
    fun triggerSettingsMenu()
}

interface IEqualizerManager {
    val isEnabled: StateFlow<Boolean>
    val isAutoMode: StateFlow<Boolean>
    val presets: StateFlow<List<Pair<Short, String>>>
    val currentPreset: StateFlow<Short>
    val bands: StateFlow<List<Short>>
    val bandLevels: StateFlow<Map<Short, Short>>
    val minLevel: StateFlow<Short>
    val maxLevel: StateFlow<Short>

    fun setEnabled(enabled: Boolean)
    fun setAutoMode(enabled: Boolean)
    fun setPreset(preset: Short)
    fun getCenterFreq(band: Short): Int
    fun setBandLevel(band: Short, level: Short)
}
