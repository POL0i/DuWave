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
import com.example.beatpulse.ui.components.backgrounds.VisualizerState
import com.example.beatpulse.ui.screens.AlbumsScreen
import com.example.beatpulse.ui.screens.LibraryScreen
import com.example.beatpulse.visualizer.AudioVisualizerManager
import com.example.beatpulse.data.PreferencesManager
import com.example.beatpulse.ui.components.player.PlayerViewModel
import com.example.beatpulse.ui.components.player.PlayerViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
object NavigationKeys {
    const val LIBRARY = "library"
    const val FOLDERS = "folders"
    const val PLAYER = "player"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var visualizerManager: AudioVisualizerManager
    @Inject lateinit var prefs: PreferencesManager
    @Inject lateinit var musicRepository: MusicRepository
    @Inject lateinit var equalizerManager: com.example.beatpulse.service.EqualizerManager

    private val playerViewModel: com.example.beatpulse.ui.components.player.PlayerViewModel by viewModels()
    private val libraryViewModel: com.example.beatpulse.ui.screens.LibraryViewModel by viewModels()
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
                                prefs.showToast(getString(R.string.download_completed_desc, title))
                                kotlinx.coroutines.GlobalScope.launch {
                                    musicRepository.scanMediaStore()
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
        enableEdgeToEdge()
        
        val cachedCenterY = prefs.albumArtCenterY
        if (cachedCenterY > 0f) {
            VisualizerState.albumArtCenterY = cachedCenterY
        }
        
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
                    MainScreen(
                        visualizerManager = visualizerManager,
                        equalizerManager = equalizerManager,
                        prefs = prefs,
                        libraryViewModel = libraryViewModel,
                        playerViewModel = playerViewModel
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        playerViewModel.isUiVisible = true
        if (::visualizerManager.isInitialized) {
            visualizerManager.isEnabled = true
            visualizerManager.start(0)
        }
    }

    override fun onPause() {
        super.onPause()
        playerViewModel.isUiVisible = false
        if (::visualizerManager.isInitialized) {
            visualizerManager.isEnabled = false
            visualizerManager.stop(decay = false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver)
    }

    private fun checkPermissionsAndSetup() {
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val missingPermissions = mutableListOf<String>()
        // RECORD_AUDIO is no longer requested at startup. It will be requested on-demand in Mic Mode.
        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(storagePermission)
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
                musicRepository.scanMediaStore()
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
        if (::visualizerManager.isInitialized) {
            // Only stop with decay; the visualizer will be restarted in onStart()
            visualizerManager.stop()
        }
    }
}

@Composable
fun MainScreen(
    visualizerManager: AudioVisualizerManager,
    equalizerManager: com.example.beatpulse.service.EqualizerManager,
    prefs: PreferencesManager,
    libraryViewModel: com.example.beatpulse.ui.screens.LibraryViewModel,
    playerViewModel: PlayerViewModel
) {
    val exoPlayer by playerViewModel.playerState.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val currentQueue by playerViewModel.currentQueue.collectAsState()
    val paletteColors by playerViewModel.paletteColors.collectAsState()
    val repeatModeState by playerViewModel.repeatMode.collectAsState()
    val shuffleModeState by playerViewModel.shuffleModeEnabled.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()
    val playbackPitch by playerViewModel.playbackPitch.collectAsState()
    val reverbEnabled by playerViewModel.reverbEnabled.collectAsState()
    val effectsPreset by playerViewModel.effectsPreset.collectAsState()
    
    val isMicModeActive by playerViewModel.isMicModeActive.collectAsState()
    val streamConfigEffectsVisible by playerViewModel.streamConfigEffectsVisible.collectAsState()
    val bgStyle by prefs.backgroundStyleFlow.collectAsState()
    val scope = rememberCoroutineScope()

    var globalToastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        prefs.toastFlow.collect { msg ->
            globalToastMessage = msg
        }
    }

    val dominantTint by animateColorAsState(targetValue = paletteColors.dominant.copy(alpha = 0.35f), animationSpec = tween(900), label = "dominant_tint")
    val vibrantTint by animateColorAsState(targetValue = paletteColors.vibrant.copy(alpha = 0.25f), animationSpec = tween(900), label = "vibrant_tint")
    val lightVibrantTint by animateColorAsState(targetValue = paletteColors.lightVibrant.copy(alpha = 0.25f), animationSpec = tween(1000), label = "lv")

    val accentColor by animateColorAsState(targetValue = paletteColors.vibrant, animationSpec = tween(700), label = "accent")
    val systemBg = MaterialTheme.colorScheme.background

    val bgModifier = when (bgStyle) {
        0 -> Modifier.background(systemBg).background(dominantTint).background(vibrantTint)
        3 -> Modifier.background(systemBg).background(lightVibrantTint)
        else -> Modifier.background(Color.Transparent)
    }

    var currentPage by remember { mutableIntStateOf(prefs.lastMainScreenPage) } // 0: Home, 1: Library, 2: Player
    LaunchedEffect(currentPage) {
        prefs.lastMainScreenPage = currentPage
        // Always keep visualizer enabled so background animations work across all screens
        if (!visualizerManager.isEnabled) {
            visualizerManager.isEnabled = true
            visualizerManager.start(0)
        }
    }
    var sortOrder by remember { mutableStateOf(prefs.librarySortOrder) }

    var trackToAddToPlaylist by remember { mutableStateOf<com.example.beatpulse.data.TrackEntity?>(null) }
    val playlists by libraryViewModel.playlists.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var sleepTimerSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(sleepTimerSeconds) {
        if (sleepTimerSeconds > 0) {
            while (sleepTimerSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                sleepTimerSeconds -= 1
            }
            if (exoPlayer?.isPlaying == true) {
                exoPlayer?.pause()
            }
        }
    }

    val content: @Composable () -> Unit = {
        var accumulatedDrag by remember { mutableFloatStateOf(0f) }
        Scaffold(
            containerColor = Color.Transparent,
                        bottomBar = {
                com.example.beatpulse.ui.components.BottomNavigationBar(
                    currentPage = currentPage,
                    onPageChange = { currentPage = it },
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                    paletteColors = paletteColors,
                    bgStyle = bgStyle,
                    prefs = prefs,
                    exoPlayer = exoPlayer,
                    onPlayPauseClick = { if (exoPlayer?.isPlaying == true) exoPlayer?.pause() else exoPlayer?.play() }
                )
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    // Slide animation for pages
                    val spec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(250)
                    val specFloat = androidx.compose.animation.core.tween<Float>(250)
                    if (targetState > initialState) {
                        if (initialState == 0 && targetState == 2) {
                            slideInHorizontally(animationSpec = spec) { width -> -width } + fadeIn(animationSpec = specFloat) togetherWith slideOutHorizontally(animationSpec = spec) { width -> width } + fadeOut(animationSpec = specFloat)
                        } else {
                            slideInHorizontally(animationSpec = spec) { width -> width } + fadeIn(animationSpec = specFloat) togetherWith slideOutHorizontally(animationSpec = spec) { width -> -width } + fadeOut(animationSpec = specFloat)
                        }
                    } else {
                        if (initialState == 2 && targetState == 0) {
                            slideInHorizontally(animationSpec = spec) { width -> width } + fadeIn(animationSpec = specFloat) togetherWith slideOutHorizontally(animationSpec = spec) { width -> -width } + fadeOut(animationSpec = specFloat)
                        } else {
                            slideInHorizontally(animationSpec = spec) { width -> -width } + fadeIn(animationSpec = specFloat) togetherWith slideOutHorizontally(animationSpec = spec) { width -> width } + fadeOut(animationSpec = specFloat)
                        }
                    }.using(SizeTransform(clip = false))
                },
                modifier = Modifier
                    .fillMaxSize(),
                label = "page_transition"
            ) { page ->
                when (page) {
                    0 -> Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        UnifiedLibraryScreen(
                        viewModel = libraryViewModel,
                        paletteColors = paletteColors,
                        currentPlayingTrack = currentTrack,
                        isPlaying = isPlaying,
                        onTrackClick = { track: com.example.beatpulse.data.TrackEntity, queue: List<com.example.beatpulse.data.TrackEntity> ->
                            playerViewModel.playTrack(track, queue)
                            currentPage = 2
                        }
                    )
                    }
                    1 -> Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        LibraryScreen(
                        viewModel = libraryViewModel,
                        paletteColors = paletteColors,
                        currentPlayingTrack = currentTrack,
                        isPlaying = isPlaying,
                        onTrackClick = { track, queue ->
                            playerViewModel.playTrack(track, queue)
                            currentPage = 2
                        }
                    )
                    }
                    2 -> Box(modifier = Modifier.fillMaxSize()) {
                        PlayerScreen(
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        playerViewModel = playerViewModel,
                        visualizerManager = visualizerManager,
                        equalizerManager = equalizerManager,
                        exoPlayer = exoPlayer,
                        currentTrack = currentTrack,
                        currentQueue = currentQueue,
                        onPlayTrack = { track, queue -> playerViewModel.playTrack(track, queue) },
                        paletteColors = paletteColors,
                        prefs = prefs,
                        repeatModeState = repeatModeState,
                        shuffleModeState = shuffleModeState,
                        sleepTimerSeconds = sleepTimerSeconds,
                        onSetSleepTimer = { sleepTimerSeconds = it },
                        onUpdateTrackMetadata = { id, title, artist, album, coverPath ->
                            playerViewModel.updateTrackMetadata(id, title, artist, album, coverPath)
                        },
                        onAddToPlaylist = { track ->
                            trackToAddToPlaylist = track
                        },
                        playbackSpeed = playbackSpeed,
                        playbackPitch = playbackPitch,
                        reverbEnabled = reverbEnabled,
                        effectsPreset = effectsPreset,
                        onSetSpeed = { playerViewModel.setSpeed(it) },
                        onSetPitch = { playerViewModel.setPitch(it) },
                        onSetReverb = { playerViewModel.setReverb(it) },
                        onApplyPreset = { playerViewModel.applyPreset(it) }
                    )
                    }
                }
            }
        }
    }

    val effectiveBgStyle = if (isMicModeActive && !streamConfigEffectsVisible) 0 else bgStyle

    // Wrap content with appropriate background, with AnimatedContent for smooth style transitions!
    AnimatedContent(
        targetState = effectiveBgStyle,
        transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) },
        label = "bg_transition"
    ) { style ->
        when (style) {
            1 -> CyberpunkBackground(
                paletteColors = paletteColors,
                visualizerManager = visualizerManager,
                isPlayerScreen = currentPage == 2
            ) { content() }
            2 -> AnimeBackground(
                paletteColors = paletteColors,
                visualizerManager = visualizerManager,
                isPlayerScreen = currentPage == 2
            ) { content() }
            3 -> com.example.beatpulse.ui.components.backgrounds.LuminousBackground(
                paletteColors = paletteColors,
                visualizerManager = visualizerManager,
                isPlayerScreen = currentPage == 2
            ) { content() }
            4 -> Y2KBackground(
                paletteColors = paletteColors,
                visualizerManager = visualizerManager,
                isPlayerScreen = currentPage == 2
            ) { content() }
            5 -> DarkAmbientBackground(
                paletteColors = paletteColors,
                visualizerManager = visualizerManager,
                isPlayerScreen = currentPage == 2
            ) { content() }
            6 -> GothicFantasyBackground(
                paletteColors = paletteColors,
                visualizerManager = visualizerManager,
                isPlayerScreen = currentPage == 2
            ) { content() }
            7 -> CathedralFantasyBackground(
                paletteColors = paletteColors,
                visualizerManager = visualizerManager,
                isPlayerScreen = currentPage == 2,
                isLibraryScreen = currentPage == 1
            ) { content() }
            8 -> TaleLegendBackground(
                paletteColors = paletteColors,
                visualizerManager = visualizerManager,
                isPlayerScreen = currentPage == 2
            ) { content() }
            else -> Box(modifier = Modifier.fillMaxSize().then(bgModifier)) { content() }
        }
    }

    // Render global overlay over everything
    StyleNotificationOverlay(message = globalToastMessage) {
        globalToastMessage = null
    }

    trackToAddToPlaylist?.let { trackToAdd ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { trackToAddToPlaylist = null },
            title = { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.add_to_playlist), color = paletteColors.vibrant) },
            text = {
                if (playlists.isEmpty()) {
                    androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.no_playlists_long), color = androidx.compose.ui.graphics.Color.White)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(playlists) { pl ->
                            androidx.compose.material3.ListItem(
                                headlineContent = { androidx.compose.material3.Text(pl.name, color = androidx.compose.ui.graphics.Color.White) },
                                colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                modifier = Modifier.clickable {
                                    libraryViewModel.addTrackToPlaylist(pl.playlistId, trackToAdd)
                                    android.widget.Toast.makeText(context, "Añadida a ${pl.name}", android.widget.Toast.LENGTH_SHORT).show()
                                    trackToAddToPlaylist = null
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { trackToAddToPlaylist = null }) {
                    androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.close), color = paletteColors.vibrant)
                }
            },
            containerColor = paletteColors.dominant.copy(alpha = 0.9f)
        )
    }

    var showTutorial by remember { mutableStateOf(!prefs.hasSeenTutorial) }
    var showSwipeHint by remember { mutableStateOf(false) }

    if (showTutorial) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable {
                    prefs.hasSeenTutorial = true
                    showTutorial = false
                    showSwipeHint = true
                },
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                androidx.compose.material3.Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.welcome_title),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.material3.Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.welcome_body),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    androidx.compose.material3.Button(
                        onClick = {
                            prefs.appLanguage = "es"
                            (context as? android.app.Activity)?.recreate()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (prefs.appLanguage == "es") paletteColors.vibrant else Color.DarkGray
                        )
                    ) { Text("🇲🇽 ES") }
                    androidx.compose.material3.Button(
                        onClick = {
                            prefs.appLanguage = "en"
                            (context as? android.app.Activity)?.recreate()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (prefs.appLanguage == "en") paletteColors.vibrant else Color.DarkGray
                        )
                    ) { Text("🇬🇧 EN") }
                    androidx.compose.material3.Button(
                        onClick = {
                            prefs.appLanguage = "pt"
                            (context as? android.app.Activity)?.recreate()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (prefs.appLanguage == "pt") paletteColors.vibrant else Color.DarkGray
                        )
                    ) { Text("🇧🇷 PT") }
                }
            }
        }
    }
    
    if (showSwipeHint) {
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
        val offsetX by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -100f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            )
        )
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.6f)).clickable { showSwipeHint = false },
            contentAlignment = androidx.compose.ui.Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 160.dp).padding(horizontal = 24.dp)
            ) {
                Box(modifier = Modifier.offset(x = offsetX.dp).size(24.dp).background(Color.White, androidx.compose.foundation.shape.CircleShape))
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Text("¡Desliza el minirreproductor a la derecha para elegir una canción!", color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}
