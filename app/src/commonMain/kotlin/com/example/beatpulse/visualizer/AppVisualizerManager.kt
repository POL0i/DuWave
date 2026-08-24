package com.example.beatpulse.visualizer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AppVisualizerManager {
    val bassMultiplier: MutableStateFlow<Float>
    val midMultiplier: MutableStateFlow<Float>
    val trebleMultiplier: MutableStateFlow<Float>
    val visualizerArchetype: MutableStateFlow<Int>
    val fftMode: MutableStateFlow<String>
    val isAdvancedMode: MutableStateFlow<Boolean>
    val filterMode: MutableStateFlow<FilterMode>
    val sensitivity: MutableStateFlow<Float>
    val reactivity: MutableStateFlow<Float>
    val combinedAmplitudes: StateFlow<FloatArray>
    val bassAmplitudes: StateFlow<FloatArray>
    val midAmplitudes: StateFlow<FloatArray>
    val highAmplitudes: StateFlow<FloatArray>

    fun startMicMode(context: Any)
    fun stopMicMode()
    var isEnabled: Boolean
    fun start(sessionId: Int)
    fun stop()
}
