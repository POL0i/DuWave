package com.example.beatpulse.visualizer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import com.example.beatpulse.ui.components.player.IAudioVisualizerManager

interface AppVisualizerManager : IAudioVisualizerManager {
    override val bassMultiplier: MutableStateFlow<Float>
    override val midMultiplier: MutableStateFlow<Float>
    override val trebleMultiplier: MutableStateFlow<Float>
    override val visualizerArchetype: MutableStateFlow<Int>
    override val fftMode: MutableStateFlow<String>
    override val isAdvancedMode: MutableStateFlow<Boolean>
    override val filterMode: MutableStateFlow<Any>
    override val sensitivity: MutableStateFlow<Float>
    override val reactivity: MutableStateFlow<Float>
    override val combinedAmplitudes: StateFlow<FloatArray>
    override val bassAmplitudes: StateFlow<FloatArray>
    override val midAmplitudes: StateFlow<FloatArray>
    override val highAmplitudes: StateFlow<FloatArray>

    fun startMicMode(context: Any)
    fun stopMicMode()
    var isEnabled: Boolean
    fun start(sessionId: Int)
    fun stop()
    
    // Desktop specific implementation
    fun processAudioBytes(pcmBuffer: ByteArray) {}
}
