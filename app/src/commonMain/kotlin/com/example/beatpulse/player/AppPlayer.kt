package com.example.beatpulse.player

interface AppPlayer {
    val isPlaying: Boolean
    val duration: Long
    val currentPosition: Long
    fun seekTo(positionMs: Long)
    fun seekToNext()
    fun seekToPrevious()
    fun play()
    fun pause()
    fun setPlaybackSpeed(speed: Float)
    fun setPlaybackPitch(pitch: Float)
    fun setTrack(uri: String)
    fun setVolume(volume: Float)
    
    // Allows us to add and remove a listener for player events
    fun addListener(listener: AppPlayerListener)
    fun removeListener(listener: AppPlayerListener)
}

interface AppPlayerListener {
    fun onIsPlayingChanged(isPlaying: Boolean) {}
}
