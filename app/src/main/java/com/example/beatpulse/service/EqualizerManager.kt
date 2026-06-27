package com.example.beatpulse.service

import android.media.audiofx.Equalizer
import com.example.beatpulse.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow

class EqualizerManager(private val prefs: PreferencesManager) {

    private var equalizer: Equalizer? = null
    private val lock = Any()
    val isEnabled = MutableStateFlow(prefs.eqEnabled)
    val currentPreset = MutableStateFlow(prefs.eqPreset)
    val isAutoMode = MutableStateFlow(prefs.eqAutoMode)
    
    val bands = MutableStateFlow<List<Short>>(emptyList())
    val bandLevels = MutableStateFlow<Map<Short, Short>>(emptyMap())
    val presets = MutableStateFlow<List<Pair<Short, String>>>(emptyList())
    val minLevel = MutableStateFlow<Short>(0)
    val maxLevel = MutableStateFlow<Short>(0)

    fun initialize(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == -1) return
        try {
            synchronized(lock) {
                equalizer?.release()
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = isEnabled.value
                }
            
            equalizer?.let { eq ->
                minLevel.value = eq.bandLevelRange[0]
                maxLevel.value = eq.bandLevelRange[1]
                
                val numberOfBands = eq.numberOfBands
                val newBands = (0 until numberOfBands).map { it.toShort() }
                bands.value = newBands
                
                val initialLevels = mutableMapOf<Short, Short>()
                for (band in newBands) {
                    initialLevels[band] = eq.getBandLevel(band)
                }
                bandLevels.value = initialLevels
                
                val numberOfPresets = eq.numberOfPresets
                val newPresets = (0 until numberOfPresets).map {
                    it.toShort() to eq.getPresetName(it.toShort())
                }
                presets.value = newPresets
            }
            synchronized(lock) {
                applySavedSettings()
            }
            } // Close synchronized(lock)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applySavedSettings() {
        val eq = equalizer ?: return
        if (isAutoMode.value) {
            applyAutoEq()
        } else if (currentPreset.value >= 0 && currentPreset.value < eq.numberOfPresets) {
            eq.usePreset(currentPreset.value)
        } else {
            val savedBands = prefs.eqCustomBands.split(",").mapNotNull { it.toShortOrNull() }
            if (savedBands.size == eq.numberOfBands.toInt()) {
                savedBands.forEachIndexed { index, level ->
                    eq.setBandLevel(index.toShort(), level)
                }
            }
        }
        updateBandLevels()
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled.value = enabled
        prefs.eqEnabled = enabled
        synchronized(lock) {
            equalizer?.enabled = enabled
        }
    }

    fun setPreset(preset: Short) {
        isAutoMode.value = false
        prefs.eqAutoMode = false
        currentPreset.value = preset
        prefs.eqPreset = preset
        if (preset >= 0) {
            synchronized(lock) {
                equalizer?.usePreset(preset)
                updateBandLevels()
            }
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        isAutoMode.value = false
        prefs.eqAutoMode = false
        synchronized(lock) {
            equalizer?.setBandLevel(band, level)
            currentPreset.value = -1
            prefs.eqPreset = -1
            updateBandLevels()
            saveCustomBands()
        }
    }

    fun setAutoMode(enabled: Boolean) {
        isAutoMode.value = enabled
        prefs.eqAutoMode = enabled
        if (enabled) {
            currentPreset.value = -1
            prefs.eqPreset = -1
            synchronized(lock) {
                applyAutoEq()
                updateBandLevels()
            }
        }
    }

    /**
     * Auto EQ: applies a gentle "loudness" curve.
     * Boosts low bass and high treble slightly, keeps mids neutral.
     * This makes quiet parts feel fuller and prevents harsh peaks.
     * Curve (for 5 bands): +3dB, +1dB, 0dB, +1dB, +3dB  (scaled to device range)
     */
    private fun applyAutoEq() {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands.toInt()
        val range = maxLevel.value - minLevel.value
        
        // Normalized loudness curve ratios (0.0 = min, 1.0 = max, 0.5 = center/flat)
        val autoCurve = when (numBands) {
            5 -> floatArrayOf(0.60f, 0.53f, 0.50f, 0.53f, 0.60f)
            10 -> floatArrayOf(0.62f, 0.58f, 0.55f, 0.52f, 0.50f, 0.50f, 0.52f, 0.55f, 0.58f, 0.62f)
            else -> FloatArray(numBands) { i ->
                val pos = i.toFloat() / (numBands - 1).coerceAtLeast(1)
                val distFromCenter = kotlin.math.abs(pos - 0.5f) * 2f
                0.50f + distFromCenter * 0.10f
            }
        }
        
        for (i in 0 until numBands) {
            val level = (minLevel.value + autoCurve[i] * range).toInt().toShort()
            eq.setBandLevel(i.toShort(), level)
        }
    }

    private fun updateBandLevels() {
        val eq = equalizer ?: return
        val currentLevels = mutableMapOf<Short, Short>()
        bands.value.forEach { band ->
            currentLevels[band] = eq.getBandLevel(band)
        }
        bandLevels.value = currentLevels
    }

    private fun saveCustomBands() {
        val eq = equalizer ?: return
        val bandsStr = bands.value.joinToString(",") { band ->
            eq.getBandLevel(band).toString()
        }
        prefs.eqCustomBands = bandsStr
    }
    
    fun getCenterFreq(band: Short): Int {
        synchronized(lock) {
            return equalizer?.getCenterFreq(band) ?: 0
        }
    }

    fun release() {
        synchronized(lock) {
            equalizer?.release()
            equalizer = null
        }
    }
}

