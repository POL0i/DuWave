package com.example.beatpulse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.lazy.items
import com.example.beatpulse.data.PlaylistEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.example.beatpulse.ui.screens.UnifiedLibraryScreen
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.beatpulse.ui.components.backgrounds.CyberpunkBackground
import com.example.beatpulse.ui.components.backgrounds.AnimeBackground
import com.example.beatpulse.ui.components.backgrounds.Y2KBackground
import com.example.beatpulse.ui.components.backgrounds.DarkAmbientBackground
import com.example.beatpulse.ui.components.backgrounds.GothicFantasyBackground
import com.example.beatpulse.ui.components.backgrounds.CathedralFantasyBackground
import com.example.beatpulse.ui.components.backgrounds.TaleLegendBackground
import com.example.beatpulse.ui.components.StyleNotificationOverlay
import com.example.beatpulse.ui.components.PixelIcons

import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.theme.BeatPulseTheme
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.player.PlayerScreen
import com.example.beatpulse.ui.screens.AlbumsScreen
import com.example.beatpulse.ui.screens.LibraryScreen
import com.example.beatpulse.visualizer.AudioVisualizerManager
import com.example.beatpulse.data.PreferencesManager
import com.example.beatpulse.ui.components.player.PlayerViewModel
import com.example.beatpulse.ui.components.player.PlayerViewModelFactory
import com.example.beatpulse.player.ExoPlayerAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first


import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.atomic.AtomicBoolean

object NavigationKeys {
    const val LIBRARY = "library"
    const val FOLDERS = "folders"
    const val PLAYER = "player"
}

class MainActivity : ComponentActivity() {

    companion object {
        var instance: MainActivity? = null
    }

    private val visualizerManager: AudioVisualizerManager by inject()
    private val prefs: com.example.beatpulse.data.PreferencesManager by inject()
    private val musicRepository: MusicRepository by inject()
    private val equalizerManager: com.example.beatpulse.service.EqualizerManager by inject()

    private val playerViewModel: com.example.beatpulse.ui.components.player.PlayerViewModel by viewModel()
    private val libraryViewModel: com.example.beatpulse.ui.screens.LibraryViewModel by viewModel()
    private val statsViewModel: com.example.beatpulse.ui.screens.StatsViewModel by viewModel()
    private val isSetupDone = AtomicBoolean(false)
    
    private val downloadReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L && context != null) {
                    val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                    val query = android.app.DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val titleIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TITLE)
                        val statusIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)
                        if (titleIndex >= 0 && statusIndex >= 0) {
                            val title = cursor.getString(titleIndex)
                            val status = cursor.getInt(statusIndex)
                            if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                                android.widget.Toast.makeText(this@MainActivity, getString(R.string.download_completed_desc, title), android.widget.Toast.LENGTH_LONG).show()
                                kotlinx.coroutines.GlobalScope.launch {
                                    libraryViewModel.scanMediaStore()
                                }
                            }
                        }
                        cursor.close()
                    }
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] ?: false
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        }

        if (storageGranted) {
            setupApp()
        } else {
            Toast.makeText(this, "Se requiere permiso de almacenamiento para buscar tu música.", Toast.LENGTH_LONG).show()
        }
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val sharedPrefs = newBase.getSharedPreferences("beatpulse_prefs", android.content.Context.MODE_PRIVATE)
        val lang = sharedPrefs.getString("appLanguage", "es") ?: "es"
        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        enableEdgeToEdge()
        
        // VisualizerState is handled in commonMain now
        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
        
        checkPermissionsAndSetup()

        setContent {
            val bgStyle by prefs.backgroundStyleFlow.collectAsState(initial = 0)
            
            var localeCode by remember { mutableStateOf(prefs.appLanguage) }
            val sharedPrefsListener = remember {
                android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    if (key == "appLanguage") {
                        localeCode = sharedPreferences.getString(key, "es") ?: "es"
                    }
                }
            }
            androidx.compose.runtime.DisposableEffect(Unit) {
                val sp = getSharedPreferences("beatpulse_prefs", android.content.Context.MODE_PRIVATE)
                sp.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
                onDispose {
                    sp.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
                }
            }
            
            val currentConfig = androidx.compose.ui.platform.LocalConfiguration.current
            val context = androidx.compose.ui.platform.LocalContext.current
            val updatedConfig = remember(localeCode, currentConfig) {
                val newConfig = android.content.res.Configuration(currentConfig).apply {
                    setLocale(java.util.Locale(localeCode))
                }
                val res = context.resources
                val resConfig = android.content.res.Configuration(res.configuration).apply {
                    setLocale(java.util.Locale(localeCode))
                }
                @Suppress("DEPRECATION")
                res.updateConfiguration(resConfig, res.displayMetrics)
                newConfig
            }
            
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalConfiguration provides updatedConfig
            ) {
                BeatPulseTheme(isPixelArt = bgStyle == 8) {
                    androidx.compose.animation.Crossfade(
                        targetState = localeCode,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600),
                        label = "language_crossfade"
                    ) { _ ->
                        com.example.beatpulse.ui.AppScreen(
                            visualizerManager = visualizerManager,
                            equalizerManager = equalizerManager,
                            prefs = prefs,
                            libraryViewModel = libraryViewModel,
                            playerViewModel = playerViewModel,
                            statsViewModel = statsViewModel
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        playerViewModel.isUiVisible = true
        visualizerManager.isEnabled = true
        visualizerManager.start(0)
    }

    override fun onPause() {
        super.onPause()
        playerViewModel.isUiVisible = false
        visualizerManager.isEnabled = false
        visualizerManager.stop(decay = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver)
        if (instance == this) instance = null
    }

    private fun checkPermissionsAndSetup() {
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val missingPermissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(storagePermission)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            setupApp()
        }
    }

    private fun setupApp() {
        if (!isSetupDone.compareAndSet(false, true)) return // Guard against duplicate calls
        lifecycleScope.launch {
            val existing = musicRepository.allTracksFlow.first()
            if (existing.isEmpty()) {
                libraryViewModel.scanMediaStore()
            }
        }
        
        lifecycleScope.launch {
            com.example.beatpulse.service.PlaybackService.audioSessionIdFlow.collect { sessionId ->
                if (sessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET) {
                    equalizerManager.initialize(sessionId)
                    kotlinx.coroutines.delay(100)
                    visualizerManager.start(sessionId)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Always restart visualizer on return — isPlaying may be stale during MediaController reconnection
        val sessionId = com.example.beatpulse.service.PlaybackService.audioSessionIdFlow.value
        if (sessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET) {
            visualizerManager.start(sessionId)
        }
    }

    override fun onStop() {
        super.onStop()
        // Only stop with decay; the visualizer will be restarted in onStart()
        visualizerManager.stop()
    }
}

