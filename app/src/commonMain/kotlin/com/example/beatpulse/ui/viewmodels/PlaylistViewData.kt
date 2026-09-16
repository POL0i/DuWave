package com.example.beatpulse.ui.viewmodels

import com.example.beatpulse.data.TrackEntity

data class PlaylistViewData(val title: String, val tracks: List<TrackEntity>, val playlistId: Long? = null, val filterType: Int? = null, val filterValue: String? = null)
