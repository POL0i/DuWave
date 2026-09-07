package com.example.beatpulse.utils

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FFmpegHelper {

    private val ffmpegDir = File(System.getProperty("user.home"), ".beatpulse/bin")
    
    // Check if ffmpeg exists in PATH or in the local bin directory
    suspend fun isFFmpegInstalled(): Boolean = withContext(Dispatchers.IO) {
        getFFmpegPath() != null
    }

    suspend fun getFFmpegPath(): String? = withContext(Dispatchers.IO) {
        // First check local binary
        val osName = System.getProperty("os.name").lowercase()
        val isWindows = osName.contains("win")
        val exeName = if (isWindows) "ffmpeg.exe" else "ffmpeg"
        
        val localFfmpeg = File(ffmpegDir, exeName)
        if (localFfmpeg.exists() && localFfmpeg.canExecute()) {
            return@withContext localFfmpeg.absolutePath
        }

        // Second check system PATH
        try {
            val process = ProcessBuilder(exeName, "-version").start()
            process.waitFor()
            if (process.exitValue() == 0) {
                return@withContext exeName
            }
        } catch (e: Exception) {
            // Not in path
        }

        null
    }

    suspend fun downloadFFmpeg(onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val osName = System.getProperty("os.name").lowercase()
        val isWindows = osName.contains("win")
        
        try {
            if (!ffmpegDir.exists()) {
                ffmpegDir.mkdirs()
            }

            val downloadUrl = if (isWindows) {
                "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip"
            } else {
                "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz"
            }

            val url = java.net.URL(downloadUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()
            
            val fileLength = connection.contentLength
            val tempFile = File(ffmpegDir, if (isWindows) "ffmpeg.zip" else "ffmpeg.tar.xz")
            
            val input = java.io.BufferedInputStream(connection.inputStream)
            val output = java.io.FileOutputStream(tempFile)
            
            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int
            
            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                output.write(data, 0, count)
                if (fileLength > 0) {
                    onProgress((total * 100f / fileLength) / 100f)
                }
            }
            output.flush()
            output.close()
            input.close()

            // Extract logic
            if (isWindows) {
                val zipIn = java.util.zip.ZipInputStream(java.io.FileInputStream(tempFile))
                var entry: java.util.zip.ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith("ffmpeg.exe")) {
                        val exeFile = File(ffmpegDir, "ffmpeg.exe")
                        val exeOut = java.io.FileOutputStream(exeFile)
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (zipIn.read(buffer).also { read = it } != -1) {
                            exeOut.write(buffer, 0, read)
                        }
                        exeOut.close()
                        break
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                zipIn.close()
            } else {
                // For Linux/Mac, use system tar command
                val process = ProcessBuilder("tar", "-xf", tempFile.absolutePath, "-C", ffmpegDir.absolutePath, "--strip-components=1").start()
                process.waitFor()
                
                val exeFile = File(ffmpegDir, "ffmpeg")
                if (exeFile.exists()) {
                    exeFile.setExecutable(true)
                }
            }
            
            // Clean up temp file
            if (tempFile.exists()) {
                tempFile.delete()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            throw IllegalStateException("Failed to download FFmpeg: ${e.message}")
        }
    }
}
