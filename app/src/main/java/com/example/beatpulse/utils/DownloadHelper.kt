package com.example.beatpulse.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.beatpulse.data.TrackEntity

object DownloadHelper {

    fun downloadTrack(context: Context, streamUrl: String, title: String, artist: String, fileExtension: String = "m4a") {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(streamUrl)
            
            // Clean up title and artist to be filesystem safe
            val safeTitle = title.replace(Regex("[^a-zA-Z0-9.\\- ]"), "_")
            val safeArtist = artist.replace(Regex("[^a-zA-Z0-9.\\- ]"), "_")
            val fileName = "${safeArtist}_-_${safeTitle}.$fileExtension"
            
            val request = DownloadManager.Request(uri)
                .setTitle(title)
                .setDescription("Descargando canción...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "DuWave/$fileName")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
