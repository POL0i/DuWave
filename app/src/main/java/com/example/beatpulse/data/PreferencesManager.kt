package com.example.beatpulse.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.channels.BufferOverflow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("beatpulse_prefs", Context.MODE_PRIVATE)

    var visualizerStyle: String
        get() = prefs.getString("visualizerStyle", "BARS") ?: "BARS"
        set(value) = prefs.edit().putString("visualizerStyle", value).apply()

    var isAdvancedMode: Boolean
        get() = prefs.getBoolean("isAdvancedMode", true)
        set(value) = prefs.edit().putBoolean("isAdvancedMode", value).apply()

    var filterMode: String
        get() = prefs.getString("filterMode", "ALL") ?: "ALL"
        set(value) = prefs.edit().putString("filterMode", value).apply()

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
    var lastPlayedTrackPath: String?
        get() = prefs.getString("lastPlayedTrackPath", null)
        set(value) = prefs.edit().putString("lastPlayedTrackPath", value).apply()

    var hasSeenTutorial: Boolean
        get() = prefs.getBoolean("hasSeenTutorial", false)
        set(value) = prefs.edit().putBoolean("hasSeenTutorial", value).apply()

    private val _backgroundStyleFlow = MutableStateFlow(prefs.getInt("backgroundStyle", 0))
    val backgroundStyleFlow: StateFlow<Int> = _backgroundStyleFlow

    var backgroundStyle: Int
        get() = prefs.getInt("backgroundStyle", 0)
        set(value) {
            prefs.edit().putInt("backgroundStyle", value).apply()
            _backgroundStyleFlow.value = value
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
