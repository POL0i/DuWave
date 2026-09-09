package com.example.beatpulse.player

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.URI
import javax.sound.sampled.*
import kotlin.math.max
import kotlin.math.min

class DesktopPlayerAdapter : AppPlayer {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var playbackJob: Job? = null
    private val playerMutex = kotlinx.coroutines.sync.Mutex()
    
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
        val targetPos = if (_duration > 0L) max(0L, min(positionMs, _duration)) else max(0L, positionMs)
        if (_currentPosition == targetPos) return
        
        scope.launch {
            playerMutex.withLock {
                val wasPlaying = _isPlaying
                if (wasPlaying) {
                    _isPlaying = false
                }
                
                stopInternal()
                
                _currentPosition = targetPos
                if (wasPlaying) {
                    playInternal()
                }
            }
        }
    }

    override fun seekToNext() {}
    override fun seekToPrevious() {}

    override fun play() {
        scope.launch {
            playerMutex.withLock {
                playInternal()
            }
        }
    }
    
    private var activeProcess: Process? = null

    override fun setTrack(uri: String) {
        setTrack(uri, 0L)
    }

    fun setTrack(uri: String, trackDurationMs: Long = 0L) {
        scope.launch {
            playerMutex.withLock {
                val wasPlaying = _isPlaying
                if (wasPlaying) {
                    _isPlaying = false
                }
                stopInternal()
                currentUri = uri
                _currentPosition = 0L
                _duration = if (trackDurationMs > 0L) trackDurationMs else calculateDuration(uri)
                _isPlaying = false
                notifyPlayingChanged()
                
                playInternal()
            }
        }
    }

    private fun playInternal() {
        if (_isPlaying) return
        val uri = currentUri ?: return
        
        _isPlaying = true
        notifyPlayingChanged()
        
        playbackJob = scope.launch {
            try {
                var process: Process? = null
                val inStream: java.io.InputStream
                val decodedFormat: AudioFormat

                val isLocal = !uri.startsWith("http")
                val inputUri = if (isLocal && uri.startsWith("file://")) URI(uri).path else uri
                
                if (isLocal) {
                    val file = File(inputUri)
                    if (!file.exists()) {
                        _isPlaying = false
                        notifyPlayingChanged()
                        return@launch
                    }
                }

                val pbArgs = mutableListOf(
                    "ffmpeg",
                    "-loglevel", "quiet",
                    "-ss", "${_currentPosition / 1000.0}"
                )
                if (!isLocal) {
                    pbArgs.add("-user_agent")
                    pbArgs.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                }
                pbArgs.addAll(listOf(
                    "-i", inputUri,
                    "-f", "s16le",
                    "-ac", "2",
                    "-ar", "44100",
                    "pipe:1"
                ))
                val pb = ProcessBuilder(pbArgs)
                pb.redirectError(ProcessBuilder.Redirect.DISCARD)
                process = pb.start()
                activeProcess = process
                inStream = process.inputStream
                decodedFormat = AudioFormat(
                    44100.0f,
                    16,
                    2,
                    true,
                    false
                )
                
                val info = DataLine.Info(SourceDataLine::class.java, decodedFormat)
                val line = AudioSystem.getLine(info) as SourceDataLine
                sourceDataLine = line
                
                line.open(decodedFormat)
                applyVolume(line)
                line.start()
                
                val bytesPerMs = (decodedFormat.sampleRate * decodedFormat.frameSize) / 1000.0f
                var totalBytesRead = (_currentPosition * bytesPerMs).toLong()
                
                val buffer = ByteArray(4096)
                var reachedEof = false
                while (isActive && _isPlaying) {
                    val bytesRead = inStream.read(buffer, 0, buffer.size)
                    if (bytesRead == -1) {
                        reachedEof = true
                        break
                    }
                    
                    val activeBytes = if (bytesRead < buffer.size) buffer.copyOfRange(0, bytesRead) else buffer
                    equalizerManager?.processAudioBytes(activeBytes, decodedFormat.sampleRate)

                    if (_volume > 1.0f) {
                        val mult = _volume
                        for (i in 0 until bytesRead step 2) {
                            val lower = activeBytes[i].toInt() and 0xFF
                            val upper = activeBytes[i+1].toInt()
                            var sample = (upper shl 8) or lower
                            // Sign extend 16-bit to 32-bit
                            sample = (sample shl 16) shr 16
                            sample = (sample * mult).toInt()
                            if (sample > 32767) sample = 32767
                            if (sample < -32768) sample = -32768
                            activeBytes[i] = (sample and 0xFF).toByte()
                            activeBytes[i+1] = ((sample shr 8) and 0xFF).toByte()
                        }
                    }
                    
                    line.write(activeBytes, 0, bytesRead)
                    audioDataCallback?.invoke(activeBytes)
                    
                    totalBytesRead += bytesRead
                    _currentPosition = (totalBytesRead / bytesPerMs).toLong()
                }
                
                if (reachedEof) {
                    line.drain()
                } else {
                    line.flush()
                }
                line.stop()
                line.close()
                inStream.close()
                process?.destroy()
                activeProcess = null
                
                if (reachedEof || (_duration > 0 && _currentPosition >= _duration - 500)) {
                    _isPlaying = false
                    _currentPosition = 0L
                    notifyPlayingChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isPlaying = false
                notifyPlayingChanged()
            }
        }
    }

    private suspend fun stopInternal() {
        val job = playbackJob
        job?.cancel()
        playbackJob = null
        try {
            activeProcess?.destroy()
            activeProcess = null
            sourceDataLine?.stop()
            sourceDataLine?.close()
            audioInputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        sourceDataLine = null
        job?.join()
    }

    override fun pause() {
        scope.launch {
            playerMutex.withLock {
                if (!_isPlaying) return@withLock
                _isPlaying = false
                notifyPlayingChanged()
                stopInternal()
            }
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        // Not easily supported in basic Java Sound without DSP.
    }

    override fun setPlaybackPitch(pitch: Float) {
        // Not natively supported by Java Sound.
    }

    private fun calculateDuration(uri: String): Long {
        try {
            val path = if (uri.startsWith("file://")) URI(uri).path else uri
            return com.example.beatpulse.utils.getAudioDuration(path)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }

    override fun setVolume(volume: Float) {
        _volume = max(0f, volume)
        sourceDataLine?.let { applyVolume(it) }
    }

    private fun applyVolume(line: SourceDataLine) {
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val minGain = gainControl.minimum
                val maxGain = gainControl.maximum
                
                val hardwareVolume = min(_volume, 1f)
                val gainDb = if (hardwareVolume <= 0.01f) minGain else {
                    20f * Math.log10(hardwareVolume.toDouble()).toFloat()
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
