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
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object DownloadHelper {
    private val client = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.MINUTES) // Long timeout to support pause
        .build()

    // 0 = downloading, 1 = paused, 2 = canceled
    private val downloadStates = ConcurrentHashMap<Int, Int>()
    private var receiverRegistered = false

    private fun registerReceiverIfNeeded(context: Context) {
        if (!receiverRegistered) {
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context, intent: android.content.Intent) {
                    val id = intent.getIntExtra("id", -1)
                    if (id != -1) {
                        when (intent.action) {
                            "com.example.beatpulse.PAUSE_DOWNLOAD" -> downloadStates[id] = 1
                            "com.example.beatpulse.RESUME_DOWNLOAD" -> downloadStates[id] = 0
                            "com.example.beatpulse.CANCEL_DOWNLOAD" -> downloadStates[id] = 2
                        }
                    }
                }
            }
            val filter = android.content.IntentFilter().apply {
                addAction("com.example.beatpulse.PAUSE_DOWNLOAD")
                addAction("com.example.beatpulse.RESUME_DOWNLOAD")
                addAction("com.example.beatpulse.CANCEL_DOWNLOAD")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.applicationContext.registerReceiver(receiver, filter)
            }
            receiverRegistered = true
        }
    }

    fun downloadTrack(context: Context, streamUrl: String, title: String, artist: String, fileExtension: String = "m4a") {
        registerReceiverIfNeeded(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "duwave_downloads"
            
            val prefs = PreferencesManager.getInstance(context)
            val lang = prefs.appLanguage
            val locale = java.util.Locale(lang)
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(locale)
            val localizedContext = context.createConfigurationContext(config)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    localizedContext.getString(localizedContext.resources.getIdentifier("notification_channel_downloads", "string", localizedContext.packageName)),
                    NotificationManager.IMPORTANCE_LOW
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notificationId = System.currentTimeMillis().toInt()
            downloadStates[notificationId] = 0
            
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
                .setContentTitle(localizedContext.getString(com.example.beatpulse.R.string.notification_downloading, title))
                .setContentText(artist)
                .setColor(color)
                .setColorized(true)
                .setProgress(100, 0, true)
                .setOngoing(true)

            // Añadir acciones iniciales (Pausar y Cancelar)
            val pauseIntent = android.content.Intent("com.example.beatpulse.PAUSE_DOWNLOAD").apply { putExtra("id", notificationId) }
            val pausePending = android.app.PendingIntent.getBroadcast(context, notificationId, pauseIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, localizedContext.getString(com.example.beatpulse.R.string.action_pause), pausePending)
            
            val cancelIntent = android.content.Intent("com.example.beatpulse.CANCEL_DOWNLOAD").apply { putExtra("id", notificationId) }
            val cancelPending = android.app.PendingIntent.getBroadcast(context, notificationId, cancelIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, localizedContext.getString(com.example.beatpulse.R.string.action_cancel), cancelPending)

            notificationManager.notify(notificationId, builder.build())

            var uri: android.net.Uri? = null
            val resolver = context.contentResolver
            
            try {
                val safeTitle = title.replace(Regex("[^a-zA-Z0-9.\\- ]"), "_")
                val safeArtist = artist.replace(Regex("[^a-zA-Z0-9.\\- ]"), "_")
                val fileName = "${safeArtist}_-_${safeTitle}.$fileExtension"

                val request = Request.Builder().url(streamUrl).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) throw Exception("Error al descargar: ${response.code}")

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

                uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        response.body?.byteStream()?.use { input ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            val contentLength = response.body?.contentLength() ?: -1L
                            var totalBytesRead = 0L
                            var lastUpdateTime = 0L

                            while (true) {
                                val state = downloadStates[notificationId] ?: 0
                                if (state == 2) { // Canceled
                                    throw Exception("Descarga cancelada")
                                }
                                if (state == 1) { // Paused
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastUpdateTime > 1000) {
                                        lastUpdateTime = currentTime
                                        builder.setContentText(localizedContext.getString(com.example.beatpulse.R.string.notification_paused_artist, artist))
                                        builder.clearActions()
                                        
                                        val resumeIntent = android.content.Intent("com.example.beatpulse.RESUME_DOWNLOAD").apply { putExtra("id", notificationId) }
                                        val resumePending = android.app.PendingIntent.getBroadcast(context, notificationId, resumeIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                                        builder.addAction(android.R.drawable.ic_media_play, localizedContext.getString(com.example.beatpulse.R.string.action_resume), resumePending)
                                        
                                        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, localizedContext.getString(com.example.beatpulse.R.string.action_cancel), cancelPending)
                                        
                                        notificationManager.notify(notificationId, builder.build())
                                    }
                                    delay(500)
                                    continue
                                }

                                // Downloading state
                                bytesRead = input.read(buffer)
                                if (bytesRead == -1) break
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastUpdateTime > 500) {
                                    lastUpdateTime = currentTime
                                    builder.clearActions()
                                    builder.addAction(android.R.drawable.ic_media_pause, localizedContext.getString(com.example.beatpulse.R.string.action_pause), pausePending)
                                    builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, localizedContext.getString(com.example.beatpulse.R.string.action_cancel), cancelPending)
                                    
                                    if (contentLength > 0) {
                                        val progress = ((totalBytesRead * 100) / contentLength).toInt()
                                        val mbRead = String.format(java.util.Locale.US, "%.1f", totalBytesRead / 1024f / 1024f)
                                        val mbTotal = String.format(java.util.Locale.US, "%.1f", contentLength / 1024f / 1024f)
                                        builder.setProgress(100, progress, false)
                                               .setContentText(localizedContext.getString(com.example.beatpulse.R.string.notification_progress, artist, mbRead, mbTotal))
                                    } else {
                                        val mbRead = String.format(java.util.Locale.US, "%.1f", totalBytesRead / 1024f / 1024f)
                                        builder.setProgress(100, 0, true)
                                               .setContentText(localizedContext.getString(com.example.beatpulse.R.string.notification_progress_indeterminate, artist, mbRead))
                                    }
                                    notificationManager.notify(notificationId, builder.build())
                                }
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                }
                
                builder.clearActions()
                builder.setContentTitle(localizedContext.getString(com.example.beatpulse.R.string.download_completed))
                    .setContentText(title)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                
                notificationManager.notify(notificationId, builder.build())
                
                withContext(Dispatchers.Main) {
                    prefs.showToast(localizedContext.getString(com.example.beatpulse.R.string.download_completed_desc, title))
                }
                
                // Forzar escaneo para que se agregue inmediatamente a la librería
                val repo = org.koin.java.KoinJavaComponent.getKoin().get<com.example.beatpulse.data.MusicRepository>()
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { repo.scanLocalLibrary() }
            } catch (e: Exception) {
                e.printStackTrace()
                builder.clearActions()
                
                // Cleanup partial file if canceled
                if (e.message == "Descarga cancelada" && uri != null) {
                    resolver.delete(uri, null, null)
                    builder.setContentTitle(localizedContext.getString(com.example.beatpulse.R.string.notification_download_canceled))
                        .setContentText(title)
                        .setSmallIcon(android.R.drawable.stat_sys_warning)
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                } else {
                    builder.setContentTitle(localizedContext.getString(com.example.beatpulse.R.string.notification_download_error))
                        .setContentText(title)
                        .setSmallIcon(android.R.drawable.stat_sys_warning)
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                }
                notificationManager.notify(notificationId, builder.build())
            } finally {
                downloadStates.remove(notificationId)
            }
        }
    }
}
