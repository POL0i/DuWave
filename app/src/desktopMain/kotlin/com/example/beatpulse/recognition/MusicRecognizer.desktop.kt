package com.example.beatpulse.recognition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import java.io.BufferedReader
import java.io.InputStreamReader

actual class MusicRecognizer {
    private val shazamClient = ShazamClient()

    actual suspend fun checkAvailability(): Boolean {
        // Assume true since we use python shazamio as fallback
        return true
    }

    actual suspend fun recognizeMusic(onProgress: (progress: Float, amplitude: Float) -> Unit): Result<RecognizedTrack> = withContext(Dispatchers.IO) {
        val sampleRate = 16000f
        val format = AudioFormat(sampleRate, 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        
        if (!AudioSystem.isLineSupported(info)) {
            return@withContext Result.failure(Exception("Micrófono no soportado o formato de audio no compatible."))
        }

        var line: TargetDataLine? = null
        try {
            line = AudioSystem.getLine(info) as TargetDataLine
            line.open(format)
            line.start()

            val recordSeconds = 12
            val targetBytes = (sampleRate * recordSeconds * 2).toInt() // 16-bit = 2 bytes per sample
            val audioBytes = ByteArray(targetBytes)

            val bufferSize = line.bufferSize
            val chunkBytes = ByteArray(bufferSize)

            var bytesRead = 0
            
            while (isActive && bytesRead < targetBytes) {
                val numBytesRead = line.read(chunkBytes, 0, chunkBytes.size)
                if (numBytesRead > 0) {
                    val toCopy = minOf(numBytesRead, targetBytes - bytesRead)
                    System.arraycopy(chunkBytes, 0, audioBytes, bytesRead, toCopy)
                    
                    var sum = 0.0
                    for (i in 0 until toCopy step 2) {
                        val low = chunkBytes[i].toInt() and 0xFF
                        val high = chunkBytes[i + 1].toInt() shl 8
                        val sample = (high or low).toShort()
                        val sampleDouble = sample.toDouble()
                        sum += sampleDouble * sampleDouble
                    }
                    bytesRead += toCopy
                    
                    val rms = kotlin.math.sqrt(sum / (toCopy / 2)).toFloat()
                    val progress = bytesRead.toFloat() / targetBytes.toFloat()
                    
                    withContext(Dispatchers.Main) {
                        onProgress(progress, rms)
                    }
                } else {
                    delay(10)
                }
            }
            
            line.stop()
            line.close()

            if (bytesRead < targetBytes) {
                return@withContext Result.failure(Exception("Grabación interrumpida"))
            }

            // Save to temporary WAV file
            val tempWav = File.createTempFile("shazam_capture", ".wav")
            val bais = ByteArrayInputStream(audioBytes)
            val ais = AudioInputStream(bais, format, audioBytes.size / 2L)
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tempWav)

            // Execute Python script
            val process = ProcessBuilder(
                "/home/denis/DuWave/.venv/bin/python",
                "/home/denis/DuWave/shazam_desktop.py",
                tempWav.absolutePath
            ).start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            tempWav.delete()

            try {
                // We use org.json.JSONObject (Desktop has sqlite-jdbc which might not have it, let's just parse manually or use GSON)
                // Actually Gson is available because ShazamClient uses it.
                val com_google_gson_Gson = com.google.gson.Gson()
                val jsonMap = com_google_gson_Gson.fromJson(output, Map::class.java)
                
                if (jsonMap.containsKey("error")) {
                    val error = jsonMap["error"].toString()
                    if (error == "No match") {
                        return@withContext Result.failure(Exception("No match"))
                    } else {
                        return@withContext Result.failure(Exception(error))
                    }
                } else if (jsonMap.containsKey("title") && jsonMap.containsKey("artist")) {
                    return@withContext Result.success(RecognizedTrack(jsonMap["title"].toString(), jsonMap["artist"].toString()))
                } else {
                    return@withContext Result.failure(Exception("Respuesta inválida de shazamio"))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Error al leer el script de shazamio: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                line?.stop()
                line?.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
