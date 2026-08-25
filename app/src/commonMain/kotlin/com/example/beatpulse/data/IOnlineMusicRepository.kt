package com.example.beatpulse.data

interface IOnlineMusicRepository {
    suspend fun searchOnlineMusic(query: String): List<TrackEntity>
    suspend fun getStreamUrl(videoId: String): String?
    
    val isServiceDown: kotlinx.coroutines.flow.StateFlow<Boolean>
    suspend fun verifyServiceStatus(prefs: AppPreferences)
}
