package com.example.beatpulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnlineMusicRepository : IOnlineMusicRepository {
    private var apiKey = ""

    init {
        // Initialization no longer requires youtube_config.json
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
