package com.example.beatpulse.data

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow

import com.example.beatpulse.ui.components.player.IPreferencesManager

interface AppPreferences : IPreferencesManager {
    var appLanguage: String
    override var visualizerStyle: String
    var visualizerArchetype: Int
    var visualizerFftMode: String
    var isAdvancedMode: Boolean
    var visualizerBandsMode: Int
    var filterMode: String
    var physicsMode: String
    var filterWhatsAppShorts: Boolean
    var sensitivity: Float
    var reactivity: Float
    var bassMultiplier: Float
    var midMultiplier: Float
    var trebleMultiplier: Float
    var usePerBandMultiplier: Boolean
    override var lastMainScreenPage: Int
    override val lastMainScreenPageFlow: kotlinx.coroutines.flow.StateFlow<Int>
    var lastLibraryTab: Int
    var lastLibraryGeneralTab: Int
    var shuffleModeEnabled: Boolean
    var repeatMode: Int
    var eqEnabled: Boolean
    var eqPreset: Short
    var eqCustomBands: String
    var eqAutoMode: Boolean
    var lastPlayedTrackPath: String?
    override var hasSeenTutorial: Boolean
    var hasSeenBottomBarHint: Boolean
    var hasSeenPlayerHints: Boolean
    var hasUsedMiniplayerGesture: Boolean
    var hasUsedCoverGesture: Boolean
    var hasUsedPlaylistGesture: Boolean
    override var albumArtCenterY: Float
    var showGestureFeedback: Boolean
    var librarySortOrder: String
    var libraryScrollIndex: Int
    var libraryScrollOffset: Int
    var playbackSpeed: Float
    var playbackPitch: Float
    var reverbEnabled: Boolean
    var effectsPreset: String
    var lastRecommendationsTimestamp: Long
    var cachedRecommendationsJson: String
    val backgroundStyleFlow: StateFlow<Int>
    var backgroundStyle: Int
    override val thumbnailShapeFlow: StateFlow<Int>
    override var thumbnailShape: Int
    val toastFlow: SharedFlow<String>
    fun showToast(message: String)
    var autoAnalyzeLyrics: Boolean
    override var hasUsedNextPrevGesture: Boolean
    override var hasUsedSeek10sGesture: Boolean
    override var hasUsedVinylSeekGesture: Boolean
    override var hasUsedPlaylistSwipeGesture: Boolean
    override var showGestureConfirmations: Boolean
    var streamAvatarUri: String?
    var lastVerifiedNewPipeVersion: String
    var lastServiceDownState: Boolean
    override var isPatreonUnlocked: Boolean
    override val isPatreonUnlockedFlow: StateFlow<Boolean>
}
