package com.example.beatpulse.data.models

import kotlinx.serialization.Serializable

@Serializable
data class SyncFolder(
    val id: String,
    val name: String,
    val path: String,
    val isPlaylist: Boolean
)

@Serializable
data class SyncTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val folderPath: String,
    val dataPath: String,
    val hasCover: Boolean
)
