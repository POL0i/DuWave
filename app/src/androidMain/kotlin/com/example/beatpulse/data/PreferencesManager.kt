package com.example.beatpulse.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.channels.BufferOverflow

class PreferencesManager private constructor(context: Context) : AppPreferences {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("beatpulse_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SHUFFLE_MODE = "shuffle_mode"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        private const val KEY_ALBUM_ART_CENTER_Y = "album_art_center_y"
        private const val KEY_THEME = "app_theme"
        private const val KEY_EQ_BAND_PREFIX = "eq_band_"
        private const val KEY_BASS_BOOST = "bass_boost"
        private const val KEY_VIRTUALIZER = "virtualizer"
        private const val KEY_PRESET = "eq_preset"
        private const val KEY_AUTO_ANALYZE_LYRICS = "auto_analyze_lyrics"
        private const val KEY_USED_NEXT_PREV = "used_next_prev"
        private const val KEY_USED_SEEK_10S = "used_seek_10s"
        private const val KEY_USED_VINYL_SEEK = "used_vinyl_seek"
        private const val KEY_USED_PLAYLIST_SWIPE = "used_playlist_swipe"
        private const val KEY_SHOW_GESTURE_CONFIRM = "show_gesture_confirm"
        private const val KEY_LANGUAGE = "appLanguage"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context).also { INSTANCE = it }
            }
        }
    }

    override var appLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "es") ?: "es"
        set(value) { prefs.edit().putString(KEY_LANGUAGE, value).commit() }

    override var visualizerStyle: String
        get() = prefs.getString("visualizerStyle", "BARS") ?: "BARS"
        set(value) = prefs.edit().putString("visualizerStyle", value).apply()

    override var visualizerArchetype: Int
        get() = prefs.getInt("visualizerArchetype", 0) // 0 = Overlapped, 1 = Segmented
        set(value) = prefs.edit().putInt("visualizerArchetype", value).apply()

    override var visualizerFftMode: String
        get() = prefs.getString("visualizerFftMode", "MAX") ?: "MAX"
        set(value) = prefs.edit().putString("visualizerFftMode", value).apply()

    override var isAdvancedMode: Boolean
        get() = prefs.getBoolean("isAdvancedMode", true)
        set(value) = prefs.edit().putBoolean("isAdvancedMode", value).apply()

    override var visualizerBandsMode: Int
        get() = prefs.getInt("visualizerBandsMode", 3) // 3 bands by default
        set(value) = prefs.edit().putInt("visualizerBandsMode", value).apply()

    override var filterMode: String
        get() = prefs.getString("filterMode", "ALL") ?: "ALL"
        set(value) = prefs.edit().putString("filterMode", value).apply()

    override var physicsMode: String
        get() = prefs.getString("physicsMode", "EQUILIBRADO") ?: "EQUILIBRADO"
        set(value) = prefs.edit().putString("physicsMode", value).apply()

    override var filterWhatsAppShorts: Boolean
        get() = prefs.getBoolean("filterWhatsAppShorts", true)
        set(value) = prefs.edit().putBoolean("filterWhatsAppShorts", value).apply()

    override var sensitivity: Float
        get() = prefs.getFloat("sensitivity", 0.8f)
        set(value) = prefs.edit().putFloat("sensitivity", value).apply()

    override var reactivity: Float
        get() = prefs.getFloat("reactivity", 0.8f)
        set(value) = prefs.edit().putFloat("reactivity", value).apply()

    override var bassMultiplier: Float
        get() = prefs.getFloat("bassMultiplier", 2.2f)
        set(value) = prefs.edit().putFloat("bassMultiplier", value).apply()

    override var midMultiplier: Float
        get() = prefs.getFloat("midMultiplier", 1.2f)
        set(value) = prefs.edit().putFloat("midMultiplier", value).apply()

    override var trebleMultiplier: Float
        get() = prefs.getFloat("trebleMultiplier", 0.8f)
        set(value) = prefs.edit().putFloat("trebleMultiplier", value).apply()

    override var usePerBandMultiplier: Boolean
        get() = prefs.getBoolean("usePerBandMultiplier", false)
        set(value) = prefs.edit().putBoolean("usePerBandMultiplier", value).apply()

    private val _lastMainScreenPageFlow = MutableStateFlow(prefs.getInt("lastMainScreenPage", 0))
    override val lastMainScreenPageFlow: StateFlow<Int> = _lastMainScreenPageFlow
    override var lastMainScreenPage: Int
        get() = prefs.getInt("lastMainScreenPage", 0)
        set(value) {
            prefs.edit().putInt("lastMainScreenPage", value).apply()
            _lastMainScreenPageFlow.value = value
        }

    override var lastLibraryTab: Int
        get() = prefs.getInt("lastLibraryTab", 0)
        set(value) = prefs.edit().putInt("lastLibraryTab", value).apply()

    override var lastLibraryGeneralTab: Int
        get() = prefs.getInt("lastLibraryGeneralTab", 0)
        set(value) = prefs.edit().putInt("lastLibraryGeneralTab", value).apply()

    override var shuffleModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHUFFLE_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_SHUFFLE_MODE, value).apply()

    override var repeatMode: Int
        get() = prefs.getInt(KEY_REPEAT_MODE, androidx.media3.common.Player.REPEAT_MODE_OFF)
        set(value) = prefs.edit().putInt(KEY_REPEAT_MODE, value).apply()

    override var eqEnabled: Boolean
        get() = prefs.getBoolean("eqEnabled", false)
        set(value) = prefs.edit().putBoolean("eqEnabled", value).apply()

    override var eqPreset: Short
        get() = prefs.getInt(KEY_PRESET, -1).toShort()
        set(value) = prefs.edit().putInt(KEY_PRESET, value.toInt()).apply()

    override var eqCustomBands: String
        get() = prefs.getString("eqCustomBands", "") ?: ""
        set(value) = prefs.edit().putString("eqCustomBands", value).apply()

    override var eqAutoMode: Boolean
        get() = prefs.getBoolean("eqAutoMode", false)
        set(value) = prefs.edit().putBoolean("eqAutoMode", value).apply()

    override var lastPlayedTrackPath: String?
        get() = prefs.getString("lastPlayedTrackPath", null)
        set(value) = prefs.edit().putString("lastPlayedTrackPath", value).apply()

    override var hasSeenTutorial: Boolean
        get() = prefs.getBoolean("hasSeenTutorial", false)
        set(value) = prefs.edit().putBoolean("hasSeenTutorial", value).apply()

    override var hasSeenBottomBarHint: Boolean
        get() = prefs.getBoolean("hasSeenBottomBarHint", false)
        set(value) = prefs.edit().putBoolean("hasSeenBottomBarHint", value).apply()

    override var hasSeenPlayerHints: Boolean
        get() = prefs.getBoolean("hasSeenPlayerHints", false)
        set(value) = prefs.edit().putBoolean("hasSeenPlayerHints", value).apply()

    override var hasUsedMiniplayerGesture: Boolean
        get() = prefs.getBoolean("hasUsedMiniplayerGesture", false)
        set(value) = prefs.edit().putBoolean("hasUsedMiniplayerGesture", value).apply()

    override var hasUsedCoverGesture: Boolean
        get() = prefs.getBoolean("hasUsedCoverGesture", false)
        set(value) = prefs.edit().putBoolean("hasUsedCoverGesture", value).apply()

    override var hasUsedPlaylistGesture: Boolean
        get() = prefs.getBoolean("hasUsedPlaylistGesture", false)
        set(value) = prefs.edit().putBoolean("hasUsedPlaylistGesture", value).apply()

    override var albumArtCenterY: Float
        get() = prefs.getFloat(KEY_ALBUM_ART_CENTER_Y, -1f)
        set(value) = prefs.edit().putFloat(KEY_ALBUM_ART_CENTER_Y, value).apply()

    override var showGestureFeedback: Boolean
        get() = prefs.getBoolean("showGestureFeedback", true)
        set(value) = prefs.edit().putBoolean("showGestureFeedback", value).apply()

    override var librarySortOrder: String
        get() = prefs.getString("librarySortOrder", "DIRECTORY") ?: "DIRECTORY"
        set(value) = prefs.edit().putString("librarySortOrder", value).apply()

    override var libraryScrollIndex: Int
        get() = prefs.getInt("libraryScrollIndex", 0)
        set(value) = prefs.edit().putInt("libraryScrollIndex", value).apply()

    override var libraryScrollOffset: Int
        get() = prefs.getInt("libraryScrollOffset", 0)
        set(value) = prefs.edit().putInt("libraryScrollOffset", value).apply()

    override var playbackSpeed: Float
        get() = prefs.getFloat("playbackSpeed", 1.0f)
        set(value) = prefs.edit().putFloat("playbackSpeed", value).apply()

    override var playbackPitch: Float
        get() = prefs.getFloat("playbackPitch", 1.0f)
        set(value) = prefs.edit().putFloat("playbackPitch", value).apply()

    override var reverbEnabled: Boolean
        get() = prefs.getBoolean("reverbEnabled", false)
        set(value) = prefs.edit().putBoolean("reverbEnabled", value).apply()

    override var effectsPreset: String
        get() = prefs.getString("effectsPreset", "NORMAL") ?: "NORMAL"
        set(value) = prefs.edit().putString("effectsPreset", value).apply()

    override var lastRecommendationsTimestamp: Long
        get() = prefs.getLong("lastRecommendationsTimestamp", 0L)
        set(value) = prefs.edit().putLong("lastRecommendationsTimestamp", value).apply()

    override var cachedRecommendationsJson: String
        get() = prefs.getString("cachedRecommendationsJson", "") ?: ""
        set(value) = prefs.edit().putString("cachedRecommendationsJson", value).apply()

    private val _backgroundStyleFlow = MutableStateFlow(prefs.getInt("backgroundStyle", 0))
    override val backgroundStyleFlow: StateFlow<Int> = _backgroundStyleFlow

    override var backgroundStyle: Int
        get() = prefs.getInt("backgroundStyle", 0)
        set(value) {
            prefs.edit().putInt("backgroundStyle", value).apply()
            _backgroundStyleFlow.value = value
            val intent = android.content.Intent(appContext, com.example.beatpulse.service.PlaybackService::class.java).apply {
                action = "UPDATE_WIDGET_STYLE"
            }
            try {
                appContext.startService(intent)
            } catch (e: Exception) {}
        }

    private val _thumbnailShapeFlow = MutableStateFlow(prefs.getInt("thumbnailShape", 0))
    override val thumbnailShapeFlow: StateFlow<Int> = _thumbnailShapeFlow

    override var thumbnailShape: Int
        get() = prefs.getInt("thumbnailShape", 0)
        set(value) {
            prefs.edit().putInt("thumbnailShape", value).apply()
            _thumbnailShapeFlow.value = value
        }
    private val _toastFlow = MutableSharedFlow<String>(extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val toastFlow: SharedFlow<String> = _toastFlow

    override fun showToast(message: String) {
        _toastFlow.tryEmit(message)
    }

    override var autoAnalyzeLyrics: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ANALYZE_LYRICS, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_ANALYZE_LYRICS, value).apply()
        
    override var hasUsedNextPrevGesture: Boolean
        get() = prefs.getBoolean(KEY_USED_NEXT_PREV, false)
        set(value) = prefs.edit().putBoolean(KEY_USED_NEXT_PREV, value).apply()

    override var hasUsedSeek10sGesture: Boolean
        get() = prefs.getBoolean(KEY_USED_SEEK_10S, false)
        set(value) = prefs.edit().putBoolean(KEY_USED_SEEK_10S, value).apply()

    override var hasUsedVinylSeekGesture: Boolean
        get() = prefs.getBoolean(KEY_USED_VINYL_SEEK, false)
        set(value) = prefs.edit().putBoolean(KEY_USED_VINYL_SEEK, value).apply()

    override var hasUsedPlaylistSwipeGesture: Boolean
        get() = prefs.getBoolean(KEY_USED_PLAYLIST_SWIPE, false)
        set(value) = prefs.edit().putBoolean(KEY_USED_PLAYLIST_SWIPE, value).apply()

    override var showGestureConfirmations: Boolean
        get() = prefs.getBoolean(KEY_SHOW_GESTURE_CONFIRM, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_GESTURE_CONFIRM, value).apply()

    override var streamAvatarUri: String?
        get() = prefs.getString("streamAvatarUri", null)
        set(value) = prefs.edit().putString("streamAvatarUri", value).apply()

    override var lastVerifiedNewPipeVersion: String
        get() = prefs.getString("lastVerifiedNewPipeVersion", "") ?: ""
        set(value) = prefs.edit().putString("lastVerifiedNewPipeVersion", value).apply()

    override var lastServiceDownState: Boolean
        get() = prefs.getBoolean("lastServiceDownState", false)
        set(value) = prefs.edit().putBoolean("lastServiceDownState", value).apply()
    override var coverOffsetX: Float
        get() = prefs.getFloat("coverOffsetX", 0f)
        set(value) = prefs.edit().putFloat("coverOffsetX", value).apply()

    override var coverOffsetY: Float
        get() = prefs.getFloat("coverOffsetY", 0f)
        set(value) = prefs.edit().putFloat("coverOffsetY", value).apply()

    override var coverScale: Float
        get() = prefs.getFloat("coverScale", 1.0f)
        set(value) = prefs.edit().putFloat("coverScale", value).apply()

    private val _isPatreonUnlockedFlow = MutableStateFlow(prefs.getBoolean("isPatreonUnlocked", false))
    override val isPatreonUnlockedFlow: StateFlow<Boolean> = _isPatreonUnlockedFlow
    override var isPatreonUnlocked: Boolean
        get() = _isPatreonUnlockedFlow.value
        set(value) {
            _isPatreonUnlockedFlow.value = value
            prefs.edit().putBoolean("isPatreonUnlocked", value).apply()
        }

    override var showFps: Boolean
        get() = prefs.getBoolean("showFps", false)
        set(value) = prefs.edit().putBoolean("showFps", value).apply()

    override var showRemainingTime: Boolean
        get() = prefs.getBoolean("showRemainingTime", false)
        set(value) = prefs.edit().putBoolean("showRemainingTime", value).apply()

    private val _favoriteBackgroundStylesFlow = MutableStateFlow(
        prefs.getStringSet("favoriteBackgroundStyles", emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    )
    override val favoriteBackgroundStylesFlow: StateFlow<Set<Int>> = _favoriteBackgroundStylesFlow

    override var favoriteBackgroundStyles: Set<Int>
        get() = _favoriteBackgroundStylesFlow.value
        set(value) {
            prefs.edit().putStringSet("favoriteBackgroundStyles", value.map { it.toString() }.toSet()).apply()
            _favoriteBackgroundStylesFlow.value = value
        }

    private val _favoriteVisualizerStylesFlow = MutableStateFlow(
        prefs.getStringSet("favoriteVisualizerStyles", emptySet()) ?: emptySet()
    )
    override val favoriteVisualizerStylesFlow: StateFlow<Set<String>> = _favoriteVisualizerStylesFlow

    override var favoriteVisualizerStyles: Set<String>
        get() = _favoriteVisualizerStylesFlow.value
        set(value) {
            prefs.edit().putStringSet("favoriteVisualizerStyles", value).apply()
            _favoriteVisualizerStylesFlow.value = value
        }
}
