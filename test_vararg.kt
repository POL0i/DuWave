fun main() {
    val result = testVararg("Hello %1$s", "World")
    println(result)
}

fun testVararg(key: String, vararg args: Any): String {
    return String.format(key, *args)
}
