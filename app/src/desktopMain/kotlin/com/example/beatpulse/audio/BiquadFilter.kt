package com.example.beatpulse.audio

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.pow

/**
 * A standard digital Biquad Filter implementation for audio DSP.
 * Used for Peaking EQ (Equalizer band).
 */
class BiquadFilter(private val sampleRate: Float) {
    private var a0 = 1.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0

    // History buffers
    private var z1 = 0.0
    private var z2 = 0.0

    /**
     * Configure a Peaking EQ filter.
     * @param frequency Center frequency in Hz (e.g. 60, 230, 910)
     * @param q Quality factor (typically 1.0 to 1.5 for octave bands)
     * @param dbGain Gain in dB (-15.0 to +15.0)
     */
    fun configurePeakingEQ(frequency: Float, q: Float, dbGain: Float) {
        val a = 10.0.pow(dbGain / 40.0)
        val w0 = 2.0 * PI * frequency / sampleRate
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2.0 * q)

        b0 = 1.0 + alpha * a
        b1 = -2.0 * cosW0
        b2 = 1.0 - alpha * a
        a0 = 1.0 + alpha / a
        a1 = -2.0 * cosW0
        a2 = 1.0 - alpha / a

        // Normalize coefficients
        b0 /= a0
        b1 /= a0
        b2 /= a0
        a1 /= a0
        a2 /= a0
    }

    /**
     * Process a single audio sample (float between -1.0 and 1.0).
     */
    fun process(sample: Float): Float {
        val input = sample.toDouble()
        val output = input * b0 + z1
        
        // Update history
        z1 = input * b1 + z2 - a1 * output
        z2 = input * b2 - a2 * output
        
        return output.toFloat()
    }
}
