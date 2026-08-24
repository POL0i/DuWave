package com.example.beatpulse.data

import android.content.Context
import com.example.beatpulse.utils.DownloadHelper
import java.io.File

class AndroidLibraryPlatformHelper(private val context: Context) : ILibraryPlatformHelper {
    override fun scanFileToSystem(filePath: String, onCompleted: () -> Unit) {
        android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(filePath),
            null
        ) { _, _ ->
            onCompleted()
        }
    }

    override fun pickFolder(onFolderPicked: (String) -> Unit) {
        // Not used in Android directly via this interface (MediaStore does everything automatically)
    }

    override fun getLocalizedString(key: String, vararg formatArgs: Any): String {
        // Simple mapping based on string resource names. 
        // We'll use reflection or just lookup the identifier.
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        if (resId == 0) return key
        return context.getString(resId, *formatArgs)
    }

    override fun getCoversDir(): String {
        val coversDir = File(context.filesDir, "covers")
        if (!coversDir.exists()) coversDir.mkdirs()
        return coversDir.absolutePath
    }

    override fun downloadTrack(streamUrl: String, title: String, artist: String) {
        DownloadHelper.downloadTrack(context, streamUrl, title, artist)
    }
}
