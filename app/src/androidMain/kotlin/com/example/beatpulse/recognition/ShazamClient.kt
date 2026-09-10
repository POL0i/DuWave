package com.example.beatpulse.recognition

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ShazamClient {
    private val gson = Gson()
    
    // Lista de servidores de rotación para asegurar el uptime al 100%
    private val servers = listOf(
        "http://127.0.0.1:8000/recognize",
        "http://localhost:8000/recognize",
        "http://10.0.2.2:8000/recognize",
        "https://duwave-shazam-1.onrender.com/recognize",
        "https://duwave-shazam-2.hf.space/recognize"
    )

    suspend fun checkApiStatus(): Boolean = withContext(Dispatchers.IO) {
        for (urlStr in servers) {
            try {
                val url = URL(urlStr.replace("/recognize", "/"))
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3000
                if (conn.responseCode in 200..299) return@withContext true
            } catch (e: Exception) {}
        }
        return@withContext false
    }

    suspend fun recognize(audioBytes: ByteArray): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        for (urlStr in servers) {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 15000
                
                val boundary = "==Boundary_${System.currentTimeMillis()}=="
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                
                conn.outputStream.use { os ->
                    os.write("--$boundary\r\n".toByteArray())
                    os.write("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n".toByteArray())
                    os.write("Content-Type: audio/wav\r\n\r\n".toByteArray())
                    os.write(audioBytes)
                    os.write("\r\n--$boundary--\r\n".toByteArray())
                }
                
                if (conn.responseCode in 200..299) {
                    val responseReader = InputStreamReader(conn.inputStream, Charsets.UTF_8)
                    val responseMap = gson.fromJson(responseReader, Map::class.java)
                    responseReader.close()
                    
                    if (responseMap["success"] == true) {
                        val track = responseMap["track"] as Map<*, *>
                        val title = track["title"] as String
                        val artist = track["artist"] as String
                        return@withContext Result.success(Pair(title, artist))
                    } else {
                        if (responseMap["message"] == "No match found") {
                            return@withContext Result.failure(Exception("No match found"))
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue to the next fallback server
                continue
            }
        }
        return@withContext Result.failure(Exception("All backend servers failed or timed out."))
    }
}
