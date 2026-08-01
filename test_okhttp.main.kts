import okhttp3.HttpUrl.Companion.toHttpUrl

val original = "https://pipedapi.kavin.rocks/search?q=test"
val url = original.toHttpUrl()
val hosts = listOf("pipedapi.adminforge.de")

for (host in hosts) {
    val newUrl = url.newBuilder().host(host).build()
    println(newUrl.toString())
    println("Host: ${newUrl.host}")
}
