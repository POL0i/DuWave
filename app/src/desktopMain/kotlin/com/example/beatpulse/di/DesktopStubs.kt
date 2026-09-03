package com.example.beatpulse.di

import com.example.beatpulse.data.AppPreferences
import com.example.beatpulse.data.IOnlineMusicRepository
import com.example.beatpulse.data.PlaylistEntity
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.data.LrcSearchResult
import com.example.beatpulse.ui.viewmodels.ILibraryViewModel
import com.example.beatpulse.player.IPlayerViewModel
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.viewmodels.PlaylistViewData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import com.example.beatpulse.utils.LyricLine

class DummyAppPreferences : AppPreferences {
    override var appLanguage = "en"
    override var visualizerStyle = "BARS"
    override var visualizerArchetype = 0
    override var visualizerFftMode = "AVERAGE"
    override var isAdvancedMode = false
    override var visualizerBandsMode = 0
    override var filterMode = "NONE"
    override var physicsMode = "SPRING"
    override var filterWhatsAppShorts = false
    override var sensitivity = 1.0f
    override var reactivity = 1.0f
    override var bassMultiplier = 1.0f
    override var midMultiplier = 1.0f
    override var trebleMultiplier = 1.0f
    override var usePerBandMultiplier = false
    private val _lastMainScreenPageFlow = kotlinx.coroutines.flow.MutableStateFlow(0)
    override val lastMainScreenPageFlow: kotlinx.coroutines.flow.StateFlow<Int> = _lastMainScreenPageFlow
    override var lastMainScreenPage: Int
        get() = _lastMainScreenPageFlow.value
        set(value) { _lastMainScreenPageFlow.value = value }
    override var lastLibraryTab = 0
    override var lastLibraryGeneralTab = 0
    override var shuffleModeEnabled = false
    override var repeatMode = 0
    override var eqEnabled = false
    override var eqPreset: Short = 0
    override var eqCustomBands = ""
    override var eqAutoMode = false
    override var lastPlayedTrackPath: String? = null
    override var hasSeenTutorial = true
    override var hasSeenBottomBarHint = true
    override var hasSeenPlayerHints = true
    override var hasUsedMiniplayerGesture = true
    override var hasUsedCoverGesture = true
    override var hasUsedPlaylistGesture = true
    override var albumArtCenterY = 0.5f
    override var showGestureFeedback = true
    override var librarySortOrder = "DEFAULT"
    override var libraryScrollIndex = 0
    override var libraryScrollOffset = 0
    override var playbackSpeed = 1.0f
    override var playbackPitch = 1.0f
    override var reverbEnabled = false
    override var effectsPreset = "NONE"
    override var lastRecommendationsTimestamp = 0L
    override var cachedRecommendationsJson = ""
    override val backgroundStyleFlow = MutableStateFlow(0)
    override var backgroundStyle = 0
    override val thumbnailShapeFlow = MutableStateFlow(0)
    override var thumbnailShape = 0
    override val toastFlow = MutableSharedFlow<String>()
    override fun showToast(message: String) {}
    override var autoAnalyzeLyrics = false
    override var hasUsedNextPrevGesture = true
    override var hasUsedSeek10sGesture = true
    override var hasUsedVinylSeekGesture = true
    override var hasUsedPlaylistSwipeGesture = true
    override var coverOffsetX = 0f
    override var coverOffsetY = 0f
    override var showGestureConfirmations = true
    override var streamAvatarUri: String? = null
    override var lastVerifiedNewPipeVersion = "v0.26.5"
    override var lastServiceDownState = false
    override var coverScale = 1f
    
    private val _isPatreonUnlockedFlow = MutableStateFlow(false)
    override val isPatreonUnlockedFlow: StateFlow<Boolean> = _isPatreonUnlockedFlow
    override var isPatreonUnlocked: Boolean
        get() = _isPatreonUnlockedFlow.value
        set(value) { _isPatreonUnlockedFlow.value = value }
    override var showFps = false
    override var showRemainingTime = false
}





