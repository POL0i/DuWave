package com.example.beatpulse.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.channels.BufferOverflow

class PreferencesManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("beatpulse_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context).also { INSTANCE = it }
            }
        }
    }

    var visualizerStyle: String
        get() = prefs.getString("visualizerStyle", "BARS") ?: "BARS"
        set(value) = prefs.edit().putString("visualizerStyle", value).apply()

    var isAdvancedMode: Boolean
        get() = prefs.getBoolean("isAdvancedMode", true)
        set(value) = prefs.edit().putBoolean("isAdvancedMode", value).apply()

    var filterMode: String
        get() = prefs.getString("filterMode", "ALL") ?: "ALL"
        set(value) = prefs.edit().putString("filterMode", value).apply()

    var filterWhatsAppShorts: Boolean
        get() = prefs.getBoolean("filterWhatsAppShorts", true)
        set(value) = prefs.edit().putBoolean("filterWhatsAppShorts", value).apply()

    var sensitivity: Float
        get() = prefs.getFloat("sensitivity", 0.9f)
        set(value) = prefs.edit().putFloat("sensitivity", value).apply()

    var reactivity: Float
        get() = prefs.getFloat("reactivity", 0.8f)
        set(value) = prefs.edit().putFloat("reactivity", value).apply()

    var bassMultiplier: Float
        get() = prefs.getFloat("bassMultiplier", 1.0f)
        set(value) = prefs.edit().putFloat("bassMultiplier", value).apply()

    var midMultiplier: Float
        get() = prefs.getFloat("midMultiplier", 1.0f)
        set(value) = prefs.edit().putFloat("midMultiplier", value).apply()

    var trebleMultiplier: Float
        get() = prefs.getFloat("trebleMultiplier", 1.0f)
        set(value) = prefs.edit().putFloat("trebleMultiplier", value).apply()

    var shuffleModeEnabled: Boolean
        get() = prefs.getBoolean("shuffleModeEnabled", false)
        set(value) = prefs.edit().putBoolean("shuffleModeEnabled", value).apply()

    var repeatMode: Int
        get() = prefs.getInt("repeatMode", androidx.media3.common.Player.REPEAT_MODE_OFF)
        set(value) = prefs.edit().putInt("repeatMode", value).apply()

    var eqEnabled: Boolean
        get() = prefs.getBoolean("eqEnabled", false)
        set(value) = prefs.edit().putBoolean("eqEnabled", value).apply()

    var eqPreset: Short
        get() = prefs.getInt("eqPreset", -1).toShort()
        set(value) = prefs.edit().putInt("eqPreset", value.toInt()).apply()

    var eqCustomBands: String
        get() = prefs.getString("eqCustomBands", "") ?: ""
        set(value) = prefs.edit().putString("eqCustomBands", value).apply()

    var eqAutoMode: Boolean
        get() = prefs.getBoolean("eqAutoMode", false)
        set(value) = prefs.edit().putBoolean("eqAutoMode", value).apply()

    var lastPlayedTrackPath: String?
        get() = prefs.getString("lastPlayedTrackPath", null)
        set(value) = prefs.edit().putString("lastPlayedTrackPath", value).apply()

    var hasSeenTutorial: Boolean
        get() = prefs.getBoolean("hasSeenTutorial", false)
        set(value) = prefs.edit().putBoolean("hasSeenTutorial", value).apply()

    var hasSeenBottomBarHint: Boolean
        get() = prefs.getBoolean("hasSeenBottomBarHint", false)
        set(value) = prefs.edit().putBoolean("hasSeenBottomBarHint", value).apply()

    var hasSeenPlayerHints: Boolean
        get() = prefs.getBoolean("hasSeenPlayerHints", false)
        set(value) = prefs.edit().putBoolean("hasSeenPlayerHints", value).apply()

    var hasUsedMiniplayerGesture: Boolean
        get() = prefs.getBoolean("hasUsedMiniplayerGesture", false)
        set(value) = prefs.edit().putBoolean("hasUsedMiniplayerGesture", value).apply()

    var hasUsedCoverGesture: Boolean
        get() = prefs.getBoolean("hasUsedCoverGesture", false)
        set(value) = prefs.edit().putBoolean("hasUsedCoverGesture", value).apply()

    var hasUsedPlaylistGesture: Boolean
        get() = prefs.getBoolean("hasUsedPlaylistGesture", false)
        set(value) = prefs.edit().putBoolean("hasUsedPlaylistGesture", value).apply()

    var librarySortOrder: String
        get() = prefs.getString("librarySortOrder", "DIRECTORY") ?: "DIRECTORY"
        set(value) = prefs.edit().putString("librarySortOrder", value).apply()

    var libraryScrollIndex: Int
        get() = prefs.getInt("libraryScrollIndex", 0)
        set(value) = prefs.edit().putInt("libraryScrollIndex", value).apply()

    var libraryScrollOffset: Int
        get() = prefs.getInt("libraryScrollOffset", 0)
        set(value) = prefs.edit().putInt("libraryScrollOffset", value).apply()

    private val _backgroundStyleFlow = MutableStateFlow(prefs.getInt("backgroundStyle", 0))
    val backgroundStyleFlow: StateFlow<Int> = _backgroundStyleFlow

    var backgroundStyle: Int
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
    val thumbnailShapeFlow: StateFlow<Int> = _thumbnailShapeFlow

    var thumbnailShape: Int
        get() = prefs.getInt("thumbnailShape", 0)
        set(value) {
            prefs.edit().putInt("thumbnailShape", value).apply()
            _thumbnailShapeFlow.value = value
        }
    private val _toastFlow = MutableSharedFlow<String>(extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val toastFlow: SharedFlow<String> = _toastFlow

    fun showToast(message: String) {
        _toastFlow.tryEmit(message)
    }
}
