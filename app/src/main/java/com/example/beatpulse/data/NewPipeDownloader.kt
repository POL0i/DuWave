package com.example.beatpulse.data

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class NewPipeDownloader private constructor(builder: OkHttpClient.Builder) : Downloader() {

    private val client: OkHttpClient = builder
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        @Volatile
        private var instance: NewPipeDownloader? = null

        fun getInstance(builder: OkHttpClient.Builder): NewPipeDownloader {
            return instance ?: synchronized(this) {
                instance ?: NewPipeDownloader(builder).also { instance = it }
            }
        }
    }

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        var requestBody: okhttp3.RequestBody? = null
        if (dataToSend != null) {
            requestBody = dataToSend.toRequestBody()
        }

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, requestBody)
            .url(url)
            
        for ((name, values) in headers) {
            for (value in values) {
                requestBuilder.addHeader(name, value)
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val body = response.body?.string() ?: ""

        // Map OkHttp Response to NewPipeExtractor Response
        val responseHeaders = mutableMapOf<String, List<String>>()
        for ((name, value) in response.headers) {
            val list = responseHeaders.getOrPut(name) { mutableListOf() } as MutableList<String>
            list.add(value)
        }

        val latestUrl = response.request.url.toString()

        return Response(response.code, response.message, responseHeaders, body, latestUrl)
    }
}
