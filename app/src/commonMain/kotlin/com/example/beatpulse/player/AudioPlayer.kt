package com.example.beatpulse.player

import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val playbackState: StateFlow<PlaybackState>
    val repeatMode: StateFlow<RepeatMode>
    val isPlaying: StateFlow<Boolean>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setDataSource(uri: String)
    fun setRepeatMode(mode: RepeatMode)
    fun setPlaybackSpeed(speed: Float)
    fun setPlaybackPitch(pitch: Float)
    fun stop()
    fun release()
}
