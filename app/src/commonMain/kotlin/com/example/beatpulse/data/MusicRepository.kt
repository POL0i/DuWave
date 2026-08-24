package com.example.beatpulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

import kotlinx.coroutines.IO

class MusicRepository(private val db: AppDatabase, private val scanner: ILibraryScanner, private val prefs: AppPreferences) {
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
            kotlin.run {
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
            kotlin.run {
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

    suspend fun scanLocalLibrary(folderPath: String? = null) {
        isScanning.value = true
        try {
            val scannedTracks = scanner.scanMusic(folderPath)
            
            val existingTracks = dao.getAllTracks().first()
            val existingMap = existingTracks.associateBy { it.id }
            val foundIds = mutableSetOf<Long>()
            val currentChunk = mutableListOf<TrackEntity>()
            
            kotlin.run {
                scannedTracks.forEach { track ->
                    var existingTrack = existingMap[track.id]
                    if (existingTrack == null) {
                        // Check if this new local file matches an existing online track
                        val normalizedDataPath = track.dataPath.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                        val onlineTrack = existingMap.values.find {
                            val normalizedTitle = it.title.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                            it.dataPath.startsWith("youtube://") &&
                            (it.title.equals(track.title, ignoreCase = true) || track.title.contains(it.title, ignoreCase = true) ||
                             normalizedDataPath.contains(normalizedTitle))
                        }
                        if (onlineTrack != null) {
                            existingTrack = onlineTrack
                            // Migrate playlist references to the new local ID
                            dao.updatePlaylistTrackId(oldId = onlineTrack.id, newId = track.id)
                        }
                    }

                    val resolvedTitle = existingTrack?.customTitle ?: (if (existingTrack?.dataPath?.startsWith("youtube://") == true) existingTrack.title else null) ?: track.title
                    val resolvedArtist = existingTrack?.customArtist ?: (if (existingTrack?.dataPath?.startsWith("youtube://") == true) existingTrack.artist else null) ?: track.artist
                    val resolvedAlbum = existingTrack?.customAlbum ?: track.album
                    
                    foundIds.add(track.id)

                    currentChunk.add(
                        track.copy(
                            title = resolvedTitle,
                            artist = resolvedArtist,
                            album = resolvedAlbum,
                            isFavorite = existingTrack?.isFavorite ?: false,
                            lastPlayedTime = existingTrack?.lastPlayedTime ?: 0,
                            playCount = existingTrack?.playCount ?: 0,
                            dateAdded = existingTrack?.dateAdded?.takeIf { d -> d > 0 } ?: track.dateAdded,
                            customTitle = existingTrack?.customTitle,
                            customArtist = existingTrack?.customArtist,
                            customAlbum = existingTrack?.customAlbum,
                            customCoverPath = existingTrack?.customCoverPath ?: track.customCoverPath
                        )
                    )

                    if (currentChunk.size >= 500) {
                        dao.insertTracks(currentChunk)
                        currentChunk.clear()
                    }
                }

                if (currentChunk.isNotEmpty()) {
                    dao.insertTracks(currentChunk)
                }
                
                // Delete local tracks that no longer exist on the device
                // On Desktop, if we pass a specific folder path, we should only clear missing files IN THAT FOLDER?
                // Wait, if it's a specific folder import, we don't want to delete tracks from other folders.
                // For now, if folderPath is null (Android MediaStore scan), we delete missing. 
                // If folderPath is provided (Desktop folder import), we don't delete others.
                if (folderPath == null) {
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
            // Note: Saving covers to files is now handled by the UI/ViewModel when picking the cover, 
            // so we just store the path here.
            dao.updateTrackMetadata(id, title, artist, album, coverPath)
        }
    }

    suspend fun insertOrUpdateTrack(track: TrackEntity) {
        withContext(Dispatchers.IO) {
            dao.insertTrack(track)
        }
    }
    suspend fun deleteTrack(trackId: Long): Any? {
        return withContext(Dispatchers.IO) {
            val track = dao.getTrackById(trackId) ?: return@withContext null
            val result = scanner.deleteTrackFile(trackId, track.dataPath)
            // Cleanup database
            dao.deleteTracksById(listOf(trackId))
            result
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
