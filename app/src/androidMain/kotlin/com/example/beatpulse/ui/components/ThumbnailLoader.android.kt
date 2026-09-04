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
import com.kmpalette.palette.graphics.Palette
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
    val thumbCache = object : AndroidLruCache<String, ImageBitmap>(cacheSize / 2) {
        override fun sizeOf(key: String, bitmap: ImageBitmap): Int {
            return (bitmap.width * bitmap.height * 4) / 1024
        }
    }

    // Caché de imágenes completas (hasta 600x600) para el reproductor
    val fullCache = object : AndroidLruCache<String, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: ImageBitmap): Int {
            return (bitmap.width * bitmap.height * 4) / 1024
        }
    }

    val noArtSet = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

    fun getTrackFingerprint(track: TrackEntity): String {
        return Math.abs((track.title + track.artist + track.album + track.duration + (track.customCoverPath ?: "")).hashCode()).toString()
    }

    fun invalidateTrack(context: android.content.Context, track: TrackEntity) {
        val fingerprint = getTrackFingerprint(track)
        thumbCache.remove(fingerprint)
        fullCache.remove(fingerprint)
        noArtSet.remove(fingerprint)
        PaletteCache.remove(fingerprint)
        
        java.io.File(context.cacheDir, "thumb_${fingerprint}.jpg").delete()
        java.io.File(context.cacheDir, "full_${fingerprint}.jpg").delete()
    }

    private suspend fun extractRawBitmap(context: android.content.Context, track: TrackEntity): ByteArray? {
        return withContext(Dispatchers.IO) {
            val coverPath = track.customCoverPath
            if (!coverPath.isNullOrEmpty()) {
                if (coverPath.startsWith("http://") || coverPath.startsWith("https://")) {
                    try {
                        val client = okhttp3.OkHttpClient.Builder()
                            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
                        val request = okhttp3.Request.Builder().url(coverPath).build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            return@withContext response.body?.bytes()
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                } else if (coverPath.startsWith("content://")) {
                    try {
                        val stream = context.contentResolver.openInputStream(android.net.Uri.parse(coverPath))
                        val bytes = stream?.readBytes()
                        stream?.close()
                        if (bytes != null) return@withContext bytes
                    } catch (e: Exception) { e.printStackTrace() }
                } else {
                    val file = java.io.File(coverPath)
                    if (file.exists()) {
                        return@withContext file.readBytes()
                    }
                }
            }

            try {
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
        val fingerprint = getTrackFingerprint(track)
        if (noArtSet.contains(fingerprint)) return@withContext null
        thumbCache.get(fingerprint)?.let { return@withContext it }

        val thumbFile = java.io.File(context.cacheDir, "thumb_${fingerprint}.jpg")
        
        if (thumbFile.exists()) {
            if (thumbFile.length() == 0L) {
                noArtSet.add(fingerprint)
                return@withContext null
            }
            try {
                val bitmap = BitmapFactory.decodeFile(thumbFile.absolutePath)
                if (bitmap != null) {
                    val imageBitmap = bitmap.asImageBitmap()
                    thumbCache.put(fingerprint, imageBitmap)
                    return@withContext imageBitmap
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        try {
            ioSemaphore.withPermit {
                thumbCache.get(fingerprint)?.let { return@withContext it }

                val art = extractRawBitmap(context, track)
                if (art != null) {
                    val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                    val scaled = Bitmap.createScaledBitmap(bitmap, 120, 120, true)

                    try {
                        val out = java.io.FileOutputStream(thumbFile)
                        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        out.close()
                    } catch (e: Exception) { e.printStackTrace() }

                    val imageBitmap = scaled.asImageBitmap()
                    thumbCache.put(fingerprint, imageBitmap)
                    return@withContext imageBitmap
                } else {
                    thumbFile.createNewFile()
                    noArtSet.add(fingerprint)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun loadFullArt(context: android.content.Context, track: TrackEntity): ImageBitmap? = withContext(Dispatchers.IO) {
        val fingerprint = getTrackFingerprint(track)
        if (noArtSet.contains(fingerprint)) return@withContext null
        fullCache.get(fingerprint)?.let { return@withContext it }

        val fullFile = java.io.File(context.cacheDir, "full_${fingerprint}.jpg")
        
        if (fullFile.exists()) {
            if (fullFile.length() == 0L) {
                noArtSet.add(fingerprint)
                return@withContext null
            }
            try {
                val bitmap = BitmapFactory.decodeFile(fullFile.absolutePath)
                if (bitmap != null) {
                    val imageBitmap = bitmap.asImageBitmap()
                    fullCache.put(fingerprint, imageBitmap)
                    return@withContext imageBitmap
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        try {
            ioSemaphore.withPermit {
                fullCache.get(fingerprint)?.let { return@withContext it }

                val art = extractRawBitmap(context, track)
                if (art != null) {
                    val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                    
                    val maxDimension = 600
                    val finalBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                        val ratio = Math.min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
                        Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                    } else bitmap

                    try {
                        val out = java.io.FileOutputStream(fullFile)
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        out.close()
                    } catch (e: Exception) { e.printStackTrace() }

                    val imageBitmap = finalBitmap.asImageBitmap()
                    fullCache.put(fingerprint, imageBitmap)
                    return@withContext imageBitmap
                } else {
                    fullFile.createNewFile()
                    noArtSet.add(fingerprint)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}

// Caché en memoria para paletas de colores por track
object PaletteCache {
    private val lruCache = AndroidLruCache<String, PaletteColors>(200)

    fun get(fingerprint: String): PaletteColors? = lruCache.get(fingerprint)
    fun put(fingerprint: String, colors: PaletteColors) { lruCache.put(fingerprint, colors) }
    fun remove(fingerprint: String) { lruCache.remove(fingerprint) }
}

@Composable
actual fun rememberAlbumArt(track: TrackEntity): ImageBitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fingerprint = ThumbnailCache.getTrackFingerprint(track)
    val initialBitmap = ThumbnailCache.thumbCache.get(fingerprint)
    var bitmap by remember(track) { mutableStateOf<ImageBitmap?>(initialBitmap) }
    val hasNoArt = ThumbnailCache.noArtSet.contains(fingerprint)

    if (initialBitmap == null && !hasNoArt) {
        LaunchedEffect(track) {
            bitmap = ThumbnailCache.loadThumbnail(context, track)
        }
    }

    return bitmap
}

// Imagen completa (hasta 600px, calidad 95%) — para el reproductor
@Composable
actual fun rememberFullAlbumArt(track: TrackEntity): ImageBitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fingerprint = ThumbnailCache.getTrackFingerprint(track)
    val initialBitmap = ThumbnailCache.fullCache.get(fingerprint)
    var bitmap by remember(track) { mutableStateOf<ImageBitmap?>(initialBitmap) }
    val hasNoArt = ThumbnailCache.noArtSet.contains(fingerprint)

    if (initialBitmap == null && !hasNoArt) {
        LaunchedEffect(track) {
            bitmap = ThumbnailCache.loadFullArt(context, track)
        }
    }
    return bitmap
}

// Paleta de colores de cada track individualmente — con caché en memoria
@Composable
actual fun rememberTrackPalette(track: TrackEntity): PaletteColors {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fingerprint = ThumbnailCache.getTrackFingerprint(track)
    var colors by remember(track) {
        mutableStateOf(PaletteCache.get(fingerprint) ?: PaletteColors())
    }

    LaunchedEffect(track) {
        if (PaletteCache.get(fingerprint) == null) {
            if (ThumbnailCache.noArtSet.contains(fingerprint)) {
                PaletteCache.put(fingerprint, PaletteColors())
            } else {
                val imageBitmap = ThumbnailCache.loadThumbnail(context, track)
                if (imageBitmap != null) {
                    withContext(Dispatchers.Default) {
                        try {
                            val palette = Palette.from(imageBitmap).generate()
                            val extracted = PaletteColors(
                                dominant = Color(palette.getDominantColor(android.graphics.Color.DKGRAY)),
                                vibrant = Color(palette.getVibrantColor(android.graphics.Color.DKGRAY)),
                                muted = Color(palette.getMutedColor(android.graphics.Color.DKGRAY)),
                                darkVibrant = Color(palette.getDarkVibrantColor(android.graphics.Color.DKGRAY)),
                                lightVibrant = Color(palette.getLightVibrantColor(android.graphics.Color.DKGRAY)),
                                darkMuted = Color(palette.getDarkMutedColor(android.graphics.Color.DKGRAY))
                            )
                            PaletteCache.put(fingerprint, extracted)
                            colors = extracted
                        } catch (_: Exception) {}
                    }
                } else {
                    PaletteCache.put(fingerprint, PaletteColors())
                }
            }
        }
    }

    return colors
}

@Composable
actual fun rememberStreamAvatar(uri: String?): ImageBitmap? {
    if (uri.isNullOrEmpty()) return null
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                val fingerprint = Math.abs(uri.hashCode()).toString()
                val avatarFile = java.io.File(context.cacheDir, "avatar_${fingerprint}.jpg")
                if (avatarFile.exists() && avatarFile.length() > 0) {
                    val bm = BitmapFactory.decodeFile(avatarFile.absolutePath)
                    if (bm != null) {
                        bitmap = bm.asImageBitmap()
                        return@withContext
                    }
                }
                
                if (uri.startsWith("content://")) {
                    try {
                        val stream = context.contentResolver.openInputStream(android.net.Uri.parse(uri))
                        val bytes = stream?.readBytes()
                        stream?.close()
                        if (bytes != null) {
                            val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bm != null) {
                                java.io.FileOutputStream(avatarFile).use {
                                    bm.compress(Bitmap.CompressFormat.JPEG, 90, it)
                                }
                                bitmap = bm.asImageBitmap()
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                } else if (uri.startsWith("http://") || uri.startsWith("https://")) {
                    val client = okhttp3.OkHttpClient()
                    val request = okhttp3.Request.Builder().url(uri).build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null) {
                            val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bm != null) {
                                java.io.FileOutputStream(avatarFile).use {
                                    bm.compress(Bitmap.CompressFormat.JPEG, 90, it)
                                }
                                bitmap = bm.asImageBitmap()
                            }
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    return bitmap
}
