package com.example.beatpulse

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.beatpulse.ui.screens.DesktopAppScreen
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import com.example.beatpulse.di.DummyAppPreferences
import com.example.beatpulse.di.DummyVisualizerManager
import com.example.beatpulse.audio.RealDesktopEqualizerManager
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.res.painterResource
import com.example.beatpulse.player.AppPlayer
import com.example.beatpulse.data.DesktopLibraryPlatformHelper
import com.example.beatpulse.data.DesktopLibraryScanner
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.getAppDatabase
import com.example.beatpulse.ui.screens.LibraryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.example.beatpulse.player.DesktopPlayerAdapter
import com.example.beatpulse.ui.components.player.DesktopPlayerViewModel
import com.example.beatpulse.data.OnlineMusicRepository
import org.schabi.newpipe.extractor.NewPipe
import com.example.beatpulse.data.NewPipeDownloader
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.localization.ContentCountry
fun main() = application {
    NewPipe.init(NewPipeDownloader.getInstance(OkHttpClient.Builder()), Localization.DEFAULT, ContentCountry.DEFAULT)
    
    val prefs = DummyAppPreferences()
    val db = getAppDatabase()
    val libraryScanner = DesktopLibraryScanner(db.trackDao())
    val musicRepository = MusicRepository(db, libraryScanner, prefs)
    val onlineRepository = OnlineMusicRepository()
    val platformHelper = DesktopLibraryPlatformHelper()
    val libraryViewModel = LibraryViewModel(platformHelper, musicRepository, onlineRepository, prefs)
    
    val appPlayer = DesktopPlayerAdapter()
    val playerViewModel = DesktopPlayerViewModel(appPlayer, musicRepository, prefs)
    
    val visualizerManager = com.example.beatpulse.visualizer.RealDesktopVisualizerManager()
    val equalizerManager = RealDesktopEqualizerManager()

    appPlayer.equalizerManager = equalizerManager
    appPlayer.audioDataCallback = { buffer ->
        visualizerManager.processAudioBytes(buffer)
    }

    val trayState = rememberTrayState()
    Tray(
        state = trayState,
        icon = painterResource("drawable/logo.xml"),
        menu = {
            Item(
                "Play / Pause",
                onClick = { playerViewModel.togglePlayPause() }
            )
            Item(
                "Siguiente",
                onClick = { playerViewModel.playNext() }
            )
            Separator()
            Item(
                "Salir",
                onClick = ::exitApplication
            )
        }
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "DuWave",
        onPreviewKeyEvent = { keyEvent ->
            if (keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                if (keyEvent.isAltPressed) {
                    when (keyEvent.key) {
                        androidx.compose.ui.input.key.Key.DirectionRight -> {
                            playerViewModel.playNext()
                            true
                        }
                        androidx.compose.ui.input.key.Key.DirectionLeft -> {
                            playerViewModel.playPrevious()
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.key == androidx.compose.ui.input.key.Key.Spacebar) {
                    playerViewModel.togglePlayPause()
                    true
                } else false
            } else false
        }
    ) {
        DesktopAppScreen(
            prefs = prefs,
            libraryViewModel = libraryViewModel,
            playerViewModel = playerViewModel,
            visualizerManager = visualizerManager,
            equalizerManager = equalizerManager,
            appPlayer = appPlayer
        )
    }
}
