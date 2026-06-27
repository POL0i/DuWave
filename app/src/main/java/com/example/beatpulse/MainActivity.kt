package com.example.beatpulse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first


object NavigationKeys {
    const val LIBRARY = "library"
    const val FOLDERS = "folders"
    const val PLAYER = "player"
}

class MainActivity : ComponentActivity() {

    private lateinit var visualizerManager: AudioVisualizerManager
    private lateinit var prefs: PreferencesManager
    private lateinit var musicRepository: MusicRepository
    private lateinit var playerViewModel: PlayerViewModel
    private lateinit var equalizerManager: com.example.beatpulse.service.EqualizerManager

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
            if (!recordAudioGranted) {
                Toast.makeText(this, "Las animaciones rítmicas están desactivadas porque no hay permiso de micrófono.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Se requiere permiso de almacenamiento para buscar tu música.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        musicRepository = MusicRepository(this)
        prefs = PreferencesManager.getInstance(this)
        equalizerManager = com.example.beatpulse.service.EqualizerManager(prefs)
        visualizerManager = AudioVisualizerManager(prefs)
        
        playerViewModel = ViewModelProvider(
            this,
            PlayerViewModelFactory(this.applicationContext, musicRepository, visualizerManager)
        )[PlayerViewModel::class.java]

        checkPermissionsAndSetup()

        setContent {
            val bgStyle by prefs.backgroundStyleFlow.collectAsState(initial = 0)
            
            BeatPulseTheme(isPixelArt = bgStyle == 8) {
                MainScreen(
                    visualizerManager = visualizerManager,
                    equalizerManager = equalizerManager,
                    prefs = prefs,
                    repository = musicRepository,
                    playerViewModel = playerViewModel
                )
            }
        }
    }

    private fun checkPermissionsAndSetup() {
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val missingPermissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.RECORD_AUDIO)
        }
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
                    kotlinx.coroutines.delay(100) // Evitar colisión de AudioFx en dispositivos Motorola
                    visualizerManager.start(sessionId)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::playerViewModel.isInitialized && playerViewModel.isPlaying.value) {
            val sessionId = com.example.beatpulse.service.PlaybackService.audioSessionIdFlow.value
            if (sessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET) {
                visualizerManager.start(sessionId)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::visualizerManager.isInitialized) {
            visualizerManager.stop()
        }
    }
}

