package com.example.beatpulse

import com.example.beatpulse.data.AppPreferences
import com.example.beatpulse.ui.components.player.IPreferencesManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class DummyAppPreferences : AppPreferences, IPreferencesManager {
    override var appLanguage: String = "en"
    override var visualizerStyle: String = "bars"
    override var visualizerArchetype: Int = 0
    override var visualizerFftMode: String = "fast"
    override var isAdvancedMode: Boolean = false
    override var visualizerBandsMode: Int = 0
    override var filterMode: String = "none"
    override var physicsMode: String = "none"
    override var filterWhatsAppShorts: Boolean = true
    override var sensitivity: Float = 1f
    override var reactivity: Float = 1f
    override var bassMultiplier: Float = 1f
    override var midMultiplier: Float = 1f
    override var trebleMultiplier: Float = 1f
    override var usePerBandMultiplier: Boolean = false
    private val _lastMainScreenPageFlow = MutableStateFlow(0)
    override val lastMainScreenPageFlow: StateFlow<Int> = _lastMainScreenPageFlow
    override var lastMainScreenPage: Int
        get() = _lastMainScreenPageFlow.value
        set(value) { _lastMainScreenPageFlow.value = value }
    override var lastLibraryTab: Int = 0
    override var lastLibraryGeneralTab: Int = 0
    override var shuffleModeEnabled: Boolean = false
    override var repeatMode: Int = 0
    override var eqEnabled: Boolean = false
    override var eqPreset: Short = 0
    override var eqCustomBands: String = ""
    override var eqAutoMode: Boolean = false
    override var lastPlayedTrackPath: String? = null
    override var hasSeenTutorial: Boolean = true
    override var hasSeenBottomBarHint: Boolean = true
    override var hasSeenPlayerHints: Boolean = true
    override var hasUsedMiniplayerGesture: Boolean = true
    override var hasUsedCoverGesture: Boolean = true
    override var hasUsedPlaylistGesture: Boolean = true
    override var albumArtCenterY: Float = 0f
    override var showGestureFeedback: Boolean = true
    override var librarySortOrder: String = "date_added"
    override var libraryScrollIndex: Int = 0
    override var libraryScrollOffset: Int = 0
    override var playbackSpeed: Float = 1f
    override var playbackPitch: Float = 1f
    override var reverbEnabled: Boolean = false
    override var effectsPreset: String = "none"
    override var lastRecommendationsTimestamp: Long = 0L
    override var cachedRecommendationsJson: String = ""
    private val _backgroundStyleFlow = MutableStateFlow(7)
    override val backgroundStyleFlow: StateFlow<Int> = _backgroundStyleFlow
    override var backgroundStyle: Int
        get() = _backgroundStyleFlow.value
        set(value) { _backgroundStyleFlow.value = value }
    private val _thumbnailShapeFlow = MutableStateFlow(0)
    override val thumbnailShapeFlow: StateFlow<Int> = _thumbnailShapeFlow
    override var thumbnailShape: Int
        get() = _thumbnailShapeFlow.value
        set(value) { _thumbnailShapeFlow.value = value }
    private val _toastFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    override val toastFlow: SharedFlow<String> = _toastFlow
    override fun showToast(message: String) {
        _toastFlow.tryEmit(message)
    }
    override var autoAnalyzeLyrics: Boolean = false
    override var hasUsedNextPrevGesture: Boolean = true
    override var hasUsedSeek10sGesture: Boolean = true
    override var hasUsedVinylSeekGesture: Boolean = true
    override var hasUsedPlaylistSwipeGesture: Boolean = true
    override var showGestureConfirmations: Boolean = false
    override var streamAvatarUri: String? = null
    override var lastVerifiedNewPipeVersion: String = ""
    override var lastServiceDownState: Boolean = false
    override var coverOffsetX: Float = 0f
    override var coverOffsetY: Float = 0f
    override var coverScale: Float = 1f
    private val _isPatreonUnlockedFlow = MutableStateFlow(false)
    override val isPatreonUnlockedFlow: StateFlow<Boolean> = _isPatreonUnlockedFlow
    override var isPatreonUnlocked: Boolean
        get() = _isPatreonUnlockedFlow.value
        set(value) { _isPatreonUnlockedFlow.value = value }
    override var showFps: Boolean = false
    override var showRemainingTime: Boolean = false

    private val _favoriteBackgroundStylesFlow = MutableStateFlow<Set<Int>>(emptySet())
    override val favoriteBackgroundStylesFlow: StateFlow<Set<Int>> = _favoriteBackgroundStylesFlow
    override var favoriteBackgroundStyles: Set<Int>
        get() = _favoriteBackgroundStylesFlow.value
        set(value) { _favoriteBackgroundStylesFlow.value = value }

    private val _favoriteVisualizerStylesFlow = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteVisualizerStylesFlow: StateFlow<Set<String>> = _favoriteVisualizerStylesFlow
    override var favoriteVisualizerStyles: Set<String>
        get() = _favoriteVisualizerStylesFlow.value
        set(value) { _favoriteVisualizerStylesFlow.value = value }

    // Keyboard Shortcuts
    override var keyMapNextPage: String = "Shift+DirectionRight"
    override var keyMapPrevPage: String = "Shift+DirectionLeft"
    override var keyMapSeekForward: String = "DirectionRight"
    override var keyMapSeekBackward: String = "DirectionLeft"
    override var keyMapNavigateUp: String = "DirectionUp"
    override var keyMapNavigateDown: String = "DirectionDown"
    override var keyMapNavigateLeft: String = "DirectionLeft"
    override var keyMapNavigateRight: String = "DirectionRight"
    override var keyMapTabNext: String = "Tab"
    override var keyMapTabPrev: String = "Shift+Tab"
    override var keyMapAction: String = "Enter"
    override var keyMapPlayPause: String = "Spacebar"
}
