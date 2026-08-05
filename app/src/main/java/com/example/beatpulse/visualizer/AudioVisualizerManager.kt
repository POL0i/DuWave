package com.example.beatpulse.visualizer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

enum class PhysicsMode {
    SUAVE,       // Smooth gliding — very low reactivity, slow ghost decay
    EQUILIBRADO, // Balanced — medium reactivity, moderate ghost decay
    VIOLENTO     // Aggressive — high reactivity, fast ghost decay
}

class AudioVisualizerManager(private val prefs: PreferencesManager) {

    private val BARS_COUNT = 180
    private val BASS_COUNT = BARS_COUNT / 3
    private val MID_COUNT = (BARS_COUNT * 2 / 3) - BASS_COUNT
    private val HIGH_COUNT = BARS_COUNT - (BASS_COUNT + MID_COUNT)

    private val _bassAmplitudes = MutableStateFlow<FloatArray>(FloatArray(0))
    val bassAmplitudes: StateFlow<FloatArray> = _bassAmplitudes.asStateFlow()

    private val _midAmplitudes = MutableStateFlow<FloatArray>(FloatArray(0))
    val midAmplitudes: StateFlow<FloatArray> = _midAmplitudes.asStateFlow()

    private val _highAmplitudes = MutableStateFlow<FloatArray>(FloatArray(0))
    val highAmplitudes: StateFlow<FloatArray> = _highAmplitudes.asStateFlow()

    val amplitudes: StateFlow<FloatArray> = _bassAmplitudes.asStateFlow()

    private val _bassGhosts = MutableStateFlow<FloatArray>(FloatArray(0))
    val bassGhostsFlow: StateFlow<FloatArray> = _bassGhosts.asStateFlow()

    private val _midGhosts = MutableStateFlow<FloatArray>(FloatArray(0))
    val midGhostsFlow: StateFlow<FloatArray> = _midGhosts.asStateFlow()

    private val _highGhosts = MutableStateFlow(FloatArray(0))
    val highGhostsFlow: StateFlow<FloatArray> = _highGhosts.asStateFlow()

    // 1-band combined arrays
    private val smoothCombinedOut = FloatArray(BARS_COUNT)
    private val combinedGhosts = FloatArray(BARS_COUNT)
    private val _combinedAmplitudes = MutableStateFlow(FloatArray(0))
    val combinedAmplitudes: StateFlow<FloatArray> = _combinedAmplitudes.asStateFlow()
    private val _combinedGhosts = MutableStateFlow(FloatArray(0))
    val combinedGhostsState: StateFlow<FloatArray> = _combinedGhosts.asStateFlow()
    
    private var smoothedBass = FloatArray(BASS_COUNT)
    private var smoothedMid = FloatArray(MID_COUNT)
    private var smoothedHigh = FloatArray(HIGH_COUNT)
    private var bassGhosts = FloatArray(BASS_COUNT)
    private var midGhosts = FloatArray(MID_COUNT)
    private var highGhosts = FloatArray(HIGH_COUNT)

    // Pre-allocated reusable buffers — zero GC per frame
    private val tempBass = FloatArray(BASS_COUNT)
    private val tempMid = FloatArray(MID_COUNT)
    private val tempHigh = FloatArray(HIGH_COUNT)
    private val smoothBassOut = FloatArray(BASS_COUNT)
    private val smoothMidOut = FloatArray(MID_COUNT)
    private val smoothHighOut = FloatArray(HIGH_COUNT)
    private val ghostsOut = FloatArray(BARS_COUNT)

    // Pre-computed lookup tables for logarithmic bin mapping
    private var cachedMagnitudeSize = 0
    private var binStartLUT = IntArray(0)
    private var binEndLUT = IntArray(0)
    private var boostLUT = FloatArray(0)

    private var decayJob: kotlinx.coroutines.Job? = null
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dataMutex = Mutex()

    var isAdvancedMode = MutableStateFlow(prefs.isAdvancedMode)
    var visualizerArchetype = MutableStateFlow(prefs.visualizerArchetype)
    var filterMode = MutableStateFlow(runCatching { FilterMode.valueOf(prefs.filterMode) }.getOrDefault(FilterMode.ALL))
    var physicsMode = MutableStateFlow(runCatching { PhysicsMode.valueOf(prefs.physicsMode) }.getOrDefault(PhysicsMode.EQUILIBRADO))
    var sensitivity = MutableStateFlow(prefs.sensitivity)
    var reactivity = MutableStateFlow(prefs.reactivity)

    var fftMode = MutableStateFlow(prefs.visualizerFftMode)

    var bassMultiplier = MutableStateFlow(prefs.bassMultiplier)
    var midMultiplier = MutableStateFlow(prefs.midMultiplier)
    var trebleMultiplier = MutableStateFlow(prefs.trebleMultiplier)
    
    val fftSink = FftAudioSink { magnitudes ->
        processFftMagnitudes(magnitudes)
    }

    fun start(audioSessionId: Int) {
        // FFT is now passive via ExoPlayer's TeeAudioProcessor.
        // We just stop any decay jobs.
        decayJob?.cancel()
    }

    var isEnabled = true

