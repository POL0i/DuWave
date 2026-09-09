package com.example.beatpulse.utils

import java.awt.Desktop
import java.net.URI
import androidx.compose.runtime.getValue
import kotlinx.coroutines.*

actual object SystemUtils {
    actual fun showToast(message: String) {
        // En Desktop, en lugar de Toast, se puede imprimir en consola temporalmente
        // o enlazarlo a un Snackbar del Scaffold global en Compose.
        println("TOAST (Desktop): $message")
    }

    actual fun openUrl(url: String) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                println("No se pudo abrir el navegador. URL: $url")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun recreateApp() {
        // En Desktop no es fácil reiniciar la app desde sí misma de manera estándar.
        // Simplemente imprimiremos un log o sugeriremos reiniciar manualmente.
        println("Por favor, reinicia la aplicación para aplicar los cambios de idioma.")
    }

    actual fun pickImageFile(): String? {
        val fileDialog = java.awt.FileDialog(java.awt.Frame(), "Seleccionar imagen", java.awt.FileDialog.LOAD)
        fileDialog.isVisible = true
        val file = fileDialog.file
        val dir = fileDialog.directory
        return if (file != null && dir != null) {
            dir + file
        } else {
            null
        }
    }

    actual val isMobilePlatform: Boolean = false

    private val backHandlers = mutableListOf<() -> Unit>()

    fun registerBackHandler(handler: () -> Unit) {
        backHandlers.add(handler)
    }

    fun unregisterBackHandler(handler: () -> Unit) {
        backHandlers.remove(handler)
    }

    actual fun dispatchSystemBack(): Boolean {
        if (backHandlers.isNotEmpty()) {
            backHandlers.last().invoke()
            return true
        }
        return false
    }

    // In desktop music players (like Spotify, VLC), standard behavior is to NOT control
    // the global OS master volume, but rather an internal application volume.
    // We maintain this internal state here to sync with the slider, while the actual 
    // audio amplification/reduction is done via DesktopPlayerAdapter multiplying the PCM samples.
    private var desktopAppVolume: Float = 1.0f

    actual fun getSystemVolumeLevel(): Float {
        return desktopAppVolume
    }

    actual fun setSystemVolumeLevel(volume: Float) {
        desktopAppVolume = volume.coerceIn(0f, 1f)
    }
}

@androidx.compose.runtime.Composable
actual fun SystemBackHandler(onBack: () -> Unit) {
    val currentOnBack = androidx.compose.runtime.rememberUpdatedState(onBack)
    androidx.compose.runtime.DisposableEffect(Unit) {
        val handler: () -> Unit = { currentOnBack.value() }
        SystemUtils.registerBackHandler(handler)
        onDispose {
            SystemUtils.unregisterBackHandler(handler)
        }
    }
}

@androidx.compose.runtime.Composable
actual fun SystemImagePicker(
    onFileSelected: (String?) -> Unit,
    colorDominant: androidx.compose.ui.graphics.Color,
    colorVibrant: androidx.compose.ui.graphics.Color
) {
    com.example.beatpulse.ui.components.ComposeImagePicker(
        onFileSelected = onFileSelected,
        colorDominant = colorDominant,
        colorVibrant = colorVibrant
    )
}

@androidx.compose.runtime.Composable
actual fun SystemMicPermissionHandler(
    requestTrigger: Boolean,
    onResult: (Boolean) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(requestTrigger) {
        if (requestTrigger) {
            onResult(true)
        }
    }
}

@androidx.compose.runtime.Composable
actual fun SystemStatusBarVisibility(visible: Boolean) {
    // No-op on desktop
}


val currentAppLanguageState = androidx.compose.runtime.mutableStateOf(
    com.example.beatpulse.DesktopAppPreferences().appLanguage
)

@androidx.compose.runtime.Composable
actual fun getLocalizedString(key: String): String {
    val lang by currentAppLanguageState
    val map = when (lang) {
        "en" -> desktopStringsEn
        "pt" -> desktopStringsPt
        else -> desktopStringsEs
    }
    return map[key] ?: key
}


actual suspend fun isAudioTrimmerReady(): Boolean {
    return FFmpegHelper.isFFmpegInstalled()
}

actual suspend fun downloadAudioTrimmerDependencies(onProgress: (Float) -> Unit) {
    FFmpegHelper.downloadFFmpeg(onProgress)
}

