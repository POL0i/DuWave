package com.example.beatpulse

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key

import com.example.beatpulse.audio.RealDesktopEqualizerManager
import com.example.beatpulse.ui.components.player.DesktopPlayerViewModel
import com.example.beatpulse.visualizer.RealDesktopVisualizerManager
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    
    val prefs = DesktopAppPreferences()
    val db = getAppDatabase()
    val libraryScanner = DesktopLibraryScanner(db.trackDao())
    val musicRepository = MusicRepository(db, libraryScanner, prefs)
    val onlineRepository = OnlineMusicRepository()
    val platformHelper = com.example.beatpulse.data.DesktopLibraryPlatformHelper(prefs)
    val libraryViewModel = com.example.beatpulse.ui.screens.LibraryViewModel(platformHelper, musicRepository, onlineRepository, prefs)
    val visualizerManager = RealDesktopVisualizerManager()
    visualizerManager.start(0)
    val equalizerManager = RealDesktopEqualizerManager()

    val appPlayer = com.example.beatpulse.player.DesktopPlayerAdapter().apply {
        this.equalizerManager = equalizerManager
        this.audioDataCallback = { bytes ->
            visualizerManager.processAudioBytes(bytes)
        }
    }
    
    val playerViewModel = DesktopPlayerViewModel(appPlayer, musicRepository, onlineRepository, prefs)
    val statsViewModel = com.example.beatpulse.ui.screens.StatsViewModel(musicRepository)

    val trayState = rememberTrayState()
    if (androidx.compose.ui.window.isTraySupported) {
        Tray(
            state = trayState,
            icon = painterResource("drawable/logo.png"),
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
    }

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
                } else {
                    when (keyEvent.key) {
                        androidx.compose.ui.input.key.Key.DirectionRight -> {
                            println("RIGHT ARROW PRESSED!")
                            prefs.lastMainScreenPage = (prefs.lastMainScreenPage + 1).coerceAtMost(2)
                            true
                        }
                        androidx.compose.ui.input.key.Key.DirectionLeft -> {
                            println("LEFT ARROW PRESSED!")
                            prefs.lastMainScreenPage = (prefs.lastMainScreenPage - 1).coerceAtLeast(0)
                            true
                        }
                        androidx.compose.ui.input.key.Key.Spacebar -> {
                            playerViewModel.togglePlayPause()
                            true
                        }
                        androidx.compose.ui.input.key.Key.S, androidx.compose.ui.input.key.Key.X, androidx.compose.ui.input.key.Key.C -> {
                            playerViewModel.triggerStreamConfigDialog()
                            true
                        }
                        androidx.compose.ui.input.key.Key.Escape -> {
                            playerViewModel.triggerSettingsMenu()
                            true
                        }
                        androidx.compose.ui.input.key.Key.F1 -> {
                            playerViewModel.triggerSupportDialog()
                            true
                        }
                        else -> false
                    }
                }
            } else false
        }
    ) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            prefs.toastFlow.collect { message ->
                if (androidx.compose.ui.window.isTraySupported) {
                    trayState.sendNotification(androidx.compose.ui.window.Notification("DuWave", message))
                }
            }
        }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val home = System.getProperty("user.home")
                val musicDirs = listOf("Music", "Música", "Downloads", "Descargas")
                for (dirName in musicDirs) {
                    val dir = java.io.File(home, dirName)
                    if (dir.exists() && dir.isDirectory) {
                        musicRepository.scanLocalLibrary(dir.absolutePath)
                    }
                }
            }
        }
        
        val isMicModeActive by playerViewModel.isMicModeActive.collectAsState()
        val selectedAudioDevice by playerViewModel.selectedAudioDevice.collectAsState()
        
        androidx.compose.runtime.LaunchedEffect(isMicModeActive, selectedAudioDevice) {
            visualizerManager.stopMicMode()
            if (isMicModeActive) {
                visualizerManager.startMicMode(selectedAudioDevice ?: "")
            }
        }

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
