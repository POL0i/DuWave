package com.example.beatpulse.player

import androidx.media3.common.Player

class ExoPlayerAdapter(val exoPlayer: Player) : AppPlayer {
    override val isPlaying: Boolean
        get() = exoPlayer.isPlaying
        
    override val duration: Long
        get() = exoPlayer.duration
        
    override val currentPosition: Long
        get() = exoPlayer.currentPosition
        
    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }
    
    override fun seekToNext() {
        exoPlayer.seekToNext()
    }
    
    override fun seekToPrevious() {
        exoPlayer.seekToPrevious()
    }
    override fun play() {
        exoPlayer.play()
    }
    override fun pause() {
        exoPlayer.pause()
    }
    override fun setPlaybackSpeed(speed: Float) {
        val params = exoPlayer.playbackParameters.withSpeed(speed)
        exoPlayer.playbackParameters = params
    }
    override fun setPlaybackPitch(pitch: Float) {
        val params = androidx.media3.common.PlaybackParameters(exoPlayer.playbackParameters.speed, pitch)
        exoPlayer.playbackParameters = params
    }
    override fun setTrack(uri: String) {
        exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }
    override fun setVolume(volume: Float) {
        exoPlayer.volume = volume
    }

    private val listenersMap = mutableMapOf<AppPlayerListener, Player.Listener>()

    override fun addListener(listener: AppPlayerListener) {
        val exoListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                listener.onIsPlayingChanged(isPlaying)
            }
        }
        listenersMap[listener] = exoListener
        exoPlayer.addListener(exoListener)
    }

    override fun removeListener(listener: AppPlayerListener) {
        listenersMap.remove(listener)?.let { exoListener ->
            exoPlayer.removeListener(exoListener)
        }
    }
}
