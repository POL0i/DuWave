package com.example.beatpulse

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import com.example.beatpulse.DummyAppPreferences
import com.example.beatpulse.RealDesktopEqualizerManager
import com.example.beatpulse.DesktopPlayerViewModel
import com.example.beatpulse.RealDesktopVisualizerManager
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
    val platformHelper = com.example.beatpulse.data.DesktopLibraryPlatformHelper()
    val libraryViewModel = com.example.beatpulse.ui.screens.LibraryViewModel(platformHelper, musicRepository, onlineRepository, prefs)
    val playerViewModel = DesktopPlayerViewModel()
    val statsViewModel = com.example.beatpulse.ui.screens.StatsViewModel(musicRepository)
    
    val visualizerManager = RealDesktopVisualizerManager()
    val equalizerManager = RealDesktopEqualizerManager()

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
                onClick = { playerViewModel.seekToNext() }
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
                            playerViewModel.seekToNext()
                            true
                        }
                        androidx.compose.ui.input.key.Key.DirectionLeft -> {
                            playerViewModel.seekToPrevious()
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
        com.example.beatpulse.theme.BeatPulseTheme { com.example.beatpulse.ui.AppScreen(
            prefs = prefs,
            libraryViewModel = libraryViewModel,
            playerViewModel = playerViewModel,
            statsViewModel = statsViewModel,
            visualizerManager = visualizerManager,
            equalizerManager = equalizerManager,
            
        )
    }
}
}
