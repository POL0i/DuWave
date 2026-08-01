package com.example.beatpulse.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.palette.graphics.Palette
import com.example.beatpulse.MainActivity
import com.example.beatpulse.R
import com.example.beatpulse.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaWidgetProvider4x1 : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, MediaWidgetProvider4x1::class.java)
                    )
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId, intent)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.beatpulse.UPDATE_WIDGET"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_COVER_PATH = "extra_cover_path"
        const val EXTRA_POSITION = "extra_position"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            intent: Intent? = null
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_media_4x1)

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
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
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
                
                val position = intent.getLongExtra(EXTRA_POSITION, 0L)
                if (isPlaying) {
                    views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                    views.setChronometer(R.id.widget_chronometer, SystemClock.elapsedRealtime() - position, null, true)
                } else {
                    views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                    views.setChronometer(R.id.widget_chronometer, SystemClock.elapsedRealtime() - position, null, false)
                }

                if (!coverPath.isNullOrEmpty()) {
                    var bitmap: android.graphics.Bitmap? = null
                    try {
                        val mmr = android.media.MediaMetadataRetriever()
                        mmr.setDataSource(coverPath)
                        val data = mmr.embeddedPicture
                        mmr.release()
                        if (data != null) {
                            bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (bitmap == null) {
                        try {
                            val uri = android.net.Uri.parse(coverPath)
                            if (uri.scheme == "content") {
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    bitmap = BitmapFactory.decodeStream(stream)
                                }
                            } else if (coverPath.startsWith("http://") || coverPath.startsWith("https://")) {
                                java.net.URL(coverPath).openStream().use { stream ->
                                    bitmap = BitmapFactory.decodeStream(stream)
                                }
                            } else {
                                bitmap = BitmapFactory.decodeFile(coverPath)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (bitmap != null) {
                        val prefs = com.example.beatpulse.data.PreferencesManager.getInstance(context)
                        val bgStyle = prefs.backgroundStyle
                        val styledBitmap = getStyledBitmap(bitmap, bgStyle)
                        views.setImageViewBitmap(R.id.widget_cover, styledBitmap)
                        
                        // Extract Palette color for dynamic background
                        val palette = Palette.from(bitmap).generate()
                        val dominantColor = palette.getDominantColor(0xFF222222.toInt())
                        val bgTint = getStyleColor(bgStyle, dominantColor)
                        val bgBitmap = getWidgetBackgroundBitmap(bgStyle, bgTint)
                        views.setImageViewBitmap(R.id.widget_dynamic_bg, bgBitmap)
                        applyTextColors(views, bgStyle)
                    } else {
                        views.setImageViewResource(R.id.widget_cover, android.R.drawable.ic_media_play)
                        val prefs = com.example.beatpulse.data.PreferencesManager.getInstance(context)
                        val bgTint = getStyleColor(prefs.backgroundStyle, 0xFF222222.toInt())
                        views.setImageViewBitmap(R.id.widget_dynamic_bg, getWidgetBackgroundBitmap(prefs.backgroundStyle, bgTint))
                        applyTextColors(views, prefs.backgroundStyle)
                    }
                } else {
                    views.setImageViewResource(R.id.widget_cover, android.R.drawable.ic_media_play)
                    val prefs = com.example.beatpulse.data.PreferencesManager.getInstance(context)
                    val bgTint = getStyleColor(prefs.backgroundStyle, 0xFF222222.toInt())
                    views.setImageViewBitmap(R.id.widget_dynamic_bg, getWidgetBackgroundBitmap(prefs.backgroundStyle, bgTint))
                    applyTextColors(views, prefs.backgroundStyle)
                }
            } else {
                val prefs = com.example.beatpulse.data.PreferencesManager.getInstance(context)
                val lastPath = prefs.lastPlayedTrackPath
                if (lastPath != null) {
                    var bitmap: android.graphics.Bitmap? = null
                    try {
                        val mmr = android.media.MediaMetadataRetriever()
                        mmr.setDataSource(lastPath)
                        val data = mmr.embeddedPicture
                        mmr.release()
                        if (data != null) {
                            bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (bitmap == null) {
                        try {
                            val uri = android.net.Uri.parse(lastPath)
                            if (uri.scheme == "content") {
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    bitmap = BitmapFactory.decodeStream(stream)
                                }
                            } else {
                                bitmap = BitmapFactory.decodeFile(lastPath)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (bitmap != null) {
                        val bgStyle = prefs.backgroundStyle
                        val styledBitmap = getStyledBitmap(bitmap, bgStyle)
                        views.setImageViewBitmap(R.id.widget_cover, styledBitmap)
                        
                        val palette = Palette.from(bitmap).generate()
                        val dominantColor = palette.getDominantColor(0xFF222222.toInt())
                        val bgTint = getStyleColor(bgStyle, dominantColor)
                        views.setImageViewBitmap(R.id.widget_dynamic_bg, getWidgetBackgroundBitmap(bgStyle, bgTint))
                        applyTextColors(views, bgStyle)
                        
                        // Extract title from filename as fallback
                        val filename = lastPath.substringAfterLast("/")
                        views.setTextViewText(R.id.widget_title, filename)
                    } else {
                        val bgTint = getStyleColor(prefs.backgroundStyle, 0xFF222222.toInt())
                        views.setImageViewBitmap(R.id.widget_dynamic_bg, getWidgetBackgroundBitmap(prefs.backgroundStyle, bgTint))
                        applyTextColors(views, prefs.backgroundStyle)
                    }
                } else {
                    val bgTint = getStyleColor(prefs.backgroundStyle, 0xFF222222.toInt())
                    views.setImageViewBitmap(R.id.widget_dynamic_bg, getWidgetBackgroundBitmap(prefs.backgroundStyle, bgTint))
                    applyTextColors(views, prefs.backgroundStyle)
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun applyTextColors(views: RemoteViews, bgStyle: Int) {
            val isLightBg = bgStyle == 2 || bgStyle == 3 || bgStyle == 4
            val textColor = if (isLightBg) android.graphics.Color.DKGRAY else android.graphics.Color.WHITE
            val subTextColor = if (isLightBg) android.graphics.Color.DKGRAY else android.graphics.Color.LTGRAY
            
            views.setTextColor(R.id.widget_title, textColor)
            views.setTextColor(R.id.widget_artist, subTextColor)
            views.setTextColor(R.id.widget_chronometer, subTextColor)
            // Use setInt to set colorFilter on image views
            views.setInt(R.id.widget_btn_play_pause, "setColorFilter", textColor)
            views.setInt(R.id.widget_btn_prev, "setColorFilter", textColor)
            views.setInt(R.id.widget_btn_next, "setColorFilter", textColor)
        }

        private fun getStyleColor(bgStyle: Int, dominantColor: Int): Int {
            val isDark = bgStyle == 1 || bgStyle == 5 || bgStyle == 6 || bgStyle == 7
            if (isDark) {
                return android.graphics.Color.argb(
                    230,
                    (android.graphics.Color.red(dominantColor) * 0.2).toInt(),
                    (android.graphics.Color.green(dominantColor) * 0.2).toInt(),
                    (android.graphics.Color.blue(dominantColor) * 0.2).toInt()
                )
            } else if (bgStyle == 2 || bgStyle == 4) { // Anime Pastel / Y2K
                return android.graphics.Color.argb(230, 255, 230, 240) // Pinkish
            } else if (bgStyle == 8) { // Corazones
                return android.graphics.Color.argb(230, 200, 20, 50)
            } else if (bgStyle == 3) { // Luminous
                return android.graphics.Color.argb(
                    230,
                    Math.min(255, (android.graphics.Color.red(dominantColor) * 1.5).toInt()),
                    Math.min(255, (android.graphics.Color.green(dominantColor) * 1.5).toInt()),
                    Math.min(255, (android.graphics.Color.blue(dominantColor) * 1.5).toInt())
                )
            }
            // Classic
            return android.graphics.Color.argb(
                210,
                (android.graphics.Color.red(dominantColor) * 0.4).toInt(),
                (android.graphics.Color.green(dominantColor) * 0.4).toInt(),
                (android.graphics.Color.blue(dominantColor) * 0.4).toInt()
            )
        }

        private fun getWidgetBackgroundBitmap(bgStyle: Int, color: Int): android.graphics.Bitmap {
            val width = 400
            val height = 100
            val output = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                this.color = color
            }
            
            val rect = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
            when (bgStyle) {
                1 -> { // Cyberpunk (Cut corners)
                    val path = android.graphics.Path().apply {
                        val cut = height * 0.3f
                        moveTo(cut, 0f)
                        lineTo(width.toFloat() - cut, 0f)
                        lineTo(width.toFloat(), cut)
                        lineTo(width.toFloat(), height - cut)
                        lineTo(width.toFloat() - cut, height.toFloat())
                        lineTo(cut, height.toFloat())
                        lineTo(0f, height - cut)
                        lineTo(0f, cut)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
                5 -> { // Black Metal (sharp edges)
                    canvas.drawRect(rect, paint)
                }
                6 -> { // Dark Fantasy (Asymmetrical cut)
                    val path = android.graphics.Path().apply {
                        val cut = height * 0.4f
                        moveTo(cut, 0f)
                        lineTo(width.toFloat(), 0f)
                        lineTo(width.toFloat(), height.toFloat())
                        lineTo(0f, height.toFloat())
                        lineTo(0f, cut)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
                7 -> { // Catedral (Gothic arch peak in the middle)
                    val path = android.graphics.Path().apply {
                        val corner = height * 0.2f
                        moveTo(0f, height.toFloat())
                        lineTo(0f, corner)
                        quadTo(0f, 0f, corner, 0f)
                        lineTo(width * 0.4f, 0f)
                        // Gothic peak
                        lineTo(width * 0.5f, -height * 0.3f)
                        lineTo(width * 0.6f, 0f)
                        lineTo(width - corner, 0f)
                        quadTo(width.toFloat(), 0f, width.toFloat(), corner)
                        lineTo(width.toFloat(), height.toFloat())
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
                2, 4, 8 -> { // Anime, Y2K, Corazones (Pill shape)
                    val radius = height / 2f
                    canvas.drawRoundRect(rect, radius, radius, paint)
                }
                else -> { // Classic and Luminous (Standard rounded corners)
                    val radius = height * 0.2f
                    canvas.drawRoundRect(rect, radius, radius, paint)
                }
            }
            return output
        }

        private fun getStyledBitmap(source: android.graphics.Bitmap, bgStyle: Int): android.graphics.Bitmap {
            val originalSize = Math.min(source.width, source.height)
            val size = Math.min(originalSize, 200)
            val scaledSource = if (originalSize > 200) android.graphics.Bitmap.createScaledBitmap(source, (source.width * 200) / originalSize, (source.height * 200) / originalSize, true) else source
            val output = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                shader = android.graphics.BitmapShader(scaledSource, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
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
