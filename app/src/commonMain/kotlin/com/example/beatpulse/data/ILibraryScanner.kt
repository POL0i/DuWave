package com.example.beatpulse.data

interface ILibraryScanner {
    suspend fun scanMusic(folderPath: String? = null): List<TrackEntity>
    fun deleteTrackFile(trackId: Long, dataPath: String): Any?
}