    private fun rebuildLookupTables(numMagnitudes: Int) {
        if (numMagnitudes == cachedMagnitudeSize) return
        cachedMagnitudeSize = numMagnitudes

        val minBin = 1.0
        val maxBin = (numMagnitudes * 0.75).coerceAtMost((numMagnitudes - 1).toDouble())

        binStartLUT = IntArray(BARS_COUNT)
        binEndLUT = IntArray(BARS_COUNT)
        boostLUT = FloatArray(BARS_COUNT)

        for (i in 0 until BARS_COUNT) {
            val ratioStart = i.toDouble() / BARS_COUNT
            val ratioEnd = (i + 1).toDouble() / BARS_COUNT
            
            // Use cubic mapping to assign frequencies correctly across the spectrum
            val startBin = (minBin + (maxBin - minBin) * Math.pow(ratioStart, 3.0)).toInt().coerceIn(1, numMagnitudes - 1)
            val endBin = (minBin + (maxBin - minBin) * Math.pow(ratioEnd, 3.0)).toInt().coerceIn(1, numMagnitudes - 1)
            binStartLUT[i] = startBin
            binEndLUT[i] = if (endBin > startBin) endBin else startBin + 1
            
            // High frequencies naturally have less energy, aggressively boost them based on their bin
            boostLUT[i] = 1.0f + Math.pow(startBin.toDouble(), 0.65).toFloat() * 1.5f
        }
    }

    private fun processFftMagnitudes(magnitudes: FloatArray) {
        if (!isEnabled) return
        if (!dataMutex.tryLock()) return
        try {
            val numMagnitudes = magnitudes.size
            if (numMagnitudes == 0) return

            rebuildLookupTables(numMagnitudes)
            
            val sens = sensitivity.value
            val currentFftMode = fftMode.value
            
            // Physics mode controls reactivity and ghost decay behavior
            val currentPhysics = physicsMode.value
            val react = when (currentPhysics) {
                PhysicsMode.SUAVE -> reactivity.value * 0.12f       // Very smooth gliding
                PhysicsMode.EQUILIBRADO -> reactivity.value * 0.25f // Balanced
                PhysicsMode.VIOLENTO -> reactivity.value * 0.55f    // Snappy, aggressive
            }
            val ghostDecay = when (currentPhysics) {
                PhysicsMode.SUAVE -> 0.008f       // Slow ghost fade
                PhysicsMode.EQUILIBRADO -> 0.02f   // Medium ghost fade
                PhysicsMode.VIOLENTO -> 0.06f      // Fast ghost fade
            }

            val isAdv = isAdvancedMode.value
            val bassMult = if (isAdv) bassMultiplier.value else sensitivity.value
            val midMult = if (isAdv) midMultiplier.value else sensitivity.value
            val trebleMult = if (isAdv) trebleMultiplier.value else sensitivity.value

            for (i in 0 until BARS_COUNT) {
                val startBin = binStartLUT[i]
                val actualEndBin = binEndLUT[i]
                
                var sum = 0f
                var maxVal = 0f
                var count = 0
                for (j in startBin until actualEndBin) {
                    if (j < magnitudes.size) {
                        sum += magnitudes[j]
                        if (magnitudes[j] > maxVal) {
                            maxVal = magnitudes[j]
                        }
                        count++
                    }
                }
                
                val binValue = if (count > 0) {
                    if (currentFftMode == "AVERAGE") sum / count else maxVal
                } else 0f
                
                val boostedValue = binValue * boostLUT[i]
                
                // Base calculation for amplitude
                val dB = 10 * Math.log10((boostedValue * 100f + 1).toDouble()).toFloat()
                // Use a curve so it doesn't hard-clip at 1.0 as easily, preventing the "flat sphere" effect
                val normalized = ((dB - 10f) / 45f) * sens
                val rawAmplitude = Math.pow(normalized.coerceIn(0f, 1.2f).toDouble(), 1.5).toFloat()

                // Smoothing physics per layer — write directly into pre-allocated temp arrays
                if (i < BASS_COUNT) {
                    smoothedBass[i] = smoothedBass[i] + react * (rawAmplitude * bassMult - smoothedBass[i])
                    tempBass[i] = smoothedBass[i].coerceIn(0f, 1f)
                } else if (i < BASS_COUNT + MID_COUNT) {
                    val mI = i - BASS_COUNT
                    smoothedMid[mI] = smoothedMid[mI] + react * (rawAmplitude * midMult - smoothedMid[mI])
                    tempMid[mI] = smoothedMid[mI].coerceIn(0f, 1f)
                } else {
                    val hI = i - (BASS_COUNT + MID_COUNT)
                    smoothedHigh[hI] = smoothedHigh[hI] + react * (rawAmplitude * trebleMult - smoothedHigh[hI])
                    tempHigh[hI] = smoothedHigh[hI].coerceIn(0f, 1f)
                }
            }

            // In-place spatial smoothing into pre-allocated output buffers
            smoothArrayInPlace(tempBass, smoothBassOut)
            smoothArrayInPlace(tempMid, smoothMidOut)
            smoothArrayInPlace(tempHigh, smoothHighOut)
            
            // Populate combined array
            System.arraycopy(tempBass, 0, smoothCombinedOut, 0, BASS_COUNT)
            System.arraycopy(tempMid, 0, smoothCombinedOut, BASS_COUNT, MID_COUNT)
            System.arraycopy(tempHigh, 0, smoothCombinedOut, BASS_COUNT + MID_COUNT, HIGH_COUNT)
            smoothArrayInPlace(smoothCombinedOut, smoothCombinedOut) // Smooth combined boundaries

            // Calculate ghosts per band
            for (i in 0 until BASS_COUNT) {
                if (tempBass[i] > bassGhosts[i]) bassGhosts[i] = tempBass[i] else bassGhosts[i] = (bassGhosts[i] - ghostDecay).coerceAtLeast(0f)
                combinedGhosts[i] = bassGhosts[i]
            }
            for (i in 0 until MID_COUNT) {
                if (tempMid[i] > midGhosts[i]) midGhosts[i] = tempMid[i] else midGhosts[i] = (midGhosts[i] - ghostDecay).coerceAtLeast(0f)
                combinedGhosts[BASS_COUNT + i] = midGhosts[i]
            }
            for (i in 0 until HIGH_COUNT) {
                if (tempHigh[i] > highGhosts[i]) highGhosts[i] = tempHigh[i] else highGhosts[i] = (highGhosts[i] - ghostDecay).coerceAtLeast(0f)
                combinedGhosts[BASS_COUNT + MID_COUNT + i] = highGhosts[i]
            }

            _bassAmplitudes.value = smoothBassOut.copyOf()
            _midAmplitudes.value = smoothMidOut.copyOf()
            _highAmplitudes.value = smoothHighOut.copyOf()
            _combinedAmplitudes.value = smoothCombinedOut.copyOf()
            
            _bassGhosts.value = bassGhosts.copyOf()
            _midGhosts.value = midGhosts.copyOf()
            _highGhosts.value = highGhosts.copyOf()
            _combinedGhosts.value = combinedGhosts.copyOf()

        } finally {
            dataMutex.unlock()
        }
    }

