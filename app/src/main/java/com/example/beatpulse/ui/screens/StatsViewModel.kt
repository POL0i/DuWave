package com.example.beatpulse.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beatpulse.data.AlbumStats
import com.example.beatpulse.data.ArtistStats
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.TrackEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    data class StatsState(
        val isLoading: Boolean = true,
        val totalListeningTimeMs: Long = 0,
        val totalTracksPlayed: Int = 0,
        val totalPlayCount: Int = 0,
        val uniqueArtists: Int = 0,
        val uniqueAlbums: Int = 0,
        val topArtists: List<ArtistStats> = emptyList(),
        val topAlbums: List<AlbumStats> = emptyList(),
        val topTracks: List<TrackEntity> = emptyList(),
        val favoriteTracks: List<TrackEntity> = emptyList()
    )

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _state.value = StatsState(isLoading = true)
            try {
                _state.value = StatsState(
                    isLoading = false,
                    totalListeningTimeMs = repository.getTotalListeningTimeMs(),
                    totalTracksPlayed = repository.getTotalTracksPlayed(),
                    totalPlayCount = repository.getTotalPlayCount(),
                    uniqueArtists = repository.getUniqueArtistsPlayed(),
                    uniqueAlbums = repository.getUniqueAlbumsPlayed(),
                    topArtists = repository.getTopArtistsByPlayCount(),
                    topAlbums = repository.getTopAlbumsByPlayCount(),
                    topTracks = repository.getTop5Tracks(),
                    favoriteTracks = repository.getFavoriteTracksList()
                )
            } catch (e: Exception) {
                _state.value = StatsState(isLoading = false)
            }
        }
    }
}
