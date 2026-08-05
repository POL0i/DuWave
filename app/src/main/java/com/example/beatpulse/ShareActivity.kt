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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import javax.inject.Inject

@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    @Inject
    lateinit var onlineMusicRepository: OnlineMusicRepository

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
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(url) {
            withContext(Dispatchers.IO) {
                try {
                    val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, url)
                    videoTitle = streamInfo.name
                    videoAuthor = streamInfo.uploaderName
                    val bestAudio = streamInfo.audioStreams.maxByOrNull { it.bitrate }
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
