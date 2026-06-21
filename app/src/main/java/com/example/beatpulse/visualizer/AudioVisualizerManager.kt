package com.example.beatpulse.visualizer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.hypot
import com.example.beatpulse.data.PreferencesManager

enum class FilterMode {
    ALL, BASS, MIDS, TREBLE
}

class AudioVisualizerManager(private val prefs: PreferencesManager) {

    private var visualizer: Visualizer? = null
    
    private val _amplitudes = MutableStateFlow<FloatArray>(FloatArray(0))
    val amplitudes: StateFlow<FloatArray> = _amplitudes.asStateFlow()

    private val _ghosts = MutableStateFlow<FloatArray>(FloatArray(0))
    val ghostsFlow: StateFlow<FloatArray> = _ghosts.asStateFlow()
    
    // We will divide the spectrum into a fixed number of bars for half of the circle
    private val BARS_COUNT = 48

    private var smoothedAmplitudes = FloatArray(BARS_COUNT)
    private var velocities = FloatArray(BARS_COUNT)
    private var ghosts = FloatArray(BARS_COUNT)
    
    // Arrays reusables para evitar alojamientos de memoria a 60 FPS
    private var magnitudes = FloatArray(0)
    private var bins = FloatArray(BARS_COUNT)
    private var rawBins = FloatArray(BARS_COUNT)
    private var smoothedSpatialBins = FloatArray(BARS_COUNT)

    private var decayJob: kotlinx.coroutines.Job? = null

    // Configurable Properties
    var isAdvancedMode = MutableStateFlow(prefs.isAdvancedMode)
    var filterMode = MutableStateFlow(runCatching { FilterMode.valueOf(prefs.filterMode) }.getOrDefault(FilterMode.ALL))
    var sensitivity = MutableStateFlow(prefs.sensitivity) // Default is now 0.9 in prefs
    var reactivity = MutableStateFlow(prefs.reactivity)

    // Multiplicadores por frecuencia (0.0 a 3.0)
    var bassMultiplier = MutableStateFlow(prefs.bassMultiplier)
    var midMultiplier = MutableStateFlow(prefs.midMultiplier)
    var trebleMultiplier = MutableStateFlow(prefs.trebleMultiplier)
    private var currentSessionId = 0

