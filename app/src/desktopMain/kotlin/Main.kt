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

                if (keyEvent.key == androidx.compose.ui.input.key.Key.Tab) {
                    if (keyEvent.isShiftPressed) {
                        com.example.beatpulse.core.focus.AppFocusManager.cycleTabNavigation(currentPage = prefs.lastMainScreenPage, forward = false)
                    } else {
                        println("DEBUG ACTION: Tab detected. Current page: ${prefs.lastMainScreenPage}, Forward: true")
                        com.example.beatpulse.core.focus.AppFocusManager.cycleTabNavigation(currentPage = prefs.lastMainScreenPage, forward = true)
                    }
                    consumed = true
                } else if (com.example.beatpulse.core.focus.AppFocusManager.isTabNavigationActive) {
                    // Only intercept unmodified arrow keys for focus navigation
                    if (!keyEvent.isShiftPressed && !keyEvent.isAltPressed && !keyEvent.isCtrlPressed && !keyEvent.isMetaPressed) {
                        val action = when (keyEvent.key) {
                            androidx.compose.ui.input.key.Key.DirectionUp -> com.example.beatpulse.core.focus.FocusAction.NAVIGATE_UP
                            androidx.compose.ui.input.key.Key.DirectionDown -> com.example.beatpulse.core.focus.FocusAction.NAVIGATE_DOWN
                            androidx.compose.ui.input.key.Key.DirectionLeft -> com.example.beatpulse.core.focus.FocusAction.NAVIGATE_LEFT
                            androidx.compose.ui.input.key.Key.DirectionRight -> com.example.beatpulse.core.focus.FocusAction.NAVIGATE_RIGHT
                            androidx.compose.ui.input.key.Key.Enter, androidx.compose.ui.input.key.Key.Spacebar -> com.example.beatpulse.core.focus.FocusAction.ACTION_ENTER
                            else -> null
                        }
                        if (action != null) {
                            println("DEBUG ACTION: isTabNavigationActive=true. Arrow detected: $action")
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchAction(action)
                            consumed = true
                        }
                    }
                }

                if (!consumed && (keyEvent.key == androidx.compose.ui.input.key.Key.DirectionRight || keyEvent.key == androidx.compose.ui.input.key.Key.DirectionLeft)) {
                    if (eventStr != prefs.keyMapNextPage && eventStr != prefs.keyMapPrevPage) {
                        // For now, to prevent the crash, we MUST return true if they are on Library screen and not using Shift.
                        if (prefs.lastMainScreenPage == 0 || prefs.lastMainScreenPage == 1) { // 0 == Unified Library, 1 == Global Lists
                            val direction = if (keyEvent.key == androidx.compose.ui.input.key.Key.DirectionRight) 1 else -1
                            println("DEBUG ACTION: Dispatching Library Navigation. Direction: $direction")
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchAction(
                                if (direction == 1) com.example.beatpulse.core.focus.FocusAction.NAVIGATE_RIGHT 
                                else com.example.beatpulse.core.focus.FocusAction.NAVIGATE_LEFT
                            )
                            consumed = true
                        } else if (prefs.lastMainScreenPage == 2) { // 2 == Player
                            // Dispatch explicitly to Player for seeking
                            val direction = if (keyEvent.key == androidx.compose.ui.input.key.Key.DirectionRight) 1 else -1
                            println("DEBUG ACTION: Dispatching Player Seeking. Direction: $direction")
                            com.example.beatpulse.core.focus.AppFocusManager.dispatchAction(
                                if (direction == 1) com.example.beatpulse.core.focus.FocusAction.NAVIGATE_RIGHT 
                                else com.example.beatpulse.core.focus.FocusAction.NAVIGATE_LEFT
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