class DummyLibraryViewModel(override val prefs: AppPreferences) : com.example.beatpulse.ui.viewmodels.ILibraryViewModel {
    override val selectedViewData = mutableStateOf<PlaylistViewData?>(null)
    override val selectedUnifiedCategory = MutableStateFlow(0)
    override val selectedUnifiedGroup = MutableStateFlow<String?>(null)
    override val resolvingTracks = MutableStateFlow<Set<Long>>(emptySet())
    override val allTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    override val recentTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    override val topTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    override val recentlyAdded = MutableStateFlow<List<TrackEntity>>(emptyList())
    override val favoriteTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    override val playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    override val isScanning = MutableStateFlow(false)
    override val searchQuery = MutableStateFlow("")
    override val onlineSearchResults = MutableStateFlow<List<TrackEntity>>(emptyList())
    override val isOnlineSearchLoading = MutableStateFlow(false)
    override val recommendations = MutableStateFlow<Map<String, List<TrackEntity>>>(emptyMap())
    override val isRecommendationsLoading = MutableStateFlow(false)
    override val changeCoverSearchResults = MutableStateFlow<List<TrackEntity>>(emptyList())
    override val isChangeCoverLoading = MutableStateFlow(false)
    override val isOnlineServiceDown = MutableStateFlow(false)

    override fun scanMediaStore() {}
    override fun pickFolderAndScan() {}
    override fun toggleFavorite(track: TrackEntity, isFavorite: Boolean) {}
    override fun completeDeletion(trackId: Long) {}
    override fun updateTrackCover(track: TrackEntity, newCoverPath: String?) {}
    override fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {}
    override fun addTrackToPlaylist(playlistId: Long, track: TrackEntity) {}
    override suspend fun createPlaylist(name: String): Long = 0L
    override fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>> = emptyFlow()
    override fun updatePlaylistOrder(playlistId: Long, updates: List<Pair<Long, Int>>) {}
    override fun getPlaylistTrackCountFlow(playlistId: Long): Flow<Int> = emptyFlow()
    override fun searchCoversForTrack(track: TrackEntity) {}
    override fun loadRecommendations(forceUpdate: Boolean) {}
    override suspend fun searchOnlineMusic(query: String): List<TrackEntity> = emptyList()
    override suspend fun resolveStreamUrl(videoId: String): String? = null
    override fun reloadMissingCoversForList(list: List<TrackEntity>) {}
    override suspend fun deleteTrack(trackId: Long): Any? = null
    override fun downloadOnlineTrack(track: TrackEntity) {}
    override fun copyMetadataForTrimmedTrack(originalTrack: TrackEntity, newFilePath: String) {}
}

class DummyVisualizerManager : com.example.beatpulse.visualizer.AppVisualizerManager {
    override val bassAmplitudes = MutableStateFlow(FloatArray(0))
    override val midAmplitudes = MutableStateFlow(FloatArray(0))
    override val highAmplitudes = MutableStateFlow(FloatArray(0))
    override val combinedAmplitudes = MutableStateFlow(FloatArray(0))
    override val isAdvancedMode = MutableStateFlow(false)
    override val filterMode: MutableStateFlow<Any> = MutableStateFlow(com.example.beatpulse.visualizer.FilterMode.ALL)
    override val sensitivity = MutableStateFlow(1.0f)
    override val reactivity = MutableStateFlow(1.0f)
    override val damping = MutableStateFlow(0.8f)
    override val bassMultiplier = MutableStateFlow(1.0f)
    override val midMultiplier = MutableStateFlow(1.0f)
    override val trebleMultiplier = MutableStateFlow(1.0f)
    override val visualizerArchetype = MutableStateFlow(0)
    override val fftMode = MutableStateFlow("AVERAGE")

    override fun startMicMode(context: Any) {}
    override fun stopMicMode() {}
    override var isEnabled: Boolean = false
    override fun start(sessionId: Int) {}
    override fun stop() {}
}

@Composable
fun PlayerSupportDialog(
    showSupportDialog: Boolean,
    onDismissRequest: () -> Unit,
    paletteColors: com.example.beatpulse.theme.PaletteColors,
    dynamicTextColor: androidx.compose.ui.graphics.Color,
    prefs: com.example.beatpulse.ui.components.player.IPreferencesManager
) {
    if (showSupportDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { androidx.compose.material3.Text("Support") },
            text = { androidx.compose.material3.Text("DuWave Desktop Edition by Denis.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = onDismissRequest) {
                    androidx.compose.material3.Text("OK")
                }
            }
        )
    }
}
