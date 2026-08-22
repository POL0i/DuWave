package com.example.beatpulse.di

import com.example.beatpulse.data.AppDatabase
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.PreferencesManager
import com.example.beatpulse.service.EqualizerManager
import com.example.beatpulse.ui.components.player.PlayerViewModel
import com.example.beatpulse.ui.screens.LibraryViewModel
import com.example.beatpulse.ui.screens.StatsViewModel
import com.example.beatpulse.visualizer.AudioVisualizerManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

import com.example.beatpulse.data.OnlineMusicRepository

val appModule = module {
    single { PreferencesManager.getInstance(androidContext()) }
    single { AppDatabase.getDatabase(androidContext()) }
    single { MusicRepository(androidContext()) }
    single { OnlineMusicRepository(androidContext()) }
    single { AudioVisualizerManager(get()) }
    single { EqualizerManager(get()) }
    
    // ViewModels
    viewModel { PlayerViewModel(get(), get(), get()) }
    viewModel { LibraryViewModel(get(), get(), get(), get()) }
    viewModel { StatsViewModel(get()) }
}
