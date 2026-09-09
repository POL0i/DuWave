package com.example.beatpulse.data

import android.content.Context
import android.provider.MediaStore
import java.io.File
import android.content.ContentUris
import android.os.Build

class AndroidLibraryScanner(private val context: Context, private val dao: TrackDao) : ILibraryScanner {
    
    override suspend fun scanMusic(folderPath: String?): List<TrackEntity> {
        val appContext = context.applicationContext
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val cursor = appContext.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        val scannedTracks = mutableListOf<TrackEntity>()
        val filterWhatsApp = PreferencesManager.getInstance(appContext).filterWhatsAppShorts
        
        // Android scanner fetches all tracks every time for now (the repo handles diffing)
        // Alternatively, the scanner just returns what it sees, and the repo processes it.
        // Wait, the repository needs the full list of what is currently on the file system to diff.
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (it.moveToNext()) {
                val duration = it.getLong(durationColumn)
                val dataPath = it.getString(dataColumn) ?: ""
                
                if (filterWhatsApp && duration < 60000 && dataPath.contains("WhatsApp", ignoreCase = true)) {
                    continue
                }

                // Trust MediaStore on Android instead of File.exists() due to Scoped Storage limitations

                val id = it.getLong(idColumn)
                var title = it.getString(titleColumn) ?: "Unknown Title"
                var artist = it.getString(artistColumn) ?: "Unknown Artist"
                var album = it.getString(albumColumn) ?: "Unknown Album"
                val dateAdded = it.getLong(dateAddedColumn) * 1000L
                val extractedFolderPath = File(dataPath).parent ?: ""

                if ((artist == "<unknown>" || artist == "Unknown Artist") && dataPath.contains("DuWave")) {
                    val fileName = File(dataPath).name
                    val parts = fileName.removeSuffix(".m4a").removeSuffix(".mp3").split("_-_")
                    if (parts.size >= 2) {
                        artist = parts[0].replace("_", " ")
                        title = parts[1].replace("_", " ")
                        album = "DuWave Downloads"
                    }
                }

                scannedTracks.add(
                    TrackEntity(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        dataPath = dataPath,
                        folderPath = extractedFolderPath,
                        dateAdded = dateAdded
                    )
                )
            }
        }
        return scannedTracks
    }

    @android.annotation.SuppressLint("NewApi")
    override fun deleteTrackFile(trackId: Long, dataPath: String): Any? {
        val appContext = context.applicationContext
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            trackId
        )
        try {
            val deleted = appContext.contentResolver.delete(contentUri, null, null)
            if (deleted <= 0) {
                // Not deleted by MediaStore, maybe just a file
                val file = File(dataPath)
                if (file.exists()) file.delete()
            }
            return null
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return MediaStore.createDeleteRequest(appContext.contentResolver, listOf(contentUri)).intentSender
            } else {
                val recoverableException = e as? android.app.RecoverableSecurityException
                return recoverableException?.userAction?.actionIntent?.intentSender
            }
        } catch (e: Exception) {
            return null
        }
    }
}
