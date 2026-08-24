package com.example.beatpulse.data

import java.io.File
import java.net.URL

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

    override fun downloadTrack(streamUrl: String, title: String, artist: String) {
        // Placeholder for Desktop download implementation.
        // Needs a JVM compatible downloader later, or we can use Ktor.
        println("Downloading $title by $artist from $streamUrl")
    }
}
