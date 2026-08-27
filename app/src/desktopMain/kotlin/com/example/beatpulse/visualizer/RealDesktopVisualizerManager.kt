package com.example.beatpulse.visualizer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.*

class RealDesktopVisualizerManager : AppVisualizerManager {
    override val bassAmplitudes = MutableStateFlow(FloatArray(0))
    override val midAmplitudes = MutableStateFlow(FloatArray(0))
    override val highAmplitudes = MutableStateFlow(FloatArray(0))
    override val combinedAmplitudes = MutableStateFlow(FloatArray(0))
    override val isAdvancedMode = MutableStateFlow(false)
    override val filterMode = MutableStateFlow<Any>(FilterMode.ALL)
    override val sensitivity = MutableStateFlow(1.0f)
    override val reactivity = MutableStateFlow(1.0f)
    override val bassMultiplier = MutableStateFlow(1.0f)
    override val midMultiplier = MutableStateFlow(1.0f)
    override val trebleMultiplier = MutableStateFlow(1.0f)
    override val visualizerArchetype = MutableStateFlow(0)
    override val fftMode = MutableStateFlow("AVERAGE")
    
    override var isEnabled: Boolean = false

    // FFT Size (must be power of 2)
    private val fftSize = 1024
    private val sampleBuffer = FloatArray(fftSize)
    private var sampleIndex = 0

    override fun startMicMode(context: Any) {}
    override fun stopMicMode() {}
    override fun start(sessionId: Int) { isEnabled = true }
    override fun stop() {
        isEnabled = false
        val empty = FloatArray(0)
        combinedAmplitudes.value = empty
        bassAmplitudes.value = empty
        midAmplitudes.value = empty
        highAmplitudes.value = empty
    }

    override fun processAudioBytes(pcmBuffer: ByteArray) {
        if (!isEnabled) return

        // 1. Convert 16-bit PCM bytes to normalized floats (-1.0 to 1.0)
        for (i in 0 until pcmBuffer.size step 2) {
            if (i + 1 >= pcmBuffer.size) break
            // Little-endian
            val byte1 = pcmBuffer[i].toInt() and 0xFF
            val byte2 = pcmBuffer[i + 1].toInt()
            val sample = (byte1 or (byte2 shl 8)).toShort()
            val floatSample = sample / 32768.0f
            
            sampleBuffer[sampleIndex] = floatSample
            sampleIndex++
            
            if (sampleIndex >= fftSize) {
                // Buffer full, compute FFT
                computeAndEmitFFT()
                sampleIndex = 0 // Or overlap buffer for smoother visuals
            }
        }
    }

