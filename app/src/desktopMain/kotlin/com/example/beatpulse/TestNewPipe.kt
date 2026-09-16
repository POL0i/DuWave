package com.example.beatpulse

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.net.HttpURLConnection
import java.net.URL
import java.io.InputStream
import java.io.ByteArrayOutputStream

class SimpleDownloader : Downloader() {
    override fun execute(request: Request): Response {
        val url = URL(request.url())
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = request.httpMethod()
        request.headers().forEach { (k, v) ->
            v.forEach { conn.addRequestProperty(k, it) }
        }
        val stream: InputStream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
        val out = ByteArrayOutputStream()
        stream.copyTo(out)
        val responseHeaders = mutableMapOf<String, List<String>>()
        conn.headerFields.forEach { (k, v) -> if (k != null) responseHeaders[k] = v }
        return Response(conn.responseCode, conn.responseMessage, responseHeaders, out.toString("UTF-8"), request.url())
    }
}

fun main() {
    NewPipe.init(SimpleDownloader())

    // === TEST 1: NewPipe (esperamos que falle) ===
    println("=== TEST 1: NewPipe Extractor ===")
    var newPipeWorks = false
    try {
        val searchExtractor = ServiceList.YouTube.getSearchExtractor("lofi hip hop", emptyList(), null)
        searchExtractor.fetchPage()
        val items = searchExtractor.initialPage.items
        println("NewPipe: Found ${items.size} items")
        newPipeWorks = items.isNotEmpty()
    } catch (e: Exception) {
        println("NewPipe: FAILED - ${e.message}")
    }
    println("NewPipe status: ${if (newPipeWorks) "WORKING" else "DOWN"}\n")

    // === TEST 2: Invidious Fallback ===
    println("=== TEST 2: Invidious API (Fallback) ===")
    val instances = listOf(
        "https://yewtu.be",
        "https://invidious.protokolla.fi",
        "https://invidious.flokinet.to",
        "https://invidious.projectsegfau.lt",
        "https://inv.in.projectsegfau.lt"
    )

    // 2a: Find a working instance
    var workingInstance: String? = null
    for (inst in instances) {
        try {
            val conn = URL("$inst/api/v1/stats").openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            if (conn.responseCode in 200..299) {
                workingInstance = inst
                println("Instance UP: $inst")
                break
            }
        } catch (e: Exception) {
            println("Instance DOWN: $inst")
        }
    }

    if (workingInstance == null) {
        println("ALL INVIDIOUS INSTANCES DOWN - Modo Seguro se activaría")
        return
    }

    // 2b: Search test
    println("\nSearching 'lofi hip hop' via $workingInstance ...")
    try {
        val searchUrl = URL("$workingInstance/api/v1/search?q=lofi+hip+hop&type=video")
        val conn = searchUrl.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        val responseText = conn.inputStream.bufferedReader().use { it.readText() }

        // Parse videoIds with regex (same approach as OnlineMusicRepository)
        val videoIds = Regex("\"videoId\":\"([^\"]+)\"").findAll(responseText).map { it.groupValues[1] }.toList()
        val titles = Regex("\"title\":\"([^\"]+)\"").findAll(responseText).map { it.groupValues[1] }.toList()
        println("Found ${videoIds.size} videos")
        for (i in 0 until minOf(3, videoIds.size)) {
            println("  ${i+1}. ${titles.getOrElse(i) { "?" }} [${videoIds[i]}]")
        }

        // 2c: Get stream URL for first result
        if (videoIds.isNotEmpty()) {
            val testId = videoIds[0]
            println("\nGetting stream URL for videoId=$testId ...")
            val videoUrl = URL("$workingInstance/api/v1/videos/$testId")
            val videoConn = videoUrl.openConnection() as HttpURLConnection
            videoConn.connectTimeout = 5000
            videoConn.readTimeout = 5000
            val videoResponse = videoConn.inputStream.bufferedReader().use { it.readText() }

            // Find best audio stream
            val audioUrls = Regex("\"type\":\"audio/[^\"]+\"[^}]*\"url\":\"([^\"]+)\"").findAll(videoResponse)
                .map { it.groupValues[1].replace("\\u0026", "&") }
                .toList()
            
            if (audioUrls.isNotEmpty()) {
                println("Audio streams found: ${audioUrls.size}")
                println("Best audio URL (first 120 chars): ${audioUrls[0].take(120)}...")
                println("\n=== INVIDIOUS FALLBACK: SUCCESS ===")
            } else {
                println("No audio streams found in response")
                println("\n=== INVIDIOUS FALLBACK: PARTIAL (search works, streams not parsed) ===")
            }
        }
    } catch (e: Exception) {
        println("Invidious search FAILED: ${e.message}")
        e.printStackTrace()
    }
}
