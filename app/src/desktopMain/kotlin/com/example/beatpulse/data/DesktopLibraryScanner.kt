package com.example.beatpulse.data

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.nio.file.Files

class DesktopLibraryScanner(private val dao: TrackDao) : ILibraryScanner {

    override suspend fun scanMusic(folderPath: String?): List<TrackEntity> {
        val scannedTracks = mutableListOf<TrackEntity>()
        val home = System.getProperty("user.home")
        var root = File(folderPath ?: (home + File.separator + "Music"))
        if (!root.exists() && folderPath == null) {
            val localizedMusic = File(home + File.separator + "Música")
            if (localizedMusic.exists()) root = localizedMusic
        }
        if (!root.exists() || !root.isDirectory) return scannedTracks

        val supportedExtensions = setOf("mp3", "flac", "wav", "m4a", "ogg")

        root.walkTopDown().forEach { file ->
            if (file.isFile && supportedExtensions.contains(file.extension.lowercase())) {
                    var title = file.nameWithoutExtension
                    var artist = "Artista Desconocido"
                    var album = "Álbum Desconocido"
                    var durationMs = 0L
                    var coverPath: String? = null

                    try {
                        val audioFile = AudioFileIO.read(file)
                        val header = audioFile.audioHeader
                        val tag = audioFile.tag

                        title = tag?.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
                        artist = tag?.getFirst(FieldKey.ARTIST)?.takeIf { it.isNotBlank() } ?: "Artista Desconocido"
                        album = tag?.getFirst(FieldKey.ALBUM)?.takeIf { it.isNotBlank() } ?: "Álbum Desconocido"
                        
                        durationMs = header?.trackLength?.toLong()?.times(1000L) ?: 0L
                        val artwork = tag?.firstArtwork
                        if (artwork != null) {
                            val coversDir = File(System.getProperty("user.home"), ".beatpulse/covers")
                            coversDir.mkdirs()
                            val coverName = "${album}_${artist}".hashCode().toString() + ".jpg"
                            val destFile = File(coversDir, coverName)
                            if (!destFile.exists()) {
                                destFile.writeBytes(artwork.binaryData)
                            }
                            coverPath = destFile.absolutePath
                        }
                    } catch (e: Exception) {
                        println("Warning: Could not read metadata for ${file.name} - ${e.message}")
                    }

                    if (durationMs == 0L) {
                        durationMs = com.example.beatpulse.utils.getAudioDuration(file.absolutePath)
                    }

                    scannedTracks.add(
                        TrackEntity(
                            id = file.absolutePath.hashCode().toLong(),
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
