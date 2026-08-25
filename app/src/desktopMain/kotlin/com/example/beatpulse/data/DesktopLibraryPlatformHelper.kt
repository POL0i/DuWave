package com.example.beatpulse.data

import java.io.File
import java.net.URL
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory

class DesktopLibraryPlatformHelper : ILibraryPlatformHelper {
    override fun scanFileToSystem(filePath: String, onCompleted: () -> Unit) {
        // Desktop doesn't have a system MediaStore, so just complete.
        onCompleted()
    }

    override fun pickFolder(onFolderPicked: (String) -> Unit) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {}
        
        val chooser = javax.swing.JFileChooser()
        chooser.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
        chooser.dialogTitle = "Select Music Folder"
        chooser.isAcceptAllFileFilterUsed = false

        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
            onFolderPicked(chooser.selectedFile.absolutePath)
        }
    }

    override fun getLocalizedString(key: String, vararg formatArgs: Any): String {
        // Desktop mock localization. We can expand this later if needed.
        return when (key) {
            "because_you_listened" -> "Because you listened to %s"
            "top_artists" -> "Top Artists"
            "based_on" -> "Based on %s"
            "for_you" -> "For you"
            "trending_music" -> "Trending Music"
            "toast_fetching_link" -> "Fetching link for %s..."
            "toast_download_started" -> "Started downloading %s"
            "toast_download_failed_link" -> "Failed to get download link for %s"
            "toast_download_error" -> "Error downloading %s"
            "covers_reloaded" -> "Covers reloaded"
            "reloading_covers_progress" -> "Reloading covers... %d/%d"
            else -> key
        }.let { 
            if (formatArgs.isNotEmpty()) {
                try {
                    it.format(*formatArgs)
                } catch (e: Exception) {
                    it
                }
            } else {
                it
            }
        }
    }

    override fun getCoversDir(): String {
        val coversDir = File(System.getProperty("user.home"), ".beatpulse/covers")
        if (!coversDir.exists()) coversDir.mkdirs()
        return coversDir.absolutePath
    }

    private val downloadScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    private val okHttpClient = okhttp3.OkHttpClient()

    override fun downloadTrack(streamUrl: String, title: String, artist: String, coverPath: String?) {
        println("Starting download: $title by $artist")
        downloadScope.launch {
            try {
                val request = okhttp3.Request.Builder().url(streamUrl).build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val musicDir = File(System.getProperty("user.home"), "Music/DuWave")
                    if (!musicDir.exists()) musicDir.mkdirs()
                    
                    val safeTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                    val safeArtist = artist.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                    val file = File(musicDir, "${safeArtist}_-_${safeTitle}.m4a")
                    
                    val inputStream = response.body()?.byteStream()
                    if (inputStream != null) {
                        val outputStream = java.io.FileOutputStream(file)
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        outputStream.close()
                        inputStream.close()
                        println("Download complete: ${file.absolutePath}")
                        
                        // Inject ID3 Tags
                        try {
                            val audioFile = AudioFileIO.read(file)
                            val tag = audioFile.tagOrCreateAndSetDefault
                            tag.setField(FieldKey.TITLE, title)
                            tag.setField(FieldKey.ARTIST, artist)
                            
                            if (coverPath != null) {
                                val coverFile = File(coverPath)
                                if (coverFile.exists()) {
                                    val artwork = ArtworkFactory.createArtworkFromFile(coverFile)
                                    tag.setField(artwork)
                                }
                            }
                            
                            audioFile.commit()
                            println("ID3 tags injected successfully.")
                        } catch (e: Exception) {
                            println("Error injecting ID3 tags: ${e.message}")
                        }
                    }
                } else {
                    println("Download failed with code: ${response.code()}")
                }
            } catch (e: Exception) {
                println("Error downloading track: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
