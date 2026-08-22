package com.example.beatpulse.ui.components.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.visualizer.AudioVisualizerManager

class PlayerViewModelFactory(
    private val context: Context,
    private val repository: MusicRepository,
    private val visualizerManager: AudioVisualizerManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(context.applicationContext, repository, visualizerManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
