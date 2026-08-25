package com.example.beatpulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnlineMusicRepository : IOnlineMusicRepository {
    private var apiKey = ""

    private val _isServiceDown = kotlinx.coroutines.flow.MutableStateFlow(false)
    override val isServiceDown: kotlinx.coroutines.flow.StateFlow<Boolean> = _isServiceDown

    init {
        // Initialization no longer requires youtube_config.json
    }

    companion object {
        const val NEWPIPE_VERSION = "0.26.5-v3"
    }

    override suspend fun verifyServiceStatus(prefs: AppPreferences) {
        val lastVerifiedVersion = prefs.lastVerifiedNewPipeVersion
        
        // Asignar el estado cacheado de manera inmediata para evitar la ventana de vulnerabilidad
        // en los primeros segundos de arranque de la app.
        _isServiceDown.value = prefs.lastServiceDownState
        
        // Si la versión de gradle no ha cambiado, ya terminamos.
        if (lastVerifiedVersion == NEWPIPE_VERSION) {
            return
        }

        // Realizar una búsqueda aleatoria para evitar que videos muy populares pasen el filtro
        val testQueries = listOf("lofi hip hop", "pop hits", "rock classics", "jazz mix", "chillout", "synthwave")
        val randomQuery = testQueries.random()
        
        var anySuccess = false
        try {
            val searchResults = searchOnlineMusic(randomQuery)
            if (searchResults.isNotEmpty()) {
                // Tomar hasta 2 resultados aleatorios para probar
                val testTracks = searchResults.shuffled().take(2)
                for (track in testTracks) {
                    val url = getStreamUrl("${track.id}|Test|Test")
                    if (url != null) {
                        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "HEAD"
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        val responseCode = connection.responseCode
                        val contentType = connection.contentType ?: ""
                        if (responseCode in 200..299 && !contentType.contains("text/html")) {
                            anySuccess = true
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val isDown = !anySuccess
        _isServiceDown.value = isDown
        
        // Guardar el estado verificado y la versión actual para futuras ejecuciones
        prefs.lastVerifiedNewPipeVersion = NEWPIPE_VERSION
        prefs.lastServiceDownState = isDown
    }

    override suspend fun searchOnlineMusic(query: String): List<TrackEntity> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<TrackEntity>()
            try {
                val searchExtractor = org.schabi.newpipe.extractor.ServiceList.YouTube.getSearchExtractor(query, emptyList(), null)
                searchExtractor.fetchPage()
                for (item in searchExtractor.initialPage.items) {
                    if (item is org.schabi.newpipe.extractor.stream.StreamInfoItem) {
                        val videoId = item.url.substringAfter("v=").substringBefore("&")
                        val thumbUrl = (item.thumbnails?.firstOrNull()?.url ?: "").replace(Regex("=w\\d+-h\\d+[^&]*"), "=w600-h600-l90-rj")
                        
                        results.add(
                            TrackEntity(
                                id = videoId.hashCode().toLong(),
                                title = item.name,
                                artist = item.uploaderName,
                                album = "YouTube",
                                duration = item.duration * 1000L,
                                dataPath = "youtube://${videoId}|${item.name.replace("|", "")}|${item.uploaderName.replace("|", "")}",
                                folderPath = "Online",
                                customCoverPath = thumbUrl
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            results.distinctBy { it.id }
        }
    }



    override suspend fun getStreamUrl(encryptedUrl: String): String? {
        val parts = encryptedUrl.split("|")
        val videoId = parts[0]

        return withContext(Dispatchers.IO) {
            try {
                val streamInfo = org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
                    org.schabi.newpipe.extractor.ServiceList.YouTube, 
                    "https://youtube.com/watch?v=$videoId"
                )
                
                var bestAudio: org.schabi.newpipe.extractor.stream.AudioStream? = null
                var bestBitrate = -1
                
                for (stream in streamInfo.audioStreams) {
                    if (stream.bitrate > bestBitrate) {
                        bestBitrate = stream.bitrate
                        bestAudio = stream
                    }
                }
                
                bestAudio?.content
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
