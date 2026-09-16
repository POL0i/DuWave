package com.example.beatpulse.player

import kotlinx.coroutines.flow.StateFlow
import com.example.beatpulse.ui.components.player.IEqualizerManager

interface AppEqualizerManager : IEqualizerManager {
    override val isEnabled: StateFlow<Boolean>
    val numBands: Int
    override val currentPreset: StateFlow<Short>
    override val isAutoMode: StateFlow<Boolean>
    override val bands: StateFlow<List<Short>>
    override val bandLevels: StateFlow<Map<Short, Short>>
    override val presets: StateFlow<List<Pair<Short, String>>>
    override val minLevel: StateFlow<Short>
    override val maxLevel: StateFlow<Short>

    override fun getCenterFreq(band: Short): Int
    override fun setBandLevel(band: Short, level: Short)
    override fun setAutoMode(enabled: Boolean)
    override fun setEnabled(enabled: Boolean)
    override fun setPreset(preset: Short)
}
