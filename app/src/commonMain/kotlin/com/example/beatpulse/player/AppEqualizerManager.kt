package com.example.beatpulse.player

import kotlinx.coroutines.flow.StateFlow

interface AppEqualizerManager {
    val isEnabled: StateFlow<Boolean>
    val numBands: Int
    val currentPreset: StateFlow<Short>
    val isAutoMode: StateFlow<Boolean>
    val bands: StateFlow<List<Short>>
    val bandLevels: StateFlow<Map<Short, Short>>
    val presets: StateFlow<List<Pair<Short, String>>>
    val minLevel: StateFlow<Short>
    val maxLevel: StateFlow<Short>

    fun getCenterFreq(band: Short): Int
    fun setBandLevel(band: Short, level: Short)
    fun setAutoMode(enabled: Boolean)
    fun setEnabled(enabled: Boolean)
    fun setPreset(preset: Short)
}
