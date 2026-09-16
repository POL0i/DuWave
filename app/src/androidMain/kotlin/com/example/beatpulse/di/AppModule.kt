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
    single<com.example.beatpulse.data.AppPreferences> { PreferencesManager.getInstance(androidContext()) }
    single { PreferencesManager.getInstance(androidContext()) }
    single<com.example.beatpulse.data.ILibraryPlatformHelper> { com.example.beatpulse.data.AndroidLibraryPlatformHelper(androidContext()) }
    single { com.example.beatpulse.data.getAppDatabase(androidContext()) }
    single<com.example.beatpulse.data.ILibraryScanner> { com.example.beatpulse.data.AndroidLibraryScanner(androidContext(), get<com.example.beatpulse.data.AppDatabase>().trackDao()) }
    single { MusicRepository(get(), get(), get()) }
    single { OnlineMusicRepository() }
    single<com.example.beatpulse.data.IOnlineMusicRepository> { get<OnlineMusicRepository>() }
    single { AudioVisualizerManager(get()) }
    single { EqualizerManager(get()) }
    
    // ViewModels
    viewModel { PlayerViewModel(get(), get(), get()) }
    viewModel { LibraryViewModel(get(), get(), get(), get()) }
    viewModel { StatsViewModel(get()) }
}
