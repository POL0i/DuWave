package com.example.beatpulse.data

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow

interface AppPreferences {
    var appLanguage: String
    var visualizerStyle: String
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
    var lastMainScreenPage: Int
    var lastLibraryTab: Int
    var lastLibraryGeneralTab: Int
    var shuffleModeEnabled: Boolean
    var repeatMode: Int
    var eqEnabled: Boolean
    var eqPreset: Short
    var eqCustomBands: String
    var eqAutoMode: Boolean
    var lastPlayedTrackPath: String?
    var hasSeenTutorial: Boolean
    var hasSeenBottomBarHint: Boolean
    var hasSeenPlayerHints: Boolean
    var hasUsedMiniplayerGesture: Boolean
    var hasUsedCoverGesture: Boolean
    var hasUsedPlaylistGesture: Boolean
    var albumArtCenterY: Float
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
    val thumbnailShapeFlow: StateFlow<Int>
    var thumbnailShape: Int
    val toastFlow: SharedFlow<String>
    fun showToast(message: String)
    var autoAnalyzeLyrics: Boolean
    var hasUsedNextPrevGesture: Boolean
    var hasUsedSeek10sGesture: Boolean
    var hasUsedVinylSeekGesture: Boolean
    var hasUsedPlaylistSwipeGesture: Boolean
    var showGestureConfirmations: Boolean
    var streamAvatarUri: String?
}
