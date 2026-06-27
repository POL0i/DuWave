package com.example.beatpulse.service

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.flow.MutableStateFlow
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import android.os.Bundle
import com.example.beatpulse.R

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        exoPlayer?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioSessionIdFlow.value = audioSessionId
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateWidget()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateWidget()
            }
        })
        exoPlayer?.let { player ->
            audioSessionIdFlow.value = player.audioSessionId

            val intent = Intent(this, com.example.beatpulse.MainActivity::class.java)
            val pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )

            val customLayout = listOf(
                CommandButton.Builder()
                    .setDisplayName("Repetir")
                    .setSessionCommand(SessionCommand("ACTION_REPEAT", Bundle.EMPTY))
                    .setIconResId(R.drawable.ic_repeat)
                    .build()
            )

            mediaSession = MediaSession.Builder(this, player)
                .setSessionActivity(pendingIntent)
                .setCustomLayout(customLayout)
                .setCallback(object : MediaSession.Callback {
                    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                        val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                            .add(SessionCommand("ACTION_REPEAT", Bundle.EMPTY))
                            .build()
                        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                            .setAvailableSessionCommands(sessionCommands)
                            .build()
                    }

                    override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                        if (customCommand.customAction == "ACTION_REPEAT") {
                            val p = session.player
                            p.repeatMode = if (p.repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) {
                                androidx.media3.common.Player.REPEAT_MODE_ONE
                            } else {
                                androidx.media3.common.Player.REPEAT_MODE_OFF
                            }
                            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        return super.onCustomCommand(session, controller, customCommand, args)
                    }

                    override fun onAddMediaItems(
                        mediaSession: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        mediaItems: MutableList<androidx.media3.common.MediaItem>
                    ): ListenableFuture<MutableList<androidx.media3.common.MediaItem>> {
                        val updatedMediaItems = mediaItems.map { item ->
                            item.buildUpon().setUri(item.requestMetadata.mediaUri ?: item.localConfiguration?.uri ?: android.net.Uri.parse("")).build()
                        }.toMutableList()
                        return Futures.immediateFuture(updatedMediaItems)
                    }
                })
                .build()
        }
    }

    companion object {
        val audioSessionIdFlow = MutableStateFlow(C.AUDIO_SESSION_ID_UNSET)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "TOGGLE_PLAY" -> {
                if (exoPlayer?.isPlaying == true) exoPlayer?.pause() else exoPlayer?.play()
                updateWidget()
            }
            "SKIP_NEXT" -> {
                exoPlayer?.seekToNextMediaItem()
                updateWidget()
            }
            "SKIP_PREV" -> {
                exoPlayer?.seekToPreviousMediaItem()
                updateWidget()
            }
            "UPDATE_WIDGET_STYLE" -> {
                updateWidget()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateWidget() {
        val player = exoPlayer ?: return
        val currentMediaItem = player.currentMediaItem
        val metadata = currentMediaItem?.mediaMetadata

        val intent = Intent(this, com.example.beatpulse.widget.MediaWidgetProvider::class.java).apply {
            action = com.example.beatpulse.widget.MediaWidgetProvider.ACTION_UPDATE_WIDGET
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_TITLE, metadata?.title?.toString() ?: "No playing")
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_ARTIST, metadata?.artist?.toString() ?: "BeatPulse")
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_IS_PLAYING, player.isPlaying)
            
            // Extract cover art path
            val coverPath = metadata?.artworkUri?.toString()?.removePrefix("file://")
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_COVER_PATH, coverPath)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }
}
