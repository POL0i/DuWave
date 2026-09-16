package com.example.beatpulse.audio

import com.example.beatpulse.player.AppEqualizerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RealDesktopEqualizerManager : AppEqualizerManager {
    override val isEnabled = MutableStateFlow(true)
    override val numBands = 5
    override val currentPreset = MutableStateFlow<Short>(0)
    override val isAutoMode = MutableStateFlow(false)
    
    // Default frequencies: 60Hz, 230Hz, 910Hz, 3600Hz, 14000Hz
    private val centerFreqs = listOf(60000, 230000, 910000, 3600000, 14000000) // in millihertz like Android
    
    private val _bands = MutableStateFlow(listOf<Short>(0, 1, 2, 3, 4))
    override val bands: StateFlow<List<Short>> = _bands
    
    private val _bandLevels = MutableStateFlow(mapOf<Short, Short>(0.toShort() to 0.toShort(), 1.toShort() to 0.toShort(), 2.toShort() to 0.toShort(), 3.toShort() to 0.toShort(), 4.toShort() to 0.toShort()))
    override val bandLevels: StateFlow<Map<Short, Short>> = _bandLevels
    
    private val _presets = MutableStateFlow(listOf<Pair<Short, String>>(
        0.toShort() to "Normal",
        1.toShort() to "Classical",
        2.toShort() to "Dance",
        3.toShort() to "Flat",
        4.toShort() to "Folk",
        5.toShort() to "Heavy Metal",
        6.toShort() to "Hip Hop",
        7.toShort() to "Jazz",
        8.toShort() to "Pop",
        9.toShort() to "Rock"
    ))
    override val presets: StateFlow<List<Pair<Short, String>>> = _presets
    
    override val minLevel = MutableStateFlow<Short>(-1500) // -15.0 dB in millibels
    override val maxLevel = MutableStateFlow<Short>(1500)  // +15.0 dB in millibels

    private val filters = Array(5) { BiquadFilter(44100f) }

    init {
        updateFilters()
    }

    override fun getCenterFreq(band: Short): Int {
        return if (band in 0 until 5) centerFreqs[band.toInt()] else 0
    }

    override fun setBandLevel(band: Short, level: Short) {
        val newLevels = _bandLevels.value.toMutableMap()
        newLevels[band] = level
        _bandLevels.value = newLevels
        updateFilters()
    }

    override fun setAutoMode(enabled: Boolean) {
        isAutoMode.value = enabled
    }

    override fun setEnabled(enabled: Boolean) {
        isEnabled.value = enabled
    }

    override fun setPreset(preset: Short) {
        currentPreset.value = preset
        // For a full implementation, this should load predefined levels per preset.
        // For simplicity, we just flat them out if Normal.
        val levels = when (preset.toInt()) {
            0 -> mapOf<Short, Short>(0.toShort() to 0.toShort(), 1.toShort() to 0.toShort(), 2.toShort() to 0.toShort(), 3.toShort() to 0.toShort(), 4.toShort() to 0.toShort()) // Normal
            2 -> mapOf<Short, Short>(0.toShort() to 600.toShort(), 1.toShort() to 0.toShort(), 2.toShort() to 200.toShort(), 3.toShort() to 400.toShort(), 4.toShort() to 100.toShort()) // Dance
            8 -> mapOf<Short, Short>(0.toShort() to (-200).toShort(), 1.toShort() to 200.toShort(), 2.toShort() to 500.toShort(), 3.toShort() to 100.toShort(), 4.toShort() to (-200).toShort()) // Pop
            9 -> mapOf<Short, Short>(0.toShort() to 500.toShort(), 1.toShort() to 300.toShort(), 2.toShort() to (-100).toShort(), 3.toShort() to 300.toShort(), 4.toShort() to 500.toShort()) // Rock
            else -> mapOf<Short, Short>(0.toShort() to 0.toShort(), 1.toShort() to 0.toShort(), 2.toShort() to 0.toShort(), 3.toShort() to 0.toShort(), 4.toShort() to 0.toShort())
        }
        _bandLevels.value = levels
        updateFilters()
    }

    private fun updateFilters() {
        // q = 1.41 (butterworth roughly)
        for (i in 0 until 5) {
            val freqHz = centerFreqs[i] / 1000f
            val gainDb = (_bandLevels.value[i.toShort()] ?: 0) / 100f
            filters[i].configurePeakingEQ(freqHz, 1.41f, gainDb)
        }
    }

    fun processAudioBytes(pcmBuffer: ByteArray, sampleRate: Float) {
        if (!isEnabled.value) return
        
        // Ensure filters are using current sample rate
        // Assuming 44100 for standard mp3s, ideally we should update if changes

        // Process 16-bit PCM
        for (i in 0 until pcmBuffer.size step 2) {
            if (i + 1 >= pcmBuffer.size) break
            
            val byte1 = pcmBuffer[i].toInt() and 0xFF
            val byte2 = pcmBuffer[i + 1].toInt()
            val sample = (byte1 or (byte2 shl 8)).toShort()
            
            var floatSample = sample / 32768.0f
            
            // Cascade through 5 filters
            floatSample = filters[0].process(floatSample)
            floatSample = filters[1].process(floatSample)
            floatSample = filters[2].process(floatSample)
            floatSample = filters[3].process(floatSample)
            floatSample = filters[4].process(floatSample)
            
            // Clip to avoid distortion
            floatSample = floatSample.coerceIn(-1.0f, 1.0f)
            
            // Convert back to 16-bit PCM
            val outSample = (floatSample * 32767).toInt().toShort()
            pcmBuffer[i] = (outSample.toInt() and 0xFF).toByte()
            pcmBuffer[i + 1] = ((outSample.toInt() shr 8) and 0xFF).toByte()
        }
    }
}