    fun start(audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (visualizer?.enabled == true && currentSessionId == audioSessionId) {
            decayJob?.cancel()
            return
        }
        try {
            stop(decay = false)
            currentSessionId = audioSessionId
            visualizer = Visualizer(audioSessionId).apply {
                val sizeRange = Visualizer.getCaptureSizeRange()
                captureSize = if (sizeRange.size >= 2) {
                    sizeRange[1].coerceAtMost(512) // Limit to 512 for better compatibility
                } else 512
                
                // Max capture rate is often too high on some OEM devices (e.g., Motorola) causing silent failures
                val safeRate = (Visualizer.getMaxCaptureRate() / 2).coerceAtMost(20000)

                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, wave: ByteArray?, rate: Int) {}

                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                        fft?.let { processFft(it) }
                    }
                }, safeRate, false, true)
                
                enabled = true
            }
        } catch (e: Exception) {
            Log.e("AudioVisualizer", "Error initializing visualizer", e)
        }
    }

    private fun processFft(fft: ByteArray) {
        val n = fft.size
        val numMagnitudes = n / 2
        if (magnitudes.size != numMagnitudes) {
            magnitudes = FloatArray(numMagnitudes)
        }
        magnitudes[0] = abs(fft[0].toFloat())
        
        for (k in 1 until numMagnitudes) {
            val i = k * 2
            if (i + 1 >= n) break
            val real = fft[i].toFloat()
            val imaginary = fft[i + 1].toFloat()
            magnitudes[k] = hypot(real, imaginary)
        }

        // Reuse bins arrays instead of allocating
        for(i in bins.indices) bins[i] = 0f
        for(i in rawBins.indices) rawBins[i] = 0f
        
        // Logarithmic Binning
        val minBin = 1.0
        val maxBin = (numMagnitudes / 2.5).coerceAtMost(numMagnitudes - 1.0)
        
        val advanced = isAdvancedMode.value
        val sens = sensitivity.value
        val filter = filterMode.value
        val react = reactivity.value

        for (i in 0 until BARS_COUNT) {
            val ratioStart = i.toDouble() / BARS_COUNT
            val ratioEnd = (i + 1).toDouble() / BARS_COUNT
            
            val startBin = (minBin * Math.pow(maxBin / minBin, ratioStart)).toInt().coerceIn(1, numMagnitudes - 1)
            val endBin = (minBin * Math.pow(maxBin / minBin, ratioEnd)).toInt().coerceIn(1, numMagnitudes - 1)
            
            val actualEndBin = if (endBin > startBin) endBin else startBin + 1
            
            var sum = 0f
            var count = 0
            for (j in startBin until actualEndBin) {
                if (j < magnitudes.size) {
                    sum += magnitudes[j]
                    count++
                }
            }
            // Add a tiny random noise to prevent exactly flat blocks
            val average = if (count > 0) sum / count else 0f
            
            val boost = 1.0f + (i.toFloat() / BARS_COUNT) * 2.5f
            val boostedAverage = average * boost
            
            val dB = 20 * Math.log10((boostedAverage + 1).toDouble()).toFloat()
            val rawAmplitude = (((dB - 15f) / 45f) * sens).coerceIn(0f, 1f)
            
            val bandMult = when {
                i < BARS_COUNT / 3 -> bassMultiplier.value
                i < (BARS_COUNT * 2) / 3 -> midMultiplier.value
                else -> trebleMultiplier.value
            }
            val amplifiedRaw = (rawAmplitude * bandMult).coerceIn(0f, 1f)
            
            val filteredAmplitude = when (filter) {
                FilterMode.ALL -> amplifiedRaw
                FilterMode.BASS -> if (i < BARS_COUNT / 3) amplifiedRaw else 0f
                FilterMode.MIDS -> if (i in (BARS_COUNT / 3)..(BARS_COUNT * 2 / 3)) amplifiedRaw else 0f
                FilterMode.TREBLE -> if (i > BARS_COUNT * 2 / 3) amplifiedRaw else 0f
            }
            
            // Fade out the highest frequencies to close the circle smoothly at the top
            val fadeOutFactor = if (i > BARS_COUNT * 0.8f) {
                1f - ((i - BARS_COUNT * 0.8f) / (BARS_COUNT * 0.2f))
            } else 1f
            rawBins[i] = filteredAmplitude * fadeOutFactor
        }

        // Spatial Smoothing (Interpolación espacial para eliminar bloques planos)
        for(i in smoothedSpatialBins.indices) smoothedSpatialBins[i] = 0f
        for (i in 0 until BARS_COUNT) {
            var sum = 0f
            var weightSum = 0f
            // Kernel [-2, -1, 0, 1, 2]
            for (j in -2..2) {
                val idx = i + j
                if (idx in 0 until BARS_COUNT) {
                    val weight = when (abs(j)) {
                        0 -> 0.4f
                        1 -> 0.2f
                        2 -> 0.1f
                        else -> 0f
                    }
                    sum += rawBins[idx] * weight
                    weightSum += weight
                }
            }
            smoothedSpatialBins[i] = sum / weightSum
        }

        // Apply physics
        for (i in 0 until BARS_COUNT) {
            val amplitude = smoothedSpatialBins[i]
            
            if (advanced) {
                val gravity = 0.005f + (react * 0.03f) 
                
                if (amplitude > smoothedAmplitudes[i]) {
                    smoothedAmplitudes[i] = amplitude
                    velocities[i] = 0f 
                } else {
                    velocities[i] += gravity
                    smoothedAmplitudes[i] = (smoothedAmplitudes[i] - velocities[i]).coerceAtLeast(0f)
                }
                
                // Fantasmas caen más lento
                if (amplitude > ghosts[i]) {
                    ghosts[i] = amplitude
                } else {
                    ghosts[i] = (ghosts[i] - (gravity * 0.5f)).coerceAtLeast(0f)
                }
            } else {
                smoothedAmplitudes[i] = smoothedAmplitudes[i] + react * (amplitude - smoothedAmplitudes[i])
                ghosts[i] = smoothedAmplitudes[i] // Not used much in classic mode
            }
            
            bins[i] = smoothedAmplitudes[i]
        }

        _amplitudes.value = bins.clone()
        _ghosts.value = ghosts.clone()
    }

    fun stop(decay: Boolean = true) {
        try {
            decayJob?.cancel()
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
            
            if (decay) {
                decayJob = CoroutineScope(Dispatchers.Default).launch {
                    var maxAmp = smoothedAmplitudes.maxOrNull() ?: 0f
                    while (maxAmp > 0.01f) {
                        for (i in 0 until BARS_COUNT) {
                            smoothedAmplitudes[i] *= 0.8f
                            ghosts[i] *= 0.8f
                        }
                        _amplitudes.value = smoothedAmplitudes.clone()
                        _ghosts.value = ghosts.clone()
                        kotlinx.coroutines.delay(16L)
                        maxAmp = smoothedAmplitudes.maxOrNull() ?: 0f
                    }
                    smoothedAmplitudes = FloatArray(BARS_COUNT)
                    velocities = FloatArray(BARS_COUNT)
                    ghosts = FloatArray(BARS_COUNT)
                    _amplitudes.value = FloatArray(BARS_COUNT)
                }
            } else {
                smoothedAmplitudes = FloatArray(BARS_COUNT)
                velocities = FloatArray(BARS_COUNT)
                ghosts = FloatArray(BARS_COUNT)
                _amplitudes.value = FloatArray(BARS_COUNT)
            }
        } catch (e: Exception) {
            Log.e("AudioVisualizer", "Error stopping visualizer", e)
        }
    }
}
