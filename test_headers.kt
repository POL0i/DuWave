import okhttp3.Headers

fun test(headers: Headers) {
    for ((name, value) in headers) {
        println("$name: $value")
    }
}
