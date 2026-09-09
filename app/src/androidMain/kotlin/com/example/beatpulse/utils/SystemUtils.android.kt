package com.example.beatpulse.utils

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.beatpulse.BeatPulseApp
import kotlinx.coroutines.isActive

actual object SystemUtils {
    actual fun showToast(message: String) {
        val context = BeatPulseApp.appContext
        if (context != null) {
            // Se debe asegurar de correr en el hilo principal (Main Thread)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    actual fun openUrl(url: String) {
        val context = BeatPulseApp.appContext
        if (context != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    actual fun recreateApp() {
        // Restart the app
        val context = BeatPulseApp.appContext
        if (context != null) {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            val componentName = intent?.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }
    }

    actual fun pickImageFile(): String? {
        // En Android esto requeriría un ActivityResultLauncher,
        // por simplicidad y porque el scope actual es Desktop, retornamos null.
        return null
    }

    actual val isMobilePlatform: Boolean = true

    actual fun dispatchSystemBack(): Boolean {
        // Android natively handles back navigation with OnBackPressedDispatcher,
        // so we don't need to manually dispatch it via Esc globally.
        return false
    }

    actual fun getSystemVolumeLevel(): Float {
        val context = BeatPulseApp.appContext ?: return 1.0f
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        if (audioManager != null) {
            val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
            val current = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
            return if (max > 0) current / max else 1.0f
        }
        return 1.0f
    }

    actual fun setSystemVolumeLevel(volume: Float) {
        val context = BeatPulseApp.appContext ?: return
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        if (audioManager != null) {
            val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val index = (volume.coerceIn(0f, 1f) * max).toInt()
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, index, 0)
        }
    }
}

@androidx.compose.runtime.Composable
actual fun SystemBackHandler(onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
}

@androidx.compose.runtime.Composable
actual fun SystemImagePicker(
    onFileSelected: (String?) -> Unit,
    colorDominant: androidx.compose.ui.graphics.Color,
    colorVibrant: androidx.compose.ui.graphics.Color
) {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        onFileSelected(uri?.toString())
    }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        launcher.launch("image/*")
    }
}

@androidx.compose.runtime.Composable
actual fun SystemMicPermissionHandler(
    requestTrigger: Boolean,
    onResult: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(isGranted)
    }

    androidx.compose.runtime.LaunchedEffect(requestTrigger) {
        if (requestTrigger) {
            val permissionCheckResult = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            )
            if (permissionCheckResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                onResult(true)
            } else {
                launcher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

@androidx.compose.runtime.Composable
actual fun SystemStatusBarVisibility(visible: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    androidx.compose.runtime.DisposableEffect(visible) {
        if (activity != null) {
            val window = activity.window
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (visible) {
                controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            } else {
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            if (activity != null) {
                val window = activity.window
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
    }
}

@androidx.compose.runtime.Composable
actual fun getLocalizedString(key: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resId = context.resources.getIdentifier(key, "string", context.packageName)
    return if (resId != 0) context.getString(resId) else key
}

actual suspend fun isAudioTrimmerReady(): Boolean = true
actual suspend fun downloadAudioTrimmerDependencies(onProgress: (Float) -> Unit) {
    // No-op for Android
}

actual suspend fun trimAudioFile(inputPath: String, outputDir: String, outputFileNameBase: String, startMs: Long, endMs: Long): String? {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        com.example.beatpulse.utils.AudioTrimmer.trimAudio(inputPath, outputDir, outputFileNameBase, startMs, endMs)
    }
}

actual fun getAudioDuration(filePath: String): Long {
    var retriever: android.media.MediaMetadataRetriever? = null
    try {
        retriever = android.media.MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
        return durationStr?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        e.printStackTrace()
        return 0L
    } finally {
        try {
            retriever?.release()
        } catch (e: Exception) {}
    }
}

@androidx.compose.runtime.Composable
actual fun AudioPreviewPlayer(
    path: String,
    isPlaying: Boolean,
    startMs: Long,
    endMs: Long,
    onPlaybackCompleted: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mediaPlayer = androidx.compose.runtime.remember { android.media.MediaPlayer() }
    
    androidx.compose.runtime.DisposableEffect(path) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(path)
            mediaPlayer.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDispose {
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (e: Exception) {}
        }
    }

    androidx.compose.runtime.LaunchedEffect(isPlaying, startMs) {
        try {
            if (isPlaying) {
                mediaPlayer.seekTo(startMs.toInt())
                mediaPlayer.start()
            } else {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Monitor for reaching endMs
    androidx.compose.runtime.LaunchedEffect(isPlaying, endMs) {
        if (isPlaying) {
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                try {
                    if (mediaPlayer.isPlaying && mediaPlayer.currentPosition >= endMs) {
                        mediaPlayer.pause()
                        onPlaybackCompleted()
                        break
                    }
                } catch (e: Exception) {
                    break
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }
}
