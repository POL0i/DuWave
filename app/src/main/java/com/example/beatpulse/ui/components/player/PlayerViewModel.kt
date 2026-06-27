package com.example.beatpulse.ui.components.player

import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.service.PlaybackService
import com.example.beatpulse.data.PreferencesManager
import com.example.beatpulse.ui.components.PaletteCache
import com.example.beatpulse.visualizer.AudioVisualizerManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.palette.graphics.Palette
import com.example.beatpulse.theme.PaletteColors
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context

import android.annotation.SuppressLint

@SuppressLint("StaticFieldLeak")
class PlayerViewModel(
    private val context: Context,
    private val repository: MusicRepository,
    val visualizerManager: AudioVisualizerManager
) : ViewModel() {

    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _playerState = MutableStateFlow<Player?>(null)
    val playerState: StateFlow<Player?> = _playerState

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val currentQueue: StateFlow<List<TrackEntity>> = _currentQueue

    private val _paletteColors = MutableStateFlow(PaletteColors())
    val paletteColors: StateFlow<PaletteColors> = _paletteColors

    init {
        setupPlayer()
    }

    private fun setupPlayer() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val controller = controllerFuture?.get()
            _playerState.value = controller
            
            controller?.let { player ->
                val prefs = PreferencesManager.getInstance(context)
                player.shuffleModeEnabled = prefs.shuffleModeEnabled
                player.repeatMode = prefs.repeatMode
            }
            
            // Restore current or last track
            viewModelScope.launch {
                val currentMediaItem = controller?.currentMediaItem
                if (currentMediaItem != null) {
                    val trackId = currentMediaItem.mediaId.toLongOrNull()
                    if (trackId != null) {
                        val allTracks = repository.allTracksFlow.first()
                        val track = allTracks.find { it.id == trackId }
                        if (track != null) {
                            _currentTrack.value = track
                            extractColors(track)
                        }
                    }
                } else {
                    val recents = repository.recentTracksFlow.first()
                    if (recents.isNotEmpty()) {
                        val lastTrack = recents.first()
                        _currentTrack.value = lastTrack
                        _currentQueue.value = recents
                        extractColors(lastTrack)
                        
                        val mediaItem = MediaItem.Builder()
                            .setUri(android.net.Uri.parse(lastTrack.dataPath))
                            .setRequestMetadata(
                                MediaItem.RequestMetadata.Builder()
                                    .setMediaUri(android.net.Uri.parse(lastTrack.dataPath))
                                    .build()
                            )
                            .setMediaId(lastTrack.id.toString())
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(lastTrack.title)
                                    .setArtist(lastTrack.artist)
                                    .setIsPlayable(true)
                                    .setArtworkUri(android.net.Uri.parse("file://" + lastTrack.dataPath))
                                    .build()
                            )
                            .build()
                        controller?.setMediaItem(mediaItem)
                        controller?.prepare()
                    }
                }
            }

            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    val sessionId = PlaybackService.audioSessionIdFlow.value
                    if (isPlaying && sessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET) {
                        visualizerManager.start(sessionId)
                    } else if (!isPlaying) {
                        visualizerManager.stop()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val trackId = mediaItem?.mediaId?.toLongOrNull()
                    if (trackId != null) {
                        val track = _currentQueue.value.find { it.id == trackId }
                        if (track != null && _currentTrack.value?.id != track.id) {
                            _currentTrack.value = track
                            viewModelScope.launch {
                                repository.markAsPlayed(track.id)
                                extractColors(track)
                            }
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    super.onPlayerError(error)
                    android.widget.Toast.makeText(context, "Error al reproducir: Archivo no encontrado o dañado.", android.widget.Toast.LENGTH_LONG).show()
                    val player = _playerState.value
                    if (player != null) {
                        val index = player.currentMediaItemIndex
                        if (index != androidx.media3.common.C.INDEX_UNSET) {
                            player.removeMediaItem(index)
                            val newList = _currentQueue.value.toMutableList()
                            if (index in newList.indices) {
                                newList.removeAt(index)
                                _currentQueue.value = newList
                            }
                            player.prepare()
                            player.play()
                        }
                    }
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    PreferencesManager.getInstance(context).shuffleModeEnabled = shuffleModeEnabled
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    PreferencesManager.getInstance(context).repeatMode = repeatMode
                }
            })
        }, MoreExecutors.directExecutor())
    }

    fun playTrack(track: TrackEntity, queue: List<TrackEntity>) {
        val player = _playerState.value ?: return
        _currentTrack.value = track
        _currentQueue.value = queue
        
        viewModelScope.launch {
            repository.markAsPlayed(track.id)
            extractColors(track)
        }

        val startIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val mediaItems = queue.map { 
            MediaItem.Builder()
                .setUri(android.net.Uri.parse(it.dataPath))
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(android.net.Uri.parse(it.dataPath))
                        .build()
                )
                .setMediaId(it.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(it.customTitle ?: it.title)
                        .setArtist(it.customArtist ?: it.artist)
                        .setAlbumTitle(it.customAlbum ?: it.album)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .apply {
                            it.customCoverPath?.let { path ->
                                setArtworkUri(android.net.Uri.parse(if (path.startsWith("/")) "file://$path" else path))
                            }
                        }
                        .build()
                )
                .build()
        }
        
        player.setMediaItems(mediaItems, startIndex, androidx.media3.common.C.TIME_UNSET)
        player.prepare()
        player.playWhenReady = true
    }

    
    private suspend fun extractColors(track: TrackEntity) {
        // Check cache first — avoids re-reading the file if already processed
        PaletteCache.get(track.id)?.let {
            _paletteColors.value = it
            return
        }
        withContext(Dispatchers.IO) {
            try {
                var bitmap: android.graphics.Bitmap? = null
                if (!track.customCoverPath.isNullOrEmpty()) {
                    val uri = android.net.Uri.parse(track.customCoverPath)
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                    } catch (e: Exception) {
                        // fallback
                    }
                }
                
                if (bitmap == null) {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(track.dataPath)
                    val data = mmr.embeddedPicture
                    mmr.release()
                    if (data != null) {
                        bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    }
                }

                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val colors = PaletteColors(
                        dominant = Color(palette.getDominantColor(android.graphics.Color.DKGRAY)),
                        vibrant = Color(palette.getVibrantColor(android.graphics.Color.DKGRAY)),
                        muted = Color(palette.getMutedColor(android.graphics.Color.DKGRAY)),
                        darkVibrant = Color(palette.getDarkVibrantColor(android.graphics.Color.DKGRAY)),
                        lightVibrant = Color(palette.getLightVibrantColor(android.graphics.Color.DKGRAY)),
                        darkMuted = Color(palette.getDarkMutedColor(android.graphics.Color.DKGRAY))
                    )
                    PaletteCache.put(track.id, colors)
                    _paletteColors.value = colors
                } else {
                    _paletteColors.value = PaletteColors()
                }
            } catch (e: Exception) {
                _paletteColors.value = PaletteColors()
            }
        }
    }

    fun updateTrackMetadata(id: Long, title: String?, artist: String?, album: String?, coverPath: String?) {
        viewModelScope.launch {
            repository.updateTrackMetadata(id, title, artist, album, coverPath)
            // Update current track if it's the one playing
            if (_currentTrack.value?.id == id) {
                _currentTrack.value = _currentTrack.value?.copy(
                    customTitle = title,
                    customArtist = artist,
                    customAlbum = album,
                    customCoverPath = coverPath
                )
                // Re-extract colors if cover changed
                _currentTrack.value?.let { extractColors(it) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        visualizerManager.stop()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        _playerState.value = null
    }
}