    private fun computeAndEmitFFT() {
        val real = sampleBuffer.clone()
        val imag = FloatArray(fftSize) { 0f }
        
        // Apply Hanning Window to reduce spectral leakage
        for (i in 0 until fftSize) {
            real[i] = real[i] * (0.5f * (1f - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
        }
        
        fft(real, imag)
        
        // Calculate magnitudes (only first half of frequencies is useful - Nyquist)
        val halfSize = fftSize / 2
        val rawMagnitudes = FloatArray(halfSize)
        for (i in 0 until halfSize) {
            rawMagnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }

        // Downsample 512 bins to 180 bars exactly like Android to prevent UI clumping
        val numBars = 180
        val downsampled = FloatArray(numBars)
        val minBin = 1.0
        val maxBin = (halfSize * 0.75).coerceAtMost((halfSize - 1).toDouble())

        for (i in 0 until numBars) {
            val ratioStart = i.toDouble() / numBars
            val ratioEnd = (i + 1).toDouble() / numBars
            // Use logarithmic mapping to assign frequencies correctly across the spectrum
            val startBin = (minBin * Math.pow(maxBin / minBin, ratioStart)).toInt().coerceIn(1, halfSize - 1)
            val endBin = (minBin * Math.pow(maxBin / minBin, ratioEnd)).toInt().coerceIn(1, halfSize - 1).let {
                if (it > startBin) it else startBin + 1
            }

            var sum = 0f
            var count = 0
            for (j in startBin until endBin) {
                if (j < halfSize) {
                    sum += rawMagnitudes[j]
                    count++
                }
            }
            
            val binVal = if (count > 0) sum / count else 0f
            
            // High frequencies naturally have less energy, aggressively boost them based on their bin
            val boostLUT = 1.0f + startBin.toDouble().pow(0.65).toFloat() * 1.5f
            val boostedValue = binVal * boostLUT
            
            // Base calculation for amplitude matching Android
            val dB = 10 * log10((boostedValue * 100f + 1).toDouble()).toFloat()
            val normalized = ((dB - 10f) / 45f) * sensitivity.value
            val rawAmplitude = normalized.coerceIn(0f, 1.2f).toDouble().pow(1.5).toFloat()
            
            // Apply reactivity smoothing against previous frame
            val currentCombined = combinedAmplitudes.value
            val prevVal = if (currentCombined.size == numBars) currentCombined[i] else 0f
            val react = (reactivity.value * 0.25f).coerceIn(0.01f, 1.0f) // Matches EQUILIBRADO physics mode
            downsampled[i] = (prevVal + (rawAmplitude - prevVal) * react).coerceIn(0f, 1f)
        }

        // Divide into bands for backwards compatibility with some backgrounds
        // Matches Android BASS_COUNT = 60, MID_COUNT = 60, HIGH_COUNT = 60
        val bassEnd = 60
        val midEnd = 120

        val bass = FloatArray(bassEnd)
        val mid = FloatArray(midEnd - bassEnd)
        val high = FloatArray(numBars - midEnd)
        
        System.arraycopy(downsampled, 0, bass, 0, bassEnd)
        System.arraycopy(downsampled, bassEnd, mid, 0, midEnd - bassEnd)
        System.arraycopy(downsampled, midEnd, high, 0, numBars - midEnd)

        combinedAmplitudes.value = downsampled.clone()
        bassAmplitudes.value = bass
        midAmplitudes.value = mid
        highAmplitudes.value = high
    }

    // Basic Cooley-Tukey Radix-2 FFT (In-place)
    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        var k = n
        var log2n = 0
        while (k > 1) {
            k = k shr 1
            log2n++
        }

        // Bit reversal sorting
        val reverse = IntArray(n)
        for (i in 0 until n) {
            var rev = 0
            var temp = i
            for (j in 0 until log2n) {
                rev = (rev shl 1) or (temp and 1)
                temp = temp shr 1
            }
            reverse[i] = rev
        }

        for (i in 0 until n) {
            val rev = reverse[i]
            if (i < rev) {
                val tReal = real[i]
                val tImag = imag[i]
                real[i] = real[rev]
                imag[i] = imag[rev]
                real[rev] = tReal
                imag[rev] = tImag
            }
        }

        // Cooley-Tukey algorithm
        var step = 2
        while (step <= n) {
            val halfStep = step / 2
            val angle = -2.0 * PI / step
            val wReal = cos(angle).toFloat()
            val wImag = sin(angle).toFloat()

            for (i in 0 until n step step) {
                var currentWReal = 1f
                var currentWImag = 0f

                for (j in 0 until halfStep) {
                    val idx = i + j
                    val idxHalf = idx + halfStep

                    val tReal = currentWReal * real[idxHalf] - currentWImag * imag[idxHalf]
                    val tImag = currentWReal * imag[idxHalf] + currentWImag * real[idxHalf]

                    real[idxHalf] = real[idx] - tReal
                    imag[idxHalf] = imag[idx] - tImag
                    real[idx] += tReal
                    imag[idx] += tImag

                    val nextWReal = currentWReal * wReal - currentWImag * wImag
                    currentWImag = currentWReal * wImag + currentWImag * wReal
                    currentWReal = nextWReal
                }
            }
            step *= 2
        }
    }
}
