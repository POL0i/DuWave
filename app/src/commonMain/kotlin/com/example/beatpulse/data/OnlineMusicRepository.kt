package com.example.beatpulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class OnlineMusicRepository : IOnlineMusicRepository {
    private var apiKey = ""

    private val _isServiceDown = kotlinx.coroutines.flow.MutableStateFlow(false)
    override val isServiceDown: kotlinx.coroutines.flow.StateFlow<Boolean> = _isServiceDown

    private var useInvidiousFallback = false
    private var fastestInvidiousInstance: String? = null

    companion object {
        const val NEWPIPE_VERSION = "0.26.5-v3"
        private val INVIDIOUS_INSTANCES = listOf(
            "https://invidious.flokinet.to",
            "https://yewtu.be",
            "https://invidious.nerdvpn.de",
            "https://inv.tux.pizza",
            "https://invidious.drgns.space",
            "https://invidious.privacyredirect.com"
        )
    }

    private suspend fun findFastestInvidiousInstance(): String? {
        if (fastestInvidiousInstance != null) return fastestInvidiousInstance

        return withContext(Dispatchers.IO) {
            val channel = kotlinx.coroutines.channels.Channel<String>(INVIDIOUS_INSTANCES.size)
            val jobs = INVIDIOUS_INSTANCES.map { instanceUrl ->
                launch {
                    try {
                        val conn = URL("$instanceUrl/api/v1/stats").openConnection() as HttpURLConnection
                        conn.requestMethod = "HEAD"
                        conn.connectTimeout = 3000
                        conn.readTimeout = 3000
                        if (conn.responseCode in 200..299) {
                            channel.trySend(instanceUrl)
                        }
                    } catch (_: Exception) {
                        // Instance unreachable, ignore
                    }
                }
            }

            val firstValid: String? = try {
                kotlinx.coroutines.withTimeoutOrNull(3500L) {
                    channel.receive()
                }
            } catch (_: Exception) {
                null
            }

            jobs.forEach { it.cancel() }
            channel.close()

            fastestInvidiousInstance = firstValid
            firstValid
        }
    }

    init {
        // Initialization no longer requires youtube_config.json
    }

    override suspend fun verifyServiceStatus(prefs: AppPreferences) {
        val lastVerifiedVersion = prefs.lastVerifiedNewPipeVersion
        
        // Asignar el estado cacheado de manera inmediata para evitar la ventana de vulnerabilidad
        // en los primeros segundos de arranque de la app.
        _isServiceDown.value = prefs.lastServiceDownState
        
        // CASO 1: Versión no cambió Y servicio estaba bien → no hacer nada.
        if (lastVerifiedVersion == NEWPIPE_VERSION && !prefs.lastServiceDownState) {
            return
        }
        
        // CASO 2: Servicio estaba caído (caché = down) → ir directo a Invidious
        // sin perder tiempo probando NewPipe (que ya sabemos que falló).
        if (prefs.lastServiceDownState) {
            useInvidiousFallback = true
            val fastInstance = findFastestInvidiousInstance()
            if (fastInstance != null) {
                // Invidious responde → quitar modo seguro, usar Invidious para esta sesión
                _isServiceDown.value = false
                prefs.lastServiceDownState = false
            } else {
                // Ni Invidious funciona → mantener modo seguro
                _isServiceDown.value = true
            }
            // No guardar la versión para que la próxima vez vuelva a intentar
            return
        }

        // CASO 3: Versión nueva de build → verificar NewPipe desde cero
        val testQueries = listOf("lofi hip hop", "pop hits", "rock classics", "jazz mix", "chillout", "synthwave")
        val randomQuery = testQueries.random()
        
        var anySuccess = false
        try {
            val searchResults = searchOnlineMusic(randomQuery)
            if (searchResults.isNotEmpty()) {
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
        
        if (anySuccess) {
            // NewPipe funciona directamente
            useInvidiousFallback = false
            _isServiceDown.value = false
            prefs.lastServiceDownState = false
        } else {
            // NewPipe falló en la verificación — probar Invidious
            useInvidiousFallback = true
            val fastInstance = findFastestInvidiousInstance()
            val isDown = fastInstance == null
            _isServiceDown.value = isDown
            prefs.lastServiceDownState = isDown
        }
        
        prefs.lastVerifiedNewPipeVersion = NEWPIPE_VERSION
    }

    override suspend fun searchOnlineMusic(query: String): List<TrackEntity> {
        if (useInvidiousFallback) return searchViaInvidious(query)

        return withContext(Dispatchers.IO) {
            val results = mutableListOf<TrackEntity>()
            try {
                val searchExtractor = org.schabi.newpipe.extractor.ServiceList.YouTube.getSearchExtractor(query, emptyList(), null)
                searchExtractor.fetchPage()
                for (item in searchExtractor.initialPage.items) {
                    if (item is org.schabi.newpipe.extractor.stream.StreamInfoItem) {
                        val videoId = item.url.substringAfter("v=").substringBefore("&")
                        val thumbUrl = (item.thumbnails.firstOrNull()?.url ?: "").replace(Regex("=w\\d+-h\\d+[^&]*"), "=w600-h600-l90-rj")
                        
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
                useInvidiousFallback = true
                return@withContext searchViaInvidious(query)
            }
            results.distinctBy { it.id }
        }
    }

    private suspend fun searchViaInvidious(query: String): List<TrackEntity> = withContext(Dispatchers.IO) {
        val instancesToTry = mutableListOf<String>()
        fastestInvidiousInstance?.let { instancesToTry.add(it) }
        instancesToTry.addAll(INVIDIOUS_INSTANCES.filter { it != fastestInvidiousInstance })

        val results = mutableListOf<TrackEntity>()
        val queryEncoded = java.net.URLEncoder.encode("$query audio", "UTF-8")

        for (instance in instancesToTry) {
            try {
                val url = URL("$instance/api/v1/search?q=$queryEncoded&type=video")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
                val responseText = stream.bufferedReader().use { it.readText() }

                if (conn.responseCode in 200..299 && !responseText.contains("\"error\"")) {
                    val items = responseText.split("\"type\":\"video\"")
                    for (i in 1 until items.size) {
                        val item = items[i]
                        val videoIdMatch = Regex("\"videoId\":\"([^\"]+)\"").find(item)
                        val titleMatch = Regex("\"title\":\"([^\"]+)\"").find(item)
                        val authorMatch = Regex("\"author\":\"([^\"]+)\"").find(item)
                        val lengthMatch = Regex("\"lengthSeconds\":(\\d+)").find(item)

                        val videoId = videoIdMatch?.groupValues?.get(1) ?: continue
                        if (videoId.isNotEmpty()) {
                            val title = titleMatch?.groupValues?.get(1) ?: ""
                            val author = authorMatch?.groupValues?.get(1) ?: ""
                            val lengthSeconds = lengthMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L

                            var thumbUrl = ""
                            val thumbMatch = Regex("\"url\":\"(https://[^\"]+)\"").findAll(item)
                            val firstThumb = thumbMatch.firstOrNull()?.groupValues?.get(1)
                            if (firstThumb != null) {
                                thumbUrl = firstThumb
                            }

                            results.add(
                                TrackEntity(
                                    id = videoId.hashCode().toLong(),
                                    title = title,
                                    artist = author,
                                    album = "YouTube",
                                    duration = lengthSeconds * 1000L,
                                    dataPath = "youtube://${videoId}|${title.replace("|", "")}|${author.replace("|", "")}",
                                    folderPath = "Online",
                                    customCoverPath = thumbUrl
                                )
                            )
                        }
                    }
                    if (results.isNotEmpty()) {
                        fastestInvidiousInstance = instance
                        return@withContext results.distinctBy { it.id }
                    }
                }
            } catch (_: Exception) {
                // Try next instance
            }
        }
        results.distinctBy { it.id }
    }

    override suspend fun getStreamUrl(videoId: String): String? {
        val parts = videoId.split("|")
        val actualVideoId = parts[0]

        if (useInvidiousFallback) return getStreamUrlViaInvidious(actualVideoId)

        return withContext(Dispatchers.IO) {
            try {
                val streamInfo = org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
                    org.schabi.newpipe.extractor.ServiceList.YouTube, 
                    "https://youtube.com/watch?v=$actualVideoId"
                )
                
                var bestAudio: org.schabi.newpipe.extractor.stream.AudioStream? = null
                var bestBitrate = -1
                
                for (stream in streamInfo.audioStreams) {
                    if (stream.bitrate > bestBitrate) {
                        bestBitrate = stream.bitrate
                        bestAudio = stream
                    }
                }
                
                bestAudio?.url
            } catch (e: Exception) {
                e.printStackTrace()
                useInvidiousFallback = true
                return@withContext getStreamUrlViaInvidious(actualVideoId)
            }
        }
    }

    private suspend fun getStreamUrlViaInvidious(videoId: String): String? = withContext(Dispatchers.IO) {
        val instancesToTry = mutableListOf<String>()
        fastestInvidiousInstance?.let { instancesToTry.add(it) }
        instancesToTry.addAll(INVIDIOUS_INSTANCES.filter { it != fastestInvidiousInstance })

        for (instance in instancesToTry) {
            try {
                val proxyUrl = "$instance/latest_version?id=$videoId&itag=140&local=true"
                val conn = URL(proxyUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "HEAD"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                conn.instanceFollowRedirects = false // We want to see the 302 or 200

                val responseCode = conn.responseCode
                if (responseCode == 302 || responseCode == 301) {
                    val location = conn.getHeaderField("Location")
                    if (location != null) {
                        return@withContext if (location.startsWith("http")) location else "$instance$location"
                    }
                    return@withContext proxyUrl
                } else if (responseCode == 200) {
                    val contentType = conn.contentType ?: ""
                    if (contentType.contains("audio") || contentType.contains("video")) {
                        return@withContext proxyUrl
                    }
                }
            } catch (_: Exception) {
                // Try next instance
            }
        }
        null
    }
}
