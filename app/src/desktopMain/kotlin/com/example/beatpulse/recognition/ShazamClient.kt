package com.example.beatpulse.recognition

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.random.Random

data class ShazamRequestJson(
    @SerializedName("geolocation") val geolocation: Geolocation,
    @SerializedName("signature") val signature: Signature,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("timezone") val timezone: String
) {
    data class Geolocation(
        @SerializedName("altitude") val altitude: Double, 
        @SerializedName("latitude") val latitude: Double, 
        @SerializedName("longitude") val longitude: Double
    )

    data class Signature(
        @SerializedName("samplems") val samplems: Long, 
        @SerializedName("timestamp") val timestamp: Long, 
        @SerializedName("uri") val uri: String
    )
}

data class ShazamResponseJson(
    @SerializedName("track") val track: Track? = null
) {
    data class Track(
        @SerializedName("title") val title: String? = null,
        @SerializedName("subtitle") val subtitle: String? = null
    )
}

class ShazamClient {
    private val gson = Gson()

    suspend fun checkApiStatus(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL("https://amp.shazam.com")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 6.0.1; SM-G920F Build/MMB29K)")
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            val code = conn.responseCode
            code in 200..404
        } catch (e: Exception) {
            false
        }
    }

    suspend fun recognize(signatureUri: String, sampleMs: Long): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val timestamp = System.currentTimeMillis() / 1000
            val request = ShazamRequestJson(
                geolocation = ShazamRequestJson.Geolocation(
                    altitude = Random.nextDouble() * 400 + 100,
                    latitude = Random.nextDouble() * 180 - 90,
                    longitude = Random.nextDouble() * 360 - 180
                ),
                signature = ShazamRequestJson.Signature(
                    samplems = sampleMs,
                    timestamp = timestamp,
                    uri = signatureUri
                ),
                timestamp = timestamp,
                timezone = "Europe/Madrid"
            )

            val uuid1 = UUID.randomUUID().toString().uppercase()
            val uuid2 = UUID.randomUUID().toString()
            
            val urlString = "https://amp.shazam.com/discovery/v5/es/ES/android/-/tag/$uuid1/$uuid2?sync=true&webv3=true&sampling=true&connected=&shazamapiversion=v3&sharehub=true&video=v3"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 6.0.1; SM-G920F Build/MMB29K)")
            conn.setRequestProperty("Content-Language", "es_ES")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 15000

            val jsonString = gson.toJson(request)
            conn.outputStream.use { os ->
                val input = jsonString.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseReader = InputStreamReader(conn.inputStream, Charsets.UTF_8)
                val responseBody = gson.fromJson(responseReader, ShazamResponseJson::class.java)
                responseReader.close()
                
                if (responseBody.track?.title != null && responseBody.track.subtitle != null) {
                    Result.success(Pair(responseBody.track.title, responseBody.track.subtitle))
                } else {
                    Result.failure(Exception("No match found"))
                }
            } else {
                Result.failure(Exception("API Error: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
