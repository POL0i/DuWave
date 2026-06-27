package com.example.beatpulse.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import com.example.beatpulse.MainActivity
import com.example.beatpulse.R
import com.example.beatpulse.service.PlaybackService

class MediaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MediaWidgetProvider::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, intent)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.beatpulse.UPDATE_WIDGET"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_COVER_PATH = "extra_cover_path"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            intent: Intent? = null
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_media_v2)

            // Setup Intents for buttons
            val playIntent = Intent(context, PlaybackService::class.java).apply { action = "TOGGLE_PLAY" }
            val playPendingIntent = PendingIntent.getService(context, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPendingIntent)

            val nextIntent = Intent(context, PlaybackService::class.java).apply { action = "SKIP_NEXT" }
            val nextPendingIntent = PendingIntent.getService(context, 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

            val prevIntent = Intent(context, PlaybackService::class.java).apply { action = "SKIP_PREV" }
            val prevPendingIntent = PendingIntent.getService(context, 2, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

            // Open App Intent
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(context, 3, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

            if (intent != null) {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "No playing"
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: "BeatPulse"
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val coverPath = intent.getStringExtra(EXTRA_COVER_PATH)

                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_artist, artist)

                if (isPlaying) {
                    views.setImageViewResource(R.id.widget_btn_play_pause, android.R.drawable.ic_media_pause)
                } else {
                    views.setImageViewResource(R.id.widget_btn_play_pause, android.R.drawable.ic_media_play)
                }

                if (!coverPath.isNullOrEmpty()) {
                    var bitmap: android.graphics.Bitmap? = null
                    try {
                        if (coverPath.endsWith(".mp3", ignoreCase = true) || coverPath.endsWith(".flac", ignoreCase = true) || coverPath.endsWith(".wav", ignoreCase = true) || coverPath.endsWith(".m4a", ignoreCase = true)) {
                            val mmr = android.media.MediaMetadataRetriever()
                            mmr.setDataSource(coverPath)
                            val data = mmr.embeddedPicture
                            mmr.release()
                            if (data != null) {
                                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                            }
                        } else {
                            bitmap = BitmapFactory.decodeFile(coverPath)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (bitmap != null) {
                        val prefs = com.example.beatpulse.data.PreferencesManager.getInstance(context)
                        val bgStyle = prefs.backgroundStyle
                        val styledBitmap = getStyledBitmap(bitmap, bgStyle)
                        views.setImageViewBitmap(R.id.widget_cover, styledBitmap)
                    } else {
                        views.setImageViewResource(R.id.widget_cover, android.R.drawable.ic_media_play)
                    }
                } else {
                    views.setImageViewResource(R.id.widget_cover, android.R.drawable.ic_media_play)
                }
            } else {
                val prefs = com.example.beatpulse.data.PreferencesManager.getInstance(context)
                val lastPath = prefs.lastPlayedTrackPath
                if (lastPath != null) {
                    var bitmap: android.graphics.Bitmap? = null
                    try {
                        if (lastPath.endsWith(".mp3", ignoreCase = true) || lastPath.endsWith(".flac", ignoreCase = true) || lastPath.endsWith(".wav", ignoreCase = true) || lastPath.endsWith(".m4a", ignoreCase = true)) {
                            val mmr = android.media.MediaMetadataRetriever()
                            mmr.setDataSource(lastPath)
                            val data = mmr.embeddedPicture
                            mmr.release()
                            if (data != null) {
                                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                            }
                        }
                    } catch (e: Exception) {}

                    if (bitmap != null) {
                        val bgStyle = prefs.backgroundStyle
                        val styledBitmap = getStyledBitmap(bitmap, bgStyle)
                        views.setImageViewBitmap(R.id.widget_cover, styledBitmap)
                        
                        // Extract title from filename as fallback
                        val filename = lastPath.substringAfterLast("/")
                        views.setTextViewText(R.id.widget_title, filename)
                    }
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getStyledBitmap(source: android.graphics.Bitmap, bgStyle: Int): android.graphics.Bitmap {
            val size = Math.min(source.width, source.height)
            val output = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                shader = android.graphics.BitmapShader(source, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
            }
            
            val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
            when (bgStyle) {
                1 -> { // Cyberpunk (Cut corners)
                    val path = android.graphics.Path().apply {
                        val cut = size * 0.2f
                        moveTo(cut, 0f)
                        lineTo(size.toFloat(), 0f)
                        lineTo(size.toFloat(), size - cut)
                        lineTo(size - cut, size.toFloat())
                        lineTo(0f, size.toFloat())
                        lineTo(0f, cut)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
                2, 4, 8 -> { // Anime Pastel / Y2K Kawaii / Corazones (Corazón / Heart)
                    val path = android.graphics.Path().apply {
                        val width = size.toFloat()
                        val height = size.toFloat()
                        moveTo(width / 2, height / 5)
                        cubicTo(width * 5 / 14, 0f, 0f, height / 15, width / 28, height * 2 / 5)
                        cubicTo(width / 14, height * 2 / 3, width * 3 / 7, height * 5 / 6, width / 2, height)
                        cubicTo(width * 4 / 7, height * 5 / 6, width * 13 / 14, height * 2 / 3, width * 27 / 28, height * 2 / 5)
                        cubicTo(width, height / 15, width * 9 / 14, 0f, width / 2, height / 5)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
                5 -> { // Black Metal (sharp)
                    canvas.drawRect(rect, paint)
                }
                6 -> { // Dark Fantasy (Cut Corner asymmetrical)
                    val path = android.graphics.Path().apply {
                        val cut = size * 0.2f
                        moveTo(cut, 0f)
                        lineTo(size.toFloat(), 0f)
                        lineTo(size.toFloat(), size.toFloat())
                        lineTo(0f, size.toFloat())
                        lineTo(0f, cut)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
                7 -> { // Catedral (Arco superior)
                    val path = android.graphics.Path().apply {
                        val r = size * 0.4f
                        moveTo(0f, size.toFloat())
                        lineTo(0f, r)
                        arcTo(android.graphics.RectF(0f, 0f, size.toFloat(), r * 2), 180f, 180f)
                        lineTo(size.toFloat(), size.toFloat())
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
                else -> { // Clásico y Luminoso (rounded corners)
                    canvas.drawRoundRect(rect, size * 0.2f, size * 0.2f, paint)
                }
            }
            return output
        }
    }
}