    private fun smoothArrayInPlace(input: FloatArray, output: FloatArray) {
        val size = input.size
        if (size == 0) return
        
        // Clone input if we are modifying in-place so previous elements aren't overwritten
        val src = if (input === output) input.clone() else input
        
        for (i in 0 until size) {
            val prev = if (i > 0) src[i - 1] else src[i]
            val next = if (i < size - 1) src[i + 1] else src[i]
            val center = src[i]
            // Weights: prev=0.15, center=0.7, next=0.15
            val weightSum = 0.7f + 0.15f + 0.15f
            output[i] = (center * 0.7f + prev * 0.15f + next * 0.15f) / weightSum
        }
    }

    fun stop(decay: Boolean = true) {
        try {
            decayJob?.cancel()
            if (decay) {
                decayJob = managerScope.launch {
                    var maxAmp = 1f
                    while (maxAmp > 0.01f) {
                        dataMutex.withLock {
                            for (i in 0 until BASS_COUNT) { smoothedBass[i] *= 0.8f; bassGhosts[i] *= 0.8f }
                            for (i in 0 until MID_COUNT) { smoothedMid[i] *= 0.8f; midGhosts[i] *= 0.8f }
                            for (i in 0 until HIGH_COUNT) { smoothedHigh[i] *= 0.8f; highGhosts[i] *= 0.8f }
                        }
                        _bassAmplitudes.value = smoothedBass.copyOf()
                        _midAmplitudes.value = smoothedMid.copyOf()
                        _highAmplitudes.value = smoothedHigh.copyOf()
                        _bassGhosts.value = bassGhosts.copyOf()
                        _midGhosts.value = midGhosts.copyOf()
                        _highGhosts.value = highGhosts.copyOf()
                        kotlinx.coroutines.delay(16L)
                        maxAmp = maxOf(
                            smoothedBass.maxOrNull() ?: 0f,
                            smoothedMid.maxOrNull() ?: 0f,
                            smoothedHigh.maxOrNull() ?: 0f
                        )
                    }
                }
            } else {
                smoothedBass = FloatArray(BASS_COUNT)
                smoothedMid = FloatArray(MID_COUNT)
                smoothedHigh = FloatArray(HIGH_COUNT)
                bassGhosts = FloatArray(BASS_COUNT)
                midGhosts = FloatArray(MID_COUNT)
                highGhosts = FloatArray(HIGH_COUNT)
                _bassAmplitudes.value = FloatArray(BASS_COUNT)
                _midAmplitudes.value = FloatArray(MID_COUNT)
                _highAmplitudes.value = FloatArray(HIGH_COUNT)
                _bassGhosts.value = FloatArray(BASS_COUNT)
                _midGhosts.value = FloatArray(MID_COUNT)
                _highGhosts.value = FloatArray(HIGH_COUNT)
            }
        } catch (e: Exception) {
            Log.e("AudioVisualizer", "Error stopping visualizer", e)
        }
    }

    fun cleanup() {
        stop(decay = false)
        managerScope.cancel()
    }
}
