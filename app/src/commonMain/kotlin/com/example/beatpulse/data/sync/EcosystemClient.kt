package com.example.beatpulse.data.sync

import com.example.beatpulse.data.models.SyncFolder
import com.example.beatpulse.data.models.SyncTrack
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class EcosystemClient(private val serverIp: String, private val port: Int = 8080) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private val baseUrl = "http://$serverIp:$port/api"

    suspend fun handshake(): Boolean {
        return try {
            val response: HttpResponse = client.get("$baseUrl/handshake")
            response.status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFolders(): List<SyncFolder> {
        return try {
            client.get("$baseUrl/sync/folders").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPlaylistTracks(folderId: String): List<SyncTrack> {
        return try {
            client.get("$baseUrl/sync/playlist/$folderId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Downloading a file would require streaming the response and writing to a local File
    // This will be implemented in the specific platform using okio or java.io.File
    
    fun close() {
        client.close()
    }
}