@Composable
fun MainScreen(
    visualizerManager: AudioVisualizerManager,
    equalizerManager: com.example.beatpulse.service.EqualizerManager,
    prefs: PreferencesManager,
    repository: MusicRepository,
    playerViewModel: PlayerViewModel
) {
    val exoPlayer by playerViewModel.playerState.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val currentQueue by playerViewModel.currentQueue.collectAsState()
    val paletteColors by playerViewModel.paletteColors.collectAsState()
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

    var currentPage by remember { mutableIntStateOf(0) } // 0: Home, 1: Library, 2: Player
    var sortOrder by remember { mutableStateOf(prefs.librarySortOrder) }

    var trackToAddToPlaylist by remember { mutableStateOf<com.example.beatpulse.data.TrackEntity?>(null) }
    val playlists by repository.playlistsFlow.collectAsState(initial = emptyList())
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
        Scaffold(
            containerColor = Color.Transparent,
                        bottomBar = {
                // isPlaying read directly from StateFlow
                
                var accumulatedDrag by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                val animatedDrag by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = accumulatedDrag,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f),
                    label = "drag"
                )

                var hintingOffset by remember { mutableFloatStateOf(0f) }
                LaunchedEffect(Unit) {
                    while (!prefs.hasUsedMiniplayerGesture) {
                        kotlinx.coroutines.delay(4000)
                        if (accumulatedDrag == 0f && !prefs.hasUsedMiniplayerGesture) {
                            hintingOffset = 30f
                            kotlinx.coroutines.delay(150)
                            hintingOffset = -30f
                            kotlinx.coroutines.delay(150)
                            hintingOffset = 0f
                        }
                    }
                }
                
                val totalOffset by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = animatedDrag + hintingOffset,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f),
                    label = "totalOffset"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { androidx.compose.ui.unit.IntOffset(totalOffset.toInt(), 0) }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = { accumulatedDrag = 0f },
                                    onDragCancel = { accumulatedDrag = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        if (!prefs.hasUsedMiniplayerGesture) {
                                            prefs.hasUsedMiniplayerGesture = true
                                        }
                                        accumulatedDrag += dragAmount
                                        if (accumulatedDrag > 150f) {
                                            currentPage = (currentPage - 1 + 3) % 3
                                            accumulatedDrag = 0f
                                        } else if (accumulatedDrag < -150f) {
                                            currentPage = (currentPage + 1) % 3
                                            accumulatedDrag = 0f
                                        }
                                    }
                                )
                            }
                    ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut()
                        }, label = "BottomBarContent"
                    ) { page ->
                        if (page == 2) {
                            // Pantalla del reproductor: solo mostrar 3 puntos con un marco resaltado
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(Color.Transparent),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                                        .padding(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        listOf(0, 1, 2).forEach { p ->
                                            val color by animateColorAsState(
                                                targetValue = if (currentPage == p) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                animationSpec = tween(300), label = "indicator"
                                            )
                                            val size by androidx.compose.animation.core.animateFloatAsState(
                                                targetValue = if (currentPage == p) 10f else 6f,
                                                animationSpec = tween(300), label = "size"
                                            )
                                            Box(modifier = Modifier.size(size.dp).background(color, androidx.compose.foundation.shape.CircleShape))
                                        }
                                    }
                                }
                            }
                        } else {
                            // Pantallas de Biblioteca/Carpetas: mostrar Mini-Reproductor si hay canción, sino 3 puntos
                            if (currentTrack != null) {

                                val miniPlayerShape = when(bgStyle) {
                                    8 -> androidx.compose.ui.graphics.RectangleShape // Pixel
                                    7 -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp) // Catedral
                                    1 -> androidx.compose.foundation.shape.CutCornerShape(8.dp) // Cyberpunk
                                    2, 4 -> androidx.compose.foundation.shape.RoundedCornerShape(24.dp) // Anime Pastel / Y2K Kawaii (very round)
                                    5 -> androidx.compose.ui.graphics.RectangleShape // Black Metal (sharp)
                                    6 -> androidx.compose.foundation.shape.CutCornerShape(16.dp) // Dark Fantasy (edgy, symmetrical to align with cover)
                                    else -> androidx.compose.foundation.shape.RoundedCornerShape(16.dp) // Clasico
                                }
                                val miniPlayerBorder = when(bgStyle) {
                                    8 -> androidx.compose.foundation.BorderStroke(3.dp, accentColor) // Pixel
                                    1 -> androidx.compose.foundation.BorderStroke(2.dp, paletteColors.vibrant) // Cyberpunk
                                    7 -> androidx.compose.foundation.BorderStroke(1.dp, paletteColors.dominant) // Catedral
                                    3 -> androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha=0.5f)) // Luminoso
                                    2 -> androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFFB7B2)) // Anime Pastel
                                    4 -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF9CEE)) // Y2K Kawaii
                                    5 -> androidx.compose.foundation.BorderStroke(2.dp, Color.DarkGray) // Black Metal
                                    6 -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B0000)) // Dark Fantasy
                                    else -> androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                                }
                                val miniPlayerBg = when(bgStyle) {
                                    8 -> Brush.horizontalGradient(listOf(Color.Black, Color.DarkGray)) // Pixel
                                    1 -> Brush.horizontalGradient(listOf(Color.Black.copy(alpha=0.8f), paletteColors.dominant.copy(alpha=0.8f))) // Cyberpunk
                                    3 -> Brush.horizontalGradient(listOf(paletteColors.lightVibrant.copy(alpha=0.6f), Color.White.copy(alpha=0.2f))) // Luminoso
                                    7 -> Brush.horizontalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D))) // Catedral
                                    2 -> Brush.horizontalGradient(listOf(paletteColors.lightVibrant.copy(alpha=0.9f), paletteColors.dominant.copy(alpha=0.6f))) // Anime Pastel based on cover
                                    4 -> Brush.horizontalGradient(listOf(paletteColors.vibrant.copy(alpha=0.8f), paletteColors.lightVibrant.copy(alpha=0.8f))) // Y2K Kawaii based on cover
                                    5 -> Brush.horizontalGradient(listOf(Color.Black, Color(0xFF111111))) // Black Metal
                                    6 -> Brush.horizontalGradient(listOf(Color(0xFF1A0000), Color(0xFF0D0D0D))) // Dark Fantasy
                                    else -> Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)))
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .background(miniPlayerBg, miniPlayerShape)
                                        .border(miniPlayerBorder, miniPlayerShape)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Portada
                                        val albumArtBitmap = currentTrack?.let { com.example.beatpulse.ui.components.rememberFullAlbumArt(it) }
                                          if (albumArtBitmap != null) {
                                              androidx.compose.foundation.Image(
                                                  bitmap = albumArtBitmap,
                                                  contentDescription = "Album Art",
                                                  modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                                  contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                              )
                                          } else {
                                              androidx.compose.foundation.layout.Box(modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).background(androidx.compose.ui.graphics.Color.DarkGray))
                                          }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        // Textos
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentTrack?.title ?: "No playing",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = currentTrack?.artist ?: "Desconocido",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        
                                        // Play/Pause
                                        IconButton(onClick = { if (exoPlayer?.isPlaying == true) exoPlayer?.pause() else exoPlayer?.play() }) {
                                            Icon(
                                                imageVector = if (isPlaying) androidx.compose.material.icons.Icons.Default.Pause else androidx.compose.material.icons.Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = accentColor,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // No hay canción, mostrar los puntos
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .background(Color.Transparent),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        listOf(0, 1, 2).forEach { p ->
                                            val color by animateColorAsState(
                                                targetValue = if (currentPage == p) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                animationSpec = tween(300), label = "indicator"
                                            )
                                            val size by androidx.compose.animation.core.animateFloatAsState(
                                                targetValue = if (currentPage == p) 10f else 6f,
                                                animationSpec = tween(300), label = "size"
                                            )
                                            Box(modifier = Modifier.size(size.dp).background(color, androidx.compose.foundation.shape.CircleShape))
                                        }
                                        
                                        if (!prefs.hasSeenTutorial) {
                                            var playAlpha by remember { mutableFloatStateOf(0.1f) }
                                            LaunchedEffect(Unit) {
                                                while(true) {
                                                    playAlpha = 0.8f
                                                    kotlinx.coroutines.delay(800)
                                                    playAlpha = 0.1f
                                                    kotlinx.coroutines.delay(800)
                                                }
                                            }
                                            val animatedPlayAlpha by androidx.compose.animation.core.animateFloatAsState(targetValue = playAlpha, animationSpec = tween(800))
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Swipe to Play",
                                                tint = accentColor.copy(alpha = animatedPlayAlpha),
                                                modifier = Modifier.size(20.dp).offset(x = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                label = "page_transition"
            ) { page ->
                when (page) {
                    0 -> AlbumsScreen(
                        repository = repository,
                        paletteColors = paletteColors,
                        prefs = prefs,
                        onTrackClick = { track, queue ->
                            playerViewModel.playTrack(track, queue)
                            currentPage = 2
                        }
                    )
                    1 -> LibraryScreen(
                        repository = repository,
                        paletteColors = paletteColors,
                        prefs = prefs,
                        onRescan = { scope.launch { repository.scanMediaStore() } },
                        onTrackClick = { track, queue ->
                            playerViewModel.playTrack(track, queue)
                            currentPage = 2
                        }
                    )
                    2 -> PlayerScreen(
                        visualizerManager = visualizerManager,
                        equalizerManager = equalizerManager,
                        exoPlayer = exoPlayer,
                        currentTrack = currentTrack,
                        currentQueue = currentQueue,
                        onPlayTrack = { track, queue -> playerViewModel.playTrack(track, queue) },
                        paletteColors = paletteColors,
                        prefs = prefs,
                        sleepTimerSeconds = sleepTimerSeconds,
                        onSetSleepTimer = { sleepTimerSeconds = it },
                        onUpdateTrackMetadata = { id, title, artist, album, coverPath ->
                            playerViewModel.updateTrackMetadata(id, title, artist, album, coverPath)
                        },
                        onAddToPlaylist = { track ->
                            trackToAddToPlaylist = track
                        }
                    )
                }
            }
        }
    }

    // Wrap content with appropriate background, with AnimatedContent for smooth style transitions!
    AnimatedContent(
        targetState = bgStyle,
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
                isPlayerScreen = currentPage == 2
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
            title = { androidx.compose.material3.Text("Añadir a Playlist", color = paletteColors.vibrant) },
            text = {
                if (playlists.isEmpty()) {
                    androidx.compose.material3.Text("No tienes playlists creadas. Crea una desde la pestaña de Álbumes.", color = androidx.compose.ui.graphics.Color.White)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(playlists) { pl ->
                            androidx.compose.material3.ListItem(
                                headlineContent = { androidx.compose.material3.Text(pl.name, color = androidx.compose.ui.graphics.Color.White) },
                                colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            repository.addTrackToPlaylist(pl.playlistId, trackToAdd.id)
                                        }
                                        android.widget.Toast.makeText(context, "Añadida a ${pl.name}", android.widget.Toast.LENGTH_SHORT).show()
                                        trackToAddToPlaylist = null
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { trackToAddToPlaylist = null }) {
                    androidx.compose.material3.Text("Cerrar", color = paletteColors.vibrant)
                }
            },
            containerColor = paletteColors.dominant.copy(alpha = 0.9f)
        )
    }

    var showTutorial by remember { mutableStateOf(!prefs.hasSeenTutorial) }
    if (showTutorial) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable {
                    prefs.hasSeenTutorial = true
                    showTutorial = false
                },
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "¡Bienvenido a BeatPulse!",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.material3.Text(
                    text = "👋 Gestos Principales:\n\n• Desliza a los lados en el mini-reproductor para cambiar entre Inicio, Biblioteca y Reproductor.\n\n• Usa la lupa en Biblioteca para buscar y el botón de recargar para buscar música nueva.\n\n• Toca aquí para comenzar a escuchar.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
