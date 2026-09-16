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
        val url = URL(request.url)
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
        return Response(conn.responseCode, conn.responseMessage, responseHeaders, out.toString("UTF-8"), request.url)
    }
}

NewPipe.init(SimpleDownloader())
try {
    val searchExtractor = ServiceList.YouTube.getSearchExtractor("Never gonna give you up", emptyList(), null)
    searchExtractor.fetchPage()
    val items = searchExtractor.initialPage.items
    println("Found ${items.size} items")
    val item = items.firstOrNull { it is org.schabi.newpipe.extractor.stream.StreamInfoItem } as? org.schabi.newpipe.extractor.stream.StreamInfoItem
    if (item != null) {
        println("Title: ${item.name}")
        println("Duration: ${item.duration}")
        println("URL: ${item.url}")
        val videoId = item.url.substringAfter("v=").substringBefore("&")
        val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, "https://youtube.com/watch?v=$videoId")
        println("Audio streams: ${streamInfo.audioStreams.size}")
        streamInfo.audioStreams.forEach {
            println("Stream: bitrate=${it.bitrate} url=${it.content}")
        }
    } else {
        println("No stream items found")
    }
} catch (e: Exception) {
    e.printStackTrace()
}
