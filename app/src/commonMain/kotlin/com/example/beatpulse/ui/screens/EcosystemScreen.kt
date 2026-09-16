package com.example.beatpulse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.beatpulse.data.models.SyncFolder
import com.example.beatpulse.data.models.SyncTrack
import com.example.beatpulse.data.sync.EcosystemClient
import com.example.beatpulse.data.sync.EcosystemManager
import com.example.beatpulse.data.TrackEntity
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.theme.PaletteColors
import org.koin.core.context.GlobalContext
import com.example.beatpulse.utils.getLocalizedString
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcosystemScreen(
    onNavigateBack: () -> Unit,
    paletteColors: PaletteColors,
    dynamicTextColor: Color
) {
    val coroutineScope = rememberCoroutineScope()
    val discoveredDevices by EcosystemManager.discovery.discoveredDevices.collectAsState()
    var client by remember { mutableStateOf<EcosystemClient?>(null) }
    var connectionStatus by remember { mutableStateOf("") }

    val strSearchingDevice = getLocalizedString("eco_searching_device")
    val strCouldNotConnect = getLocalizedString("eco_could_not_connect_to")
    val strUnexpectedError = getLocalizedString("eco_unexpected_error")
    val strDeviceNotFound = getLocalizedString("eco_device_not_found")
    val strConnectingTo = getLocalizedString("eco_connecting_to")
    val strDeviceFound = getLocalizedString("eco_device_found")

    
    var remoteFolders by remember { mutableStateOf<List<SyncFolder>>(emptyList()) }
    var selectedFolder by remember { mutableStateOf<SyncFolder?>(null) }
    var remoteTracks by remember { mutableStateOf<List<SyncTrack>>(emptyList()) }
    
    val repository = GlobalContext.get().get<MusicRepository>()

    LaunchedEffect(Unit) {
        EcosystemManager.startEcosystem()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(getLocalizedString("song_transfer"), color = dynamicTextColor) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = getLocalizedString("eco_back"), tint = paletteColors.vibrant)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = dynamicTextColor
            )
        )
        
        if (client == null) {
            // Devices View
            var inputPin by remember { mutableStateOf("") }
            var isPairing by remember { mutableStateOf(false) }
            
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                
                // Emisor Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = paletteColors.dominant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(getLocalizedString("pairing_code"), style = MaterialTheme.typography.titleMedium, color = dynamicTextColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = EcosystemManager.discovery.currentPin,
                            style = MaterialTheme.typography.displayMedium,
                            color = paletteColors.vibrant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(getLocalizedString("enter_code_other_device"), style = MaterialTheme.typography.bodyMedium, color = dynamicTextColor.copy(alpha = 0.7f))
                    }
                }
                
                // Receptor Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = paletteColors.dominant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(getLocalizedString("connect_device"), style = MaterialTheme.typography.titleMedium, color = dynamicTextColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = inputPin,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) inputPin = it },
                            label = { Text(getLocalizedString("four_digit_code"), color = dynamicTextColor) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = dynamicTextColor,
                                unfocusedTextColor = dynamicTextColor,
                                focusedBorderColor = paletteColors.vibrant,
                                unfocusedBorderColor = dynamicTextColor.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (inputPin.length == 4) {
                                    isPairing = true
                                    connectionStatus = strSearchingDevice
                                    EcosystemManager.discovery.pairWithPin(inputPin) { success ->
                                        if (success) {
                                            connectionStatus = strDeviceFound
                                            val devices = EcosystemManager.discovery.discoveredDevices.value
                                            val firstDevice = devices.entries.firstOrNull()
                                            if (firstDevice != null) {
                                                coroutineScope.launch {
                                                    val newClient = EcosystemClient(firstDevice.key)
                                                    if (newClient.handshake()) {
                                                        client = newClient
                                                        connectionStatus = ""
                                                        remoteFolders = newClient.getFolders()
                                                    } else {
                                                        connectionStatus = strCouldNotConnect.replace("%1\$s", firstDevice.value)
                                                        newClient.close()
                                                        isPairing = false
                                                    }
                                                }
                                            } else {
                                                isPairing = false
                                                connectionStatus = strUnexpectedError
                                            }
                                        } else {
                                            isPairing = false
                                            connectionStatus = strDeviceNotFound
                                        }
                                    }
                                }
                            },
                            enabled = inputPin.length == 4 && !isPairing,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = paletteColors.vibrant)
                        ) {
                            if (isPairing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = paletteColors.dominant)
                            } else {
                                Text(getLocalizedString("eco_pair"), color = paletteColors.dominant)
                            }
                        }
                    }
                }
                
                if (connectionStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(connectionStatus, color = paletteColors.vibrant)
                }
                
                if (discoveredDevices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(getLocalizedString("eco_found_devices"), style = MaterialTheme.typography.titleSmall, color = dynamicTextColor)
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(discoveredDevices.toList()) { (ip, name) ->
                            ListItem(
                                headlineContent = { Text(name, color = dynamicTextColor) },
                                supportingContent = { Text(ip, color = dynamicTextColor.copy(alpha = 0.7f)) },
                                leadingContent = { Icon(Icons.Default.Computer, contentDescription = null, tint = paletteColors.vibrant) },
                                modifier = Modifier.clickable {
                                    coroutineScope.launch {
                                        connectionStatus = strConnectingTo.replace("%1\$s", name)
                                        val newClient = EcosystemClient(ip)
                                        if (newClient.handshake()) {
                                            client = newClient
                                            connectionStatus = ""
                                            remoteFolders = newClient.getFolders()
                                        } else {
                                            connectionStatus = strCouldNotConnect.replace("%1\$s", name)
                                            newClient.close()
                                        }
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        } else if (selectedFolder == null) {
            // Folders View
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { 
                        client?.close()
                        client = null 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = paletteColors.vibrant)
                ) {
                    Text(getLocalizedString("eco_disconnect"), color = paletteColors.dominant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    getLocalizedString("eco_available_lists"),
                    style = MaterialTheme.typography.titleMedium,
                    color = dynamicTextColor
                )
            }
            
            LazyColumn {
                items(remoteFolders) { folder ->
                    ListItem(
                        headlineContent = { Text(folder.name, color = dynamicTextColor) },
                        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, tint = paletteColors.vibrant) },
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                selectedFolder = folder
                                remoteTracks = client!!.getPlaylistTracks(folder.id)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        } else {
            // Tracks View
            val localTracks by repository.allTracksFlow.collectAsState(initial = emptyList())
            var isDownloadingAll by remember { mutableStateOf(false) }
            val downloadedTrackIds = remember { mutableStateListOf<String>() }
            
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedFolder = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = getLocalizedString("eco_back_to_lists"), tint = paletteColors.vibrant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        selectedFolder?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = dynamicTextColor
                    )
                }
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isDownloadingAll = true
                            val osName = java.lang.System.getProperty("os.name") ?: ""
                            val isWindows = osName.contains("Windows", ignoreCase = true)
                            val isAndroid = java.lang.System.getProperty("java.vendor")?.contains("Android") == true
                            
                            val baseFolder = if (isAndroid) {
                                "/storage/emulated/0/Download/DuWave_Transfer"
                            } else if (isWindows) {
                                java.lang.System.getProperty("user.home") + "\\Downloads\\DuWave_Transfer"
                            } else {
                                java.lang.System.getProperty("user.home") + "/Downloads/DuWave_Transfer"
                            }
                            
                            java.io.File(baseFolder).mkdirs()
                            
                            for (track in remoteTracks) {
                                val isDuplicate = localTracks.any { it.title.equals(track.title, ignoreCase = true) && it.artist.equals(track.artist, ignoreCase = true) }
                                if (!isDuplicate && !downloadedTrackIds.contains(track.id)) {
                                    val extension = track.dataPath.substringAfterLast('.', "mp3")
                                    val safeTitle = track.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                    val destFile = java.io.File("$baseFolder/$safeTitle.$extension")
                                    
                                    val success = client?.downloadFile(track.id, destFile) == true
                                    if (success) {
                                        downloadedTrackIds.add(track.id)
                                        val newTrack = TrackEntity(
                                            id = destFile.absolutePath.hashCode().toLong(),
                                            title = track.title,
                                            artist = track.artist,
                                            album = track.album,
                                            duration = track.duration,
                                            dataPath = destFile.absolutePath,
                                            folderPath = baseFolder,
                                            dateAdded = System.currentTimeMillis()
                                        )
                                        repository.insertOrUpdateTrack(newTrack)
                                    }
                                }
                            }
                            isDownloadingAll = false
                            // Scan library to ensure everything is updated properly
                            repository.scanLocalLibrary(baseFolder)
                        }
                    },
                    enabled = !isDownloadingAll,
                    colors = ButtonDefaults.buttonColors(containerColor = paletteColors.vibrant)
                ) {
                    if (isDownloadingAll) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = paletteColors.dominant, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.SelectAll, contentDescription = null, tint = paletteColors.dominant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(getLocalizedString("eco_download_all"), color = paletteColors.dominant)
                    }
                }
            }
            
            LazyColumn {
                items(remoteTracks, key = { it.id }) { track ->
                    val isDuplicate = localTracks.any { it.title.equals(track.title, ignoreCase = true) && it.artist.equals(track.artist, ignoreCase = true) }
                    var isDownloading by remember { mutableStateOf(false) }
                    val isDownloaded = downloadedTrackIds.contains(track.id)
                    
                    val dummyTrack = TrackEntity(
                        id = track.id.hashCode().toLong(),
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        duration = track.duration,
                        dataPath = "",
                        folderPath = "",
                        customCoverPath = null
                    )
                    
                    Box {
                        TrackItem(
                            track = dummyTrack,
                            paletteColors = paletteColors,
                            textColor = dynamicTextColor,
                            onClick = {},
                            onToggleFavorite = {},
                            hasMenuOptions = false
                        )
                        
                        // Overlay action button for download
                        Box(
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                        ) {
                            if (isDuplicate || isDownloaded) {
                                Icon(Icons.Default.Check, contentDescription = getLocalizedString("eco_done"), tint = paletteColors.vibrant)
                            } else if (isDownloading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = paletteColors.vibrant)
                            } else {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        isDownloading = true
                                        val osName = java.lang.System.getProperty("os.name") ?: ""
                                        val isWindows = osName.contains("Windows", ignoreCase = true)
                                        val isAndroid = java.lang.System.getProperty("java.vendor")?.contains("Android") == true
                                        
                                        val baseFolder = if (isAndroid) {
                                            "/storage/emulated/0/Download/DuWave_Transfer"
                                        } else if (isWindows) {
                                            java.lang.System.getProperty("user.home") + "\\Downloads\\DuWave_Transfer"
                                        } else {
                                            java.lang.System.getProperty("user.home") + "/Downloads/DuWave_Transfer"
                                        }
                                        java.io.File(baseFolder).mkdirs()
                                        
                                        val extension = track.dataPath.substringAfterLast('.', "mp3")
                                        val safeTitle = track.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                        val destFile = java.io.File("$baseFolder/$safeTitle.$extension")
                                        
                                        val success = client?.downloadFile(track.id, destFile) == true
                                        if (success) {
                                            downloadedTrackIds.add(track.id)
                                            val newTrack = TrackEntity(
                                                id = destFile.absolutePath.hashCode().toLong(),
                                                title = track.title,
                                                artist = track.artist,
                                                album = track.album,
                                                duration = track.duration,
                                                dataPath = destFile.absolutePath,
                                                folderPath = baseFolder,
                                                dateAdded = System.currentTimeMillis()
                                            )
                                            repository.insertOrUpdateTrack(newTrack)
                                            repository.scanLocalLibrary(baseFolder)
                                        }
                                        isDownloading = false
                                    }
                                }) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = getLocalizedString("eco_download"), tint = dynamicTextColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
