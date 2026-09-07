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
import java.io.File

class EcosystemServer {

    private var server: ApplicationEngine? = null
    
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    fun startServer(port: Int = 8080) {
        if (_isServerRunning.value) return
        
        server = embeddedServer(CIO, port = port) {
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
                    // TODO: Retrieve actual local folders (Downloads, Audio, DuWave, Cache, User Playlists)
                    val mockFolders = listOf(
                        SyncFolder("1", "Downloads", "/storage/emulated/0/Download", false),
                        SyncFolder("2", "DuWave Music", "/storage/emulated/0/DuWave", false)
                    )
                    call.respond(mockFolders)
                }
                
                get("/api/sync/playlist/{id}") {
                    val id = call.parameters["id"]
                    // TODO: Fetch tracks belonging to the folder/playlist ID
                    val mockTracks = listOf(
                        SyncTrack("track_1", "Sample Song", "Artist", "Album", 180000, "Downloads", "path/to/song.mp3", false)
                    )
                    call.respond(mockTracks)
                }
                
                get("/api/sync/file/{id}") {
                    val id = call.parameters["id"]
                    // TODO: Lookup actual file path from DB by track ID
                    val file = File("/dummy/path/$id.mp3")
                    if (file.exists()) {
                        call.respondFile(file)
                    } else {
                        call.respondText("File not found", status = io.ktor.http.HttpStatusCode.NotFound)
                    }
                }
            }
        }.start(wait = false)
        
        _isServerRunning.value = true
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
        _isServerRunning.value = false
    }
}
