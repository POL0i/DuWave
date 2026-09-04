package com.example.beatpulse

import com.example.beatpulse.data.AppPreferences
import com.example.beatpulse.ui.components.player.IPreferencesManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class DesktopAppPreferences : AppPreferences, IPreferencesManager {
    private val prefsFile = File(System.getProperty("user.home"), ".beatpulse/prefs.json")
    private val cache = mutableMapOf<String, String>()

    init {
        load()
    }

    private fun load() {
        if (!prefsFile.exists()) return
        try {
            val text = prefsFile.readText().trim()
            val regex = "\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|[^,\\}]+)".toRegex()
            for (match in regex.findAll(text)) {
                val key = match.groupValues[1]
                var value = match.groupValues[2].trim()
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length - 1)
                }
                cache[key] = value
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun save() {
        try {
            prefsFile.parentFile?.mkdirs()
            val json = cache.entries.joinToString(prefix = "{\n", postfix = "\n}", separator = ",\n") { (k, v) ->
                val vStr = if (v == "true" || v == "false" || v.toDoubleOrNull() != null) v else "\"$v\""
                "  \"$k\": $vStr"
            }
            prefsFile.writeText(json)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun getString(key: String, def: String): String = cache[key] ?: def
    private fun setString(key: String, value: String?) { 
        if (value == null) cache.remove(key) else cache[key] = value
        save() 
    }

    private fun getBoolean(key: String, def: Boolean): Boolean = cache[key]?.toBooleanStrictOrNull() ?: def
    private fun setBoolean(key: String, value: Boolean) { cache[key] = value.toString(); save() }

    private fun getInt(key: String, def: Int): Int = cache[key]?.toIntOrNull() ?: def
    private fun setInt(key: String, value: Int) { cache[key] = value.toString(); save() }

    private fun getLong(key: String, def: Long): Long = cache[key]?.toLongOrNull() ?: def
    private fun setLong(key: String, value: Long) { cache[key] = value.toString(); save() }

    private fun getFloat(key: String, def: Float): Float = cache[key]?.toFloatOrNull() ?: def
    private fun setFloat(key: String, value: Float) { cache[key] = value.toString(); save() }

    override var appLanguage: String
        get() = getString("appLanguage", "en")
        set(value) = setString("appLanguage", value)
    override var visualizerStyle: String
        get() = getString("visualizerStyle", "bars")
        set(value) = setString("visualizerStyle", value)
    override var visualizerArchetype: Int
        get() = getInt("visualizerArchetype", 0)
        set(value) = setInt("visualizerArchetype", value)
    override var visualizerFftMode: String
        get() = getString("visualizerFftMode", "fast")
        set(value) = setString("visualizerFftMode", value)
    override var isAdvancedMode: Boolean
        get() = getBoolean("isAdvancedMode", false)
        set(value) = setBoolean("isAdvancedMode", value)
    override var visualizerBandsMode: Int
        get() = getInt("visualizerBandsMode", 0)
        set(value) = setInt("visualizerBandsMode", value)
    override var filterMode: String
        get() = getString("filterMode", "none")
        set(value) = setString("filterMode", value)
    override var physicsMode: String
        get() = getString("physicsMode", "none")
        set(value) = setString("physicsMode", value)
    override var filterWhatsAppShorts: Boolean
        get() = getBoolean("filterWhatsAppShorts", true)
        set(value) = setBoolean("filterWhatsAppShorts", value)
    override var sensitivity: Float
        get() = getFloat("sensitivity", 1f)
        set(value) = setFloat("sensitivity", value)
    override var reactivity: Float
        get() = getFloat("reactivity", 1f)
        set(value) = setFloat("reactivity", value)
    override var bassMultiplier: Float
        get() = getFloat("bassMultiplier", 1f)
        set(value) = setFloat("bassMultiplier", value)
    override var midMultiplier: Float
        get() = getFloat("midMultiplier", 1f)
        set(value) = setFloat("midMultiplier", value)
    override var trebleMultiplier: Float
        get() = getFloat("trebleMultiplier", 1f)
        set(value) = setFloat("trebleMultiplier", value)
    override var usePerBandMultiplier: Boolean
        get() = getBoolean("usePerBandMultiplier", false)
        set(value) = setBoolean("usePerBandMultiplier", value)

    private val _lastMainScreenPageFlow = MutableStateFlow(getInt("lastMainScreenPage", 0))
    override val lastMainScreenPageFlow: StateFlow<Int> = _lastMainScreenPageFlow
    override var lastMainScreenPage: Int
        get() = _lastMainScreenPageFlow.value
        set(value) { 
            _lastMainScreenPageFlow.value = value
            setInt("lastMainScreenPage", value)
        }

    override var lastLibraryTab: Int
        get() = getInt("lastLibraryTab", 0)
        set(value) = setInt("lastLibraryTab", value)
    override var lastLibraryGeneralTab: Int
        get() = getInt("lastLibraryGeneralTab", 0)
        set(value) = setInt("lastLibraryGeneralTab", value)
    override var shuffleModeEnabled: Boolean
        get() = getBoolean("shuffleModeEnabled", false)
        set(value) = setBoolean("shuffleModeEnabled", value)
    override var repeatMode: Int
        get() = getInt("repeatMode", 0)
        set(value) = setInt("repeatMode", value)
    override var eqEnabled: Boolean
        get() = getBoolean("eqEnabled", false)
        set(value) = setBoolean("eqEnabled", value)
    override var eqPreset: Short
        get() = getInt("eqPreset", 0).toShort()
        set(value) = setInt("eqPreset", value.toInt())
    override var eqCustomBands: String
        get() = getString("eqCustomBands", "")
        set(value) = setString("eqCustomBands", value)
    override var eqAutoMode: Boolean
        get() = getBoolean("eqAutoMode", false)
        set(value) = setBoolean("eqAutoMode", value)
    override var lastPlayedTrackPath: String?
        get() = cache["lastPlayedTrackPath"]
        set(value) = setString("lastPlayedTrackPath", value)
    override var hasSeenTutorial: Boolean
        get() = getBoolean("hasSeenTutorial", false)
        set(value) = setBoolean("hasSeenTutorial", value)
    override var hasSeenBottomBarHint: Boolean
        get() = getBoolean("hasSeenBottomBarHint", false)
        set(value) = setBoolean("hasSeenBottomBarHint", value)
    override var hasSeenPlayerHints: Boolean
        get() = getBoolean("hasSeenPlayerHints", false)
        set(value) = setBoolean("hasSeenPlayerHints", value)
    override var hasUsedMiniplayerGesture: Boolean
        get() = getBoolean("hasUsedMiniplayerGesture", false)
        set(value) = setBoolean("hasUsedMiniplayerGesture", value)
    override var hasUsedCoverGesture: Boolean
        get() = getBoolean("hasUsedCoverGesture", false)
        set(value) = setBoolean("hasUsedCoverGesture", value)
    override var hasUsedPlaylistGesture: Boolean
        get() = getBoolean("hasUsedPlaylistGesture", false)
        set(value) = setBoolean("hasUsedPlaylistGesture", value)
    override var albumArtCenterY: Float
        get() = getFloat("albumArtCenterY", 0f)
        set(value) = setFloat("albumArtCenterY", value)
    override var showGestureFeedback: Boolean
        get() = getBoolean("showGestureFeedback", true)
        set(value) = setBoolean("showGestureFeedback", value)
    override var librarySortOrder: String
        get() = getString("librarySortOrder", "date_added")
        set(value) = setString("librarySortOrder", value)
    override var libraryScrollIndex: Int
        get() = getInt("libraryScrollIndex", 0)
        set(value) = setInt("libraryScrollIndex", value)
    override var libraryScrollOffset: Int
        get() = getInt("libraryScrollOffset", 0)
        set(value) = setInt("libraryScrollOffset", value)
    override var playbackSpeed: Float
        get() = getFloat("playbackSpeed", 1f)
        set(value) = setFloat("playbackSpeed", value)
    override var playbackPitch: Float
        get() = getFloat("playbackPitch", 1f)
        set(value) = setFloat("playbackPitch", value)
    override var reverbEnabled: Boolean
        get() = getBoolean("reverbEnabled", false)
        set(value) = setBoolean("reverbEnabled", value)
    override var effectsPreset: String
        get() = getString("effectsPreset", "none")
        set(value) = setString("effectsPreset", value)
    override var lastRecommendationsTimestamp: Long
        get() = getLong("lastRecommendationsTimestamp", 0L)
        set(value) = setLong("lastRecommendationsTimestamp", value)
    override var cachedRecommendationsJson: String
        get() = getString("cachedRecommendationsJson", "")
        set(value) = setString("cachedRecommendationsJson", value)

    private val _backgroundStyleFlow = MutableStateFlow(getInt("backgroundStyle", 7))
    override val backgroundStyleFlow: StateFlow<Int> = _backgroundStyleFlow
    override var backgroundStyle: Int
        get() = _backgroundStyleFlow.value
        set(value) { 
            _backgroundStyleFlow.value = value
            setInt("backgroundStyle", value)
        }

    private val _thumbnailShapeFlow = MutableStateFlow(getInt("thumbnailShape", 0))
    override val thumbnailShapeFlow: StateFlow<Int> = _thumbnailShapeFlow
    override var thumbnailShape: Int
        get() = _thumbnailShapeFlow.value
        set(value) { 
            _thumbnailShapeFlow.value = value
            setInt("thumbnailShape", value)
        }

    private val _toastFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    override val toastFlow: SharedFlow<String> = _toastFlow
    override fun showToast(message: String) {
        _toastFlow.tryEmit(message)
    }

    override var autoAnalyzeLyrics: Boolean
        get() = getBoolean("autoAnalyzeLyrics", false)
        set(value) = setBoolean("autoAnalyzeLyrics", value)
    override var hasUsedNextPrevGesture: Boolean
        get() = getBoolean("hasUsedNextPrevGesture", false)
        set(value) = setBoolean("hasUsedNextPrevGesture", value)
    override var hasUsedSeek10sGesture: Boolean
        get() = getBoolean("hasUsedSeek10sGesture", false)
        set(value) = setBoolean("hasUsedSeek10sGesture", value)
    override var hasUsedVinylSeekGesture: Boolean
        get() = getBoolean("hasUsedVinylSeekGesture", false)
        set(value) = setBoolean("hasUsedVinylSeekGesture", value)
    override var hasUsedPlaylistSwipeGesture: Boolean
        get() = getBoolean("hasUsedPlaylistSwipeGesture", false)
        set(value) = setBoolean("hasUsedPlaylistSwipeGesture", value)
    override var showGestureConfirmations: Boolean
        get() = getBoolean("showGestureConfirmations", false)
        set(value) = setBoolean("showGestureConfirmations", value)
    override var streamAvatarUri: String?
        get() = cache["streamAvatarUri"]
        set(value) = setString("streamAvatarUri", value)
    override var lastVerifiedNewPipeVersion: String
        get() = getString("lastVerifiedNewPipeVersion", "")
        set(value) = setString("lastVerifiedNewPipeVersion", value)
    override var lastServiceDownState: Boolean
        get() = getBoolean("lastServiceDownState", false)
        set(value) = setBoolean("lastServiceDownState", value)

    override var coverOffsetX: Float
        get() = getFloat("coverOffsetX", 0f)
        set(value) = setFloat("coverOffsetX", value)

    override var coverOffsetY: Float
        get() = getFloat("coverOffsetY", 0f)
        set(value) = setFloat("coverOffsetY", value)

    override var coverScale: Float
        get() = getFloat("coverScale", 1f)
        set(value) = setFloat("coverScale", value)

    private val _isPatreonUnlockedFlow = MutableStateFlow(getBoolean("isPatreonUnlocked", false))
    override val isPatreonUnlockedFlow: StateFlow<Boolean> = _isPatreonUnlockedFlow
    override var isPatreonUnlocked: Boolean
        get() = _isPatreonUnlockedFlow.value
        set(value) {
            _isPatreonUnlockedFlow.value = value
            setBoolean("isPatreonUnlocked", value)
        }

    override var showFps: Boolean
        get() = getBoolean("showFps", false)
        set(value) = setBoolean("showFps", value)

    override var showRemainingTime: Boolean
        get() = getBoolean("showRemainingTime", false)
        set(value) = setBoolean("showRemainingTime", value)

    private val _favoriteBackgroundStylesFlow = MutableStateFlow(
        getString("favoriteBackgroundStyles", "").split(",").mapNotNull { it.toIntOrNull() }.toSet()
    )
    override val favoriteBackgroundStylesFlow: StateFlow<Set<Int>> = _favoriteBackgroundStylesFlow

    override var favoriteBackgroundStyles: Set<Int>
        get() = _favoriteBackgroundStylesFlow.value
        set(value) {
            _favoriteBackgroundStylesFlow.value = value
            setString("favoriteBackgroundStyles", value.joinToString(","))
        }

    private val _favoriteVisualizerStylesFlow = MutableStateFlow(
        getString("favoriteVisualizerStyles", "").split(",").filter { it.isNotBlank() }.toSet()
    )
    override val favoriteVisualizerStylesFlow: StateFlow<Set<String>> = _favoriteVisualizerStylesFlow

    override var favoriteVisualizerStyles: Set<String>
        get() = _favoriteVisualizerStylesFlow.value
        set(value) {
            _favoriteVisualizerStylesFlow.value = value
            setString("favoriteVisualizerStyles", value.joinToString(","))
        }
}
