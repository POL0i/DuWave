package com.example.beatpulse.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object ThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 6

    private val ioSemaphore = Semaphore(2)

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

    fun invalidateTrack(context: android.content.Context, track: TrackEntity) {
        val trackId = track.id
        thumbCache.remove(trackId)
        fullCache.remove(trackId)
        noArtSet.remove(trackId)
        PaletteCache.remove(trackId)
        
        val fingerprint = getTrackFingerprint(track)
        java.io.File(context.cacheDir, "thumb_${fingerprint}.jpg").delete()
        java.io.File(context.cacheDir, "full_${fingerprint}.jpg").delete()
    }

    private suspend fun extractRawBitmap(context: android.content.Context, track: TrackEntity): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                if (!track.customCoverPath.isNullOrEmpty()) {
                    try {
                        val customFile = java.io.File(track.customCoverPath)
                        if (customFile.exists()) {
                            return@withContext customFile.readBytes()
                        } else {
                            val uri = android.net.Uri.parse(track.customCoverPath)
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                return@withContext stream.readBytes()
                            }
                        }
                    } catch (e: Exception) {
                        // Fallback to default loading if custom cover fails
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

    suspend fun loadThumbnail(context: android.content.Context, track: TrackEntity): ImageBitmap? = withContext(Dispatchers.IO) {
        if (noArtSet.contains(track.id)) return@withContext null
        thumbCache.get(track.id)?.let { return@withContext it }

        val fingerprint = getTrackFingerprint(track)
        val thumbFile = java.io.File(context.cacheDir, "thumb_${fingerprint}.jpg")
        
        if (thumbFile.exists()) {
            if (thumbFile.length() == 0L) {
                noArtSet.add(track.id)
                return@withContext null
            }
            try {
                val bitmap = BitmapFactory.decodeFile(thumbFile.absolutePath)
                if (bitmap != null) {
                    val imageBitmap = bitmap.asImageBitmap()
                    thumbCache.put(track.id, imageBitmap)
                    return@withContext imageBitmap
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        try {
            while (isPriorityLoading) {
                kotlinx.coroutines.delay(50)
            }
            ioSemaphore.withPermit {
                thumbCache.get(track.id)?.let { return@withContext it }

                val art = extractRawBitmap(context, track)
                if (art != null) {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(art, 0, art.size, options)
                    
                    options.inSampleSize = calculateInSampleSize(options, 120, 120)
                    options.inJustDecodeBounds = false
                    
                    val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size, options)
                    val scaled = if (bitmap != null) Bitmap.createScaledBitmap(bitmap, 120, 120, true) else null

                    if (scaled != null) {
                        try {
                            val out = java.io.FileOutputStream(thumbFile)
                            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
                            out.close()
                        } catch (e: Exception) { e.printStackTrace() }

                        val imageBitmap = scaled.asImageBitmap()
                        thumbCache.put(track.id, imageBitmap)
                        return@withContext imageBitmap
                    }
                }
                
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

    suspend fun loadFullArt(context: android.content.Context, track: TrackEntity): ImageBitmap? = withContext(Dispatchers.IO) {
        if (noArtSet.contains(track.id)) return@withContext null
        fullCache.get(track.id)?.let { return@withContext it }

        val fingerprint = getTrackFingerprint(track)
        val fullFile = java.io.File(context.cacheDir, "full_${fingerprint}.jpg")
        
        if (fullFile.exists()) {
            if (fullFile.length() == 0L) {
                noArtSet.add(track.id)
                return@withContext null
            }
            try {
                val bitmap = BitmapFactory.decodeFile(fullFile.absolutePath)
                if (bitmap != null) {
                    val imageBitmap = bitmap.asImageBitmap()
                    fullCache.put(track.id, imageBitmap)
                    return@withContext imageBitmap
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        try {
            isPriorityLoading = true
            fullCache.get(track.id)?.let { return@withContext it }

            val art = extractRawBitmap(context, track)
            if (art != null) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(art, 0, art.size, options)
                
                options.inSampleSize = calculateInSampleSize(options, 600, 600)
                options.inJustDecodeBounds = false
                
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size, options)
                
                // Solo escalar exactamente si es necesario después de inSampleSize
                val maxDimension = 600
                val finalBitmap = if (bitmap != null && (bitmap.width > maxDimension || bitmap.height > maxDimension)) {
                    val ratio = Math.min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                } else bitmap

                if (finalBitmap != null) {
                    try {
                        val out = java.io.FileOutputStream(fullFile)
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        out.close()
                    } catch (e: Exception) { e.printStackTrace() }

                    val imageBitmap = finalBitmap.asImageBitmap()
                    fullCache.put(track.id, imageBitmap)
                    return@withContext imageBitmap
                }
            } 
            
            fullFile.createNewFile()
            noArtSet.add(track.id)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isPriorityLoading = false
        }
        null
    }
}

// Caché en memoria para paletas de colores por track
object PaletteCache {
    private val cache = AndroidLruCache<Long, PaletteColors>(200)

    fun get(trackId: Long): PaletteColors? = cache.get(trackId)
    fun put(trackId: Long, colors: PaletteColors) { cache.put(trackId, colors) }
    fun remove(trackId: Long) { cache.remove(trackId) }
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

// Paleta de colores de cada track individualmente — con caché en memoria
@Composable
fun rememberTrackPalette(track: TrackEntity): PaletteColors {
    val context = androidx.compose.ui.platform.LocalContext.current
    var colors by remember(track.id) {
        mutableStateOf(PaletteCache.get(track.id) ?: PaletteColors())
    }

    LaunchedEffect(track.id) {
        if (PaletteCache.get(track.id) == null) {
            if (ThumbnailCache.noArtSet.contains(track.id)) {
                PaletteCache.put(track.id, PaletteColors())
            } else {
                val imageBitmap = ThumbnailCache.loadThumbnail(context, track)
                if (imageBitmap != null) {
                    withContext(Dispatchers.Default) {
                        try {
                            val bitmap = imageBitmap.asAndroidBitmap()
                            val palette = Palette.from(bitmap).generate()
                            val extracted = PaletteColors(
                                dominant = Color(palette.getDominantColor(android.graphics.Color.DKGRAY)),
                                vibrant = Color(palette.getVibrantColor(android.graphics.Color.DKGRAY)),
                                muted = Color(palette.getMutedColor(android.graphics.Color.DKGRAY)),
                                darkVibrant = Color(palette.getDarkVibrantColor(android.graphics.Color.DKGRAY)),
                                lightVibrant = Color(palette.getLightVibrantColor(android.graphics.Color.DKGRAY)),
                                darkMuted = Color(palette.getDarkMutedColor(android.graphics.Color.DKGRAY))
                            )
                            PaletteCache.put(track.id, extracted)
                            colors = extracted
                        } catch (_: Exception) {}
                    }
                } else {
                    PaletteCache.put(track.id, PaletteColors())
                }
            }
        }
    }

    return colors
}
