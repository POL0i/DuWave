package com.example.beatpulse

import android.app.Application


import org.schabi.newpipe.extractor.NewPipe
import com.example.beatpulse.data.NewPipeDownloader
import okhttp3.OkHttpClient

import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.localization.ContentCountry

import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.example.beatpulse.di.appModule

class BeatPulseApp : Application() {
    companion object {
        lateinit var appContext: Application
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        NewPipe.init(NewPipeDownloader.getInstance(OkHttpClient.Builder()), Localization.DEFAULT, ContentCountry.DEFAULT)
        
        startKoin {
            androidContext(this@BeatPulseApp)
            modules(appModule)
        }
    }
}