actual suspend fun trimAudioFile(inputPath: String, outputDir: String, outputFileNameBase: String, startMs: Long, endMs: Long): String? {
    return try {
        val ffmpegPath = FFmpegHelper.getFFmpegPath() ?: return null
        
        val startSec = startMs / 1000.0
        val durationSec = (endMs - startMs) / 1000.0
        
        // Determine original extension
        val originalExt = if (inputPath.contains(".")) inputPath.substringAfterLast(".") else "m4a"
        val outputFile = java.io.File(outputDir, "$outputFileNameBase.$originalExt")
        
        val processBuilder = ProcessBuilder(
            ffmpegPath,
            "-y",
            "-ss", startSec.toString(),
            "-i", inputPath,
            "-t", durationSec.toString(),
            "-c:a", "aac",
            outputFile.absolutePath
        )
        processBuilder.redirectErrorStream(true)
        val process = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            processBuilder.start()
        }
        
        val output = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            process.inputStream.bufferedReader().readText()
        }
        val exitCode = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            process.waitFor()
        }
        println("FFmpeg trim output: $output, exitCode: $exitCode")
        
        if (exitCode == 0 && outputFile.exists()) {
            outputFile.absolutePath
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

actual fun getAudioDuration(filePath: String): Long {
    try {
        val ffmpegPath = kotlinx.coroutines.runBlocking { com.example.beatpulse.utils.FFmpegHelper.getFFmpegPath() }
            ?: return 0L

        val process = ProcessBuilder(ffmpegPath, "-i", filePath).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        
        val regex = Regex("Duration:\\s+(\\d+):(\\d+):(\\d+\\.\\d+)")
        val match = regex.find(output)
        if (match != null) {
            val hours = match.groupValues[1].toLong()
            val minutes = match.groupValues[2].toLong()
            val seconds = match.groupValues[3].toDouble()
            
            val totalSeconds = (hours * 3600) + (minutes * 60) + seconds
            return (totalSeconds * 1000).toLong()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return 0L
}

@androidx.compose.runtime.Composable
actual fun AudioPreviewPlayer(
    path: String,
    isPlaying: Boolean,
    startMs: Long,
    endMs: Long,
    onPlaybackCompleted: () -> Unit
) {
    val processRef = androidx.compose.runtime.remember { java.util.concurrent.atomic.AtomicReference<Process?>() }
    val lineRef = androidx.compose.runtime.remember { java.util.concurrent.atomic.AtomicReference<javax.sound.sampled.SourceDataLine?>() }

    androidx.compose.runtime.DisposableEffect(path) {
        onDispose {
            lineRef.get()?.stop()
            lineRef.get()?.close()
            processRef.get()?.destroy()
        }
    }

    androidx.compose.runtime.LaunchedEffect(isPlaying, startMs, endMs) {
        if (isPlaying) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val ffmpegPath = com.example.beatpulse.utils.FFmpegHelper.getFFmpegPath() ?: return@withContext
                    val startSec = startMs / 1000.0
                    val durationSec = (endMs - startMs) / 1000.0
                    
                    val process = ProcessBuilder(
                        ffmpegPath, "-loglevel", "quiet",
                        "-ss", startSec.toString(),
                        "-t", durationSec.toString(),
                        "-i", path, "-f", "s16le", "-ac", "2", "-ar", "44100", "pipe:1"
                    ).redirectErrorStream(false).start()
                    
                    processRef.set(process)
                    
                    val inStream = process.inputStream
                    val decodedFormat = javax.sound.sampled.AudioFormat(44100.0f, 16, 2, true, false)
                    val line = javax.sound.sampled.AudioSystem.getLine(javax.sound.sampled.DataLine.Info(javax.sound.sampled.SourceDataLine::class.java, decodedFormat)) as javax.sound.sampled.SourceDataLine
                    
                    lineRef.set(line)
                    line.open(decodedFormat, 44100 * 4) // 1 second buffer (44100 frames * 4 bytes)
                    line.start()
                    
                    val buffer = ByteArray(32768)
                    while (isActive && isPlaying) {
                        val read = inStream.read(buffer)
                        if (read == -1) break
                        line.write(buffer, 0, read)
                    }
                    
                    line.drain()
                    line.stop()
                    line.close()
                    inStream.close()
                    process.destroy()
                    
                    if (isActive) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onPlaybackCompleted()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            lineRef.get()?.stop()
            lineRef.get()?.close()
            processRef.get()?.destroy()
        }
    }
}
