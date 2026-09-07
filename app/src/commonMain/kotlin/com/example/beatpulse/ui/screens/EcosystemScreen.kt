package com.example.beatpulse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.beatpulse.data.models.SyncFolder
import com.example.beatpulse.data.models.SyncTrack
import com.example.beatpulse.data.sync.EcosystemClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcosystemScreen(
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var ipAddress by remember { mutableStateOf("192.168.1.") }
    var client by remember { mutableStateOf<EcosystemClient?>(null) }
    var connectionStatus by remember { mutableStateOf("Desconectado") }
    
    var remoteFolders by remember { mutableStateOf<List<SyncFolder>>(emptyList()) }
    var selectedFolder by remember { mutableStateOf<SyncFolder?>(null) }
    var remoteTracks by remember { mutableStateOf<List<SyncTrack>>(emptyList()) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Ecosistema DuWave") },
            navigationIcon = {
                // Should add back button, omitting for brevity in this snippet
            }
        )
        
        if (client == null) {
            // Connection View
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Conectar a dispositivo", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("Dirección IP local") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        connectionStatus = "Conectando..."
                        val newClient = EcosystemClient(ipAddress)
                        if (newClient.handshake()) {
                            client = newClient
                            connectionStatus = "Conectado"
                            remoteFolders = newClient.getFolders()
                        } else {
                            connectionStatus = "Fallo en la conexión"
                            newClient.close()
                        }
                    }
                }) {
                    Text("Conectar")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(connectionStatus)
            }
        } else if (selectedFolder == null) {
            // Folders View
            Text(
                "Carpetas Disponibles en ${ipAddress}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn {
                items(remoteFolders) { folder ->
                    ListItem(
                        headlineContent = { Text(folder.name) },
                        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                selectedFolder = folder
                                remoteTracks = client!!.getPlaylistTracks(folder.id)
                            }
                        }
                    )
                }
            }
        } else {
            // Tracks View
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { selectedFolder = null }) {
                    Text("Volver")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Pistas en ${selectedFolder?.name}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            LazyColumn {
                items(remoteTracks) { track ->
                    ListItem(
                        headlineContent = { Text(track.title) },
                        supportingContent = { Text(track.artist) },
                        trailingContent = {
                            IconButton(onClick = {
                                // TODO: Trigger download logic for track.id
                            }) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "Descargar")
                            }
                        }
                    )
                }
            }
        }
    }
}
