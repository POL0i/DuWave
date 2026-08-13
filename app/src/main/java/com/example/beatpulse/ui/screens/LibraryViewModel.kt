package com.example.beatpulse.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.PreferencesManager
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.data.PlaylistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import android.content.IntentSender
import android.content.Context
import com.example.beatpulse.utils.DownloadHelper

import com.example.beatpulse.data.OnlineMusicRepository

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val repository: MusicRepository,
    private val onlineRepository: OnlineMusicRepository,
    val prefs: PreferencesManager
) : ViewModel() {

    val selectedUnifiedCategory = MutableStateFlow(0)
    val selectedUnifiedGroup = MutableStateFlow<String?>(null)
    
    val resolvingTracks = MutableStateFlow<Set<Long>>(emptySet())

    val allTracks: StateFlow<List<TrackEntity>> = repository.allTracksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTracks: StateFlow<List<TrackEntity>> = repository.recentTracksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topTracks: StateFlow<List<TrackEntity>> = repository.topPlayedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<TrackEntity>> = repository.recentlyAddedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<TrackEntity>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isScanning: StateFlow<Boolean> = repository.isScanning

    fun scanMediaStore() {
        viewModelScope.launch {
            repository.scanMediaStore()
        }
    }

    fun copyMetadataForTrimmedTrack(originalTrack: TrackEntity, newFilePath: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Escanear el archivo nuevo
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(newFilePath),
                null // Auto-detect mime type (supports .opus, .m4a, etc)
            ) { _, _ ->
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    // Refrescar DB
                    repository.scanMediaStore()
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

    fun toggleFavorite(track: com.example.beatpulse.data.TrackEntity, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.insertOrUpdateTrack(track)
            repository.toggleFavorite(track.id, isFavorite)
        }
    }

    suspend fun deleteTrack(trackId: Long): IntentSender? {
        return repository.deleteTrack(trackId)
    }

    fun completeDeletion(trackId: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.completeDeletion(trackId)
        }
    }

    fun updateTrackCover(track: TrackEntity, newCoverPath: String?) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.updateTrackMetadata(track.id, track.title, track.artist, track.album, newCoverPath)
            // Invalidar el caché actual del thumbnail viejo
            com.example.beatpulse.ui.components.ThumbnailCache.invalidateTrack(context, track)
            // Hacer un rescan para forzar la actualización en la interfaz
            scanMediaStore()
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: com.example.beatpulse.data.TrackEntity) {
        viewModelScope.launch {
            repository.insertOrUpdateTrack(track)
            repository.addTrackToPlaylist(playlistId, track.id)
        }
    }
    
    suspend fun createPlaylist(name: String): Long {
        return repository.createPlaylist(name)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>> {
        return repository.getTracksForPlaylist(playlistId)
    }

    fun updatePlaylistOrder(playlistId: Long, updates: List<Pair<Long, Int>>) {
        viewModelScope.launch {
            repository.updatePlaylistOrder(playlistId, updates)
        }
    }

    fun getPlaylistTrackCountFlow(playlistId: Long): Flow<Int> {
        return repository.getPlaylistTrackCountFlow(playlistId)
    }

    val searchQuery = MutableStateFlow("")
    val onlineSearchResults = MutableStateFlow<List<TrackEntity>>(emptyList())
    val isOnlineSearchLoading = MutableStateFlow(false)

    val recommendations = MutableStateFlow<Map<String, List<TrackEntity>>>(emptyMap())
    val isRecommendationsLoading = MutableStateFlow(false)
    val selectedViewData = androidx.compose.runtime.mutableStateOf<PlaylistViewData?>(null)

    val changeCoverSearchResults = MutableStateFlow<List<TrackEntity>>(emptyList())
    val isChangeCoverLoading = MutableStateFlow(false)

    fun searchCoversForTrack(track: TrackEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isChangeCoverLoading.value = true
            try {
                val results = searchOnlineMusic("${track.title} ${track.artist}")
                changeCoverSearchResults.value = results.filter { !it.customCoverPath.isNullOrEmpty() }.take(3)
            } catch (e: Exception) {
                changeCoverSearchResults.value = emptyList()
            } finally {
                isChangeCoverLoading.value = false
            }
        }
    }

    fun loadRecommendations(forceUpdate: Boolean = false) {
        if (!forceUpdate && recommendations.value.isNotEmpty()) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isRecommendationsLoading.value = true
            try {
                val now = System.currentTimeMillis()
                val cachedJson = prefs.cachedRecommendationsJson
                val lastTime = prefs.lastRecommendationsTimestamp

                val timeSinceLast = now - lastTime
                
                if (cachedJson.isNotEmpty() && timeSinceLast < 24 * 60 * 60 * 1000L && !forceUpdate) {
                    try {
                        val map = mutableMapOf<String, List<TrackEntity>>()
                        val jsonObject = org.json.JSONObject(cachedJson)
                        val keys = jsonObject.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val array = jsonObject.getJSONArray(key)
                            val list = mutableListOf<TrackEntity>()
                            for (i in 0 until array.length()) {
                                val item = array.getJSONObject(i)
                                list.add(
                                    TrackEntity(
                                        id = item.getLong("id"),
                                        title = item.getString("title"),
                                        artist = item.getString("artist"),
                                        album = item.getString("album"),
                                        duration = item.getLong("duration"),
                                        dataPath = item.getString("dataPath"),
                                        folderPath = item.getString("folderPath"),
                                        customCoverPath = if (item.has("customCoverPath")) item.getString("customCoverPath") else null,
                                        dateAdded = item.getLong("dateAdded"),
                                        isFavorite = item.getBoolean("isFavorite")
                                    )
                                )
                            }
                            map[key] = list
                        }
                        recommendations.value = map
                        return@launch
                    } catch (e: Exception) {
                        e.printStackTrace()
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

                val artistResults: List<TrackEntity> = try { filterNew(onlineRepository.searchOnlineMusic(artistQuery)) } catch (e: Exception) { emptyList() }
                val similarResults: List<TrackEntity> = try { filterNew(onlineRepository.searchOnlineMusic(similarQuery)) } catch (e: Exception) { emptyList() }
                val trendingResults: List<TrackEntity> = try { filterNew(onlineRepository.searchOnlineMusic(trendingQuery)) } catch (e: Exception) { emptyList() }
                
                val newMap: Map<String, List<TrackEntity>> = mapOf(
                    (if (artistName != null) context.getString(com.example.beatpulse.R.string.because_you_listened, artistName) else context.getString(com.example.beatpulse.R.string.top_artists)) to artistResults,
                    (if (trackName != null) context.getString(com.example.beatpulse.R.string.based_on, trackName) else context.getString(com.example.beatpulse.R.string.for_you)) to similarResults,
                    context.getString(com.example.beatpulse.R.string.trending_music) to trendingResults
                )
                recommendations.value = newMap

                try {
                    val jsonObject = org.json.JSONObject()
                    for ((key, list) in newMap) {
                        val array = org.json.JSONArray()
                        for (track in list) {
                            val trackObj = org.json.JSONObject().apply {
                                put("id", track.id)
                                put("title", track.title)
                                put("artist", track.artist)
                                put("album", track.album)
                                put("duration", track.duration)
                                put("dataPath", track.dataPath)
                                put("folderPath", track.folderPath)
                                put("customCoverPath", track.customCoverPath ?: "")
                                put("dateAdded", track.dateAdded)
                                put("isFavorite", track.isFavorite)
                            }
                            array.put(trackObj)
                        }
                        jsonObject.put(key, array)
                    }
                    prefs.cachedRecommendationsJson = jsonObject.toString()
                    prefs.lastRecommendationsTimestamp = now
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRecommendationsLoading.value = false
            }
        }
    }

    suspend fun searchOnlineMusic(query: String): List<TrackEntity> {
        return onlineRepository.searchOnlineMusic(query)
    }

    suspend fun resolveStreamUrl(videoId: String): String? {
        return onlineRepository.getStreamUrl(videoId)
    }

    fun downloadOnlineTrack(context: Context, track: TrackEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                resolvingTracks.value += track.id
                prefs.showToast("Obteniendo enlace de ${track.title}...")
                // El dataPath viene como youtube://videoId|...
                val videoId = track.dataPath.removePrefix("youtube://").substringBefore("|")
                val url = onlineRepository.getStreamUrl(videoId)
                if (url != null) {
                    // Guardar en la base de datos para recordar la metadata (portada, título real) cuando MediaStore lo escanee
                    var coverUrl = track.customCoverPath
                    
                    // Descargar la miniatura offline
                    if (coverUrl != null && (coverUrl.startsWith("http://") || coverUrl.startsWith("https://"))) {
                        try {
                            val coversDir = java.io.File(context.filesDir, "covers")
                            if (!coversDir.exists()) coversDir.mkdirs()
                            val destFile = java.io.File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                            java.net.URL(coverUrl).openStream().use { input ->
                                destFile.outputStream().use { output ->
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
                        DownloadHelper.downloadTrack(
                            context = context,
                            streamUrl = url,
                            title = track.customTitle ?: track.title,
                            artist = track.customArtist ?: track.artist
                        )
                        prefs.showToast("Descarga iniciada: ${track.title}")
                    }
                } else {
                    prefs.showToast("No se pudo obtener el enlace de ${track.title}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                prefs.showToast("Error al procesar descarga de ${track.title}")
            } finally {
                resolvingTracks.value -= track.id
            }
        }
    }

    fun reloadMissingCoversForList(list: List<TrackEntity>) {
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
                    val locale = java.util.Locale(prefs.appLanguage)
                    val config = android.content.res.Configuration(context.resources.configuration)
                    config.setLocale(locale)
                    val localizedContext = context.createConfigurationContext(config)
                    prefs.showToast(localizedContext.getString(com.example.beatpulse.R.string.covers_reloaded))
                }
                return@launch
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val locale = java.util.Locale(prefs.appLanguage)
                val config = android.content.res.Configuration(context.resources.configuration)
                config.setLocale(locale)
                val localizedContext = context.createConfigurationContext(config)
                prefs.showToast(localizedContext.getString(com.example.beatpulse.R.string.reloading_covers_progress, 0, tracksWithoutCover.size))
            }

            for (track in tracksWithoutCover) {
                try {
                    val minutes = track.duration / 60000
                    val query = "${track.title} ${track.artist} $minutes min"
                    val results = onlineRepository.searchOnlineMusic(query)
                    
                    val bestResult = results.firstOrNull { !it.customCoverPath.isNullOrEmpty() }
                    
                    if (bestResult != null && bestResult.customCoverPath != null) {
                        var coverUrl = bestResult.customCoverPath!!
                        // Descargar portada para guardarla localmente
                        if (coverUrl.startsWith("http://") || coverUrl.startsWith("https://")) {
                            try {
                                val coversDir = java.io.File(context.filesDir, "covers")
                                if (!coversDir.exists()) coversDir.mkdirs()
                                val destFile = java.io.File(coversDir, "cover_reloaded_${System.currentTimeMillis()}.jpg")
                                java.net.URL(coverUrl).openStream().use { input ->
                                    destFile.outputStream().use { output ->
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
                        val locale = java.util.Locale(prefs.appLanguage)
                        val config = android.content.res.Configuration(context.resources.configuration)
                        config.setLocale(locale)
                        val localizedContext = context.createConfigurationContext(config)
                        prefs.showToast(localizedContext.getString(com.example.beatpulse.R.string.reloading_covers_progress, processed, tracksWithoutCover.size))
                    }
                }
            }
        }
    }
}
