package com.example.beatpulse.player

import kotlinx.coroutines.*
import java.io.File
import java.net.URI
import javax.sound.sampled.*
import kotlin.math.max
import kotlin.math.min

class DesktopPlayerAdapter : AppPlayer {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var playbackJob: Job? = null
    
    private val listeners = mutableListOf<AppPlayerListener>()
    
    var equalizerManager: com.example.beatpulse.audio.RealDesktopEqualizerManager? = null
    var audioDataCallback: ((ByteArray) -> Unit)? = null

    @Volatile
    private var _isPlaying = false
    @Volatile
    private var _currentPosition = 0L
    @Volatile
    private var _duration = 0L
    @Volatile
    private var _volume = 1.0f

    private var currentUri: String? = null
    private var sourceDataLine: SourceDataLine? = null
    private var audioInputStream: AudioInputStream? = null
    
    override val isPlaying: Boolean
        get() = _isPlaying

    override val duration: Long
        get() = _duration

    override val currentPosition: Long
        get() = _currentPosition

    override fun seekTo(positionMs: Long) {
        val targetPos = max(0L, min(positionMs, _duration))
        if (_currentPosition == targetPos) return
        
        val wasPlaying = _isPlaying
        stopInternal()
        
        _currentPosition = targetPos
        if (wasPlaying) {
            play()
        }
    }

    override fun seekToNext() {}
    override fun seekToPrevious() {}

    override fun play() {
        if (_isPlaying) return
        val uri = currentUri ?: return
        
        _isPlaying = true
        notifyPlayingChanged()
        
        playbackJob = scope.launch {
            try {
                val file = if (uri.startsWith("file://")) File(URI(uri)) else File(uri)
                if (!file.exists()) {
                    _isPlaying = false
                    notifyPlayingChanged()
                    return@launch
                }

                val fileStream = java.io.BufferedInputStream(java.io.FileInputStream(file))
                val inStream = AudioSystem.getAudioInputStream(fileStream)
                val baseFormat = inStream.format
                val decodedFormat = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.sampleRate,
                    16,
                    baseFormat.channels,
                    baseFormat.channels * 2,
                    baseFormat.sampleRate,
                    false
                )
                
                val din = AudioSystem.getAudioInputStream(decodedFormat, inStream)
                audioInputStream = din
                
                val info = DataLine.Info(SourceDataLine::class.java, decodedFormat)
                val line = AudioSystem.getLine(info) as SourceDataLine
                sourceDataLine = line
                
                line.open(decodedFormat)
                applyVolume(line)
                line.start()
                
                val frameRate = decodedFormat.frameRate
                val frameSize = decodedFormat.frameSize
                val bytesPerMs = (frameRate * frameSize) / 1000.0f
                
                // Seek logic (approximate bytes)
                if (_currentPosition > 0L) {
                    val bytesToSkip = (_currentPosition * bytesPerMs).toLong()
                    din.skip(bytesToSkip)
                }
                
                val buffer = ByteArray(4096)
                while (isActive && _isPlaying) {
                    val bytesRead = din.read(buffer, 0, buffer.size)
                    if (bytesRead == -1) break
                    
                    equalizerManager?.processAudioBytes(buffer, decodedFormat.sampleRate)
                    
                    line.write(buffer, 0, bytesRead)
                    audioDataCallback?.invoke(buffer.copyOfRange(0, bytesRead))
                    _currentPosition += (bytesRead / bytesPerMs).toLong()
                }
                
                line.drain()
                line.stop()
                line.close()
                din.close()
                inStream.close()
                
                if (_currentPosition >= _duration - 500) { // EOF
                     _isPlaying = false
                     _currentPosition = 0L
                     notifyPlayingChanged()
                }
                
            } catch (e: Exception) {
                File("/tmp/player_error.log").writeText(e.stackTraceToString())
                e.printStackTrace()
                _isPlaying = false
                notifyPlayingChanged()
            }
        }
    }

    override fun pause() {
        if (!_isPlaying) return
        _isPlaying = false
        notifyPlayingChanged()
        stopInternal()
    }

    private fun stopInternal() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            sourceDataLine?.stop()
            sourceDataLine?.close()
            audioInputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        sourceDataLine = null
        audioInputStream = null
    }

    override fun setPlaybackSpeed(speed: Float) {
        // Not easily supported in basic Java Sound without DSP.
    }

    override fun setPlaybackPitch(pitch: Float) {
        // Not natively supported by Java Sound.
    }

    override fun setTrack(uri: String) {
        stopInternal()
        currentUri = uri
        _currentPosition = 0L
        _duration = calculateDuration(uri)
        _isPlaying = false
        notifyPlayingChanged()
        
        play()
    }

    private fun calculateDuration(uri: String): Long {
        try {
            val file = if (uri.startsWith("file://")) File(URI(uri)) else File(uri)
            if (!file.exists()) return 0L
            
            val fileFormat = AudioSystem.getAudioFileFormat(file)
            val properties = fileFormat.properties()
            val durationMicroseconds = properties["duration"] as? Long
            if (durationMicroseconds != null) {
                return durationMicroseconds / 1000L
            }
            
            // Fallback estimation
            val lengthInBytes = file.length()
            val sampleRate = fileFormat.format.sampleRate
            val frameSize = fileFormat.format.frameSize
            val frameRate = fileFormat.format.frameRate
            if (frameSize > 0 && frameRate > 0) {
                return (lengthInBytes * 1000L / (frameSize * frameRate)).toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }

    override fun setVolume(volume: Float) {
        _volume = max(0f, min(volume, 1f))
        sourceDataLine?.let { applyVolume(it) }
    }

    private fun applyVolume(line: SourceDataLine) {
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val minGain = gainControl.minimum
                val maxGain = gainControl.maximum
                
                // Convert linear volume (0.0 to 1.0) to decibels
                // dB = 20 * log10(linear)
                val gainDb = if (_volume <= 0.01f) minGain else {
                    20f * Math.log10(_volume.toDouble()).toFloat()
                }
                
                gainControl.value = max(minGain, min(gainDb, maxGain))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun addListener(listener: AppPlayerListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: AppPlayerListener) {
        listeners.remove(listener)
    }
    
    private fun notifyPlayingChanged() {
        val state = _isPlaying
        listeners.toList().forEach { it.onIsPlayingChanged(state) }
    }
}
