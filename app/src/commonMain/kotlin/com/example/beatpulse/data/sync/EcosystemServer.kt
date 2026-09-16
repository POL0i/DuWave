package com.example.beatpulse.data.sync

import com.example.beatpulse.data.models.SyncFolder
import com.example.beatpulse.data.models.SyncTrack
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.io.File

class EcosystemServer {

    private var server: io.ktor.server.engine.EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    fun startServer(port: Int = 8080) {
        if (_isServerRunning.value) return
        
        try {
            server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
                
                install(CORS) {
                    anyHost()
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                    allowMethod(HttpMethod.Options)
                    allowHeader("Content-Type")
                }
                
                routing {
                    get("/api/handshake") {
                        call.respond(mapOf("app" to "DuWave", "version" to "2.0.1", "device" to "Host"))
                    }
                    
                    get("/api/sync/folders") {
                        val repo = org.koin.core.context.GlobalContext.get().get<com.example.beatpulse.data.MusicRepository>()
                        val playlists = repo.playlistsFlow.first()
                        val allTracks = repo.allTracksFlow.first()
                        
                        val folders = playlists.map { p ->
                            SyncFolder(p.playlistId.toString(), p.name, "playlist_${p.playlistId}", true)
                        }.toMutableList()
                        
                        val distinctFolders = allTracks.map { it.folderPath }.distinct().filter { it.isNotEmpty() }
                        distinctFolders.forEach { path ->
                            val folderName = path.substringAfterLast('/')
                            folders.add(SyncFolder("sys_folder_${path.hashCode()}", folderName, path, false))
                        }
                        
                        call.respond(folders)
                    }
                    
                    get("/api/sync/playlist/{id}") {
                        val idStr = call.parameters["id"]
                        val repo = org.koin.core.context.GlobalContext.get().get<com.example.beatpulse.data.MusicRepository>()
                        
                        val tracks = if (idStr?.startsWith("sys_folder_") == true) {
                            val hash = idStr.removePrefix("sys_folder_").toIntOrNull()
                            val all = repo.allTracksFlow.first()
                            all.filter { it.folderPath.hashCode() == hash }
                        } else if (idStr != null) {
                            repo.getTracksForPlaylist(idStr.toLongOrNull() ?: -1L).first()
                        } else {
                            emptyList()
                        }
                        
                        val syncTracks = tracks.map { t ->
                            SyncTrack(
                                id = t.id.toString(),
                                title = t.title,
                                artist = t.artist,
                                album = t.album ?: "",
                                duration = t.duration,
                                folderPath = idStr ?: "",
                                dataPath = t.dataPath,
                                hasCover = t.customCoverPath != null
                            )
                        }
                        call.respond(syncTracks)
                    }
                    
                    get("/api/sync/file/{id}") {
                        val idStr = call.parameters["id"]
                        val repo = org.koin.core.context.GlobalContext.get().get<com.example.beatpulse.data.MusicRepository>()
                        val allTracks = repo.allTracksFlow.first()
                        val track = allTracks.find { it.id.toString() == idStr }
                        
                        if (track != null && !track.dataPath.startsWith("youtube://") && !track.dataPath.startsWith("http")) {
                            val file = java.io.File(track.dataPath)
                            if (file.exists()) {
                                call.respondFile(file)
                            } else {
                                call.respondText("File not found on disk", status = io.ktor.http.HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respondText("Invalid track or online track", status = io.ktor.http.HttpStatusCode.NotFound)
                        }
                    }
                }
            }.start(wait = false)
            
            _isServerRunning.value = true
        } catch (e: Exception) {
            e.printStackTrace()
            _isServerRunning.value = false
        }
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
        _isServerRunning.value = false
    }
}
