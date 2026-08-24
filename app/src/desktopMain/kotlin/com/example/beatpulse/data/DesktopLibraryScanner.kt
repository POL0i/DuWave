package com.example.beatpulse.data

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.nio.file.Files

class DesktopLibraryScanner(private val dao: TrackDao) : ILibraryScanner {

    override suspend fun scanMusic(folderPath: String?): List<TrackEntity> {
        val scannedTracks = mutableListOf<TrackEntity>()
        if (folderPath == null) return scannedTracks

        val root = File(folderPath)
        if (!root.exists() || !root.isDirectory) return scannedTracks

        val supportedExtensions = setOf("mp3", "flac", "wav", "m4a", "ogg")

        root.walkTopDown().forEach { file ->
            if (file.isFile && supportedExtensions.contains(file.extension.lowercase())) {
                try {
                    val audioFile = AudioFileIO.read(file)
                    val header = audioFile.audioHeader
                    val tag = audioFile.tag

                    val title = tag?.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
                    val artist = tag?.getFirst(FieldKey.ARTIST)?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
                    val album = tag?.getFirst(FieldKey.ALBUM)?.takeIf { it.isNotBlank() } ?: "Unknown Album"
                    
                    val durationMs = header?.trackLength?.toLong()?.times(1000L) ?: 0L
                    
                    // Handle artwork
                    var coverPath: String? = null
                    val artwork = tag?.firstArtwork
                    if (artwork != null) {
                        val coversDir = File(System.getProperty("user.home"), ".beatpulse/covers")
                        coversDir.mkdirs()
                        // Use a hash of the album+artist to avoid duplicating cover files for the same album
                        val coverName = "${album}_${artist}".hashCode().toString() + ".jpg"
                        val destFile = File(coversDir, coverName)
                        if (!destFile.exists()) {
                            destFile.writeBytes(artwork.binaryData)
                        }
                        coverPath = destFile.absolutePath
                    }

                    scannedTracks.add(
                        TrackEntity(
                            id = file.absolutePath.hashCode().toLong(), // Generate a consistent ID based on path
                            title = title,
                            artist = artist,
                            album = album,
                            duration = durationMs,
                            dataPath = file.absolutePath,
                            folderPath = file.parent ?: "",
                            customCoverPath = coverPath,
                            dateAdded = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return scannedTracks
    }

    override fun deleteTrackFile(trackId: Long, dataPath: String): Any? {
        val file = File(dataPath)
        if (file.exists()) {
            file.delete()
        }
        return null
    }
}
