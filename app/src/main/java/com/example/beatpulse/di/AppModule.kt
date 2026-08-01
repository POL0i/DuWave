package com.example.beatpulse.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.example.beatpulse.data.AppDatabase
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.PreferencesManager
import com.example.beatpulse.visualizer.AudioVisualizerManager
import com.example.beatpulse.service.EqualizerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Removed JioSaavnApiClient

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideMusicDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideMusicRepository(
        @ApplicationContext context: Context
    ): MusicRepository {
        return MusicRepository(context)
    }

    @Provides
    @Singleton
    fun provideAudioVisualizerManager(prefs: PreferencesManager): AudioVisualizerManager {
        return AudioVisualizerManager(prefs)
    }

    @Provides
    @Singleton
    fun provideEqualizerManager(prefs: PreferencesManager): EqualizerManager {
        return EqualizerManager(prefs)
    }
}
