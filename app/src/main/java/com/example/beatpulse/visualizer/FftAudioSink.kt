package com.example.beatpulse.visualizer

import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

class FftAudioSink(private val listener: (FloatArray) -> Unit) : TeeAudioProcessor.AudioBufferSink {
    
    private val FFT_SIZE = 1024
    private val sampleBuffer = FloatArray(FFT_SIZE)
    private var sampleIndex = 0

    private val cosTable = FloatArray(FFT_SIZE / 2)
    private val sinTable = FloatArray(FFT_SIZE / 2)
    private val window = FloatArray(FFT_SIZE)

    init {
        for (i in 0 until FFT_SIZE / 2) {
            val angle = -2 * PI * i / FFT_SIZE
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
        for (i in 0 until FFT_SIZE) {
            window[i] = (0.5 - 0.5 * cos(2 * PI * i / (FFT_SIZE - 1))).toFloat()
        }
    }

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        sampleIndex = 0
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        val readBuffer = buffer.asReadOnlyBuffer().order(ByteOrder.nativeOrder())
        
        while (readBuffer.remaining() >= 2) { // 16-bit PCM = 2 bytes
            val sample = readBuffer.short.toFloat() / 32768f
            
            // Just take one channel for simplicity if stereo is interleaved, 
            // or we just mix it. For simplicity we just read sequentially.
            // A more robust implementation would check channelCount.
            sampleBuffer[sampleIndex++] = sample

            if (sampleIndex >= FFT_SIZE) {
                processFft()
                sampleIndex = 0
            }
        }
    }

    private fun processFft() {
        val real = FloatArray(FFT_SIZE)
        val imag = FloatArray(FFT_SIZE)

        for (i in 0 until FFT_SIZE) {
            real[i] = sampleBuffer[i] * window[i]
        }

        val shift = 32 - Integer.numberOfTrailingZeros(FFT_SIZE)
        for (i in 0 until FFT_SIZE) {
            val j = Integer.reverse(i shl shift)
            if (j > i) {
                val temp = real[i]
                real[i] = real[j]
                real[j] = temp
            }
        }

        var step = 2
        while (step <= FFT_SIZE) {
            val halfStep = step / 2
            val tableStep = FFT_SIZE / step
            for (i in 0 until FFT_SIZE step step) {
                for (j in 0 until halfStep) {
                    val k = j * tableStep
                    val cosVal = cosTable[k]
                    val sinVal = sinTable[k]
                    val realT = cosVal * real[i + j + halfStep] - sinVal * imag[i + j + halfStep]
                    val imagT = sinVal * real[i + j + halfStep] + cosVal * imag[i + j + halfStep]
                    
                    real[i + j + halfStep] = real[i + j] - realT
                    imag[i + j + halfStep] = imag[i + j] - imagT
                    real[i + j] += realT
                    imag[i + j] += imagT
                }
            }
            step *= 2
        }

        val magnitudes = FloatArray(FFT_SIZE / 2)
        for (i in 0 until FFT_SIZE / 2) {
            magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }

        listener(magnitudes)
    }
}
