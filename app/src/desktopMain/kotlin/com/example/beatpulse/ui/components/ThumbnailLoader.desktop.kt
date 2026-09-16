package com.example.beatpulse.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.theme.PaletteColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.File
import java.net.URL
import org.jaudiotagger.audio.AudioFileIO

private val thumbnailCache = mutableMapOf<String, ImageBitmap>()
private val fullArtCache = mutableMapOf<String, ImageBitmap>()
private val paletteCache = mutableMapOf<String, PaletteColors>()
private val noArtSet = mutableSetOf<String>()

private fun getTrackFingerprint(track: TrackEntity): String {
    return Math.abs((track.title + track.artist + track.album + track.duration + (track.customCoverPath ?: "")).hashCode()).toString()
}

@Composable
actual fun rememberAlbumArt(track: TrackEntity): ImageBitmap? {
    val fingerprint = remember(track) { getTrackFingerprint(track) }
    var bitmap by remember(fingerprint) { mutableStateOf(thumbnailCache[fingerprint]) }
    
    if (bitmap == null && !noArtSet.contains(fingerprint)) {
        LaunchedEffect(fingerprint) {
            val loaded = loadDesktopThumbnail(track)
            if (loaded != null) {
                bitmap = loaded
            }
        }
    }
    
    return bitmap
}

@Composable
actual fun rememberFullAlbumArt(track: TrackEntity): ImageBitmap? {
    val fingerprint = remember(track) { getTrackFingerprint(track) }
    var bitmap by remember(fingerprint) { mutableStateOf(fullArtCache[fingerprint]) }
    
    if (bitmap == null && !noArtSet.contains(fingerprint)) {
        LaunchedEffect(fingerprint) {
            val loaded = loadDesktopThumbnail(track) // We can use the same for now, or load a bigger one
            if (loaded != null) {
                bitmap = loaded
            }
        }
    }
    
    return bitmap
}

@Composable
actual fun rememberTrackPalette(track: TrackEntity): PaletteColors {
    // For desktop, just return default palette or extract basic colors if we add a library
    return PaletteColors()
}

@Composable
actual fun rememberStreamAvatar(uri: String?): ImageBitmap? {
    if (uri.isNullOrEmpty()) return null
    var bitmap by remember(uri) { mutableStateOf(thumbnailCache[uri]) }
    
    if (bitmap == null && !noArtSet.contains(uri)) {
        LaunchedEffect(uri) {
            try {
                withContext(Dispatchers.IO) {
                    val bytes = if (uri.startsWith("http") || uri.startsWith("file://")) {
                        URL(uri).readBytes()
                    } else {
                        File(uri).readBytes()
                    }
                    val imageBitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
                    thumbnailCache[uri] = imageBitmap
                    bitmap = imageBitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
                noArtSet.add(uri)
            }
        }
    }
    return bitmap
}

internal suspend fun loadDesktopThumbnail(track: TrackEntity): ImageBitmap? = withContext(Dispatchers.IO) {
    val fingerprint = getTrackFingerprint(track)
    if (noArtSet.contains(fingerprint)) return@withContext null
    thumbnailCache[fingerprint]?.let { return@withContext it }
    
    try {
        val coverPath = track.customCoverPath
        if (coverPath != null && coverPath.isNotEmpty()) {
            if (coverPath.startsWith("http://") || coverPath.startsWith("https://")) {
                try {
                    val connection = URL(coverPath).openConnection() as java.net.HttpURLConnection
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    val bytes = connection.inputStream.use { it.readBytes() }
                    val imageBitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
                    thumbnailCache[fingerprint] = imageBitmap
                    fullArtCache[fingerprint] = imageBitmap
                    return@withContext imageBitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                val file = File(coverPath)
                if (file.exists()) {
                    val bytes = file.readBytes()
                    val imageBitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
                    thumbnailCache[fingerprint] = imageBitmap
                    fullArtCache[fingerprint] = imageBitmap
                    return@withContext imageBitmap
                }
            }
        }
        
        // If it's a YouTube track, the folderPath usually contains the thumbnail URL in the new implementation if we want to save it, but let's check if there is a cover file cached.
        val coversDir = File(System.getProperty("user.home"), ".beatpulse/covers")
        val fallbackFile = File(coversDir, "${track.id}.jpg")
        if (fallbackFile.exists()) {
            val bytes = fallbackFile.readBytes()
            val imageBitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
            thumbnailCache[fingerprint] = imageBitmap
            fullArtCache[fingerprint] = imageBitmap
            return@withContext imageBitmap
        }

        // Try extracting from MP3 directly
        if (track.dataPath.startsWith("/")) {
            val file = File(track.dataPath)
            if (file.exists()) {
                val audioFile = AudioFileIO.read(file)
                val tag = audioFile.tag
                if (tag != null) {
                    val artwork = tag.firstArtwork
                    if (artwork != null) {
                        val bytes = artwork.binaryData
                        val imageBitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
                        thumbnailCache[fingerprint] = imageBitmap
                        fullArtCache[fingerprint] = imageBitmap
                        return@withContext imageBitmap
                    }
                }
            }
        }
        
        // No cover found
        noArtSet.add(fingerprint)
    } catch (e: Exception) {
        e.printStackTrace()
        noArtSet.add(fingerprint)
    }
    return@withContext null
}
