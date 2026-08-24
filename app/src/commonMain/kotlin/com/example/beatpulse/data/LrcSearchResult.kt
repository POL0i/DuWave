package com.example.beatpulse.data

data class LrcSearchResult(
    val id: Long,
    val name: String,
    val artistName: String,
    val albumName: String,
    val duration: Long,
    val syncedLyrics: String,
    val plainLyrics: String,
    val score: Double = 0.0
)
