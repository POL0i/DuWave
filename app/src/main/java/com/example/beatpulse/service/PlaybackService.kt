package com.example.beatpulse.service

import android.content.Intent
import android.media.audiofx.PresetReverb
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
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
import com.example.beatpulse.visualizer.AudioVisualizerManager
import com.example.beatpulse.data.OnlineMusicRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var equalizerManager: EqualizerManager
    @Inject lateinit var visualizerManager: AudioVisualizerManager
    @Inject lateinit var onlineRepository: OnlineMusicRepository

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var presetReverb: PresetReverb? = null
    private var currentSpeed: Float = 1.0f
    private var currentPitch: Float = 1.0f

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val teeProcessor = androidx.media3.exoplayer.audio.TeeAudioProcessor(visualizerManager.fftSink)
        val renderersFactory = object : androidx.media3.exoplayer.DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink? {
                return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(teeProcessor))
                    .build()
            }
        }

        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this)
        val resolvingDataSourceFactory = androidx.media3.datasource.ResolvingDataSource.Factory(
            dataSourceFactory,
            androidx.media3.datasource.ResolvingDataSource.Resolver { dataSpec ->
                val uriStr = dataSpec.uri.toString()
                if (uriStr.startsWith("youtube://")) {
                    val encryptedUrl = uriStr.removePrefix("youtube://")
                    val resolved = runBlocking { onlineRepository.getStreamUrl(encryptedUrl) }
                    if (resolved != null) {
                        return@Resolver dataSpec.buildUpon().setUri(android.net.Uri.parse(resolved)).build()
                    } else {
                        throw java.io.IOException("Stream not found on the backend service")
                    }
                }
                dataSpec
            }
        )
        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(resolvingDataSourceFactory, extractorsFactory)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2500, // min buffer
                50000, // max buffer
                250, // buffer for playback
                500 // buffer for playback after rebuffer
            )
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
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
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBufferingFlow.value = (playbackState == androidx.media3.common.Player.STATE_BUFFERING)
                updateWidget()
            }
            override fun onRepeatModeChanged(repeatMode: Int) {
                updateCustomLayout(repeatMode)
            }
        })
        exoPlayer?.let { player ->
            audioSessionIdFlow.value = player.audioSessionId

            // Initialize PresetReverb
            try {
                presetReverb = PresetReverb(0, player.audioSessionId).apply {
                    preset = PresetReverb.PRESET_LARGEHALL
                    enabled = reverbEnabledFlow.value
                }
            } catch (e: Exception) {
                // Some devices may not support PresetReverb
            }

            // Restore saved speed/pitch
            val savedSpeed = playbackSpeedFlow.value
            val savedPitch = playbackPitchFlow.value
            if (savedSpeed != 1.0f || savedPitch != 1.0f) {
                currentSpeed = savedSpeed
                currentPitch = savedPitch
                player.playbackParameters = PlaybackParameters(savedSpeed, savedPitch)
            }

            val intent = Intent(this, com.example.beatpulse.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )

            mediaSession = MediaSession.Builder(this, player)
                .setSessionActivity(pendingIntent)
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
                            p.repeatMode = when (p.repeatMode) {
                                androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                            }
                            updateCustomLayout(p.repeatMode)
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
            
            updateCustomLayout(player.repeatMode)
        }
    }

    private fun updateCustomLayout(repeatMode: Int) {
        val iconRes = when (repeatMode) {
            androidx.media3.common.Player.REPEAT_MODE_ONE -> androidx.media3.ui.R.drawable.exo_icon_repeat_one
            androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.ui.R.drawable.exo_icon_repeat_all
            else -> androidx.media3.ui.R.drawable.exo_icon_repeat_off
        }
        val customLayout = listOf(
            CommandButton.Builder()
                .setDisplayName("Repetir")
                .setSessionCommand(SessionCommand("ACTION_REPEAT", Bundle.EMPTY))
                .setIconResId(iconRes)
                .build()
        )
        mediaSession?.setCustomLayout(customLayout)
    }

    companion object {
        val audioSessionIdFlow = MutableStateFlow(C.AUDIO_SESSION_ID_UNSET)
        val playbackSpeedFlow = MutableStateFlow(1.0f)
        val playbackPitchFlow = MutableStateFlow(1.0f)
        val reverbEnabledFlow = MutableStateFlow(false)
        val isBufferingFlow = MutableStateFlow(false)
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
            "SET_SPEED" -> {
                val speed = intent.getFloatExtra("speed", 1.0f)
                currentSpeed = speed
                playbackSpeedFlow.value = speed
                exoPlayer?.playbackParameters = PlaybackParameters(currentSpeed, currentPitch)
            }
            "SET_PITCH" -> {
                val pitch = intent.getFloatExtra("pitch", 1.0f)
                currentPitch = pitch
                playbackPitchFlow.value = pitch
                exoPlayer?.playbackParameters = PlaybackParameters(currentSpeed, currentPitch)
            }
            "TOGGLE_REVERB" -> {
                val enabled = intent.getBooleanExtra("enabled", false)
                reverbEnabledFlow.value = enabled
                try {
                    presetReverb?.enabled = enabled
                } catch (e: Exception) { }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateWidget() {
        val player = exoPlayer ?: return
        val currentMediaItem = player.currentMediaItem
        val metadata = currentMediaItem?.mediaMetadata

        val intent1 = Intent(this, com.example.beatpulse.widget.MediaWidgetProvider::class.java).apply {
            action = com.example.beatpulse.widget.MediaWidgetProvider.ACTION_UPDATE_WIDGET
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_TITLE, metadata?.title?.toString() ?: "No playing")
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_ARTIST, metadata?.artist?.toString() ?: "DuWave")
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_IS_PLAYING, player.isPlaying)
            
            // Extract cover art path
            val coverPath = metadata?.artworkUri?.toString()?.removePrefix("file://")
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_COVER_PATH, coverPath)
            
            // Add current position for Chronometer
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_POSITION, player.currentPosition)
        }
        sendBroadcast(intent1)

        val intent2 = Intent(this, com.example.beatpulse.widget.MediaWidgetProvider4x1::class.java).apply {
            action = com.example.beatpulse.widget.MediaWidgetProvider.ACTION_UPDATE_WIDGET
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_TITLE, metadata?.title?.toString() ?: "No playing")
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_ARTIST, metadata?.artist?.toString() ?: "DuWave")
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_IS_PLAYING, player.isPlaying)
            
            val coverPath = metadata?.artworkUri?.toString()?.removePrefix("file://")
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_COVER_PATH, coverPath)
            putExtra(com.example.beatpulse.widget.MediaWidgetProvider.EXTRA_POSITION, player.currentPosition)
        }
        sendBroadcast(intent2)
    }

    override fun onDestroy() {
        try {
            presetReverb?.release()
            presetReverb = null
        } catch (e: Exception) { }
        equalizerManager.release()
        audioSessionIdFlow.value = C.AUDIO_SESSION_ID_UNSET
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }
}
