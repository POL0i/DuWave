package com.example.beatpulse.recognition

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.mrsep.musicrecognizer.core.recognition.shazam.SongRecSignature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

actual class MusicRecognizer actual constructor() {
    private val shazamClient = ShazamClient()

    actual suspend fun checkAvailability(): Boolean {
        return shazamClient.checkApiStatus()
    }

    @SuppressLint("MissingPermission")
    actual suspend fun recognizeMusic(onProgress: (progress: Float, amplitude: Float) -> Unit): Result<RecognizedTrack> = withContext(Dispatchers.IO) {
        val activity = com.example.beatpulse.MainActivity.instance
        if (activity != null) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(activity, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                withContext(Dispatchers.Main) {
                    androidx.core.app.ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.RECORD_AUDIO), 100)
                }
                return@withContext Result.failure(Exception("Permiso de micrófono solicitado. Intenta de nuevo."))
            }
        }

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        
        val recordSeconds = 12
        val targetSamples = sampleRate * recordSeconds
        val audioData = ShortArray(targetSamples)
        
        var audioRecord: AudioRecord? = null

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize.coerceAtLeast(sampleRate * 2) // Ensure buffer is large enough
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext Result.failure(Exception("No se pudo inicializar el micrófono"))
            }

            audioRecord.startRecording()

            var samplesRead = 0
            val chunk = ShortArray(bufferSize)

            while (isActive && samplesRead < targetSamples) {
                val readResult = audioRecord.read(chunk, 0, chunk.size)
                if (readResult > 0) {
                    val toCopy = minOf(readResult, targetSamples - samplesRead)
                    System.arraycopy(chunk, 0, audioData, samplesRead, toCopy)
                    samplesRead += toCopy
                    // Calculate RMS for visualization
                    var sum = 0.0
                    for (i in 0 until toCopy) {
                        val sample = chunk[i].toDouble()
                        sum += sample * sample
                    }
                    val rms = kotlin.math.sqrt(sum / toCopy).toFloat()
                    
                    val progress = samplesRead.toFloat() / targetSamples.toFloat()
                    withContext(Dispatchers.Main) {
                        onProgress(progress, rms)
                    }
                } else {
                    delay(10)
                }
            }

            audioRecord.stop()
            
            if (samplesRead < targetSamples) {
                return@withContext Result.failure(Exception("Grabación interrumpida"))
            }

            // 1. Generate Signature
            val signature = try {
                SongRecSignature.fromPcm16Mono16kHz(audioData)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Error generando huella acústica: ${e.message}"))
            }

            // 2. Query API
            val apiResult = shazamClient.recognize(signature, (recordSeconds * 1000).toLong())
            apiResult.map { RecognizedTrack(it.first, it.second) }
            
        } catch (e: SecurityException) {
            Result.failure(Exception("Permiso de micrófono denegado"))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            audioRecord?.release()
        }
    }
}
