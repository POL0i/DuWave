package com.example.beatpulse.data

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction

class MusicRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    private val dao = db.trackDao()

    val isScanning = kotlinx.coroutines.flow.MutableStateFlow(false)

    val allTracksFlow = dao.getAllTracks()
    val favoritesFlow = dao.getFavorites()
    val recentTracksFlow = dao.getRecentTracks()
    val topPlayedFlow = dao.getTopPlayedTracks()
    val recentlyAddedFlow = dao.getRecentlyAddedTracks()

    
    val playlistsFlow = dao.getAllPlaylists()

    suspend fun createPlaylist(name: String): Long {
        return withContext(Dispatchers.IO) {
            dao.insertPlaylist(PlaylistEntity(name = name))
        }
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        withContext(Dispatchers.IO) {
            val maxIndex = dao.getMaxOrderIndex(playlistId) ?: -1
            dao.insertPlaylistTrack(PlaylistTrackCrossRef(playlistId, trackId, maxIndex + 1))
        }
    }

    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) {
        withContext(Dispatchers.IO) {
            db.withTransaction {
                var currentIndex = (dao.getMaxOrderIndex(playlistId) ?: -1) + 1
                val crossRefs = trackIds.map { trackId ->
                    PlaylistTrackCrossRef(playlistId, trackId, currentIndex++)
                }
                crossRefs.forEach { dao.insertPlaylistTrack(it) }
            }
        }
    }

    suspend fun updatePlaylistOrder(playlistId: Long, updates: List<Pair<Long, Int>>) {
        withContext(Dispatchers.IO) {
            db.withTransaction {
                updates.forEach { (trackId, orderIndex) ->
                    dao.updatePlaylistTrackOrder(playlistId, trackId, orderIndex)
                }
            }
        }
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        withContext(Dispatchers.IO) {
            dao.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun getTracksForPlaylist(playlistId: Long) = dao.getTracksForPlaylist(playlistId)

    fun getPlaylistTrackCountFlow(playlistId: Long) = dao.getPlaylistTrackCountFlow(playlistId)

    suspend fun scanMediaStore() {
        isScanning.value = true
        try {
        withContext(Dispatchers.IO) {
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

            val existingTracks = dao.getAllTracks().first()
            val existingMap = existingTracks.associateBy { it.id }

            val foundIds = mutableSetOf<Long>()
            val currentChunk = mutableListOf<TrackEntity>()
            val prefs = PreferencesManager.getInstance(appContext)
            val filterWhatsApp = prefs.filterWhatsAppShorts

            db.withTransaction {
                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                    while (it.moveToNext()) {
                        kotlinx.coroutines.yield()
                        val duration = it.getLong(durationColumn)
                        val dataPath = it.getString(dataColumn) ?: ""
                        
                        if (filterWhatsApp && duration < 60000 && dataPath.contains("WhatsApp", ignoreCase = true)) {
                            continue
                        }

                        // Check if file physically exists (MediaStore might be out of sync)
                        if (dataPath.isNotEmpty() && !dataPath.startsWith("http") && !dataPath.startsWith("youtube://")) {
                            if (!File(dataPath).exists()) {
                                continue
                            }
                        }

                        val id = it.getLong(idColumn)
                        var title = it.getString(titleColumn) ?: "Unknown Title"
                        var artist = it.getString(artistColumn) ?: "Unknown Artist"
                        var album = it.getString(albumColumn) ?: "Unknown Album"
                        val dateAdded = it.getLong(dateAddedColumn) * 1000L // Convert seconds to ms
                        
                        val folderPath = File(dataPath).parent ?: ""

                        if ((artist == "<unknown>" || artist == "Unknown Artist") && dataPath.contains("DuWave")) {
                            val fileName = File(dataPath).name
                            val parts = fileName.removeSuffix(".m4a").removeSuffix(".mp3").split("_-_")
                            if (parts.size >= 2) {
                                artist = parts[0].replace("_", " ")
                                title = parts[1].replace("_", " ")
                                album = "DuWave Downloads"
                            }
                        }
                        
                        var existingTrack = existingMap[id]
                        if (existingTrack == null) {
                            // Check if this new local file matches an existing online track
                            val normalizedDataPath = dataPath.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                            val onlineTrack = existingMap.values.find {
                                val normalizedTitle = it.title.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                                it.dataPath.startsWith("youtube://") &&
                                (it.title.equals(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) ||
                                 normalizedDataPath.contains(normalizedTitle))
                            }
                            if (onlineTrack != null) {
                                existingTrack = onlineTrack
                                // Migrate playlist references to the new local ID
                                dao.updatePlaylistTrackId(oldId = onlineTrack.id, newId = id)
                            }
                        }

                        val resolvedTitle = existingTrack?.customTitle ?: (if (existingTrack?.dataPath?.startsWith("youtube://") == true) existingTrack.title else null) ?: title
                        val resolvedArtist = existingTrack?.customArtist ?: (if (existingTrack?.dataPath?.startsWith("youtube://") == true) existingTrack.artist else null) ?: artist
                        val resolvedAlbum = existingTrack?.customAlbum ?: album
                        
                        foundIds.add(id)

                        currentChunk.add(
                            TrackEntity(
                                id = id,
                                title = resolvedTitle,
                                artist = resolvedArtist,
                                album = resolvedAlbum,
                                duration = duration,
                                dataPath = dataPath,
                                folderPath = folderPath,
                                isFavorite = existingTrack?.isFavorite ?: false,
                                lastPlayedTime = existingTrack?.lastPlayedTime ?: 0,
                                playCount = existingTrack?.playCount ?: 0,
                                dateAdded = existingTrack?.dateAdded?.takeIf { d -> d > 0 } ?: dateAdded,
                                customTitle = existingTrack?.customTitle,
                                customArtist = existingTrack?.customArtist,
                                customAlbum = existingTrack?.customAlbum,
                                customCoverPath = existingTrack?.customCoverPath
                            )
                        )

                        if (currentChunk.size >= 500) {
                            dao.insertTracks(currentChunk)
                            currentChunk.clear()
                        }
                    }
                }

                if (currentChunk.isNotEmpty()) {
                    dao.insertTracks(currentChunk)
                }
                
                // Delete local tracks that no longer exist on the device
                val localExistingIds = existingMap.values.filter { !it.dataPath.startsWith("youtube://") && !it.dataPath.startsWith("http") }.map { it.id }.toSet()
                val missingIds = localExistingIds - foundIds
                if (missingIds.isNotEmpty()) {
                    missingIds.chunked(500).forEach { chunk ->
                        dao.deleteTracksById(chunk)
                    }
                }
            }
        }
        } finally {
            isScanning.value = false
        }
    }

    suspend fun toggleFavorite(trackId: Long, isFav: Boolean) {
        withContext(Dispatchers.IO) {
            dao.updateFavorite(trackId, isFav)
        }
    }

    suspend fun markAsPlayed(trackId: Long) {
        withContext(Dispatchers.IO) {
            dao.updateLastPlayed(trackId, System.currentTimeMillis())
        }
    }

    suspend fun updateLastPlayed(trackId: Long, time: Long) {
        withContext(Dispatchers.IO) {
            dao.updateLastPlayed(trackId, time)
        }
    }

    suspend fun insertTrack(track: TrackEntity) {
        withContext(Dispatchers.IO) {
            dao.insertTrack(track)
        }
    }

    suspend fun updateTrackMetadata(id: Long, title: String?, artist: String?, album: String?, coverPath: String?) {
        withContext(Dispatchers.IO) {
            var finalCoverPath = coverPath
            if (coverPath != null && coverPath.startsWith("content://")) {
                try {
                    val coversDir = File(appContext.filesDir, "covers")
                    if (!coversDir.exists()) coversDir.mkdirs()
                    val destFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                    appContext.contentResolver.openInputStream(android.net.Uri.parse(coverPath))?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    finalCoverPath = destFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (coverPath != null && coverPath.startsWith("file://")) {
                try {
                    val coversDir = File(appContext.filesDir, "covers")
                    if (!coversDir.exists()) coversDir.mkdirs()
                    val destFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                    File(java.net.URI(coverPath)).inputStream().use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    finalCoverPath = destFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val oldTrack = dao.getTrackById(id)
            if (oldTrack != null) {
                com.example.beatpulse.ui.components.ThumbnailCache.invalidateTrack(appContext, oldTrack)
            }
            dao.updateTrackMetadata(id, title, artist, album, finalCoverPath)
            val updatedTrack = dao.getTrackById(id)
            if (updatedTrack != null) {
                com.example.beatpulse.ui.components.ThumbnailCache.invalidateTrack(appContext, updatedTrack)
            }
        }
    }

    suspend fun insertOrUpdateTrack(track: TrackEntity) {
        withContext(Dispatchers.IO) {
            dao.insertTrack(track)
        }
    }
    suspend fun deleteTrack(trackId: Long): android.content.IntentSender? {
        return withContext(Dispatchers.IO) {
            val track = dao.getTrackById(trackId) ?: return@withContext null
            val contentUri = android.content.ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                trackId
            )
            try {
                val deleted = appContext.contentResolver.delete(contentUri, null, null)
                if (deleted > 0) {
                    dao.deleteTracksById(listOf(trackId))
                } else {
                    dao.deleteTracksById(listOf(trackId))
                }
                null
            } catch (e: SecurityException) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    android.provider.MediaStore.createDeleteRequest(appContext.contentResolver, listOf(contentUri)).intentSender
                } else {
                    val recoverableException = e as? android.app.RecoverableSecurityException
                    recoverableException?.userAction?.actionIntent?.intentSender
                }
            } catch (e: Exception) {
                // If it's another exception (like IllegalArgumentException because file is gone), clean our DB
                dao.deleteTracksById(listOf(trackId))
                null
            }
        }
    }

    suspend fun completeDeletion(trackId: Long) {
        withContext(Dispatchers.IO) {
            dao.deleteTracksById(listOf(trackId))
        }
    }

    // Statistics methods
    suspend fun getTopArtistsByPlayCount() = withContext(Dispatchers.IO) { dao.getTopArtistsByPlayCount() }
    suspend fun getTopAlbumsByPlayCount() = withContext(Dispatchers.IO) { dao.getTopAlbumsByPlayCount() }
    suspend fun getTotalListeningTimeMs() = withContext(Dispatchers.IO) { dao.getTotalListeningTimeMs() ?: 0L }
    suspend fun getTotalTracksPlayed() = withContext(Dispatchers.IO) { dao.getTotalTracksPlayed() }
    suspend fun getTotalPlayCount() = withContext(Dispatchers.IO) { dao.getTotalPlayCount() ?: 0 }
    suspend fun getUniqueArtistsPlayed() = withContext(Dispatchers.IO) { dao.getUniqueArtistsPlayed() }
    suspend fun getUniqueAlbumsPlayed() = withContext(Dispatchers.IO) { dao.getUniqueAlbumsPlayed() }
    suspend fun getTop5Tracks() = withContext(Dispatchers.IO) { dao.getTop5Tracks() }
    suspend fun getFavoriteTracksList() = withContext(Dispatchers.IO) { dao.getFavoriteTracks() }
}
