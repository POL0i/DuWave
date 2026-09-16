package com.example.beatpulse

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.input.key.*

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
    val platformHelper = com.example.beatpulse.data.DesktopLibraryPlatformHelper(prefs, libraryScanner)
    val libraryViewModel = com.example.beatpulse.ui.screens.LibraryViewModel(platformHelper, musicRepository, onlineRepository, prefs)
    
    org.koin.core.context.startKoin {
        modules(org.koin.dsl.module {
            single { db }
            single { musicRepository }
            single { prefs }
        })
    }
    val visualizerManager = RealDesktopVisualizerManager(prefs)
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
        icon = painterResource("drawable/logo.png"),
        onPreviewKeyEvent = { keyEvent ->
            if (keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                val keyName = keyEvent.key.toString()
                val shift = if (keyEvent.isShiftPressed) "Shift+" else ""
                val alt = if (keyEvent.isAltPressed) "Alt+" else ""
                val ctrl = if (keyEvent.isCtrlPressed) "Ctrl+" else ""
                val meta = if (keyEvent.isMetaPressed) "Meta+" else ""
                
                val baseKey = when(keyEvent.key) {
                    androidx.compose.ui.input.key.Key.DirectionRight -> "DirectionRight"
                    androidx.compose.ui.input.key.Key.DirectionLeft -> "DirectionLeft"
                    androidx.compose.ui.input.key.Key.DirectionUp -> "DirectionUp"
                    androidx.compose.ui.input.key.Key.DirectionDown -> "DirectionDown"
                    androidx.compose.ui.input.key.Key.Enter, androidx.compose.ui.input.key.Key.NumPadEnter -> "Enter"
                    androidx.compose.ui.input.key.Key.Spacebar -> "Spacebar"
                    androidx.compose.ui.input.key.Key.Escape -> "Escape"
                    androidx.compose.ui.input.key.Key.Tab -> "Tab"
                    else -> {
                        val name = keyEvent.key.toString()
                        var ext = name.substringAfterLast("Key: ").substringBefore(")")
                        if (!name.contains("Key:")) ext = keyEvent.key.nativeKeyCode.toString()
                        if (ext.contains("Unknown")) ext = "Unknown"
                        ext
                    }
                }
                
                val eventStr = "$ctrl$alt$meta$shift$baseKey"

                println("DEBUG KEY: Raw=${keyEvent.key}, Parsed=${eventStr}, Next=${prefs.keyMapNextPage}, Prev=${prefs.keyMapPrevPage}")

                // Global consumption of Arrow keys to prevent Focus Search crashes on desktop Compose Pagers
                var consumed = false

                if (keyEvent.key == androidx.compose.ui.input.key.Key.Escape) {
                    com.example.beatpulse.core.focus.AppFocusManager.cancelTabNavigation()
                }

                if (keyEvent.isShiftPressed) {
                    when (keyEvent.key) {
                        androidx.compose.ui.input.key.Key.L -> {
                            prefs.lastLibraryGeneralTab = 0
                            if (prefs.lastMainScreenPage == 1) com.example.beatpulse.core.focus.AppFocusManager.dispatchAction(com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_GLOBAL_TODOS)
                            prefs.lastMainScreenPage = 1
                            consumed = true
                        }
                        androidx.compose.ui.input.key.Key.K -> {
                            prefs.lastLibraryGeneralTab = 1
                            if (prefs.lastMainScreenPage == 1) com.example.beatpulse.core.focus.AppFocusManager.dispatchAction(com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_GLOBAL_NAVEGADOR)
                            prefs.lastMainScreenPage = 1
                            consumed = true
                        }
                        androidx.compose.ui.input.key.Key.J -> {
                            prefs.lastLibraryGeneralTab = 2
                            if (prefs.lastMainScreenPage == 1) com.example.beatpulse.core.focus.AppFocusManager.dispatchAction(com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_GLOBAL_RECOMENDACIONES)
                            prefs.lastMainScreenPage = 1
                            consumed = true
                        }
                        androidx.compose.ui.input.key.Key.M -> {
                            prefs.lastLibraryTab = 0
                            if (prefs.lastMainScreenPage == 0) com.example.beatpulse.core.focus.AppFocusManager.dispatchAction(com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_LIBRARY_PLAYLISTS)
                            prefs.lastMainScreenPage = 0
                            consumed = true
                        }
                        androidx.compose.ui.input.key.Key.N -> {
                            prefs.lastLibraryTab = 1
                            if (prefs.lastMainScreenPage == 0) com.example.beatpulse.core.focus.AppFocusManager.dispatchAction(com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_LIBRARY_ARTISTS)
                            prefs.lastMainScreenPage = 0
                            consumed = true
                        }
                        androidx.compose.ui.input.key.Key.B -> {
                            prefs.lastLibraryTab = 2
                            if (prefs.lastMainScreenPage == 0) com.example.beatpulse.core.focus.AppFocusManager.dispatchAction(com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_LIBRARY_ALBUMS)
                            prefs.lastMainScreenPage = 0
                            consumed = true
                        }
                        androidx.compose.ui.input.key.Key.V -> {
                            prefs.lastLibraryTab = 3
                            if (prefs.lastMainScreenPage == 0) com.example.beatpulse.core.focus.AppFocusManager.dispatchAction(com.example.beatpulse.core.focus.FocusAction.NAVIGATE_TO_LIBRARY_FOLDERS)
                            prefs.lastMainScreenPage = 0
                            consumed = true
                        }
                        else -> {}
                    }
                }

                if (!consumed && (keyEvent.key == androidx.compose.ui.input.key.Key.DirectionRight || keyEvent.key == androidx.compose.ui.input.key.Key.DirectionLeft)) {
                    if (eventStr != prefs.keyMapNextPage && eventStr != prefs.keyMapPrevPage) {
                        if (!com.example.beatpulse.core.focus.AppFocusManager.isTabNavigationActive) {
                            val direction = if (keyEvent.key == androidx.compose.ui.input.key.Key.DirectionRight) 1 else -1
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchAction(
                                if (direction == 1) com.example.beatpulse.core.focus.FocusAction.NAVIGATE_RIGHT 
                                else com.example.beatpulse.core.focus.FocusAction.NAVIGATE_LEFT,
                                withCoyoteTime = true
                            )
                            consumed = true
                        }
                    }
                }

                if (consumed) {
                    true
                } else {
                    when (eventStr) {
                    prefs.keyMapNextPage -> {
                        prefs.lastMainScreenPage = (prefs.lastMainScreenPage + 1) % 3
                        true
                    }
                    prefs.keyMapPrevPage -> {
                        prefs.lastMainScreenPage = (prefs.lastMainScreenPage + 2) % 3 // equivalent to (val - 1) % 3 safely
                        true
                    }
                    prefs.keyMapPlayPause -> {
                        playerViewModel.togglePlayPause()
                        true
                    }
                    else -> {
                        // Fallback global shortcuts for legacy behaviors if needed,
                        // but avoid capturing plain arrows which are now for focus navigation.
                        when (keyEvent.key) {
                            androidx.compose.ui.input.key.Key.S, androidx.compose.ui.input.key.Key.X, androidx.compose.ui.input.key.Key.C -> {
                                if (keyEvent.isAltPressed) {
                                    playerViewModel.triggerStreamConfigDialog()
                                    true
                                } else false
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
        val bgStyle by prefs.backgroundStyleFlow.collectAsState()
        
        androidx.compose.runtime.LaunchedEffect(isMicModeActive, selectedAudioDevice) {
            visualizerManager.stopMicMode()
            if (isMicModeActive) {
                visualizerManager.startMicMode(selectedAudioDevice ?: "")
            }
        }

        com.example.beatpulse.theme.BeatPulseTheme(isPixelArt = bgStyle == 8 || bgStyle == 4) { com.example.beatpulse.ui.AppScreen(
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
