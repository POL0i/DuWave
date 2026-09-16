package com.example.beatpulse.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.File
import java.util.prefs.Preferences

private val prefs = Preferences.userRoot().node("com.example.beatpulse.imagepicker")

@Composable
fun ComposeImagePicker(
    onFileSelected: (String?) -> Unit,
    colorDominant: Color = Color(0xFF1E1E1E),
    colorVibrant: Color = Color(0xFF00E5FF)
) {
    var currentDir by remember { 
        val lastPath = prefs.get("last_dir", System.getProperty("user.home"))
        val lastFile = File(lastPath)
        mutableStateOf(if (lastFile.exists() && lastFile.isDirectory) lastFile else File(System.getProperty("user.home")))
    }
    
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(currentDir) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val allFiles = currentDir.listFiles() ?: emptyArray()
            val validFiles = allFiles.filter {
                if (it.isHidden) return@filter false
                if (it.isDirectory) return@filter true
                val name = it.name.lowercase()
                name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
            }
            files = validFiles.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        }
        isLoading = false
    }

    Dialog(onDismissRequest = { onFileSelected(null) }) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colorDominant),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { 
                        val parent = currentDir.parentFile
                        if (parent != null) {
                            currentDir = parent
                            prefs.put("last_dir", parent.absolutePath)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentDir.absolutePath,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colorVibrant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(100.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files) { file ->
                            if (file.isDirectory) {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .clickable { 
                                            currentDir = file
                                            prefs.put("last_dir", file.absolutePath)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.Folder, contentDescription = "Carpeta", tint = colorVibrant, modifier = Modifier.size(48.dp))
                                        Text(file.name, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(4.dp))
                                    }
                                }
                            } else {
                                ImageThumbnail(file, onClick = { onFileSelected(file.absolutePath) })
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { onFileSelected(null) }) {
                        Text("Cancelar", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ImageThumbnail(file: File, onClick: () -> Unit) {
    var bitmap by remember(file.absolutePath) { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(file.absolutePath) {
        withContext(Dispatchers.IO) {
            try {
                // Leer solo los bytes necesarios, pero para simplificar leemos el archivo
                val bytes = file.readBytes()
                val image = Image.makeFromEncoded(bytes).toComposeImageBitmap()
                bitmap = image
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = file.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Gray, strokeWidth = 2.dp)
        }
        
        // Etiqueta del archivo pequeña al fondo
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(2.dp)
        ) {
            Text(
                text = file.name,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
