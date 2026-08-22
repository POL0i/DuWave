package com.example.beatpulse.ui.components

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.util.LruCache
import android.util.LruCache as AndroidLruCache
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.palette.graphics.Palette
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.theme.PaletteColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 6

    // Caché de miniaturas pequeñas (120x120) para listas
    val thumbCache = object : LruCache<Long, ImageBitmap>(cacheSize / 2) {
        override fun sizeOf(key: Long, bitmap: ImageBitmap): Int {
            return (bitmap.width * bitmap.height * 4) / 1024
        }
    }

    // Caché de imágenes completas (hasta 600x600) para el reproductor
    val fullCache = object : LruCache<Long, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: Long, bitmap: ImageBitmap): Int {
            return (bitmap.width * bitmap.height * 4) / 1024
        }
    }

    val noArtSet = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<Long, Boolean>())

    @Volatile
    var isPriorityLoading = false

    fun getTrackFingerprint(track: TrackEntity): String {
        return Math.abs((track.title + track.artist + track.album + track.duration + (track.customCoverPath ?: "")).hashCode()).toString()
    }

    fun invalidateTrack(context: Context, track: TrackEntity) {
        val trackId = track.id
        thumbCache.remove(trackId)
        fullCache.remove(trackId)
        noArtSet.remove(trackId)
        PaletteCache.remove(context, trackId)
        
        val fingerprint = getTrackFingerprint(track)
        java.io.File(context.cacheDir, "thumb_${fingerprint}.jpg").delete()
        java.io.File(context.cacheDir, "full_${fingerprint}.jpg").delete()
    }

    private suspend fun extractRawBitmap(context: Context, track: TrackEntity): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                if (!track.customCoverPath.isNullOrEmpty()) {
                    try {
                        if (track.customCoverPath.startsWith("http://") || track.customCoverPath.startsWith("https://")) {
                            val url = java.net.URL(track.customCoverPath)
                            val connection = url.openConnection() as java.net.HttpURLConnection
                            connection.requestMethod = "GET"
                            connection.connect()
                            if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                                return@withContext connection.inputStream.readBytes()
                            }
                        } else if (track.customCoverPath.startsWith("embedded://")) {
                            val realPath = track.customCoverPath.removePrefix("embedded://")
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(realPath)
                            val art = retriever.embeddedPicture
                            retriever.release()
                            return@withContext art
                        } else {
                            val customFile = java.io.File(track.customCoverPath)
                            if (customFile.exists()) {
                                return@withContext customFile.readBytes()
                            } else {
                                val uri = android.net.Uri.parse(track.customCoverPath)
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    return@withContext stream.readBytes()
                                }
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(track.dataPath)
                val art = retriever.embeddedPicture
                retriever.release()
                art
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun loadThumbnail(context: Context, track: TrackEntity): ImageBitmap? = withContext(Dispatchers.IO) {
        if (noArtSet.contains(track.id)) return@withContext null
        thumbCache.get(track.id)?.let { return@withContext it }

        val fingerprint = getTrackFingerprint(track)
        val thumbFile = java.io.File(context.cacheDir, "thumb_${fingerprint}.jpg")
        
        if (thumbFile.exists()) {
            if (thumbFile.length() == 0L) {
                thumbFile.delete()
            } else {
                try {
                    val bitmap = BitmapFactory.decodeFile(thumbFile.absolutePath)
                    if (bitmap != null) {
                        val imageBitmap = bitmap.asImageBitmap()
                        thumbCache.put(track.id, imageBitmap)
                        return@withContext imageBitmap
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        try {
            while (isPriorityLoading) {
                kotlinx.coroutines.delay(50)
            }
            
            thumbCache.get(track.id)?.let { return@withContext it }

            var bitmap: Bitmap? = null
            var triedFastPath = false
            
            // Fast path for Android 10+ using ContentResolver
            if (track.customCoverPath.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                triedFastPath = true
                try {
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.id)
                    bitmap = context.contentResolver.loadThumbnail(uri, Size(120, 120), null)
                } catch (e: Exception) {
                    // Si falla aquí en API 29+, es porque NO hay carátula.
                    // No hacemos fallback a MediaMetadataRetriever porque es súper lento.
                }
            }

            // Fallback for custom covers or old Android versions
            if (bitmap == null && !triedFastPath) {
                val art = extractRawBitmap(context, track)
                if (art != null) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(art, 0, art.size, options)
                    options.inSampleSize = calculateInSampleSize(options, 120, 120)
                    options.inJustDecodeBounds = false
                    val decoded = BitmapFactory.decodeByteArray(art, 0, art.size, options)
                    if (decoded != null) {
                        bitmap = Bitmap.createScaledBitmap(decoded, 120, 120, true)
                    }
                }
            }

            if (bitmap != null) {
                try {
                    val out = java.io.FileOutputStream(thumbFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    out.close()
                } catch (e: Exception) { e.printStackTrace() }

                val imageBitmap = bitmap.asImageBitmap()
                thumbCache.put(track.id, imageBitmap)
                return@withContext imageBitmap
            } else {
                thumbFile.createNewFile()
                noArtSet.add(track.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    suspend fun loadFullArt(context: Context, track: TrackEntity): ImageBitmap? = withContext(Dispatchers.IO) {
        if (noArtSet.contains(track.id)) return@withContext null
        fullCache.get(track.id)?.let { return@withContext it }

        val fingerprint = getTrackFingerprint(track)
        val fullFile = java.io.File(context.cacheDir, "full_${fingerprint}.jpg")
        
        if (fullFile.exists()) {
            if (fullFile.length() == 0L) {
                fullFile.delete()
            } else {
                try {
                    val bitmap = BitmapFactory.decodeFile(fullFile.absolutePath)
                    if (bitmap != null) {
                        val imageBitmap = bitmap.asImageBitmap()
                        fullCache.put(track.id, imageBitmap)
                        return@withContext imageBitmap
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        try {
            isPriorityLoading = true
            fullCache.get(track.id)?.let { return@withContext it }
            
            var bitmap: Bitmap? = null
            var triedFastPath = false
            
            // Fast path for Android 10+ using ContentResolver
            if (track.customCoverPath.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                triedFastPath = true
                try {
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.id)
                    bitmap = context.contentResolver.loadThumbnail(uri, Size(600, 600), null)
                } catch (e: Exception) {
                    // Si falla en API 29+, no hay carátula. Evitar fallback lento.
                }
            }

            // Fallback for custom covers or old Android versions
            if (bitmap == null && !triedFastPath) {
                val art = extractRawBitmap(context, track)
                if (art != null) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(art, 0, art.size, options)
                    options.inSampleSize = calculateInSampleSize(options, 600, 600)
                    options.inJustDecodeBounds = false
                    val decoded = BitmapFactory.decodeByteArray(art, 0, art.size, options)
                    val maxDimension = 600
                    bitmap = if (decoded != null && (decoded.width > maxDimension || decoded.height > maxDimension)) {
                        val ratio = Math.min(maxDimension.toFloat() / decoded.width, maxDimension.toFloat() / decoded.height)
                        Bitmap.createScaledBitmap(decoded, (decoded.width * ratio).toInt(), (decoded.height * ratio).toInt(), true)
                    } else decoded
                }
            }

            if (bitmap != null) {
                try {
                    val out = java.io.FileOutputStream(fullFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    out.close()
                } catch (e: Exception) { e.printStackTrace() }

                val imageBitmap = bitmap.asImageBitmap()
                fullCache.put(track.id, imageBitmap)
                return@withContext imageBitmap
            } else {
                fullFile.createNewFile()
                noArtSet.add(track.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isPriorityLoading = false
        }
        null
    }
}

// Caché persistente para paletas de colores por track
object PaletteCache {
    private val memCache = AndroidLruCache<Long, PaletteColors>(200)
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("PaletteCache", Context.MODE_PRIVATE)
    }

    fun get(context: Context, trackId: Long): PaletteColors? {
        memCache.get(trackId)?.let { return it }
        
        // Cargar desde disco si existe
        val prefs = getPrefs(context)
        val saved = prefs.getString(trackId.toString(), null)
        if (saved != null) {
            try {
                val parts = saved.split(",")
                if (parts.size == 6) {
                    val colors = PaletteColors(
                        dominant = Color(parts[0].toInt()),
                        vibrant = Color(parts[1].toInt()),
                        muted = Color(parts[2].toInt()),
                        darkVibrant = Color(parts[3].toInt()),
                        lightVibrant = Color(parts[4].toInt()),
                        darkMuted = Color(parts[5].toInt())
                    )
                    memCache.put(trackId, colors)
                    return colors
                }
            } catch (e: Exception) {}
        }
        return null
    }
    
    fun put(context: Context, trackId: Long, colors: PaletteColors) { 
        memCache.put(trackId, colors) 
        
        // Guardar en disco permanentemente
        val colorString = "${colors.dominant.toArgb()},${colors.vibrant.toArgb()},${colors.muted.toArgb()}," +
                "${colors.darkVibrant.toArgb()},${colors.lightVibrant.toArgb()},${colors.darkMuted.toArgb()}"
        getPrefs(context).edit().putString(trackId.toString(), colorString).apply()
    }
    
    fun remove(context: Context, trackId: Long) { 
        memCache.remove(trackId) 
        getPrefs(context).edit().remove(trackId.toString()).apply()
    }
}

@Composable
fun rememberAlbumArt(track: TrackEntity): ImageBitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val initialBitmap = ThumbnailCache.thumbCache.get(track.id)
    var bitmap by remember(track) { mutableStateOf<ImageBitmap?>(initialBitmap) }
    val hasNoArt = ThumbnailCache.noArtSet.contains(track.id)

    if (initialBitmap == null && !hasNoArt) {
        LaunchedEffect(track) {
            bitmap = ThumbnailCache.loadThumbnail(context, track)
        }
    }

    return bitmap
}

// Imagen completa (hasta 600px, calidad 95%) — para el reproductor
@Composable
fun rememberFullAlbumArt(track: TrackEntity): ImageBitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val initialBitmap = ThumbnailCache.fullCache.get(track.id)
    var bitmap by remember(track) { mutableStateOf<ImageBitmap?>(initialBitmap) }
    val hasNoArt = ThumbnailCache.noArtSet.contains(track.id)

    if (initialBitmap == null && !hasNoArt) {
        LaunchedEffect(track) {
            bitmap = ThumbnailCache.loadFullArt(context, track)
        }
    }
    return bitmap
}

// Paleta de colores de cada track individualmente — con caché persistente
@Composable
fun rememberTrackPalette(track: TrackEntity): PaletteColors {
    val context = androidx.compose.ui.platform.LocalContext.current
    var colors by remember(track) {
        mutableStateOf(PaletteCache.get(context, track.id) ?: PaletteColors())
    }

    LaunchedEffect(track) {
        if (PaletteCache.get(context, track.id) == null) {
            if (ThumbnailCache.noArtSet.contains(track.id)) {
                PaletteCache.put(context, track.id, PaletteColors())
            } else {
                val imageBitmap = ThumbnailCache.loadThumbnail(context, track)
                if (imageBitmap != null) {
                    withContext(Dispatchers.Default) {
                        try {
                            val bitmap = imageBitmap.asAndroidBitmap()
                            // Usamos un bitmap pequeñito (64x64) para que Palette.generate sea instantáneo < 1ms
                            val smallBitmap = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
                            val palette = Palette.from(smallBitmap).generate()
                            val extracted = PaletteColors(
                                dominant = Color(palette.getDominantColor(android.graphics.Color.DKGRAY)),
                                vibrant = Color(palette.getVibrantColor(android.graphics.Color.DKGRAY)),
                                muted = Color(palette.getMutedColor(android.graphics.Color.DKGRAY)),
                                darkVibrant = Color(palette.getDarkVibrantColor(android.graphics.Color.DKGRAY)),
                                lightVibrant = Color(palette.getLightVibrantColor(android.graphics.Color.DKGRAY)),
                                darkMuted = Color(palette.getDarkMutedColor(android.graphics.Color.DKGRAY))
                            )
                            PaletteCache.put(context, track.id, extracted)
                            colors = extracted
                        } catch (_: Exception) {}
                    }
                } else {
                    PaletteCache.put(context, track.id, PaletteColors())
                }
            }
        }
    }

    return colors
}

// Helper to convert Compose Color to ARGB int format for SharedPreferences
fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

@Composable
fun rememberStreamAvatar(uri: String?): ImageBitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                if (!uri.isNullOrEmpty()) {
                    val parsedUri = android.net.Uri.parse(uri)
                    context.contentResolver.openInputStream(parsedUri)?.use { stream ->
                        val decoded = BitmapFactory.decodeStream(stream)
                        if (decoded != null) {
                            bitmap = decoded.asImageBitmap()
                        }
                    }
                }
                
                if (bitmap == null) {
                    val defaultLogo = BitmapFactory.decodeResource(context.resources, com.example.beatpulse.R.mipmap.ic_launcher)
                    if (defaultLogo != null) {
                        bitmap = defaultLogo.asImageBitmap()
                    }
                }
            } catch (e: Exception) {
                val defaultLogo = BitmapFactory.decodeResource(context.resources, com.example.beatpulse.R.mipmap.ic_launcher)
                if (defaultLogo != null) {
                    bitmap = defaultLogo.asImageBitmap()
                }
            }
        }
    }
    
    return bitmap
}
