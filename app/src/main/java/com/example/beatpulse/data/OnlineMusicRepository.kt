package com.example.beatpulse.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineMusicRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var apiKey = ""
    private var searchClient = JSONObject()
    private var playerClient = JSONObject()

    init {
        // Initialization no longer requires youtube_config.json
    }

    suspend fun searchOnlineMusic(query: String): List<TrackEntity> {
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

    private fun extractSearchResults(json: JSONObject): List<TrackEntity> {
        val results = mutableListOf<TrackEntity>()
        val renderers = findKeyRecursively(json, "musicResponsiveListItemRenderer")
        for (renderer in renderers) {
            if (renderer !is JSONObject) continue
            val videoId = renderer.optJSONObject("playlistItemData")?.optString("videoId")
            if (!videoId.isNullOrEmpty()) {
                val flexColumns = renderer.optJSONArray("flexColumns")
                val title = flexColumns?.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "Unknown Title"
                
                val artistRuns = flexColumns?.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")?.optJSONArray("runs")
                
                var artist = "Unknown Artist"
                var album = "YouTube"
                if (artistRuns != null) {
                    val parts = mutableListOf<String>()
                    for (i in 0 until artistRuns.length()) {
                        val text = artistRuns.optJSONObject(i)?.optString("text")?.trim() ?: ""
                        if (text.isNotBlank() && text != "•") {
                            parts.add(text)
                        }
                    }
                    
                    if (parts.size >= 2) {
                        val firstLower = parts[0].lowercase()
                        if (firstLower == "song" || firstLower == "canción" || firstLower == "video" || firstLower == "vídeo") {
                            artist = parts[1]
                            if (parts.size >= 3 && !parts[2].lowercase().contains("view") && !parts[2].lowercase().contains("vista") && !parts[2].contains(":")) {
                                album = parts[2]
                            }
                        } else {
                            artist = parts[0]
                            if (parts.size >= 2 && !parts[1].contains(":")) {
                                album = parts[1]
                            }
                        }
                    } else if (parts.isNotEmpty()) {
                        artist = parts[0]
                    }
                }
                
                val thumbnails = renderer.optJSONObject("thumbnail")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail")
                    ?.optJSONArray("thumbnails")
                
                var thumbUrl = ""
                if (thumbnails != null && thumbnails.length() > 0) {
                    val lastThumb = thumbnails.optJSONObject(thumbnails.length() - 1)
                    thumbUrl = lastThumb?.optString("url") ?: ""
                    if (thumbUrl.startsWith("//")) thumbUrl = "https:$thumbUrl"
                    // Force high resolution thumbnails from YouTube
                    thumbUrl = thumbUrl.replace(Regex("=w\\d+-h\\d+[^&]*"), "=w600-h600-l90-rj")
                }

                results.add(
                        TrackEntity(
                            id = videoId.hashCode().toLong(),
                            title = title,
                            artist = artist,
                            album = album,
                            duration = 0L, // InnerTube search often omits exact duration in simple runs
                            dataPath = "youtube://${videoId}|${title.replace("|", "")}|${artist.replace("|", "")}",
                            folderPath = "Online",
                            customCoverPath = thumbUrl
                        )              )
            }
        }
        // Deduplicate by ID
        return results.distinctBy { it.id }
    }

    private fun findKeyRecursively(obj: Any, targetKey: String): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        when (obj) {
            is JSONObject -> {
                if (obj.has(targetKey)) {
                    val target = obj.optJSONObject(targetKey)
                    if (target != null) results.add(target)
                }
                for (key in obj.keys()) {
                    results.addAll(findKeyRecursively(obj.opt(key) ?: continue, targetKey))
                }
            }
            is JSONArray -> {
                for (i in 0 until obj.length()) {
                    results.addAll(findKeyRecursively(obj.opt(i) ?: continue, targetKey))
                }
            }
        }
        return results
    }

    suspend fun getStreamUrl(encryptedUrl: String): String? {
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
