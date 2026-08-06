package com.example.beatpulse

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.beatpulse.data.OnlineMusicRepository
import com.example.beatpulse.utils.DownloadHelper
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.StreamInfo
import javax.inject.Inject
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.TrackEntity

@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    @Inject
    lateinit var onlineMusicRepository: OnlineMusicRepository
    
    @Inject
    lateinit var musicRepository: MusicRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val url = extractUrl(sharedText)

        if (url == null || !url.contains("youtube.com") && !url.contains("youtu.be")) {
            Toast.makeText(this, "Solo se admiten enlaces de YouTube", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MaterialTheme {
                ShareConfirmationDialog(url = url, onDismiss = { finish() })
            }
        }
    }

    private fun extractUrl(text: String): String? {
        val urlRegex = "(https?://[a-zA-Z0-9./?=_-]+)".toRegex()
        val matchResult = urlRegex.find(text)
        return matchResult?.value
    }

    @Composable
    fun ShareConfirmationDialog(url: String, onDismiss: () -> Unit) {
        val coroutineScope = rememberCoroutineScope()
        var videoTitle by remember { mutableStateOf<String?>(null) }
        var videoAuthor by remember { mutableStateOf<String?>(null) }
        var streamUrl by remember { mutableStateOf<String?>(null) }
        var streamInfo by remember { mutableStateOf<StreamInfo?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(url) {
            withContext(Dispatchers.IO) {
                try {
                    val info = StreamInfo.getInfo(org.schabi.newpipe.extractor.ServiceList.YouTube, url)
                    streamInfo = info
                    videoTitle = info.name
                    videoAuthor = info.uploaderName
                    val bestAudio = info.audioStreams.maxByOrNull { it.bitrate }
                    streamUrl = bestAudio?.content
                    
                    if (streamUrl == null) {
                        error = "No se pudo extraer el audio"
                    }
                } catch (e: Exception) {
                    error = e.message ?: "Error al obtener información"
                } finally {
                    isLoading = false
                }
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(stringResource(R.string.confirm_link_download))
            },
            text = {
                if (isLoading) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Text("Error: $error")
                } else {
                    Text(stringResource(R.string.download_from_youtube_desc, videoTitle ?: ""))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isLoading && error == null && videoTitle != null && streamUrl != null) {
                            val highResThumb = streamInfo?.thumbnails?.firstOrNull()?.url?.replace(Regex("=w\\d+-h\\d+[^&]*"), "=w600-h600-l90-rj") ?: ""
                            
                            // Guardar el thumbnail y metadatos preliminares para que se asimilen al descargar
                            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                var localCover = ""
                                try {
                                    val coversDir = java.io.File(this@ShareActivity.filesDir, "covers")
                                    if (!coversDir.exists()) coversDir.mkdirs()
                                    val destFile = java.io.File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                                    
                                    java.net.URL(highResThumb).openStream().use { input ->
                                        destFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    localCover = destFile.absolutePath
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                
                                val trackToSave = TrackEntity(
                                    id = url.hashCode().toLong(),
                                    title = videoTitle!!,
                                    artist = videoAuthor ?: "YouTube",
                                    album = "YouTube Downloads",
                                    duration = streamInfo?.duration?.times(1000L) ?: 0L,
                                    dataPath = "youtube://${url.hashCode()}",
                                    folderPath = "Online",
                                    customCoverPath = localCover.takeIf { it.isNotEmpty() }
                                )
                                musicRepository.insertOrUpdateTrack(trackToSave)
                            }
                            
                            DownloadHelper.downloadTrack(
                                context = this@ShareActivity,
                                streamUrl = streamUrl!!,
                                title = videoTitle!!,
                                artist = videoAuthor ?: "YouTube",
                                fileExtension = "m4a"
                            )
                            Toast.makeText(this@ShareActivity, "Descargando...", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    },
                    enabled = !isLoading && error == null
                ) {
                    Text(stringResource(R.string.download))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
