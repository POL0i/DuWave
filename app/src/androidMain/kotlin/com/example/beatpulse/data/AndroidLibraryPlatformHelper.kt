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
        val sharedPrefs = context.getSharedPreferences("beatpulse_prefs", Context.MODE_PRIVATE)
        val lang = sharedPrefs.getString("appLanguage", "es") ?: "es"
        val locale = java.util.Locale(lang)
        
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)
        
        val resId = localizedContext.resources.getIdentifier(key, "string", localizedContext.packageName)
        val template = if (resId == 0) key else localizedContext.getString(resId)
        
        return if (formatArgs.isNotEmpty()) {
            try {
                val args = formatArgs.map { it }.toTypedArray()
                java.lang.String.format(locale, template, *args)
            } catch (e: Exception) {
                template
            }
        } else {
            template
        }
    }

    override fun getCoversDir(): String {
        val coversDir = File(context.filesDir, "covers")
        if (!coversDir.exists()) coversDir.mkdirs()
        return coversDir.absolutePath
    }

    override fun downloadTrack(streamUrl: String, title: String, artist: String, coverPath: String?) {
        DownloadHelper.downloadTrack(context, streamUrl, title, artist)
    }
}
