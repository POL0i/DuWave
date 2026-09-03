package com.example.beatpulse.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.IOnlineMusicRepository
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.data.PlaylistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import com.example.beatpulse.ui.viewmodels.ILibraryViewModel
import com.example.beatpulse.ui.viewmodels.PlaylistViewData
import com.example.beatpulse.data.ILibraryPlatformHelper

class LibraryViewModel(
    private val platformHelper: ILibraryPlatformHelper,
    private val repository: MusicRepository,
    private val onlineRepository: IOnlineMusicRepository,
    override val prefs: com.example.beatpulse.data.AppPreferences
) : ViewModel(), ILibraryViewModel {

    override val selectedUnifiedCategory = MutableStateFlow(0)
    override val selectedUnifiedGroup = MutableStateFlow<String?>(null)
    
    override val resolvingTracks = MutableStateFlow<Set<Long>>(emptySet())

    override val allTracks: StateFlow<List<TrackEntity>> = repository.allTracksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val recentTracks: StateFlow<List<TrackEntity>> = repository.recentTracksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val topTracks: StateFlow<List<TrackEntity>> = repository.topPlayedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val recentlyAdded: StateFlow<List<TrackEntity>> = repository.recentlyAddedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val favoriteTracks: StateFlow<List<TrackEntity>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val playlists: StateFlow<List<PlaylistEntity>> = repository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val isScanning: StateFlow<Boolean> = repository.isScanning

    override fun scanMediaStore() {
        viewModelScope.launch {
            repository.scanLocalLibrary() // Use the new generic method
        }
    }

    override fun pickFolderAndScan() {
        platformHelper.pickFolder { folderPath ->
            viewModelScope.launch {
                repository.scanLocalLibrary(folderPath)
            }
        }
    }

    override fun copyMetadataForTrimmedTrack(originalTrack: TrackEntity, newFilePath: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            platformHelper.scanFileToSystem(newFilePath) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    repository.scanLocalLibrary()
                    // Buscar la nueva pista y actualizar su metadata
                    var newTrack: TrackEntity? = null
                    // Room's Flow might take a moment to emit the new list, retry up to 3 times
                    for (i in 0 until 3) {
                        val allTracksList = repository.allTracksFlow.first()
                        newTrack = allTracksList.find { it.dataPath == newFilePath }
                        if (newTrack != null) break
                        kotlinx.coroutines.delay(300)
                    }
                    
                    if (newTrack != null) {
                        repository.updateTrackMetadata(
                            id = newTrack.id,
                            title = originalTrack.title + " (trim)",
                            artist = originalTrack.artist,
                            album = originalTrack.album,
                            coverPath = originalTrack.customCoverPath ?: "embedded://${originalTrack.dataPath}"
                        )
                    }
                }
            }
        }
    }

    override fun toggleFavorite(track: com.example.beatpulse.data.TrackEntity, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.insertOrUpdateTrack(track)
            repository.toggleFavorite(track.id, isFavorite)
        }
    }

    override suspend fun deleteTrack(trackId: Long): Any? {
        return repository.deleteTrack(trackId)
    }

    override fun completeDeletion(trackId: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.completeDeletion(trackId)
        }
    }

    override fun updateTrackCover(track: TrackEntity, newCoverPath: String?) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.updateTrackMetadata(track.id, track.title, track.artist, track.album, newCoverPath)
            // Hacer un rescan para forzar la actualización en la interfaz
            scanMediaStore()
        }
    }

    override fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    override fun addTrackToPlaylist(playlistId: Long, track: com.example.beatpulse.data.TrackEntity) {
        viewModelScope.launch {
            repository.insertOrUpdateTrack(track)
            repository.addTrackToPlaylist(playlistId, track.id)
        }
    }
    
    override suspend fun createPlaylist(name: String): Long {
        return repository.createPlaylist(name)
    }

    override fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>> {
        return repository.getTracksForPlaylist(playlistId)
    }

    override fun updatePlaylistOrder(playlistId: Long, updates: List<Pair<Long, Int>>) {
        viewModelScope.launch {
            repository.updatePlaylistOrder(playlistId, updates)
        }
    }

    override fun getPlaylistTrackCountFlow(playlistId: Long): Flow<Int> {
        return repository.getPlaylistTrackCountFlow(playlistId)
    }

    override val searchQuery = MutableStateFlow("")
    override val onlineSearchResults = MutableStateFlow<List<TrackEntity>>(emptyList())
    override val isOnlineSearchLoading = MutableStateFlow(false)
    
    override val isOnlineServiceDown: StateFlow<Boolean> = onlineRepository.isServiceDown

    private val searchCache = mutableMapOf<String, List<TrackEntity>>()
    private val searchCacheLock = Mutex()

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            onlineRepository.verifyServiceStatus(prefs)
        }
    }

    override val recommendations = MutableStateFlow<Map<String, List<TrackEntity>>>(emptyMap())
    override val isRecommendationsLoading = MutableStateFlow(false)
    override val selectedViewData = androidx.compose.runtime.mutableStateOf<PlaylistViewData?>(null)

    override val changeCoverSearchResults = MutableStateFlow<List<TrackEntity>>(emptyList())
    override val isChangeCoverLoading = MutableStateFlow(false)

    override fun searchCoversForTrack(track: TrackEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isChangeCoverLoading.value = true
            try {
                val coverResults = mutableListOf<TrackEntity>()
                val cleanTitle = track.title
                    .replace(Regex("\\(Official.*?\\)", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\(trim\\)", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\.opus|\\.m4a|\\.mp3", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\bFC\\b", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\b\\-1\\b", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\b4k\\b", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\[.*?]"), "")
                    .replace(Regex("\\|.*"), "")
                    .replace(Regex("composed by.*", RegexOption.IGNORE_CASE), "")
                    .trim()
                val artist = if (track.artist != "Unknown Artist" && track.artist != "Artista Desconocido") track.artist else ""

                // 1. Try iTunes API first (fast, reliable, high quality covers)
                try {
                    val query = "$cleanTitle $artist".replace(Regex("\\s+"), " ").trim()
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    val itunesApi = "https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=5"
                    
                    val connection = java.net.URL(itunesApi).openConnection() as java.net.HttpURLConnection
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    
                    val response = connection.inputStream.bufferedReader().readText()
                    // Parse artworkUrl100 entries
                    var searchFrom = 0
                    while (coverResults.size < 5) {
                        val artIdx = response.indexOf("\"artworkUrl100\":\"", searchFrom)
                        if (artIdx == -1) break
                        val start = artIdx + 17
                        val end = response.indexOf("\"", start)
                        val lowResUrl = response.substring(start, end)
                        val hiResUrl = lowResUrl.replace("100x100bb", "600x600bb")
                        
                        // Also grab trackName for display
                        val nameIdx = response.lastIndexOf("\"trackName\":\"", artIdx)
                        val trackName = if (nameIdx != -1) {
                            val ns = nameIdx + 13
                            val ne = response.indexOf("\"", ns)
                            response.substring(ns, ne)
                        } else "iTunes Result"
                        
                        coverResults.add(TrackEntity(
                            id = hiResUrl.hashCode().toLong(),
                            title = trackName,
                            artist = artist.ifEmpty { "iTunes" },
                            album = "iTunes",
                            duration = 0L,
                            dataPath = "",
                            folderPath = "",
                            customCoverPath = hiResUrl
                        ))
                        searchFrom = end + 1
                    }
                    connection.disconnect()
                } catch (_: Exception) {}
                
                // 2. Try YouTube search (full query, then title-only fallback)
                if (coverResults.size < 3) {
                    try {
                        val fullQuery = if (artist.isNotEmpty()) "$cleanTitle $artist" else cleanTitle
                        val fullResults = searchOnlineMusic(fullQuery)
                            .filter { !it.customCoverPath.isNullOrEmpty() }
                        coverResults.addAll(fullResults.take(3))
                    } catch (_: Exception) {}
                }
                
                if (coverResults.size < 3) {
                    try {
                        val titleOnly = searchOnlineMusic(cleanTitle)
                            .filter { !it.customCoverPath.isNullOrEmpty() }
                        coverResults.addAll(titleOnly.take(3))
                    } catch (_: Exception) {}
                }
                
                // 3. Fallback for complex names: split by hyphens and take the last prominent chunk
                if (coverResults.size == 0 && cleanTitle.contains("-")) {
                    try {
                        val chunks = cleanTitle.split("-").map { it.trim() }.filter { it.length > 2 }
                        if (chunks.size >= 2) {
                            val targetChunk = chunks.last()
                            val fallbackYt = searchOnlineMusic(targetChunk).filter { !it.customCoverPath.isNullOrEmpty() }
                            coverResults.addAll(fallbackYt.take(3))
                        }
                    } catch (_: Exception) {}
                }

                
                changeCoverSearchResults.value = coverResults.distinctBy { it.customCoverPath }.take(6)
            } catch (e: Exception) {
                changeCoverSearchResults.value = emptyList()
            } finally {
                isChangeCoverLoading.value = false
            }
        }
    }

    override fun loadRecommendations(forceUpdate: Boolean) {
        if (!forceUpdate && recommendations.value.isNotEmpty()) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isRecommendationsLoading.value = true
            try {
                val now = System.currentTimeMillis()
                val cachedJson = prefs.cachedRecommendationsJson
                val lastTime = prefs.lastRecommendationsTimestamp
                val timeSinceLast = now - lastTime

                // Try to restore from cache if less than 24h old
                if (cachedJson.isNotEmpty() && timeSinceLast < 24 * 60 * 60 * 1000L && !forceUpdate) {
                    val restored = deserializeRecommendations(cachedJson)
                    if (restored.isNotEmpty()) {
                        recommendations.value = restored
                        isRecommendationsLoading.value = false
                        return@launch
                    }
                }

                val topArtists = repository.getTopArtistsByPlayCount()
                val topTracks = repository.getTop5Tracks()
                val allLocalTracks = allTracks.value
                val localTitles = allLocalTracks.map { it.title.lowercase().trim() }.toSet()
                
                val artistName = topArtists.firstOrNull()?.artist
                val trackName = topTracks.firstOrNull()?.title
                
                val artistQuery = if (artistName != null) "Música de $artistName" else "Éxitos pop actuales"
                val similarQuery = if (trackName != null) "Mix de $trackName" else "Rock clasico éxitos"
                val trendingQuery = "Éxitos musicales de moda ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}"

                // Filter function to skip already owned tracks (by title similarity to avoid exact match misses)
                fun filterNew(results: List<TrackEntity>): List<TrackEntity> {
                    return results.filter { track ->
                        val lowerTitle = track.title.lowercase().trim()
                        !localTitles.any { local -> local.contains(lowerTitle) || lowerTitle.contains(local) }
                    }.take(10)
                }

                val artistResults: List<TrackEntity> = try { filterNew(searchOnlineMusic(artistQuery)) } catch (e: Exception) { emptyList() }
                val similarResults: List<TrackEntity> = try { filterNew(searchOnlineMusic(similarQuery)) } catch (e: Exception) { emptyList() }
                val trendingResults: List<TrackEntity> = try { filterNew(searchOnlineMusic(trendingQuery)) } catch (e: Exception) { emptyList() }
                
                val newMap: Map<String, List<TrackEntity>> = mapOf(
                    (if (artistName != null) platformHelper.getLocalizedString("because_you_listened", artistName) else platformHelper.getLocalizedString("top_artists")) to artistResults,
                    (if (trackName != null) platformHelper.getLocalizedString("based_on", trackName) else platformHelper.getLocalizedString("for_you")) to similarResults,
                    platformHelper.getLocalizedString("trending_music") to trendingResults
                )
                recommendations.value = newMap

                // Save to cache
                try {
                    prefs.cachedRecommendationsJson = serializeRecommendations(newMap)
                    prefs.lastRecommendationsTimestamp = now
                } catch (e: Exception) { e.printStackTrace() }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRecommendationsLoading.value = false
            }
        }
    }

    // Simple delimiter-based serialization for recommendations cache (no kotlinx.serialization needed)
    private fun serializeRecommendations(map: Map<String, List<TrackEntity>>): String {
        val sb = StringBuilder()
        for ((category, tracks) in map) {
            sb.append("CAT::").append(category.replace("\n", "\\n")).append("\n")
            for (t in tracks) {
                sb.append("T::${t.id}||${t.title.replace("||","| |")}||${t.artist.replace("||","| |")}||${t.duration}||${t.dataPath.replace("||","| |")}||${t.customCoverPath?.replace("||","| |") ?: ""}\n")
            }
        }
        return sb.toString()
    }

    private fun deserializeRecommendations(data: String): Map<String, List<TrackEntity>> {
        val result = mutableMapOf<String, MutableList<TrackEntity>>()
        var currentCategory = ""
        for (line in data.lines()) {
            when {
                line.startsWith("CAT::") -> {
                    currentCategory = line.removePrefix("CAT::").replace("\\n", "\n")
                    result[currentCategory] = mutableListOf()
                }
                line.startsWith("T::") && currentCategory.isNotEmpty() -> {
                    val parts = line.removePrefix("T::").split("||")
                    if (parts.size >= 5) {
                        result[currentCategory]?.add(TrackEntity(
                            id = parts[0].toLongOrNull() ?: 0L,
                            title = parts[1],
                            artist = parts[2],
                            duration = parts[3].toLongOrNull() ?: 0L,
                            dataPath = parts[4],
                            folderPath = "",
                            album = "",
                            customCoverPath = parts.getOrNull(5)?.takeIf { it.isNotEmpty() }
                        ))
                    }
                }
            }
        }
        return result
    }

    override suspend fun searchOnlineMusic(query: String): List<TrackEntity> {
        val trimmedQuery = query.trim()
        searchCacheLock.withLock {
            if (searchCache.containsKey(trimmedQuery)) {
                return searchCache[trimmedQuery]!!
            }
        }
        val results = onlineRepository.searchOnlineMusic(trimmedQuery)
        searchCacheLock.withLock {
            searchCache[trimmedQuery] = results
        }
        return results
    }

    override suspend fun resolveStreamUrl(videoId: String): String? {
        return onlineRepository.getStreamUrl(videoId)
    }

    override fun downloadOnlineTrack(track: TrackEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                resolvingTracks.value += track.id
                prefs.showToast(platformHelper.getLocalizedString("toast_fetching_link", track.title))
                // El dataPath viene como youtube://videoId|...
                val videoId = track.dataPath.removePrefix("youtube://").substringBefore("|")
                val url = onlineRepository.getStreamUrl(videoId)
                if (url != null) {
                    // Guardar en la base de datos para recordar la metadata (portada, título real) cuando MediaStore lo escanee
                    var coverUrl = track.customCoverPath
                    
                    // Descargar la miniatura offline
                    if (coverUrl != null && (coverUrl.startsWith("http://") || coverUrl.startsWith("https://"))) {
                        try {
                            val coversDir = java.io.File(platformHelper.getCoversDir())
                            val destFile = java.io.File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                            val connection = java.net.URL(coverUrl).openConnection() as java.net.HttpURLConnection
                            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                            connection.inputStream.use { input ->
                                java.io.FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            coverUrl = destFile.absolutePath
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val trackToSave = track.copy(
                        customTitle = track.customTitle ?: track.title,
                        customArtist = track.customArtist ?: track.artist,
                        customCoverPath = coverUrl
                    )
                    repository.insertTrack(trackToSave)
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        platformHelper.downloadTrack(
                            streamUrl = url,
                            title = track.customTitle ?: track.title,
                            artist = track.customArtist ?: track.artist,
                            coverPath = coverUrl
                        )
                        prefs.showToast(platformHelper.getLocalizedString("toast_download_started", track.title))
                    }
                } else {
                    prefs.showToast(platformHelper.getLocalizedString("toast_download_failed_link", track.title))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                prefs.showToast(platformHelper.getLocalizedString("toast_download_error", track.title))
            } finally {
                resolvingTracks.value -= track.id
            }
        }
    }

    override fun reloadMissingCoversForList(list: List<TrackEntity>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var processed = 0
            val tracksWithoutCover = list.filter { 
                it.customCoverPath.isNullOrEmpty() || 
                it.customCoverPath == "null" || 
                it.customCoverPath?.startsWith("embedded://") == true ||
                (!it.customCoverPath!!.startsWith("/") && !it.customCoverPath!!.startsWith("http"))
            }

            if (tracksWithoutCover.isEmpty()) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    prefs.showToast(platformHelper.getLocalizedString("covers_reloaded"))
                }
                return@launch
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                prefs.showToast(platformHelper.getLocalizedString("reloading_covers_progress", 0, tracksWithoutCover.size))
            }

            for (track in tracksWithoutCover) {
                try {
                    var coverUrl: String? = null
                    
                    // Try iTunes API first (Fast, high quality, independent of NewPipe)
                    try {
                        val query = "${track.title} ${if (track.artist != "Unknown Artist" && track.artist != "Artista Desconocido") track.artist else ""}".trim()
                        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                        val itunesApi = "https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=1"
                        
                        val connection = java.net.URL(itunesApi).openConnection() as java.net.HttpURLConnection
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        
                        val response = connection.inputStream.bufferedReader().readText()
                        val artworkIndex = response.indexOf("\"artworkUrl100\":\"")
                        if (artworkIndex != -1) {
                            val start = artworkIndex + 17
                            val end = response.indexOf("\"", start)
                            val lowResUrl = response.substring(start, end)
                            coverUrl = lowResUrl.replace("100x100bb", "600x600bb")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    // Fallback to NewPipe search
                    if (coverUrl == null) {
                        val minutes = track.duration / 60000
                        val query = "${track.title} ${track.artist} $minutes min"
                        val results = searchOnlineMusic(query)
                        coverUrl = results.firstOrNull { !it.customCoverPath.isNullOrEmpty() }?.customCoverPath
                    }
                    
                    if (coverUrl != null) {
                        // Descargar portada para guardarla localmente
                        if (coverUrl.startsWith("http://") || coverUrl.startsWith("https://")) {
                            try {
                                val coversDir = java.io.File(platformHelper.getCoversDir())
                                val destFile = java.io.File(coversDir, "cover_reloaded_${System.currentTimeMillis()}.jpg")
                                val connection = java.net.URL(coverUrl).openConnection() as java.net.HttpURLConnection
                                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                                connection.connectTimeout = 5000
                                connection.readTimeout = 5000
                                connection.inputStream.use { input ->
                                    java.io.FileOutputStream(destFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                coverUrl = destFile.absolutePath
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        repository.updateTrackMetadata(
                            id = track.id,
                            title = track.customTitle,
                            artist = track.customArtist,
                            album = track.customAlbum,
                            coverPath = coverUrl
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                processed++
                if (processed % 5 == 0 || processed == tracksWithoutCover.size) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        prefs.showToast(platformHelper.getLocalizedString("reloading_covers_progress", processed, tracksWithoutCover.size))
                    }
                }
            }
        }
    }
}
