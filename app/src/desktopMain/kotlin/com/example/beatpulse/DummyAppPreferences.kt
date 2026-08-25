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
    override var lastMainScreenPage: Int = 0
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
    override val backgroundStyleFlow: StateFlow<Int> = MutableStateFlow(0)
    override var backgroundStyle: Int = 0
    override val thumbnailShapeFlow: StateFlow<Int> = MutableStateFlow(0)
    override var thumbnailShape: Int = 0
    override val toastFlow: SharedFlow<String> = MutableSharedFlow()
    override fun showToast(message: String) {}
    override var autoAnalyzeLyrics: Boolean = false
    override var hasUsedNextPrevGesture: Boolean = true
    override var hasUsedSeek10sGesture: Boolean = true
    override var hasUsedVinylSeekGesture: Boolean = true
    override var hasUsedPlaylistSwipeGesture: Boolean = true
    override var showGestureConfirmations: Boolean = false
    override var streamAvatarUri: String? = null
    override var lastVerifiedNewPipeVersion: String = ""
    override var lastServiceDownState: Boolean = false
}
