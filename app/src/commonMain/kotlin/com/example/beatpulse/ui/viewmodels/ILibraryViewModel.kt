package com.example.beatpulse.ui.viewmodels

import com.example.beatpulse.data.PlaylistEntity
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.data.AppPreferences
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.MutableState
import com.example.beatpulse.ui.viewmodels.PlaylistViewData

interface ILibraryViewModel {
    val prefs: AppPreferences
    val selectedViewData: MutableState<PlaylistViewData?>

    val selectedUnifiedCategory: MutableStateFlow<Int>
    val selectedUnifiedGroup: MutableStateFlow<String?>
    val resolvingTracks: StateFlow<Set<Long>>

    val allTracks: StateFlow<List<TrackEntity>>
    val recentTracks: StateFlow<List<TrackEntity>>
    val topTracks: StateFlow<List<TrackEntity>>
    val recentlyAdded: StateFlow<List<TrackEntity>>
    val favoriteTracks: StateFlow<List<TrackEntity>>
    val playlists: StateFlow<List<PlaylistEntity>>
    val isScanning: StateFlow<Boolean>

    val searchQuery: MutableStateFlow<String>
    val onlineSearchResults: MutableStateFlow<List<TrackEntity>>
    val isOnlineSearchLoading: MutableStateFlow<Boolean>
    val isOnlineServiceDown: StateFlow<Boolean>

    val recommendations: StateFlow<Map<String, List<TrackEntity>>>
    val isRecommendationsLoading: StateFlow<Boolean>

    val changeCoverSearchResults: MutableStateFlow<List<TrackEntity>>
    val isChangeCoverLoading: MutableStateFlow<Boolean>

    fun scanMediaStore()
    fun pickFolderAndScan()
    fun toggleFavorite(track: TrackEntity, isFavorite: Boolean)
    fun completeDeletion(trackId: Long)
    fun updateTrackCover(track: TrackEntity, newCoverPath: String?)
    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)
    fun addTrackToPlaylist(playlistId: Long, track: TrackEntity)
    suspend fun createPlaylist(name: String): Long
    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>
    fun updatePlaylistOrder(playlistId: Long, updates: List<Pair<Long, Int>>)
    fun getPlaylistTrackCountFlow(playlistId: Long): Flow<Int>

    fun searchCoversForTrack(track: TrackEntity)
    fun loadRecommendations(forceUpdate: Boolean = false)
    suspend fun searchOnlineMusic(query: String): List<TrackEntity>
    suspend fun resolveStreamUrl(videoId: String): String?
    fun reloadMissingCoversForList(list: List<TrackEntity>)
    
    // Abstracting intent sender to a simple Long for track ID, the platform implementation handles the OS specific logic
    suspend fun deleteTrack(trackId: Long): Any?
    fun downloadOnlineTrack(track: TrackEntity)
    fun copyMetadataForTrimmedTrack(originalTrack: TrackEntity, newFilePath: String)
}
