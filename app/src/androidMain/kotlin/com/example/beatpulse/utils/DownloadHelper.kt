package com.example.beatpulse.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.example.beatpulse.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object DownloadHelper {
    private val client = OkHttpClient()

    fun downloadTrack(context: Context, streamUrl: String, title: String, artist: String, fileExtension: String = "m4a") {
        CoroutineScope(Dispatchers.IO).launch {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "duwave_downloads"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Descargas de DuWave",
                    NotificationManager.IMPORTANCE_LOW
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notificationId = System.currentTimeMillis().toInt()
            
            val prefs = PreferencesManager.getInstance(context)
            val color = when (prefs.backgroundStyle) {
                1 -> 0xFFFF003C.toInt() // Cyberpunk
                2 -> 0xFFFF9800.toInt() // Anime
                3 -> 0xFF000000.toInt() // Amoled
                4 -> 0xFFFFFFFF.toInt() // Minimalist
                5 -> 0xFFFFC0CB.toInt() // Aesthetic
                6 -> 0xFF00FFFF.toInt() // Vaporwave
                7 -> 0xFF191970.toInt() // Space
                8 -> 0xFF228B22.toInt() // Forest
                else -> 0xFF6200EE.toInt() // Default
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Descargando: $title")
                .setContentText(artist)
                .setColor(color)
                .setColorized(true)
                .setProgress(100, 0, true)
                .setOngoing(true)

            notificationManager.notify(notificationId, builder.build())

            try {
                val safeTitle = title.replace(Regex("[^a-zA-Z0-9.\\- ]"), "_")
                val safeArtist = artist.replace(Regex("[^a-zA-Z0-9.\\- ]"), "_")
                val fileName = "${safeArtist}_-_${safeTitle}.$fileExtension"

                val request = Request.Builder().url(streamUrl).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) throw Exception("Error al descargar: ${response.code}")

                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.TITLE, safeTitle) // Asegurar etiqueta Título
                    put(MediaStore.Audio.Media.ARTIST, safeArtist) // Asegurar etiqueta Artista
                    put(MediaStore.Audio.Media.MIME_TYPE, if (fileExtension == "mp3") "audio/mpeg" else "audio/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/DuWave")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                }

                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        response.body?.byteStream()?.use { input ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                }

                builder.setContentTitle(context.getString(com.example.beatpulse.R.string.download_completed))
                    .setContentText(title)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                
                notificationManager.notify(notificationId, builder.build())
                
                withContext(Dispatchers.Main) {
                    prefs.showToast(context.getString(com.example.beatpulse.R.string.download_completed_desc, title))
                }
                
                // Forzar escaneo para que se agregue inmediatamente a la librería con las etiquetas correctas
                val musicRepo = com.example.beatpulse.data.MusicRepository(context)
                musicRepo.scanMediaStore()
                
            } catch (e: Exception) {
                e.printStackTrace()
                builder.setContentTitle("Error en la descarga")
                    .setContentText(title)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                notificationManager.notify(notificationId, builder.build())
            }
        }
    }
}
